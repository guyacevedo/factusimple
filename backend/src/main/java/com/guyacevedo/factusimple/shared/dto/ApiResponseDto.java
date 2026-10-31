package com.guyacevedo.factusimple.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Envelope estándar de respuesta de la API. Todas las respuestas siguen esta estructura.")
public record ApiResponseDto<T>(

    @Schema(description = "true si la operación fue exitosa, false si ocurrió un error", example = "true")
    boolean success,

    @Schema(description = "Mensaje descriptivo del resultado de la operación", example = "Factura creada exitosamente")
    String message,

    @Schema(description = "Código de error en caso de fallo (null en respuestas exitosas). Ej: VALIDATION_ERROR, RESOURCE_NOT_FOUND, RATE_LIMIT_EXCEEDED",
            example = "null", nullable = true)
    String errorCode,

    @Schema(description = "Datos de la respuesta (null en respuestas de error). El tipo varía según el endpoint.")
    T data,

    @Schema(description = "Timestamp UTC del momento de la respuesta", example = "2024-01-15T10:30:00Z")
    Instant timestamp
) {
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(true, message, null, data, Instant.now());
    }

    public static <T> ApiResponseDto<T> error(String message, String errorCode) {
        return new ApiResponseDto<>(false, message, errorCode, null, Instant.now());
    }

    public static <T> ApiResponseDto<T> error(String message, String errorCode, T data) {
        return new ApiResponseDto<>(false, message, errorCode, data, Instant.now());
    }
}
