package com.guyacevedo.factusimple.features.pos.dto;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record HeldSaleResponseDto(
    UUID id,
    String label,
    JsonNode content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
