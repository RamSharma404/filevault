package com.project.service;

import com.project.model.FileMetadata;
import com.project.model.Folder;
import com.project.repository.FileRepository;
import com.project.repository.FolderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled job that permanently purges items from trash after 30 days.
 *
 * Key design decisions:
 * 1. S3-first deletion order — delete from S3 before DB to prevent orphaned objects
 * 2. Batched processing — process in chunks to avoid memory issues on large datasets
 * 3. Redis SETNX lock — prevent overlapping runs if container restarts
 */
@Service
public class TrashCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TrashCleanupService.class);
    private static final String LOCK_KEY = "trash_cleanup_lock";
    private static final int BATCH_SIZE = 100;

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;

    public TrashCleanupService(FileRepository fileRepository, FolderRepository folderRepository,
                               S3Service s3Service, StringRedisTemplate redisTemplate) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.s3Service = s3Service;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Runs every 6 hours. Acquires a Redis lock to prevent double-runs
     * from container restarts or multiple replicas.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // every 6 hours
    public void cleanupExpiredTrash() {
        // Acquire distributed lock via Redis SETNX with TTL
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "running", 30, TimeUnit.MINUTES);

        if (acquired == null || !acquired) {
            logger.info("Trash cleanup: another instance is already running, skipping");
            return;
        }

        try {
            logger.info("Trash cleanup: starting purge of items deleted > 30 days ago");
            int totalFiles = purgeExpiredFiles();
            int totalFolders = purgeExpiredFolders();
            logger.info("Trash cleanup: purged {} files and {} folders", totalFiles, totalFolders);
        } finally {
            // Release the lock
            redisTemplate.delete(LOCK_KEY);
        }
    }

    @Transactional
    private int purgeExpiredFiles() {
        int total = 0;
        List<FileMetadata> batch;

        do {
            batch = fileRepository.findExpiredTrashed(BATCH_SIZE);
            for (FileMetadata file : batch) {
                try {
                    // S3 first, then DB
                    s3Service.deleteFile(file.getObjectKey());
                    fileRepository.delete(file);
                    total++;
                } catch (Exception e) {
                    // If S3 delete fails, skip this file — it will be retried on the next run
                    logger.warn("Failed to purge file {} from S3, will retry: {}", file.getId(), e.getMessage());
                }
            }
        } while (batch.size() == BATCH_SIZE);

        return total;
    }

    @Transactional
    private int purgeExpiredFolders() {
        int total = 0;
        List<Folder> batch;

        do {
            batch = folderRepository.findExpiredTrashed(BATCH_SIZE);
            // Sort by path length descending (deepest first) to avoid FK violations
            batch.sort((a, b) -> b.getPath().length() - a.getPath().length());
            for (Folder folder : batch) {
                try {
                    folderRepository.delete(folder);
                    total++;
                } catch (Exception e) {
                    logger.warn("Failed to purge folder {}, will retry: {}", folder.getId(), e.getMessage());
                }
            }
        } while (batch.size() == BATCH_SIZE);

        return total;
    }
}
