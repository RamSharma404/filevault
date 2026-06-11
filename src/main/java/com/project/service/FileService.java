package com.project.service;

import com.project.dto.DownloadResponse;
import com.project.dto.FileResponse;
import com.project.model.FileMetadata;
import com.project.model.Folder;
import com.project.model.User;
import com.project.repository.FileRepository;
import com.project.repository.FolderRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final long MAX_QUOTA_SIZE = 1L * 1024 * 1024 * 1024; // 1GB
    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "video/mp4",
            "video/mpeg",
            "application/zip",
            "application/x-zip-compressed",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "video/quicktime",
            "video/x-msvideo",
            "video/webm",
            "image/svg+xml",
            "image/bmp",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            "application/json",
            "text/csv"
    );

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final S3Service s3Service;
    private final CacheManager cacheManager;

    public FileService(FileRepository fileRepository, FolderRepository folderRepository, S3Service s3Service, CacheManager cacheManager) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.s3Service = s3Service;
        this.cacheManager = cacheManager;
    }

    public FileResponse uploadFile(MultipartFile file, User user, Long folderId) {
        validateFile(file, user);

        String originalFilename = file.getOriginalFilename();
        String objectKey = generateObjectKey(originalFilename);

        s3Service.uploadFile(file, objectKey);

        Folder folder = null;
        if (folderId != null) {
            folder = folderRepository.findByIdAndOwner(folderId, user)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
        }

        FileMetadata metadata = new FileMetadata(
                originalFilename,
                objectKey,
                file.getContentType(),
                file.getSize(),
                user,
                folder
        );

        fileRepository.save(metadata);

        if (cacheManager.getCache("files") != null) {
            cacheManager.getCache("files").evict(user.getId() + "-" + (folderId != null ? folderId : "root"));
        }

        return toFileResponse(metadata);
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

    public DownloadResponse getDownloadUrl(Long fileId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        String url = s3Service.getPresignedUrl(metadata.getObjectKey(), metadata.getOriginalFilename(), 5);
        return new DownloadResponse(url, 300L);
    }

    public String getShareUrl(Long fileId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        // Generate a URL valid for 7 days (10080 minutes)
        return s3Service.getPresignedUrl(metadata.getObjectKey(), metadata.getOriginalFilename(), 10080);
    }

    @Transactional
    public void deleteFile(Long fileId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        s3Service.deleteFile(metadata.getObjectKey());
        fileRepository.delete(metadata);

        Long fId = metadata.getFolder() != null ? metadata.getFolder().getId() : null;
        if (cacheManager.getCache("files") != null) {
            cacheManager.getCache("files").evict(user.getId() + "-" + (fId != null ? fId : "root"));
        }
    }

    private void validateFile(MultipartFile file, User user) {
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty file");
        }
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("File type not allowed: " + file.getContentType());
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
        return new FileResponse(
                metadata.getId(),
                metadata.getOriginalFilename(),
                metadata.getSize(),
                metadata.getContentType(),
                metadata.getUploadedAt(),
                metadata.getFolder() != null ? metadata.getFolder().getId() : null
        );
    }
}
