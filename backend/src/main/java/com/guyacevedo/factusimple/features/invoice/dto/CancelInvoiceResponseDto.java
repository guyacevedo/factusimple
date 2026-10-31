package com.guyacevedo.factusimple.features.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CancelInvoiceResponseDto(
    InvoiceResponseDto invoice,
    CreditNoteResponseDto creditNote,

    @Schema(description = "Dinero que hay que devolverle al cliente: lo pagado de contado más lo abonado a la deuda.",
            example = "30000.00")
    BigDecimal amountToRefund
) {}
