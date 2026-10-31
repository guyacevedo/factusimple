package com.guyacevedo.factusimple.features.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Payload para crear una factura. Queda en PENDING hasta que se sincronice con la DIAN")
public record InvoiceRequestDto(

        @Schema(description = "UUID de un cliente activo del establecimiento.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El cliente es requerido")
        UUID customerId,

        @Schema(description = "Código de referencia propio, único por establecimiento (máximo 50: la nota crédito le antepone NC-). Si se omite se genera FV-{n}.",
                example = "FV-120")
        @Size(max = 50, message = "El código de referencia no puede superar 50 caracteres")
        String referenceCode,

        @Schema(description = "ID del rango de numeración de Factus para facturas de venta.",
                example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El rango de numeración es requerido")
        @Positive(message = "El rango de numeración debe ser mayor a 0")
        Integer numberingRangeId,

        @Schema(description = "Observación de la factura.", example = "Entrega a domicilio")
        String observation,

        @Schema(description = "Enviar la factura al correo del cliente cuando se sincronice. Por defecto false.",
                example = "false", defaultValue = "false")
        Boolean sendEmail,

        @Schema(description = "Líneas de la factura. Mínimo una.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "La factura debe tener al menos un producto")
        List<@NotNull(message = "Hay una línea vacía") @Valid InvoiceItemRequestDto> items,

        @Schema(description = "Pagos. Mínimo uno; deben sumar el total y usar la misma forma de pago.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "La factura debe tener al menos un pago")
        List<@NotNull(message = "Hay un pago vacío") @Valid InvoicePaymentRequestDto> payments,

        @Schema(description = "Efectivo que entregó el cliente; solo con pagos en efectivo (medio 10). "
                + "Se usa para calcular el cambio.", example = "50000")
        @DecimalMin(value = "0", message = "El efectivo recibido no puede ser negativo")
        @Digits(integer = 14, fraction = 2, message = "El efectivo recibido admite 14 enteros y 2 decimales")
        BigDecimal cashReceived,

        @Schema(description = "UUID de la venta en espera que se está cobrando (POS). "
                + "Se elimina de la lista en la misma operación; si la factura falla, sigue en espera.")
        UUID heldSaleId
) {}
