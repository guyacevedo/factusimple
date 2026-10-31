package com.guyacevedo.factusimple.features.inventory.dto;

import com.guyacevedo.factusimple.features.inventory.entity.AdjustmentReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockAdjustmentResponseDto(
    UUID id,
    UUID productId,
    String sku,
    String productName,
    BigDecimal previousQuantity,
    BigDecimal newQuantity,
    BigDecimal difference,
    AdjustmentReason reason,
    String notes,
    UUID createdBy,
    LocalDateTime createdAt
) {}
