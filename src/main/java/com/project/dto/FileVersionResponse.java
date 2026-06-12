package com.project.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record FileVersionResponse(
    Long id,
    Long fileId,
    Long size,
    String contentType,
    LocalDateTime uploadedAt,
    Integer versionNumber
) implements Serializable {}
