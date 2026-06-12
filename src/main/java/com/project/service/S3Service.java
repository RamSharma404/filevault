package com.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;
import java.time.Duration;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner,
                     @Value("${minio.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }


    public void uploadFile(MultipartFile file, String objectKey) {
        long fileSize = file.getSize();
        long partSize = 10 * 1024 * 1024; // 10MB chunks for lower memory footprint

        if (fileSize <= partSize) {
            // Standard single put for small files
            try (InputStream is = file.getInputStream()) {
                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(objectKey)
                                .contentType(file.getContentType())
                                .build(),
                        RequestBody.fromInputStream(is, file.getSize()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload file", e);
            }
            return;
        }

        // S3 Multipart Upload for large files
        String uploadId = null;
        try {
            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(b -> b.bucket(bucket).key(objectKey).contentType(file.getContentType()));
            uploadId = createResponse.uploadId();

            List<CompletedPart> completedParts = new ArrayList<>();
            try (InputStream is = file.getInputStream()) {
                int partNumber = 1;
                byte[] buffer = new byte[(int) partSize];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) > 0) {
                    final int currentPartNumber = partNumber;
                    UploadPartResponse partResponse = s3Client.uploadPart(b -> b.bucket(bucket).key(objectKey).uploadId(createResponse.uploadId()).partNumber(currentPartNumber),
                            RequestBody.fromBytes(java.util.Arrays.copyOf(buffer, bytesRead)));
                    
                    completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(partResponse.eTag()).build());
                    partNumber++;
                }
            }

            s3Client.completeMultipartUpload(b -> b.bucket(bucket).key(objectKey).uploadId(createResponse.uploadId())
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build()));

        } catch (Exception e) {
            if (uploadId != null) {
                final String currentUploadId = uploadId;
                try {
                    s3Client.abortMultipartUpload(b -> b.bucket(bucket).key(objectKey).uploadId(currentUploadId));
                } catch (Exception ignored) {}
            }
            throw new RuntimeException("Failed to upload file in parts", e);
        }
    }

    public void deleteFile(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public String getPresignedUrl(String objectKey, String filename, int expiryMinutes) {
        try {
            return s3Presigner.presignGetObject(
                    r -> r.signatureDuration(Duration.ofMinutes(expiryMinutes))
                            .getObjectRequest(ro -> ro.bucket(bucket).key(objectKey)
                                    .responseContentDisposition("attachment; filename=\"" + filename + "\""))
            ).url().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}
