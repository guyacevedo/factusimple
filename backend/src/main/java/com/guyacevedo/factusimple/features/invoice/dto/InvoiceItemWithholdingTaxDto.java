package com.guyacevedo.factusimple.features.invoice.dto;

import java.math.BigDecimal;

public record InvoiceItemWithholdingTaxDto(
    String withholdingTaxCode,
    BigDecimal withholdingTaxRate,
    BigDecimal baseAmount,
    BigDecimal amount
) {}
