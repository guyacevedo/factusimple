package com.guyacevedo.factusimple.features.receivable.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** creditBalance is what the customer owes; it equals the sum of balanceDue of its pending invoices. */
public record CustomerDebtResponseDto(
    UUID customerId,
    String customerName,
    BigDecimal creditLimit,
    BigDecimal creditBalance,
    BigDecimal availableCredit,
    List<PendingInvoiceDto> pendingInvoices
) {}
