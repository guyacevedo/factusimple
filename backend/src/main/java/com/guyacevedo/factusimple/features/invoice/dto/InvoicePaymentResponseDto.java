package com.guyacevedo.factusimple.features.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoicePaymentResponseDto(
    UUID id,
    String paymentForm,
    String paymentMethodCode,
    String referenceCode,
    BigDecimal amount,
    LocalDate dueDate
) {}
