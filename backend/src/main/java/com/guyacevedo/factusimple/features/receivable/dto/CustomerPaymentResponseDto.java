package com.guyacevedo.factusimple.features.receivable.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerPaymentResponseDto(
    UUID id,
    UUID customerId,
    String customerName,
    UUID invoiceId,
    String invoiceReferenceCode,
    BigDecimal amount,
    String paymentMethodCode,
    String referenceCode,
    String notes,
    LocalDateTime createdAt
) {}
