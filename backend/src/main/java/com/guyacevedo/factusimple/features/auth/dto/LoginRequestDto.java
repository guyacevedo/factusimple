package com.guyacevedo.factusimple.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para autenticación")
public record LoginRequestDto(

    @Schema(description = "Email registrado del usuario", example = "usuario@empresa.com.co",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    String email,

    @Schema(description = "Contraseña del usuario", example = "MiPassword123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La contraseña es requerida")
    String password
) {}
