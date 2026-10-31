package com.guyacevedo.factusimple.features.category.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.category.dto.CategoryRequestDto;
import com.guyacevedo.factusimple.features.category.dto.CategoryResponseDto;
import com.guyacevedo.factusimple.features.category.service.CategoryService;
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
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTABLISHMENT')")
@Tag(name = "Categories", description = """
        Gestión de las categorías de productos del establecimiento autenticado.

        Cada establecimiento ve y administra solo sus propias categorías. El nombre es único
        por establecimiento sin distinguir mayúsculas ("Bebidas" y "bebidas" son la misma).

        **Solo ESTABLISHMENT** en todos los endpoints.
        - `GET /v1/categories` — categorías activas (para formularios de producto y POS)
        - `GET /v1/categories/all` — incluye inactivas, para revisarlas o reactivarlas

        Una categoría con productos no puede eliminarse: desactívala con `isActive=false`.
        """)
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "Crear categoría",
            description = "Crea una categoría en el catálogo del establecimiento autenticado.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Validación fallida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ESTABLISHMENT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya tienes una categoría con ese nombre",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponseDto<CategoryResponseDto>> createCategory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CategoryRequestDto requestDto) {
        CategoryResponseDto categoryDto = categoryService.createCategory(principal.getUserId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Categoría creada exitosamente", categoryDto));
    }

    @Operation(
            summary = "Obtener categoría por ID",
            description = "Retorna una categoría del establecimiento autenticado.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CategoryResponseDto>> getCategory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la categoría", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponseDto.success("Categoría obtenida exitosamente",
                categoryService.getCategory(principal.getUserId(), id)));
    }

    @Operation(
            summary = "Listar categorías activas",
            description = "Retorna las categorías activas ordenadas por `sortOrder` y nombre.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de categorías activas")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<CategoryResponseDto>>> listCategories(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by("sortOrder", "name"));
        return ResponseEntity.ok(ApiResponseDto.success("Categorías obtenidas",
                PageResponseDto.from(categoryService.listActiveCategories(principal.getUserId(), pageable))));
    }

    @Operation(
            summary = "Listar todas las categorías",
            description = "Incluye las categorías inactivas, para poder revisarlas o reactivarlas.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de categorías")
    @GetMapping("/all")
    public ResponseEntity<ApiResponseDto<PageResponseDto<CategoryResponseDto>>> listAllCategories(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by("sortOrder", "name"));
        return ResponseEntity.ok(ApiResponseDto.success("Categorías obtenidas",
                PageResponseDto.from(categoryService.listAllCategories(principal.getUserId(), pageable))));
    }

    @Operation(
            summary = "Actualizar categoría",
            description = """
                    Actualiza una categoría del establecimiento autenticado.

                    Si `isActive` o `sortOrder` se omiten, conservan su valor actual.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
            @ApiResponse(responseCode = "400", description = "Validación fallida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya tienes otra categoría con ese nombre",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<CategoryResponseDto>> updateCategory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la categoría", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponseDto.success("Categoría actualizada exitosamente",
                categoryService.updateCategory(principal.getUserId(), id, requestDto)));
    }

    @Operation(
            summary = "Eliminar categoría",
            description = """
                    Elimina una categoría que no tenga productos.

                    Si tiene productos responde 409: desactívala con `isActive=false` en su lugar.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría eliminada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "La categoría tiene productos y no puede eliminarse",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "UUID de la categoría", required = true)
            @PathVariable UUID id) {
        categoryService.deleteCategory(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("Categoría eliminada exitosamente", null));
    }
}
