package com.project.repository;

import com.project.model.FileMetadata;
import com.project.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findAllByFileOrderByUploadedAtDesc(FileMetadata file);
}
