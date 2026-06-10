package com.project.repository;

import com.project.model.FileMetadata;
import com.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findAllByUploadedByOrderByUploadedAtDesc(User uploadedBy);
    Optional<FileMetadata> findByIdAndUploadedBy(Long id, User uploadedBy);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f WHERE f.uploadedBy = :user")
    Long getTotalSizeByUser(@org.springframework.data.repository.query.Param("user") User user);
}
