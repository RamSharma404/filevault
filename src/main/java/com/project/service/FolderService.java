package com.project.service;

import com.project.dto.FolderResponse;
import com.project.model.Folder;
import com.project.model.User;
import com.project.repository.FolderRepository;
import com.project.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final S3Service s3Service;

    public FolderService(FolderRepository folderRepository, FileRepository fileRepository, S3Service s3Service) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.s3Service = s3Service;
    }

    public FolderResponse createFolder(String name, Long parentId, User owner) {
        Folder parent = null;
        if (parentId != null) {
            parent = folderRepository.findByIdAndOwner(parentId, owner)
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
        }
        Folder folder = new Folder(name, parent, owner);
        folderRepository.save(folder);
        return toFolderResponse(folder);
    }

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

    public List<FolderResponse> getBreadcrumbs(Long folderId, User owner) {
        List<FolderResponse> breadcrumbs = new ArrayList<>();
        if (folderId == null) return breadcrumbs;
        Folder current = folderRepository.findByIdAndOwner(folderId, owner).orElse(null);
        while (current != null) {
            breadcrumbs.add(toFolderResponse(current));
            current = current.getParent();
        }
        Collections.reverse(breadcrumbs);
        return breadcrumbs;
    }

    @Transactional
    public void deleteFolder(Long folderId, User owner) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, owner)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        // Delete all files in this folder
        fileRepository.findAllByUploadedByAndFolderOrderByUploadedAtDesc(owner, folder)
                .forEach(file -> {
                    s3Service.deleteFile(file.getObjectKey());
                    fileRepository.delete(file);
                });
        // Delete sub-folders recursively
        folderRepository.findAllByOwnerAndParentOrderByCreatedAtDesc(owner, folder)
                .forEach(sub -> deleteFolder(sub.getId(), owner));
        folderRepository.delete(folder);
    }

    private FolderResponse toFolderResponse(Folder folder) {
        return new FolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getParent() != null ? folder.getParent().getId() : null,
                folder.getCreatedAt()
        );
    }
}
