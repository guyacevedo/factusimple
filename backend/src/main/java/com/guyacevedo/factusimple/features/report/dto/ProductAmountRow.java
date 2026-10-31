package com.guyacevedo.factusimple.features.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** What left (or came back) of a product in a period, grouped by the name frozen in the line. */
public record ProductAmountRow(UUID productId, String name, BigDecimal quantity, BigDecimal total) {}
