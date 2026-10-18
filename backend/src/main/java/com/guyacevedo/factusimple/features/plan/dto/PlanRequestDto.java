package com.guyacevedo.factusimple.features.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para crear o actualizar un plan de suscripción (solo ADMIN)")
public record PlanRequestDto(

        @Schema(description = "Nombre del plan.", example = "Plan Básico", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre del plan es requerido")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        @Schema(description = "Máximo de productos que el usuario puede tener en su catálogo. Mínimo 1.",
                example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El máximo de productos es requerido")
        @Min(value = 1, message = "El máximo de productos debe ser al menos 1")
        Integer maxProducts,

        @Schema(description = "Máximo de clientes que el usuario puede registrar. Mínimo 1.",
                example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El máximo de clientes es requerido")
        @Min(value = 1, message = "El máximo de clientes debe ser al menos 1")
        Integer maxCustomers,

        @Schema(description = "Máximo de facturas electrónicas que el usuario puede emitir en total. Mínimo 1.",
                example = "200", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El máximo de facturas es requerido")
        @Min(value = 1, message = "El máximo de facturas debe ser al menos 1")
        Integer maxInvoices,

        @Schema(description = "Si es true, los usuarios deben presentar el campo 'code' para suscribirse a este plan. Si se omite: false al crear, sin cambios al actualizar.",
                example = "false", defaultValue = "false")
        Boolean requiredInvitedCode,

        @Schema(description = "Código de invitación para suscribirse al plan. Obligatorio si requiredInvitedCode=true.",
                example = "PREMIUM-2024")
        @Size(max = 255, message = "El código no puede superar 255 caracteres")
        String code,

        @Schema(description = "Descripción del plan para mostrar a los usuarios.",
                example = "Ideal para pequeñas empresas con hasta 50 productos y 200 facturas.")
        String description,

        @Schema(description = "Si el plan está activo y disponible para nuevas suscripciones. Si se omite: true al crear, sin cambios al actualizar.",
                example = "true", defaultValue = "true")
        Boolean isActive
) {

    @Schema(hidden = true)
    @AssertTrue(message = "El código es requerido cuando el plan exige código de invitación")
    public boolean isCodeProvidedWhenRequired() {
        return !Boolean.TRUE.equals(requiredInvitedCode) || (code != null && !code.isBlank());
    }
}