package com.project.service;

import com.project.dto.DownloadResponse;
import com.project.dto.FileResponse;
import com.project.dto.FileVersionResponse;
import com.project.model.FileMetadata;
import com.project.model.FileVersion;
import com.project.model.Folder;
import com.project.model.User;
import com.project.repository.FileRepository;
import com.project.repository.FileVersionRepository;
import com.project.repository.FolderRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final long MAX_QUOTA_SIZE = 1L * 1024 * 1024 * 1024; // 1GB

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final FileVersionRepository fileVersionRepository;
    private final S3Service s3Service;
    private final CacheManager cacheManager;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    public FileService(FileRepository fileRepository, FolderRepository folderRepository, FileVersionRepository fileVersionRepository, S3Service s3Service, CacheManager cacheManager, org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.fileVersionRepository = fileVersionRepository;
        this.s3Service = s3Service;
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public FileResponse uploadFile(MultipartFile file, User user, Long folderId) {
        validateFile(file, user);

        String originalFilename = file.getOriginalFilename();

        Folder folder = null;
        if (folderId != null) {
            folder = folderRepository.findByIdAndOwner(folderId, user)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
        }

        // Check if file already exists in this folder for this user
        FileMetadata existingMetadata = folder == null
            ? fileRepository.findByOriginalFilenameAndFolderIsNullAndUploadedBy(originalFilename, user).orElse(null)
            : fileRepository.findByOriginalFilenameAndFolderAndUploadedBy(originalFilename, folder, user).orElse(null);

        String objectKey = generateObjectKey(originalFilename);
        s3Service.uploadFile(file, objectKey);

        FileMetadata metadataToReturn;

        if (existingMetadata != null) {
            // Save current state as a version
            FileVersion version = new FileVersion(
                existingMetadata, 
                existingMetadata.getObjectKey(), 
                existingMetadata.getContentType(), 
                existingMetadata.getSize(), 
                existingMetadata.getUploadedAt(),
                existingMetadata.getCurrentVersion()
            );
            fileVersionRepository.save(version);

            // Update metadata with new file
            existingMetadata.setObjectKey(objectKey);
            existingMetadata.setContentType(file.getContentType());
            existingMetadata.setSize(file.getSize());
            existingMetadata.setUploadedAt(LocalDateTime.now());
            existingMetadata.setCurrentVersion(existingMetadata.getCurrentVersion() + 1);

            metadataToReturn = fileRepository.save(existingMetadata);

            // Prune old versions (keep max 4 versions in history, + 1 current = 5)
            List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByUploadedAtDesc(existingMetadata);
            if (versions.size() > 4) {
                List<FileVersion> toDelete = versions.subList(4, versions.size());
                for (FileVersion v : toDelete) {
                    final String keyToDelete = v.getObjectKey();
                    // Delete from S3 only after DB transaction commits
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                s3Service.deleteFile(keyToDelete);
                            } catch(Exception e) {}
                        }
                    });
                    fileVersionRepository.delete(v);
                }
            }
        } else {
            FileMetadata newMetadata = new FileMetadata(
                    originalFilename,
                    objectKey,
                    file.getContentType(),
                    file.getSize(),
                    user,
                    folder
            );
            metadataToReturn = fileRepository.save(newMetadata);
        }

        if (cacheManager.getCache("files") != null) {
            cacheManager.getCache("files").evict(user.getId() + "-" + (folderId != null ? folderId : "root"));
        }

        // Push to thumbnail generation queue if it's an allowed image type
        if (file.getContentType() != null && 
            (file.getContentType().equals("image/jpeg") || 
             file.getContentType().equals("image/png") || 
             file.getContentType().equals("image/webp"))) {
            try {
                java.util.Map<String, Object> job = new java.util.HashMap<>();
                job.put("fileId", metadataToReturn.getId());
                job.put("attempt", 1);
                String jobJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(job);
                redisTemplate.opsForList().leftPush("thumbnail_queue", jobJson);
            } catch(Exception e) {}
        }

        return toFileResponse(metadataToReturn);
    }

    @Cacheable(value = "files", key = "#user.id + '-' + (#folderId != null ? #folderId : 'root')")
    public List<FileResponse> getUserFiles(User user, Long folderId) {
        List<FileMetadata> files;
        if (folderId == null) {
            files = fileRepository.findAllByUploadedByAndFolderIsNullOrderByUploadedAtDesc(user);
        } else {
            Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            files = fileRepository.findAllByUploadedByAndFolderOrderByUploadedAtDesc(user, folder);
        }
        return files.stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }

    public List<FileResponse> searchFiles(User user, String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return fileRepository.findAllByUploadedByAndOriginalFilenameContainingIgnoreCaseOrderByUploadedAtDesc(user, query.trim())
                .stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }

    public DownloadResponse getDownloadUrl(Long fileId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        String url = s3Service.getPresignedUrl(metadata.getObjectKey(), metadata.getOriginalFilename(), 5, true);
        return new DownloadResponse(url, 300L);
    }

    public String getShareUrl(Long fileId, User user, boolean isDownload) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        // Generate a URL valid for 1 hour (60 minutes)
        return s3Service.getPresignedUrl(metadata.getObjectKey(), metadata.getOriginalFilename(), 60, isDownload);
    }

    @Transactional
    public void deleteFile(Long fileId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        // Soft delete — set deletedAt instead of removing from S3
        metadata.setDeletedAt(LocalDateTime.now());
        fileRepository.save(metadata);

        Long fId = metadata.getFolder() != null ? metadata.getFolder().getId() : null;
        if (cacheManager.getCache("files") != null) {
            cacheManager.getCache("files").evict(user.getId() + "-" + (fId != null ? fId : "root"));
        }
    }

    public List<FileVersionResponse> getFileVersions(Long fileId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));
        return fileVersionRepository.findAllByFileOrderByUploadedAtDesc(metadata).stream()
                .map(v -> new FileVersionResponse(v.getId(), v.getFile().getId(), v.getSize(), v.getContentType(), v.getUploadedAt(), v.getVersionNumber()))
                .collect(Collectors.toList());
    }

    @Transactional
    public FileResponse restoreVersion(Long fileId, Long versionId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));
                
        FileVersion versionToRestore = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
                
        if (!versionToRestore.getFile().getId().equals(metadata.getId())) {
            throw new RuntimeException("Version does not belong to this file");
        }
        
        // Swap current state into a new version record
        FileVersion newVersion = new FileVersion(
            metadata, 
            metadata.getObjectKey(), 
            metadata.getContentType(), 
            metadata.getSize(), 
            metadata.getUploadedAt(),
            metadata.getCurrentVersion()
        );
        fileVersionRepository.save(newVersion);
        
        // Restore metadata
        metadata.setObjectKey(versionToRestore.getObjectKey());
        metadata.setContentType(versionToRestore.getContentType());
        metadata.setSize(versionToRestore.getSize());
        metadata.setUploadedAt(LocalDateTime.now());
        metadata.setCurrentVersion(versionToRestore.getVersionNumber());
        fileRepository.save(metadata);
        
        // Delete the restored version record
        fileVersionRepository.delete(versionToRestore);
        
        Long fId = metadata.getFolder() != null ? metadata.getFolder().getId() : null;
        if (cacheManager.getCache("files") != null) {
            cacheManager.getCache("files").evict(user.getId() + "-" + (fId != null ? fId : "root"));
        }
        
        return toFileResponse(metadata);
    }
    
    public DownloadResponse getVersionDownloadUrl(Long fileId, Long versionId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));
        FileVersion version = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        if (!version.getFile().getId().equals(metadata.getId())) {
            throw new RuntimeException("Version does not belong to this file");
        }

        String url = s3Service.getPresignedUrl(version.getObjectKey(), metadata.getOriginalFilename(), 5, true);
        return new DownloadResponse(url, 300L);
    }

    private void validateFile(MultipartFile file, User user) {
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty file");
        }

        Long currentUsage = fileRepository.getTotalSizeByUser(user);
        if (currentUsage + file.getSize() > MAX_QUOTA_SIZE) {
            throw new RuntimeException("Storage quota exceeded. You can only store up to 1GB of data.");
        }
    }

    private String generateObjectKey(String originalFilename) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return uuid + "_" + originalFilename;
    }

    private FileResponse toFileResponse(FileMetadata metadata) {
        String thumbnailUrl = null;
        if (metadata.getThumbnailKey() != null) {
            try {
                // Generate a public-read presigned URL for the thumbnail, valid for 1 hour
                thumbnailUrl = s3Service.getPresignedUrl(metadata.getThumbnailKey(), metadata.getOriginalFilename() + "_thumb.jpg", 60, false);
            } catch (Exception e) {}
        }
        return new FileResponse(
                metadata.getId(),
                metadata.getOriginalFilename(),
                metadata.getSize(),
                metadata.getContentType(),
                metadata.getUploadedAt(),
                metadata.getFolder() != null ? metadata.getFolder().getId() : null,
                metadata.getDeletedAt(),
                thumbnailUrl
        );
    }
}
