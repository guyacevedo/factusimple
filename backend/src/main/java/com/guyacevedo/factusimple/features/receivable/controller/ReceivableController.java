package com.guyacevedo.factusimple.features.receivable.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.receivable.dto.CustomerDebtResponseDto;
import com.guyacevedo.factusimple.features.receivable.dto.CustomerPaymentRequestDto;
import com.guyacevedo.factusimple.features.receivable.dto.CustomerPaymentResponseDto;
import com.guyacevedo.factusimple.features.receivable.service.ReceivableService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/receivables")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "Receivables", description = """
        Cartera del establecimiento autenticado: abonos a las ventas a crédito.

        Cada venta a crédito deja un saldo en la factura (`balanceDue`) y en el cliente
        (`creditBalance`). Un abono baja los dos. Se puede abonar a una factura concreta o a la
        deuda total del cliente, que se reparte de la factura más antigua a la más reciente.

        **Solo ESTABLISHMENT** en todos los endpoints.
        """)
public class ReceivableController {

    private final ReceivableService receivableService;

    @Operation(
            summary = "Registrar abono",
            description = """
                    Registra un abono a la deuda del cliente.

                    - Con `invoiceId`: abona a esa factura; no puede superar su saldo.
                    - Sin `invoiceId`: se reparte entre las facturas con saldo, de la más antigua a la más
                      reciente; no puede superar la deuda total. Devuelve un abono por cada factura tocada.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Abono registrado"),
            @ApiResponse(responseCode = "400", description = "Validación fallida, factura de otro cliente o monto mayor al saldo",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Cliente o factura no encontrados",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Factura cancelada o sin saldo, o cliente sin deuda",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/payments")
    public ResponseEntity<ApiResponseDto<List<CustomerPaymentResponseDto>>> registerPayment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CustomerPaymentRequestDto requestDto) {
        List<CustomerPaymentResponseDto> payments = receivableService.registerPayment(principal.getUserId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Abono registrado exitosamente", payments));
    }

    @Operation(
            summary = "Listar abonos",
            description = "Historial de abonos, del más reciente al más antiguo, con filtros opcionales por cliente y factura.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de abonos")
    @GetMapping("/payments")
    public ResponseEntity<ApiResponseDto<PageResponseDto<CustomerPaymentResponseDto>>> listPayments(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del cliente") @RequestParam(required = false) UUID customerId,
            @Parameter(description = "UUID de la factura") @RequestParam(required = false) UUID invoiceId,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponseDto.success("Abonos obtenidos",
                PageResponseDto.from(receivableService.listPayments(principal.getUserId(), customerId, invoiceId, pageable))));
    }

    @Operation(
            summary = "Deuda de un cliente",
            description = "Cupo, saldo, crédito disponible y facturas pendientes del cliente, de la más antigua a la más reciente.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deuda del cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponseDto<CustomerDebtResponseDto>> getCustomerDebt(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del cliente", required = true)
            @PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiResponseDto.success("Deuda del cliente obtenida",
                receivableService.getCustomerDebt(principal.getUserId(), customerId)));
    }
}
