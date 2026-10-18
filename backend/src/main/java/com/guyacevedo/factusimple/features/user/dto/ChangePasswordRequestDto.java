package com.guyacevedo.factusimple.features.user.dto;

import com.guyacevedo.factusimple.shared.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para cambiar la contraseña del usuario autenticado")
public record ChangePasswordRequestDto(

        @Schema(description = "Contraseña actual del usuario.", example = "MiPasswordActual123!",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña actual es requerida")
        String currentPassword,

        @Schema(description = "Nueva contraseña. Mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula y 1 dígito. No puede ser igual a la actual.",
                example = "NuevaPassword456", requiredMode = Schema.RequiredMode.REQUIRED)
        @ValidPassword
        String newPassword
) {}