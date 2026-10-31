package com.guyacevedo.factusimple.features.invoice.dto;

import com.guyacevedo.factusimple.shared.entity.UnitMeasurement;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InvoiceItemResponseDto(
    UUID id,
    UUID productId,
    String codeReference,
    String name,
    BigDecimal quantity,
    UnitMeasurement unitMeasure,
    String standardCode,
    BigDecimal unitPrice,
    BigDecimal discountRate,
    BigDecimal discountAmount,
    BigDecimal baseAmount,
    BigDecimal taxAmount,
    BigDecimal withholdingTaxAmount,
    BigDecimal total,
    String note,
    List<InvoiceItemTaxDto> taxes,
    List<InvoiceItemWithholdingTaxDto> withholdingTaxes
) {}
