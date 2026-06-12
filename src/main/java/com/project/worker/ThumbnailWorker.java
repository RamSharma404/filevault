package com.project.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.model.FileMetadata;
import com.project.repository.FileRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ThumbnailWorker {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailWorker.class);

    private final StringRedisTemplate redisTemplate;
    private final FileRepository fileRepository;
    private final S3Client s3Client;
    private final String bucket;
    private final ObjectMapper objectMapper;

    private ExecutorService executorService;
    private volatile boolean running = true;

    public ThumbnailWorker(StringRedisTemplate redisTemplate, FileRepository fileRepository, S3Client s3Client, @Value("${minio.bucket}") String bucket, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.fileRepository = fileRepository;
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void startWorker() {
        log.info("Starting ThumbnailWorker thread...");
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::pollQueue);
    }

    @PreDestroy
    public void stopWorker() {
        log.info("Shutting down ThumbnailWorker...");
        running = false;
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(6, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void pollQueue() {
        while (running) {
            String jobJson = null;
            try {
                // BRPOP blocks for up to 5 seconds
                jobJson = redisTemplate.boundListOps("thumbnail_queue").rightPop(Duration.ofSeconds(5));
                if (jobJson == null) {
                    continue; // Nothing in queue, loop again and check 'running'
                }

                Map<String, Object> job = objectMapper.readValue(jobJson, new TypeReference<>() {});
                Long fileId = Long.valueOf(job.get("fileId").toString());
                int attempt = (int) job.getOrDefault("attempt", 1);

                try {
                    processJob(fileId);
                } catch (Exception e) {
                    log.error("Failed to process thumbnail job for fileId {} on attempt {}", fileId, attempt, e);
                    if (attempt < 3) {
                        job.put("attempt", attempt + 1);
                        redisTemplate.opsForList().leftPush("thumbnail_queue", objectMapper.writeValueAsString(job));
                        log.info("Re-queued thumbnail job for fileId {}", fileId);
                    } else {
                        log.error("Job for fileId {} failed after 3 attempts, moving to DLQ", fileId);
                        redisTemplate.opsForList().leftPush("image_processing_dlq", jobJson);
                    }
                }
            } catch (Exception e) {
                log.error("Error in ThumbnailWorker polling loop. Raw job: {}", jobJson, e);
                // Catch-all to prevent thread death
            }
        }
        log.info("ThumbnailWorker thread exited gracefully.");
    }

    private void processJob(Long fileId) throws Exception {
        FileMetadata metadata = fileRepository.findById(fileId).orElse(null);
        
        if (metadata == null || metadata.getDeletedAt() != null) {
            log.info("File {} not found or deleted, skipping thumbnail generation", fileId);
            return;
        }

        log.info("Generating thumbnail for file {}", fileId);
        
        // Download the original image from S3
        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(metadata.getObjectKey())
                .build());

        // Generate thumbnail using Thumbnailator
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(s3Object)
                .size(200, 200)
                .outputFormat("jpg")
                .toOutputStream(baos);
                
        byte[] thumbBytes = baos.toByteArray();

        // Upload thumbnail to S3
        String thumbKey = "thumb_" + metadata.getObjectKey() + ".jpg";
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(thumbKey)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromBytes(thumbBytes));

        // Update metadata
        metadata.setThumbnailKey(thumbKey);
        fileRepository.save(metadata);

        log.info("Successfully generated thumbnail for file {}", fileId);
    }
}
