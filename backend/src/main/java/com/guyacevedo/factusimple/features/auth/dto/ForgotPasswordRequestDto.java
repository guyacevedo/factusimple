package com.guyacevedo.factusimple.features.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para solicitar el token de recuperación de contraseña")
public record ForgotPasswordRequestDto(

    @Schema(description = """
            Email del usuario que olvidó su contraseña.
            Por seguridad, la respuesta es la misma independientemente de si el email existe.
            """,
            example = "usuario@empresa.com.co", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    String email
) {}
