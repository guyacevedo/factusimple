package com.guyacevedo.factusimple.features.inventory.dto;

import com.guyacevedo.factusimple.features.product.entity.SaleType;
import com.guyacevedo.factusimple.shared.entity.UnitMeasurement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockResponseDto(
    UUID productId,
    String sku,
    String productName,
    UnitMeasurement unitMeasure,
    SaleType saleType,
    BigDecimal quantity,
    BigDecimal minStock,
    Boolean belowMinimum,
    LocalDateTime updatedAt
) {}
