package com.project.service;

import com.project.dto.FolderResponse;
import com.project.model.Folder;
import com.project.model.User;
import com.project.repository.FolderRepository;
import com.project.repository.FileRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final CacheManager cacheManager;

    public FolderService(FolderRepository folderRepository, FileRepository fileRepository, S3Service s3Service, CacheManager cacheManager) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.s3Service = s3Service;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public FolderResponse createFolder(String name, Long parentId, User owner) {
        Folder parent = null;
        if (parentId != null) {
            parent = folderRepository.findByIdAndOwner(parentId, owner)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
        }
        Folder folder = new Folder(name, parent, owner);
        folderRepository.save(folder); // Generates ID

        // Compute materialized path now that ID exists
        folder.computePath();
        folderRepository.save(folder); // Persist the path

        evictFolderCaches(owner, parentId);
        return toFolderResponse(folder);
    }

    @Cacheable(value = "folders", key = "#owner.id + '-' + (#parentId != null ? #parentId : 'root')")
    public List<FolderResponse> getFolders(Long parentId, User owner) {
        List<Folder> folders;
        if (parentId == null) {
            folders = folderRepository.findAllByOwnerAndParentIsNullOrderByCreatedAtDesc(owner);
        } else {
            Folder parent = folderRepository.findByIdAndOwner(parentId, owner)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            folders = folderRepository.findAllByOwnerAndParentOrderByCreatedAtDesc(owner, parent);
        }
        return folders.stream().map(this::toFolderResponse).collect(Collectors.toList());
    }

    /**
     * Breadcrumbs from materialized path — no recursive parent walk needed.
     * Path like "/1/5/12/" is split to [1, 5, 12], then fetched in order.
     */
    @Cacheable(value = "breadcrumbs", key = "#owner.id + '-' + #folderId")
    public List<FolderResponse> getBreadcrumbs(Long folderId, User owner) {
        if (folderId == null) return List.of();

        Folder folder = folderRepository.findByIdAndOwner(folderId, owner).orElse(null);
        if (folder == null || folder.getPath() == null) return List.of();

        // Parse path: "/1/5/12/" → ["1", "5", "12"]
        List<Long> ancestorIds = Arrays.stream(folder.getPath().split("/"))
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // Fetch all ancestors in one query and preserve order
        List<Folder> ancestors = folderRepository.findAllById(ancestorIds);
        // Sort by path length (root first)
        ancestors.sort((a, b) -> a.getPath().length() - b.getPath().length());

        return ancestors.stream().map(this::toFolderResponse).collect(Collectors.toList());
    }

    /**
     * Soft-delete folder and entire subtree using materialized path.
     * Single UPDATE on folders WHERE path LIKE '/id/%', plus cascading files.
     */
    @Transactional
    public void deleteFolder(Long folderId, User owner) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, owner)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        String pathPrefix = folder.getPath() + "%";

        // 1. Find all folder IDs in the subtree (for cascading to files)
        List<Folder> subtreeFolders = folderRepository.findAllByPathStartingWith(folder.getPath());
        List<Long> subtreeFolderIds = subtreeFolders.stream().map(Folder::getId).collect(Collectors.toList());

        // 2. Soft-delete the folder itself
        folder.setDeletedAt(LocalDateTime.now());
        folderRepository.save(folder);

        // 3. Soft-delete all descendant folders via path
        folderRepository.softDeleteSubtree(pathPrefix);

        // 4. Soft-delete all files in the subtree
        if (!subtreeFolderIds.isEmpty()) {
            fileRepository.softDeleteByFolderIds(subtreeFolderIds);
        }

        evictAllCaches();
    }

    @Transactional
    public FolderResponse moveFolder(Long folderId, Long newParentId, User owner) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, owner)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        String oldParentPath = folder.getParent() != null ? folder.getParent().getPath() : "/";

        Folder newParent = null;
        if (newParentId != null) {
            newParent = folderRepository.findByIdAndOwner(newParentId, owner)
                    .orElseThrow(() -> new RuntimeException("New parent folder not found"));
            if (newParentId.equals(folderId) || newParent.getPath().startsWith(folder.getPath())) {
                throw new RuntimeException("Cannot move folder into itself or its descendant");
            }
        }

        String newParentPath = newParent != null ? newParent.getPath() : "/";

        if (oldParentPath.equals(newParentPath)) {
            return toFolderResponse(folder); // Nothing to do
        }

        String folderPathPrefix = folder.getPath() + "%";
        
        folder.setParent(newParent);
        folderRepository.save(folder);

        // Update paths for the folder and all its descendants using the DB
        folderRepository.moveSubtree(newParentPath, oldParentPath, folderPathPrefix);

        evictAllCaches();
        return toFolderResponse(folder);
    }

    private void evictFolderCaches(User owner, Long parentId) {
        if (cacheManager.getCache("folders") != null) {
            cacheManager.getCache("folders").evict(owner.getId() + "-" + (parentId != null ? parentId : "root"));
        }
    }

    private void evictAllCaches() {
        if (cacheManager.getCache("folders") != null) cacheManager.getCache("folders").clear();
        if (cacheManager.getCache("files") != null) cacheManager.getCache("files").clear();
        if (cacheManager.getCache("breadcrumbs") != null) cacheManager.getCache("breadcrumbs").clear();
    }

    private FolderResponse toFolderResponse(Folder folder) {
        return new FolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getParent() != null ? folder.getParent().getId() : null,
                folder.getCreatedAt(),
                folder.getDeletedAt()
        );
    }
}
