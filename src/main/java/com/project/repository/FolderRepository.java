package com.project.repository;

import com.project.model.Folder;
import com.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findAllByOwnerAndParentIsNullOrderByCreatedAtDesc(User owner);
    List<Folder> findAllByOwnerAndParentOrderByCreatedAtDesc(User owner, Folder parent);
    Optional<Folder> findByIdAndOwner(Long id, User owner);

    // === Subtree operations ===
    List<Folder> findAllByPathStartingWith(String pathPrefix);

    @Modifying
    @Query("UPDATE Folder f SET f.path = CONCAT(:newParentPath, SUBSTRING(f.path, LENGTH(:oldPath) + 1)) WHERE f.path LIKE :pathPrefix")
    int moveSubtree(@Param("newParentPath") String newParentPath, @Param("oldPath") String oldPath, @Param("pathPrefix") String pathPrefix);

    // === Soft-delete operations (path-based cascade) ===

    @Modifying
    @Query("UPDATE Folder f SET f.deletedAt = CURRENT_TIMESTAMP WHERE f.path LIKE :pathPrefix")
    int softDeleteSubtree(@Param("pathPrefix") String pathPrefix);

    @Modifying
    @Query("UPDATE Folder f SET f.deletedAt = NULL WHERE f.path LIKE :pathPrefix")
    int restoreSubtree(@Param("pathPrefix") String pathPrefix);

    // === Trash queries (bypass @SQLRestriction) ===

    @Query(value = "SELECT * FROM folders WHERE owner_id = :ownerId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
    List<Folder> findTrashedByOwner(@Param("ownerId") Long ownerId);

    @Query(value = "SELECT * FROM folders WHERE id = :id AND owner_id = :ownerId AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Folder> findTrashedByIdAndOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    // For cleanup job — find folders trashed > 30 days ago
    @Query(value = "SELECT * FROM folders WHERE deleted_at IS NOT NULL AND deleted_at < NOW() - INTERVAL '30 days' LIMIT :batchSize", nativeQuery = true)
    List<Folder> findExpiredTrashed(@Param("batchSize") int batchSize);
}
