package com.guyacevedo.factusimple.features.category.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponseDto(
    UUID id,
    String name,
    String description,
    Integer sortOrder,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
