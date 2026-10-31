package com.guyacevedo.factusimple.features.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** Price, name, unit and taxes come from the product: the client only says what and how much. */
@Schema(description = "Línea de la factura: un producto del catálogo y la cantidad vendida")
public record InvoiceItemRequestDto(

        @Schema(description = "UUID de un producto activo del establecimiento.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El producto es requerido")
        UUID productId,

        @Schema(description = "Cantidad vendida. Entera si el producto se vende por unidad. "
                + "Desde minStockWholesale del producto se aplica el precio mayorista.",
                example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La cantidad es requerida")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite 9 enteros y 3 decimales")
        BigDecimal quantity,

        @Schema(description = "Descuento en porcentaje sobre la línea (0-100).", example = "5")
        @DecimalMin(value = "0", message = "El descuento no puede ser negativo")
        @DecimalMax(value = "100", message = "El descuento no puede superar 100")
        @Digits(integer = 3, fraction = 2, message = "El descuento admite 2 decimales")
        BigDecimal discountRate,

        @Schema(description = "Nota de la línea.", example = "Sin hielo")
        String note
) {}
