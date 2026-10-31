package com.guyacevedo.factusimple.features.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Cancelación de una factura: se emite una nota crédito por el total")
public record CancelInvoiceRequestDto(

        @Schema(description = "Motivo de la cancelación; queda en la factura y en la nota crédito.",
                example = "El cliente desistió de la compra", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El motivo de la cancelación es requerido")
        String reason,

        @Schema(description = "ID del rango de numeración de Factus para notas crédito.",
                example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El rango de numeración de la nota crédito es requerido")
        @Positive(message = "El rango de numeración debe ser mayor a 0")
        Integer numberingRangeId
) {}
