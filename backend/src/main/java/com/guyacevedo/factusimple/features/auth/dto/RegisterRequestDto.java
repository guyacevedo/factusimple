package com.guyacevedo.factusimple.features.auth.dto;

import com.guyacevedo.factusimple.shared.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Payload para registrar una nueva cuenta de usuario junto con su establecimiento")
public record RegisterRequestDto(

        @Schema(description = "Email del usuario. Debe ser único en el sistema (sin distinguir mayúsculas).",
                example = "usuario@empresa.com.co", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El email es requerido")
        @Email(message = "El email debe ser válido")
        @Size(max = 100, message = "El email no puede superar 100 caracteres")
        String email,

        @Schema(description = "Contraseña. Mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula y 1 dígito.",
                example = "MiPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
        @ValidPassword
        String password,

        @Schema(description = "Nombre del usuario", example = "Carlos", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String firstName,

        @Schema(description = "Apellido del usuario", example = "González", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El apellido es requerido")
        @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
        String lastName,

        @Schema(description = "Número de teléfono de contacto. Formato: 7-20 dígitos, con o sin prefijo +.",
                example = "+573001234567")
        @Pattern(regexp = "^[+]?[0-9]{7,19}$", message = "Teléfono inválido")
        String phone,

        @Schema(description = "UUID del plan de suscripción elegido. Obtener de GET /v1/plans.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El planId es requerido")
        UUID planId,

        @Schema(description = "Código de invitación requerido si el plan tiene requiredInvitedCode=true.",
                example = "INVITE-2024")
        String inviteCode,

        @Schema(description = "Datos fiscales del establecimiento.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La información del establecimiento es requerida")
        @Valid
        EstablishmentRequestDto establishment
) {}