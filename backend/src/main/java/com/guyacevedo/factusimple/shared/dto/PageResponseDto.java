package com.guyacevedo.factusimple.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Respuesta paginada. Navegar incrementando 'page' hasta que isLast=true.")
public record PageResponseDto<T>(

    @Schema(description = "Lista de elementos de la página actual")
    List<T> content,

    @Schema(description = "Número de página actual (0-indexed)", example = "0")
    Integer pageNumber,

    @Schema(description = "Tamaño de página solicitado", example = "10")
    Integer pageSize,

    @Schema(description = "Total de elementos en todas las páginas", example = "47")
    Long totalElements,

    @Schema(description = "Total de páginas disponibles", example = "5")
    Integer totalPages,

    @Schema(description = "true si esta es la última página disponible", example = "false")
    Boolean isLast
) {
    public static <T> PageResponseDto<T> from(Page<T> page) {
        return new PageResponseDto<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}
