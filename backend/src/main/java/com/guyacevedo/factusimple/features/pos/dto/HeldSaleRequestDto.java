package com.guyacevedo.factusimple.features.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

@Schema(description = "Venta puesta en espera para atender a otro cliente")
public record HeldSaleRequestDto(

        @Schema(description = "Etiqueta para reconocerla. Única por establecimiento sin distinguir mayúsculas.",
                example = "Mesa 3", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La etiqueta es requerida")
        @Size(max = 120, message = "La etiqueta no puede superar 120 caracteres")
        String label,

        @Schema(description = "Carrito del POS tal como lo maneja el frontend (objeto o arreglo JSON). "
                + "El backend lo guarda sin interpretarlo: no reserva stock ni valida precios.",
                example = "{\"customerId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"items\": [{\"productId\": \"…\", \"quantity\": 2}]}",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El contenido del carrito es requerido")
        JsonNode content
) {}
