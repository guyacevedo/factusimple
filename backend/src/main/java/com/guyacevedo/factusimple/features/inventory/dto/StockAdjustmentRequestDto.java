package com.guyacevedo.factusimple.features.inventory.dto;

import com.guyacevedo.factusimple.features.inventory.entity.AdjustmentReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ajuste de inventario: fija el stock real de un producto y registra el motivo")
public record StockAdjustmentRequestDto(

        @Schema(description = "UUID del producto a ajustar.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El producto es requerido")
        UUID productId,

        @Schema(description = "Cantidad final en stock tras el ajuste (no la diferencia). "
                + "Entera si el producto se vende por unidad.",
                example = "36", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La nueva cantidad es requerida")
        @DecimalMin(value = "0", message = "La cantidad no puede ser negativa")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite 9 enteros y 3 decimales")
        BigDecimal newQuantity,

        @Schema(description = "Motivo del ajuste.", example = "RECEIPT", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El motivo del ajuste es requerido")
        AdjustmentReason reason,

        @Schema(description = "Observaciones del ajuste.", example = "Llegó el pedido del proveedor")
        String notes
) {}
