package com.guyacevedo.factusimple.features.product.dto;

import com.guyacevedo.factusimple.features.product.entity.SaleType;
import com.guyacevedo.factusimple.shared.entity.UnitMeasurement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponseDto(
    UUID id,
    UUID categoryId,
    String categoryName,
    String sku,
    String barcode,
    String name,
    String description,
    BigDecimal retailPrice,
    BigDecimal wholesalePrice,
    BigDecimal costPrice,
    BigDecimal minStockWholesale,
    BigDecimal minStock,
    UnitMeasurement unitMeasure,
    String standardCode,
    SaleType saleType,
    String imageUrl,
    Boolean isActive,
    List<ProductTaxDto> taxes,
    List<ProductWithholdingTaxDto> withholdingTaxes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
