package com.guyacevedo.factusimple.features.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Payload para cambiar el plan de suscripción del establishment (solo rol ESTABLISHMENT)")
public record ChangePlanRequestDto(

    @Schema(description = "UUID del nuevo plan al que se quiere cambiar. Obtener de GET /v1/plans.",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID del plan es requerido")
    UUID planId,

    @Schema(description = "Código de invitación requerido si el plan tiene requiredInvitedCode=true.",
            example = "PREMIUM-2024")
    String inviteCode
) {}
