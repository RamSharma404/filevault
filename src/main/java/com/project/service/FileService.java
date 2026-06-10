package com.project.service;

import com.project.dto.DownloadResponse;
import com.project.dto.FileResponse;
import com.project.model.FileMetadata;
import com.project.model.User;
import com.project.repository.FileRepository;
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
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final FileRepository fileRepository;
    private final S3Service s3Service;

    public FileService(FileRepository fileRepository, S3Service s3Service) {
        this.fileRepository = fileRepository;
        this.s3Service = s3Service;
    }

    public FileResponse uploadFile(MultipartFile file, User user) {
        validateFile(file, user);

        String originalFilename = file.getOriginalFilename();
        String objectKey = generateObjectKey(originalFilename);

        s3Service.uploadFile(file, objectKey);

        FileMetadata metadata = new FileMetadata(
                originalFilename,
                objectKey,
                file.getContentType(),
                file.getSize(),
                user
        );

        fileRepository.save(metadata);

        return toFileResponse(metadata);
    }

    public List<FileResponse> getUserFiles(User user) {
        return fileRepository.findAllByUploadedByOrderByUploadedAtDesc(user)
                .stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }

    public DownloadResponse getDownloadUrl(Long fileId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        String url = s3Service.getPresignedUrl(metadata.getObjectKey(), 5);
        return new DownloadResponse(url, 300L);
    }

    @Transactional
    public void deleteFile(Long fileId, User user) {
        FileMetadata metadata = fileRepository.findByIdAndUploadedBy(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        s3Service.deleteFile(metadata.getObjectKey());
        fileRepository.delete(metadata);
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
                metadata.getUploadedAt()
        );
    }
}
