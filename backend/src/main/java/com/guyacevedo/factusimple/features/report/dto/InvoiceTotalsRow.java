package com.guyacevedo.factusimple.features.report.dto;

import java.math.BigDecimal;

/** Aggregate of the non-cancelled invoices of a period (ReportRepository.invoiceTotals). */
public record InvoiceTotalsRow(
    long invoices,
    BigDecimal total,
    BigDecimal subtotal,
    BigDecimal totalTaxes,
    BigDecimal totalDiscounts
) {}
