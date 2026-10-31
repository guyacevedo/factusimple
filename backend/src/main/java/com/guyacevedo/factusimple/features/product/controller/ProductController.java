package com.guyacevedo.factusimple.features.product.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.product.dto.ProductRequestDto;
import com.guyacevedo.factusimple.features.product.dto.ProductResponseDto;
import com.guyacevedo.factusimple.features.product.service.ProductService;
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
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "Products", description = """
        Gestión del catálogo de productos del establecimiento autenticado.

        Cada establecimiento ve y administra solo sus propios productos. El SKU es único sin
        distinguir mayúsculas y el código de barras es único cuando se indica. Cada producto
        cuenta para el límite `maxProducts` del plan.

        **Solo ESTABLISHMENT** en todos los endpoints.
        - `GET /v1/products` — productos activos (para facturación y POS)
        - `GET /v1/products/all` — incluye inactivos, para revisarlos o reactivarlos

        La unidad de medida debe corresponder al tipo de venta (BY_UNIT: 94, SET, DZN, DPR;
        BY_WEIGHT: KGM, LBR, GRM, ONZ; BY_VOLUME: LTR, MLT, GLL). El stock se consulta y
        se ajusta en `/v1/inventory`. Un producto con movimientos de inventario no puede
        eliminarse: desactívalo con `isActive=false`.
        """)
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "Crear producto",
            description = """
                    Crea un producto con sus impuestos y retenciones, y su fila de stock.

                    Si se envía `initialStock` > 0 queda registrado como ajuste `RECEIPT`.
                    Consume un cupo del límite `maxProducts` del plan.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Validación fallida, unidad que no corresponde al tipo de venta, cantidades con decimales en venta por unidad o categoría desactivada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ESTABLISHMENT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya tienes un producto con ese SKU o código de barras",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Alcanzaste el máximo de productos de tu plan",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> createProduct(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ProductRequestDto requestDto) {
        ProductResponseDto productDto = productService.createProduct(principal.getUserId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Producto creado exitosamente", productDto));
    }

    @Operation(
            summary = "Buscar productos (POS)",
            description = """
                    Busca a la vez por nombre, SKU o código de barras (contiene, sin distinguir mayúsculas),
                    ordenado por nombre. Sin `q` devuelve todos.

                    Por defecto solo trae productos activos; `activeOnly=false` incluye los inactivos.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de productos encontrados")
    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<PageResponseDto<ProductResponseDto>>> searchProducts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Texto a buscar en nombre, SKU o código de barras", example = "gaseosa")
            @RequestParam(required = false) String q,
            @Parameter(description = "UUID de la categoría para filtrar (opcional)")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Solo productos activos", example = "true")
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("name"));
        return ResponseEntity.ok(ApiResponseDto.success("Productos obtenidos",
                PageResponseDto.from(productService.searchProducts(
                        principal.getUserId(), q, categoryId, activeOnly, pageable))));
    }

    @Operation(
            summary = "Obtener producto por ID",
            description = "Retorna un producto del establecimiento autenticado con sus impuestos y retenciones.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> getProduct(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del producto", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponseDto.success("Producto obtenido exitosamente",
                productService.getProduct(principal.getUserId(), id)));
    }

    @Operation(
            summary = "Listar productos activos",
            description = "Retorna los productos activos ordenados por nombre.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de productos activos")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<ProductResponseDto>>> listProducts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("name"));
        return ResponseEntity.ok(ApiResponseDto.success("Productos obtenidos",
                PageResponseDto.from(productService.listActiveProducts(principal.getUserId(), pageable))));
    }

    @Operation(
            summary = "Listar todos los productos",
            description = "Incluye los productos inactivos, para poder revisarlos o reactivarlos.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de productos")
    @GetMapping("/all")
    public ResponseEntity<ApiResponseDto<PageResponseDto<ProductResponseDto>>> listAllProducts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("name"));
        return ResponseEntity.ok(ApiResponseDto.success("Productos obtenidos",
                PageResponseDto.from(productService.listAllProducts(principal.getUserId(), pageable))));
    }

    @Operation(
            summary = "Actualizar producto",
            description = """
                    Actualiza un producto del establecimiento autenticado.

                    - Los campos con valor por defecto (`isActive`, `costPrice`, `minStock`, `minStockWholesale`,
                      `unitMeasure`, `saleType`) conservan su valor si se omiten.
                    - `taxes` / `withholdingTaxes` omitidos se conservan; una lista (incluso vacía) los reemplaza.
                    - `categoryId` null deja el producto sin categoría.
                    - `initialStock` se ignora: el stock se cambia con `POST /v1/inventory/adjustments`.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "400", description = "Validación fallida, unidad que no corresponde al tipo de venta, cantidades con decimales en venta por unidad o categoría desactivada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Producto o categoría no encontrados",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya tienes otro producto con ese SKU o código de barras",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> updateProduct(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del producto", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponseDto.success("Producto actualizado exitosamente",
                productService.updateProduct(principal.getUserId(), id, requestDto)));
    }

    @Operation(
            summary = "Eliminar producto",
            description = """
                    Elimina un producto sin movimientos de inventario y libera su cupo en el plan.

                    Si tiene movimientos (incluido un stock inicial) responde 409: desactívalo con
                    `isActive=false` en su lugar.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto eliminado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "El producto tiene movimientos de inventario y no puede eliminarse",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteProduct(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID del producto", required = true)
            @PathVariable UUID id) {
        productService.deleteProduct(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("Producto eliminado exitosamente", null));
    }
}
