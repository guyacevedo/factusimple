package com.guyacevedo.factusimple.features.invoice.dto;

import com.guyacevedo.factusimple.features.invoice.entity.CreditNoteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreditNoteResponseDto(
    UUID id,
    UUID invoiceId,
    String invoiceReferenceCode,
    UUID customerId,
    String referenceCode,
    Integer numberingRangeId,
    String correctionConceptCode,
    String customizationId,
    String observation,
    CreditNoteStatus status,
    String factusNumber,
    String cufe,
    LocalDateTime createdAt
) {}
