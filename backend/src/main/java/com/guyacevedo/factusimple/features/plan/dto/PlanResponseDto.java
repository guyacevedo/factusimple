package com.guyacevedo.factusimple.features.plan.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanResponseDto(
    UUID id,
    String name,
    Integer maxProducts,
    Integer maxCustomers,
    Integer maxInvoices,
    Boolean isActive,
    String description,
    Boolean requiredInvitedCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
