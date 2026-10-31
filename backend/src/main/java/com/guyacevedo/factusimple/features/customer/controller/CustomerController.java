package com.guyacevedo.factusimple.features.customer.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.customer.dto.CustomerRequestDto;
import com.guyacevedo.factusimple.features.customer.dto.CustomerResponseDto;
import com.guyacevedo.factusimple.features.customer.service.CustomerService;
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

import java.util.UUID;

@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "Customers", description = """
        Gestión de los clientes (adquirientes) del establecimiento autenticado.

        Cada establecimiento ve y administra solo sus propios clientes. La identificación es
        única por establecimiento, y cada cliente cuenta para el límite `maxCustomers` del plan.

        **Solo ESTABLISHMENT** en todos los endpoints.
        - `GET /v1/customers` — clientes activos (para facturación y POS)
        - `GET /v1/customers/all` — incluye inactivos, para revisarlos o reactivarlos

        `creditBalance` (saldo pendiente) lo mueven las ventas a crédito y no se edita.
        Un cliente con facturas no puede eliminarse: desactívalo con `isActive=false`.
        """)
public class CustomerController {

    private final CustomerService customerService;

    @Operation(
            summary = "Crear cliente",
            description = """
                    Crea un cliente del establecimiento autenticado.

                    Debe indicarse `company` (persona jurídica) o `names` (persona natural).
                    Consume un cupo del límite `maxCustomers` del plan.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Validación fallida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ESTABLISHMENT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya tienes un cliente con esa identificación",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Alcanzaste el máximo de clientes de tu plan",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> createCustomer(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CustomerRequestDto requestDto) {
        CustomerResponseDto customerDto = customerService.createCustomer(principal.getUserId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Cliente creado exitosamente", customerDto));
    }

    @Operation(
            summary = "Obtener cliente por ID",
            description = "Retorna un cliente del establecimiento autenticado, incluido su cupo y saldo de crédito.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> getCustomer(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del cliente", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponseDto.success("Cliente obtenido exitosamente",
                customerService.getCustomer(principal.getUserId(), id)));
    }

    @Operation(
            summary = "Listar clientes activos",
            description = "Retorna los clientes activos, del más reciente al más antiguo.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de clientes activos")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<CustomerResponseDto>>> listCustomers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        // By date, not name: legal entities use company and natural persons use names.
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponseDto.success("Clientes obtenidos",
                PageResponseDto.from(customerService.listActiveCustomers(principal.getUserId(), pageable))));
    }

    @Operation(
            summary = "Listar todos los clientes",
            description = "Incluye los clientes inactivos, para poder revisarlos o reactivarlos.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de clientes")
    @GetMapping("/all")
    public ResponseEntity<ApiResponseDto<PageResponseDto<CustomerResponseDto>>> listAllCustomers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponseDto.success("Clientes obtenidos",
                PageResponseDto.from(customerService.listAllCustomers(principal.getUserId(), pageable))));
    }

    @Operation(
            summary = "Actualizar cliente",
            description = """
                    Actualiza un cliente del establecimiento autenticado.

                    Si `isActive` o `creditLimit` se omiten, conservan su valor actual.
                    `creditLimit` no puede quedar por debajo de `creditBalance`, que no es editable.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado"),
            @ApiResponse(responseCode = "400", description = "Validación fallida o cupo de crédito menor al saldo pendiente",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya tienes otro cliente con esa identificación",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> updateCustomer(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del cliente", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponseDto.success("Cliente actualizado exitosamente",
                customerService.updateCustomer(principal.getUserId(), id, requestDto)));
    }

    @Operation(
            summary = "Eliminar cliente",
            description = """
                    Elimina un cliente que no tenga facturas y libera su cupo en el plan.

                    Si tiene facturas responde 409: desactívalo con `isActive=false` en su lugar.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente eliminado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "El cliente tiene facturas y no puede eliminarse",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteCustomer(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del cliente", required = true)
            @PathVariable UUID id) {
        customerService.deleteCustomer(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("Cliente eliminado exitosamente", null));
    }
}
