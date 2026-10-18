package com.guyacevedo.factusimple.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para renovar el access token usando el refresh token")
public record RefreshTokenRequestDto(

    @Schema(description = "Token de renovación obtenido en POST /auth/login o en un refresh previo. Expira en 30 días.",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El refresh token es requerido")
    String refreshToken
) {}
