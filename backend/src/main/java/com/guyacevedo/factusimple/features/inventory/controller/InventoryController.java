package com.guyacevedo.factusimple.features.inventory.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.inventory.dto.StockAdjustmentRequestDto;
import com.guyacevedo.factusimple.features.inventory.dto.StockAdjustmentResponseDto;
import com.guyacevedo.factusimple.features.inventory.dto.StockResponseDto;
import com.guyacevedo.factusimple.features.inventory.service.InventoryService;
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
@RequestMapping("/v1/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "Inventory", description = """
        Stock y ajustes de inventario de los productos del establecimiento autenticado.

        Cada producto tiene una fila de stock creada al darlo de alta. Todo cambio que no sea
        una venta se hace con un ajuste, que guarda la cantidad anterior, la nueva y el motivo:
        `PHYSICAL_COUNT`, `SHRINKAGE`, `DAMAGE`, `EXPIRATION`, `RECEIPT`, `RETURN`, `CORRECTION`.

        **Solo ESTABLISHMENT** en todos los endpoints.
        """)
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(
            summary = "Listar stock",
            description = """
                    Retorna el stock de los productos ordenado por nombre.

                    Con `lowStock=true` solo devuelve los productos activos en su stock mínimo o por debajo.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de stock")
    @GetMapping("/stocks")
    public ResponseEntity<ApiResponseDto<PageResponseDto<StockResponseDto>>> listStocks(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Solo productos activos con stock en el mínimo o por debajo", example = "false")
            @RequestParam(defaultValue = "false") boolean lowStock,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("product.name"));
        return ResponseEntity.ok(ApiResponseDto.success("Stock obtenido",
                PageResponseDto.from(inventoryService.listStocks(principal.getUserId(), lowStock, pageable))));
    }

    @Operation(
            summary = "Obtener stock de un producto",
            description = "Retorna la cantidad en stock de un producto y si está en su mínimo o por debajo.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/stocks/{productId}")
    public ResponseEntity<ApiResponseDto<StockResponseDto>> getStock(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del producto", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponseDto.success("Stock obtenido exitosamente",
                inventoryService.getStock(principal.getUserId(), productId)));
    }

    @Operation(
            summary = "Registrar ajuste de inventario",
            description = """
                    Fija el stock de un producto en `newQuantity` (la cantidad final, no la diferencia)
                    y registra el ajuste con su motivo. La diferencia se calcula sola.

                    Si el producto se vende por unidad, la cantidad debe ser entera.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ajuste de inventario registrado"),
            @ApiResponse(responseCode = "400", description = "Validación fallida, cantidad con decimales en venta por unidad o igual al stock actual",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/adjustments")
    public ResponseEntity<ApiResponseDto<StockAdjustmentResponseDto>> adjustStock(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody StockAdjustmentRequestDto requestDto) {
        StockAdjustmentResponseDto adjustmentDto = inventoryService.adjustStock(principal.getUserId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Ajuste de inventario registrado", adjustmentDto));
    }

    @Operation(
            summary = "Listar ajustes de inventario",
            description = "Historial de ajustes, del más reciente al más antiguo. Con `productId` filtra por producto.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de ajustes")
    @GetMapping("/adjustments")
    public ResponseEntity<ApiResponseDto<PageResponseDto<StockAdjustmentResponseDto>>> listAdjustments(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del producto (opcional)")
            @RequestParam(required = false) UUID productId,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponseDto.success("Ajustes obtenidos",
                PageResponseDto.from(inventoryService.listAdjustments(principal.getUserId(), productId, pageable))));
    }
}
