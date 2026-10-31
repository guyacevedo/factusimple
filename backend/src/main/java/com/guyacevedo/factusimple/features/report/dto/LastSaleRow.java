package com.guyacevedo.factusimple.features.report.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** When a product was last sold in an invoice that was not cancelled. */
public record LastSaleRow(UUID productId, LocalDateTime lastSale) {}
