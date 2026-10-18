package com.guyacevedo.factusimple.features.auth.dto;

import com.guyacevedo.factusimple.shared.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para activar una cuenta recién registrada y establecer la contraseña definitiva")
public record ActivateAccountRequestDto(

        @Schema(description = "Email del usuario a activar.", example = "usuario@empresa.com.co",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email debe ser válido")
        String email,

        @Schema(description = "Contraseña inicial provista en el registro.", example = "PasswordInicial123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña actual es requerida")
        String currentPassword,

        @Schema(description = "Nueva contraseña definitiva. Mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula y 1 dígito.",
                example = "NuevaPassword456", requiredMode = Schema.RequiredMode.REQUIRED)
        @ValidPassword
        String newPassword
) {}