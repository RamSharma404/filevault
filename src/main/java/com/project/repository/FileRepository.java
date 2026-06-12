package com.project.repository;

import com.project.model.FileMetadata;
import com.project.model.Folder;
import com.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findAllByUploadedByOrderByUploadedAtDesc(User uploadedBy);
    Optional<FileMetadata> findByIdAndUploadedBy(Long id, User uploadedBy);
    List<FileMetadata> findAllByUploadedByAndFolderIsNullOrderByUploadedAtDesc(User uploadedBy);
    List<FileMetadata> findAllByUploadedByAndFolderOrderByUploadedAtDesc(User uploadedBy, Folder folder);
    List<FileMetadata> findAllByUploadedByAndOriginalFilenameContainingIgnoreCaseOrderByUploadedAtDesc(User uploadedBy, String query);

    Optional<FileMetadata> findByOriginalFilenameAndFolderAndUploadedBy(String originalFilename, Folder folder, User uploadedBy);
    Optional<FileMetadata> findByOriginalFilenameAndFolderIsNullAndUploadedBy(String originalFilename, User uploadedBy);

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f WHERE f.uploadedBy = :user")
    Long getTotalSizeByUser(@Param("user") User user);

    // === Soft-delete cascade: soft-delete all files in a set of folder IDs ===

    @Modifying
    @Query("UPDATE FileMetadata f SET f.deletedAt = CURRENT_TIMESTAMP WHERE f.folder.id IN :folderIds AND f.deletedAt IS NULL")
    int softDeleteByFolderIds(@Param("folderIds") List<Long> folderIds);

    @Modifying
    @Query("UPDATE FileMetadata f SET f.deletedAt = NULL WHERE f.folder.id IN :folderIds AND f.deletedAt IS NOT NULL")
    int restoreByFolderIds(@Param("folderIds") List<Long> folderIds);

    // === Trash queries (bypass @SQLRestriction via native) ===

    @Query(value = "SELECT * FROM files WHERE uploaded_by = :userId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
    List<FileMetadata> findTrashedByUser(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM files WHERE id = :id AND uploaded_by = :userId AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<FileMetadata> findTrashedByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    // For cleanup job — find files trashed > 30 days ago
    @Query(value = "SELECT * FROM files WHERE deleted_at IS NOT NULL AND deleted_at < NOW() - INTERVAL '30 days' LIMIT :batchSize", nativeQuery = true)
    List<FileMetadata> findExpiredTrashed(@Param("batchSize") int batchSize);
}
