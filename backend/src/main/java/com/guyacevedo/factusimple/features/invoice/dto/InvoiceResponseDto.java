package com.guyacevedo.factusimple.features.invoice.dto;

import com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * amountPaid is what was paid towards the debt with credit payments (0 for cash
 * sales); it is not stored in the invoice, InvoiceService fills it in.
 */
public record InvoiceResponseDto(
    UUID id,
    String referenceCode,
    InvoiceStatus status,
    UUID customerId,
    String customerName,
    String customerIdentification,
    Integer numberingRangeId,
    String documentType,
    String operationType,
    Boolean sendEmail,
    String observation,
    BigDecimal subtotal,
    BigDecimal totalDiscounts,
    BigDecimal totalTaxes,
    BigDecimal totalHoldingTaxes,
    BigDecimal cashRounding,
    BigDecimal total,
    BigDecimal cashReceived,
    BigDecimal changeAmount,
    BigDecimal balanceDue,
    BigDecimal amountPaid,
    LocalDateTime cancelledAt,
    String cancellationReason,
    String cufe,
    String factusNumber,
    List<InvoiceItemResponseDto> items,
    List<InvoicePaymentResponseDto> payments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
