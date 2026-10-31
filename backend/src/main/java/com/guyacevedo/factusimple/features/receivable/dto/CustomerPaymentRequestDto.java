package com.guyacevedo.factusimple.features.receivable.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Abono a la deuda de un cliente: a una factura concreta o a su deuda total")
public record CustomerPaymentRequestDto(

        @Schema(description = "UUID del cliente que abona.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El cliente es requerido")
        UUID customerId,

        @Schema(description = "UUID de la factura a crédito que se abona. Si se omite, el abono se reparte "
                + "entre las facturas con saldo, de la más antigua a la más reciente.")
        UUID invoiceId,

        @Schema(description = "Monto del abono. No puede superar el saldo de la factura o la deuda total.",
                example = "30000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor a 0")
        @Digits(integer = 14, fraction = 2, message = "El monto admite 14 enteros y 2 decimales")
        BigDecimal amount,

        @Schema(description = "Medio de pago (catálogo DIAN): 10 = efectivo, 47 = transferencia, etc.",
                example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El medio de pago es requerido")
        @Pattern(regexp = "^[0-9A-Z]{1,4}$", message = "El medio de pago debe ser un código DIAN de hasta 4 caracteres")
        String paymentMethodCode,

        @Schema(description = "Referencia del pago (transferencia, voucher, etc.).", example = "TRX-778812")
        @Size(max = 50, message = "La referencia no puede superar 50 caracteres")
        String referenceCode,

        @Schema(description = "Notas del abono.", example = "Pagó en la tienda")
        String notes
) {}
