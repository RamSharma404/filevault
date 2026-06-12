package com.project.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record FolderResponse(
    Long id,
    String name,
    Long parentId,
    LocalDateTime createdAt,
    LocalDateTime deletedAt
) implements Serializable {}
