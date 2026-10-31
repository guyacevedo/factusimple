package com.guyacevedo.factusimple.features.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para crear o actualizar una categoría del establecimiento autenticado")
public record CategoryRequestDto(

        @Schema(description = "Nombre de la categoría. Único por establecimiento sin distinguir mayúsculas.",
                example = "Bebidas", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre de la categoría es requerido")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        @Schema(description = "Descripción de la categoría.", example = "Gaseosas, jugos y agua")
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description,

        @Schema(description = "Posición en los listados (menor primero). Si se omite: 0 al crear, sin cambios al actualizar.",
                example = "0", defaultValue = "0")
        @Min(value = 0, message = "El orden no puede ser negativo")
        Integer sortOrder,

        @Schema(description = "Si la categoría está activa. Si se omite: true al crear, sin cambios al actualizar.",
                example = "true", defaultValue = "true")
        Boolean isActive
) {}
