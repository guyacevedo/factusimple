package com.guyacevedo.factusimple.features.invoice.dto;

import java.math.BigDecimal;

public record InvoiceItemTaxDto(
    String taxCode,
    BigDecimal taxRate,
    Boolean isExcluded,
    BigDecimal baseAmount,
    BigDecimal amount
) {}
