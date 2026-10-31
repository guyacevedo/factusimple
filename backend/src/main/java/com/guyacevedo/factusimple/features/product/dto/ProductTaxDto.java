package com.guyacevedo.factusimple.features.product.dto;

import com.guyacevedo.factusimple.features.catalog.Catalog;
import com.guyacevedo.factusimple.features.catalog.validation.ValidCatalogCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Used both in the product request and in its response. */
@Schema(description = "Impuesto del producto (catálogo DIAN)")
public record ProductTaxDto(

        @Schema(description = "Código del tributo: 01 = IVA, 04 = INC, etc.",
                example = "01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El código del impuesto es requerido")
        @ValidCatalogCode(Catalog.TAX)
        String taxCode,

        @Schema(description = "Tarifa en porcentaje.", example = "19.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La tarifa del impuesto es requerida")
        @DecimalMin(value = "0", message = "La tarifa no puede ser negativa")
        @DecimalMax(value = "100", message = "La tarifa no puede superar 100")
        @Digits(integer = 3, fraction = 2, message = "La tarifa admite 2 decimales")
        BigDecimal taxRate,

        @Schema(description = "Si el producto está excluido de este impuesto. Si se omite: false.",
                example = "false", defaultValue = "false")
        Boolean isExcluded
) {}
