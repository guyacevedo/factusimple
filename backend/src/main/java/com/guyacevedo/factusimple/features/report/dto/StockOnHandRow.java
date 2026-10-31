package com.guyacevedo.factusimple.features.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** An active product with stock, valued at its current cost. */
public record StockOnHandRow(UUID productId, String sku, String name, BigDecimal quantity, BigDecimal costPrice) {}
