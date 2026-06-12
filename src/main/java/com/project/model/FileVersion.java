package com.project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_versions")
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    public FileVersion() {}

    public FileVersion(FileMetadata file, String objectKey, String contentType, Long size, LocalDateTime uploadedAt, Integer versionNumber) {
        this.file = file;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.size = size;
        this.uploadedAt = uploadedAt;
        this.versionNumber = versionNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FileMetadata getFile() { return file; }
    public void setFile(FileMetadata file) { this.file = file; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
}
