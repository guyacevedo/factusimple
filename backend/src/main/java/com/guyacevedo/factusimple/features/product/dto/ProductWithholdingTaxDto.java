package com.guyacevedo.factusimple.features.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Used both in the product request and in its response. */
@Schema(description = "Retención del producto (catálogo DIAN)")
public record ProductWithholdingTaxDto(

        @Schema(description = "Código de la retención: 05 = ReteIVA, 06 = ReteRenta, etc.",
                example = "06", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El código de la retención es requerido")
        @Size(max = 4, message = "El código de la retención no puede superar 4 caracteres")
        String withholdingTaxCode,

        @Schema(description = "Tarifa en porcentaje.", example = "2.50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La tarifa de la retención es requerida")
        @DecimalMin(value = "0", message = "La tarifa no puede ser negativa")
        @DecimalMax(value = "100", message = "La tarifa no puede superar 100")
        @Digits(integer = 3, fraction = 2, message = "La tarifa admite 2 decimales")
        BigDecimal withholdingTaxRate
) {}
