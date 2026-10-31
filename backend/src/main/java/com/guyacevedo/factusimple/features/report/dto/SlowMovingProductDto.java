package com.guyacevedo.factusimple.features.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A product in stock that has not sold for a while. lastSale and
 * daysWithoutSales are null when it never sold, which is the worst case and
 * comes first; making up a big number would mix it with products that did sell.
 */
public record SlowMovingProductDto(
    UUID productId,
    String sku,
    String name,
    BigDecimal quantity,
    LocalDateTime lastSale,
    Long daysWithoutSales,
    @Schema(description = "Lo que cuesta el stock parado: cantidad × costo actual") BigDecimal idleCapital
) {

    public boolean neverSold() {
        return lastSale == null;
    }
}
