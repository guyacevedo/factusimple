package com.guyacevedo.factusimple.features.auth.dto;

import com.guyacevedo.factusimple.shared.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para restablecer la contraseña usando el token de recuperación")
public record ResetPasswordRequestDto(

        @Schema(description = "Email del usuario que restablece la contraseña.", example = "usuario@empresa.com.co",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email debe ser válido")
        String email,

        @Schema(description = "Token de recuperación obtenido de POST /auth/forgot-password. Expira en 15 minutos.",
                example = "a1b2c3d4e5f6...", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El token es requerido")
        String resetToken,

        @Schema(description = "Nueva contraseña. Mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula y 1 dígito.",
                example = "NuevaPassword789", requiredMode = Schema.RequiredMode.REQUIRED)
        @ValidPassword
        String newPassword
) {}