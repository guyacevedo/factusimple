package com.guyacevedo.factusimple.features.invoice.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.invoice.dto.CancelInvoiceRequestDto;
import com.guyacevedo.factusimple.features.invoice.dto.CancelInvoiceResponseDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoiceRequestDto;
import com.guyacevedo.factusimple.features.invoice.dto.InvoiceResponseDto;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceService;
import com.guyacevedo.factusimple.shared.dto.ApiResponseDto;
import com.guyacevedo.factusimple.shared.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "Invoices", description = """
        Facturas de venta del establecimiento autenticado.

        Una factura creada queda en **PENDING**: está validada localmente (totales, stock, cupo
        de crédito) y pendiente de envío a la DIAN, que llegará con la integración de Factus.

        - Los precios, impuestos y unidades salen del catálogo y quedan **congelados** en la factura:
          cambiar el producto después no la altera. Los precios incluyen IVA.
        - Crear una factura descuenta el stock y consume un cupo de `maxInvoices` del plan.
        - Una venta a crédito (forma de pago 2) suma al saldo del cliente sin pasar su cupo; se
          paga con abonos en `/v1/receivables`.
        - Cancelar emite una **nota crédito PENDING** por el total, devuelve el stock y libera
          el saldo pendiente. Las facturas no se editan ni se eliminan.

        **Solo ESTABLISHMENT** en todos los endpoints.
        """)
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(
            summary = "Crear factura",
            description = """
                    Crea la factura en estado PENDING.

                    - El precio de cada línea es el del producto: mayorista desde `minStockWholesale`.
                    - Los pagos deben sumar exactamente el total y usar una sola forma de pago.
                    - Crédito (forma 2): requiere `dueDate` y que el total quepa en el cupo del cliente.
                    - `cashReceived` (opcional, solo efectivo) calcula el cambio.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Factura creada en estado PENDING"),
            @ApiResponse(responseCode = "400", description = "Validación fallida, cliente o producto desactivado, cantidad inválida o pagos que no cuadran con el total",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Cliente o producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una factura con ese código de referencia",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Límite de facturas del plan, stock insuficiente o cupo de crédito superado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<InvoiceResponseDto>> createInvoice(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody InvoiceRequestDto requestDto) {
        InvoiceResponseDto invoiceDto = invoiceService.createInvoice(principal.getUserId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Factura creada exitosamente", invoiceDto));
    }

    @Operation(
            summary = "Obtener factura por ID",
            description = "Retorna la factura con sus líneas, impuestos congelados, pagos y lo abonado.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Factura encontrada"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<InvoiceResponseDto>> getInvoice(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la factura", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponseDto.success("Factura obtenida exitosamente",
                invoiceService.getInvoice(principal.getUserId(), id)));
    }

    @Operation(
            summary = "Listar facturas",
            description = "Historial de facturas, de la más reciente a la más antigua, con filtros opcionales.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de facturas")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<InvoiceResponseDto>>> listInvoices(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Estado de la factura") @RequestParam(required = false) InvoiceStatus status,
            @Parameter(description = "UUID del cliente") @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Forma de pago: 1 = contado, 2 = crédito", example = "2")
            @RequestParam(required = false) String paymentForm,
            @Parameter(description = "Solo facturas con saldo pendiente", example = "false")
            @RequestParam(defaultValue = "false") boolean withBalance,
            @Parameter(description = "Desde esta fecha (inclusive)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta esta fecha (inclusive)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponseDto.success("Facturas obtenidas",
                PageResponseDto.from(invoiceService.listInvoices(principal.getUserId(), status, customerId,
                        paymentForm, withBalance, from, to, pageable))));
    }

    @Operation(
            summary = "Cancelar factura",
            description = """
                    Cancela la factura y emite una nota crédito PENDING por el total (concepto 2, anulación).

                    - El stock de los productos vuelve al inventario.
                    - El saldo pendiente de una venta a crédito se libera del cupo del cliente.
                    - `amountToRefund` es lo que hay que devolverle al cliente: lo pagado de contado más lo abonado.
                    - El cupo de facturas del plan no se recupera.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Factura cancelada y nota crédito emitida"),
            @ApiResponse(responseCode = "400", description = "Validación fallida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "La factura ya está cancelada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponseDto<CancelInvoiceResponseDto>> cancelInvoice(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la factura", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody CancelInvoiceRequestDto requestDto) {
        CancelInvoiceResponseDto cancelDto = invoiceService.cancelInvoice(principal.getUserId(), id, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Factura cancelada y nota crédito emitida", cancelDto));
    }
}
