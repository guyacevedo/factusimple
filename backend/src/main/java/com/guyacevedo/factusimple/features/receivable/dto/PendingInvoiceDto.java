package com.guyacevedo.factusimple.features.receivable.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PendingInvoiceDto(
    UUID invoiceId,
    String referenceCode,
    BigDecimal total,
    BigDecimal balanceDue,
    LocalDate dueDate,
    LocalDateTime createdAt
) {}
