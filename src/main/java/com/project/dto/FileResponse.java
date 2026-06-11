package com.project.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record FileResponse(
    Long id,
    String originalFilename,
    Long size,
    String contentType,
    LocalDateTime uploadedAt,
    Long folderId
) implements Serializable {}
