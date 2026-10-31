package com.guyacevedo.factusimple.features.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * productId is null when the product was deleted; name is the one frozen in
 * the invoice. quantityCancelled and the net total discount the cancellations
 * of the period, counted on the day they happened.
 */
public record TopProductDto(UUID productId, String name, BigDecimal quantitySold, BigDecimal quantityCancelled,
                            BigDecimal netTotal) {}
