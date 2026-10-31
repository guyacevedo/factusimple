package com.guyacevedo.factusimple.features.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Pago de la factura. Todos los pagos de una factura usan la misma forma de pago")
public record InvoicePaymentRequestDto(

        @Schema(description = "Forma de pago DIAN: 1 = contado, 2 = crédito (requiere dueDate).",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La forma de pago es requerida")
        @Pattern(regexp = "^[12]$", message = "La forma de pago debe ser 1 (contado) o 2 (crédito)")
        String paymentForm,

        @Schema(description = "Medio de pago (catálogo DIAN): 10 = efectivo, 47 = transferencia, etc.",
                example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El medio de pago es requerido")
        @Pattern(regexp = "^[0-9A-Z]{1,4}$", message = "El medio de pago debe ser un código DIAN de hasta 4 caracteres")
        String paymentMethodCode,

        @Schema(description = "Referencia del pago (transferencia, voucher, etc.).", example = "TRX-778812")
        @Size(max = 50, message = "La referencia no puede superar 50 caracteres")
        String referenceCode,

        @Schema(description = "Monto. La suma de los pagos debe ser igual al total de la factura.",
                example = "23800.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor a 0")
        @Digits(integer = 14, fraction = 2, message = "El monto admite 14 enteros y 2 decimales")
        BigDecimal amount,

        @Schema(description = "Fecha de vencimiento. Requerida, y desde hoy, cuando paymentForm = 2.",
                example = "2026-12-15")
        LocalDate dueDate
) {}
