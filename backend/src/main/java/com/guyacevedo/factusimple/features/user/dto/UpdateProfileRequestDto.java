package com.guyacevedo.factusimple.features.user.dto;

import com.guyacevedo.factusimple.features.auth.dto.EstablishmentRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Same rules as the registration (RegisterRequestDto) for the fields both have. */
@Schema(description = "Datos del usuario y datos fiscales del establecimiento")
public record UpdateProfileRequestDto(

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

        @Schema(description = "Datos fiscales. El NIT y el DV solo se pueden cambiar mientras no haya facturas emitidas.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Los datos del establecimiento son requeridos")
        @Valid
        EstablishmentRequestDto establishment
) {}
