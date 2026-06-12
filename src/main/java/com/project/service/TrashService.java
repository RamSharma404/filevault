package com.project.service;

import com.project.dto.FileResponse;
import com.project.dto.FolderResponse;
import com.project.model.FileMetadata;
import com.project.model.Folder;
import com.project.model.User;
import com.project.repository.FileRepository;
import com.project.repository.FolderRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrashService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final S3Service s3Service;
    private final CacheManager cacheManager;

    public TrashService(FileRepository fileRepository, FolderRepository folderRepository,
                        S3Service s3Service, CacheManager cacheManager) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.s3Service = s3Service;
        this.cacheManager = cacheManager;
    }

    public List<FileResponse> getTrashedFiles(User user) {
        return fileRepository.findTrashedByUser(user.getId()).stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }

    public List<FolderResponse> getTrashedFolders(User user) {
        return folderRepository.findTrashedByOwner(user.getId()).stream()
                .map(this::toFolderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Restore a single file from trash.
     */
    @Transactional
    public void restoreFile(Long fileId, User user) {
        FileMetadata file = fileRepository.findTrashedByIdAndUser(fileId, user.getId())
                .orElseThrow(() -> new RuntimeException("Trashed file not found"));
        file.setDeletedAt(null);
        fileRepository.save(file);
        evictAllCaches();
    }

    /**
     * Restore a folder and its entire subtree.
     * All descendant folders (matched by path prefix) and their files are restored.
     */
    @Transactional
    public void restoreFolder(Long folderId, User user) {
        Folder folder = folderRepository.findTrashedByIdAndOwner(folderId, user.getId())
                .orElseThrow(() -> new RuntimeException("Trashed folder not found"));

        // Restore the folder itself
        folder.setDeletedAt(null);
        folderRepository.save(folder);

        // Restore all descendant folders via path
        String pathPrefix = folder.getPath() + "%";
        folderRepository.restoreSubtree(pathPrefix);

        // Restore all files in the subtree
        List<Folder> subtree = folderRepository.findAllByPathStartingWith(folder.getPath());
        List<Long> subtreeIds = subtree.stream().map(Folder::getId).collect(Collectors.toList());
        subtreeIds.add(folderId);
        fileRepository.restoreByFolderIds(subtreeIds);

        evictAllCaches();
    }

    /**
     * Permanently delete a single file — S3 first, then DB row.
     */
    @Transactional
    public void permanentlyDeleteFile(Long fileId, User user) {
        FileMetadata file = fileRepository.findTrashedByIdAndUser(fileId, user.getId())
                .orElseThrow(() -> new RuntimeException("Trashed file not found"));

        // S3 first, then DB
        s3Service.deleteFile(file.getObjectKey());
        fileRepository.delete(file);
        evictAllCaches();
    }

    /**
     * Permanently delete a folder and its entire subtree — S3 first for each file.
     */
    @Transactional
    public void permanentlyDeleteFolder(Long folderId, User user) {
        Folder folder = folderRepository.findTrashedByIdAndOwner(folderId, user.getId())
                .orElseThrow(() -> new RuntimeException("Trashed folder not found"));

        // Find all files in the subtree and delete from S3 first
        List<Folder> subtree = folderRepository.findAllByPathStartingWith(folder.getPath());
        List<Long> subtreeIds = subtree.stream().map(Folder::getId).collect(Collectors.toList());
        subtreeIds.add(folderId);

        // Delete files from S3 first, then DB
        for (Long fId : subtreeIds) {
            fileRepository.findAllByUploadedByAndFolderOrderByUploadedAtDesc(user, folderRepository.findById(fId).orElse(null));
        }
        // Use native query to find trashed files in these folders
        List<FileMetadata> trashedFiles = fileRepository.findTrashedByUser(user.getId()).stream()
                .filter(f -> f.getFolder() != null && subtreeIds.contains(f.getFolder().getId()))
                .collect(Collectors.toList());

        for (FileMetadata file : trashedFiles) {
            try {
                s3Service.deleteFile(file.getObjectKey());
            } catch (Exception e) {
                // Log but continue — orphaned S3 objects will be cleaned by next cleanup run
            }
            fileRepository.delete(file);
        }

        // Delete folders (children first based on path length)
        subtree.sort((a, b) -> b.getPath().length() - a.getPath().length());
        for (Folder f : subtree) {
            folderRepository.delete(f);
        }
        folderRepository.delete(folder);

        evictAllCaches();
    }

    private void evictAllCaches() {
        if (cacheManager.getCache("folders") != null) cacheManager.getCache("folders").clear();
        if (cacheManager.getCache("files") != null) cacheManager.getCache("files").clear();
        if (cacheManager.getCache("breadcrumbs") != null) cacheManager.getCache("breadcrumbs").clear();
    }

    private FileResponse toFileResponse(FileMetadata metadata) {
        return new FileResponse(
                metadata.getId(),
                metadata.getOriginalFilename(),
                metadata.getSize(),
                metadata.getContentType(),
                metadata.getUploadedAt(),
                metadata.getFolder() != null ? metadata.getFolder().getId() : null,
                metadata.getDeletedAt(),
                null // thumbnailUrl not needed in trash
        );
    }

    private FolderResponse toFolderResponse(Folder f) {
        return new FolderResponse(f.getId(), f.getName(),
                f.getParent() != null ? f.getParent().getId() : null,
                f.getCreatedAt(), f.getDeletedAt());
    }
}
