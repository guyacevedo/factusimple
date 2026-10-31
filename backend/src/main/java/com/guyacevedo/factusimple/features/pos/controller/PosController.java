package com.guyacevedo.factusimple.features.pos.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.pos.dto.HeldSaleRequestDto;
import com.guyacevedo.factusimple.features.pos.dto.HeldSaleResponseDto;
import com.guyacevedo.factusimple.features.pos.service.HeldSaleService;
import com.guyacevedo.factusimple.shared.dto.ApiResponseDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/pos/held-sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "POS", description = """
        Ventas en espera del punto de venta: dejar un carrito a un lado para atender a otro cliente.

        - El carrito se guarda tal como lo envía el frontend; **no reserva stock** ni fija precios.
        - Para cobrarla se crea la factura con `POST /v1/invoices` enviando `heldSaleId`: al
          facturar, la venta en espera desaparece de la lista en la misma operación.
        - La etiqueta es única por establecimiento y hay un máximo de ventas en espera a la vez.

        **Solo ESTABLISHMENT** en todos los endpoints.
        """)
public class PosController {

    private final HeldSaleService heldSaleService;

    @Operation(
            summary = "Poner una venta en espera",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venta puesta en espera"),
            @ApiResponse(responseCode = "400", description = "Validación fallida o carrito inválido",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una venta en espera con esa etiqueta",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Se alcanzó el máximo de ventas en espera",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<HeldSaleResponseDto>> hold(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody HeldSaleRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Venta puesta en espera", heldSaleService.hold(principal.getUserId(), requestDto)));
    }

    @Operation(
            summary = "Listar ventas en espera",
            description = "De la que más lleva esperando a la más reciente.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Ventas en espera")
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<HeldSaleResponseDto>>> list(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponseDto.success("Ventas en espera obtenidas",
                heldSaleService.list(principal.getUserId())));
    }

    @Operation(
            summary = "Obtener venta en espera",
            description = "Retorna el carrito para retomarlo. No la elimina: se elimina al cobrarla o descartarla.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta en espera encontrada"),
            @ApiResponse(responseCode = "404", description = "Venta en espera no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<HeldSaleResponseDto>> get(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la venta en espera", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponseDto.success("Venta en espera obtenida",
                heldSaleService.get(principal.getUserId(), id)));
    }

    @Operation(
            summary = "Actualizar venta en espera",
            description = "Reemplaza la etiqueta y el carrito, por ejemplo al agregar productos a una venta retomada.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta en espera actualizada"),
            @ApiResponse(responseCode = "400", description = "Validación fallida o carrito inválido",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Venta en espera no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe otra venta en espera con esa etiqueta",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<HeldSaleResponseDto>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la venta en espera", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody HeldSaleRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponseDto.success("Venta en espera actualizada",
                heldSaleService.update(principal.getUserId(), id, requestDto)));
    }

    @Operation(
            summary = "Descartar venta en espera",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta en espera descartada"),
            @ApiResponse(responseCode = "404", description = "Venta en espera no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la venta en espera", required = true)
            @PathVariable UUID id) {
        heldSaleService.delete(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("Venta en espera descartada", null));
    }
}
