package com.guyacevedo.factusimple.features.plan.controller;

import com.guyacevedo.factusimple.features.plan.dto.PlanRequestDto;
import com.guyacevedo.factusimple.features.plan.dto.PlanResponseDto;
import com.guyacevedo.factusimple.features.plan.service.PlanService;
import com.guyacevedo.factusimple.shared.dto.ApiResponseDto;
import com.guyacevedo.factusimple.shared.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = """
        Gestión de planes de suscripción.

        Los planes definen los límites máximos de uso para cada usuario:
        - `maxProducts`: máximo de productos que puede tener en su catálogo
        - `maxCustomers`: máximo de clientes que puede registrar
        - `maxInvoices`: máximo de facturas electrónicas que puede emitir

        **Acceso público:** `GET /v1/plans` — planes activos, sin autenticación (para la landing page).
        **Solo ADMIN:** `GET /v1/plans/all` (incluye inactivos), crear, actualizar y eliminar planes.
        **Cualquier usuario autenticado:** obtener un plan específico por ID.

        Si `requiredInvitedCode=true`, solo usuarios con el `code` correcto pueden suscribirse.
        """)
public class PlanController {

    private final PlanService planService;

    @Operation(
            summary = "Crear plan (solo ADMIN)",
            description = """
                    Crea un nuevo plan de suscripción con sus límites de uso.

                    Si `requiredInvitedCode=true`, el campo `code` es obligatorio y los usuarios
                    deberán presentar ese código al registrarse o cambiar a este plan.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plan creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Validación fallida: límites deben ser >= 1, o falta el código",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un plan con ese nombre",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> createPlan(
            @Valid @RequestBody PlanRequestDto requestDto) {
        PlanResponseDto planDto = planService.createPlan(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Plan creado exitosamente", planDto));
    }

    @Operation(
            summary = "Obtener plan por ID",
            description = "Retorna el detalle de un plan específico incluyendo límites y si exige código de invitación.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan encontrado"),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> getPlan(
            @Parameter(description = "UUID del plan", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponseDto.success("Plan obtenido exitosamente", planService.getPlan(id)));
    }

    @Operation(
            summary = "Listar planes activos (público)",
            description = """
                    Retorna los planes activos, del más pequeño al más grande. **Endpoint público — no requiere autenticación.**

                    Útil para mostrar opciones de suscripción en la landing page antes del registro.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Lista paginada de planes activos")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<PlanResponseDto>>> listPlans(
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by("maxProducts", "name"));
        return ResponseEntity.ok(ApiResponseDto.success("Planes obtenidos",
                PageResponseDto.from(planService.listActivePlans(pageable))));
    }

    @Operation(
            summary = "Listar todos los planes (solo ADMIN)",
            description = "Incluye los planes inactivos, para poder revisarlos o reactivarlos.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de planes"),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<PageResponseDto<PlanResponseDto>>> listAllPlans(
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by("maxProducts", "name"));
        return ResponseEntity.ok(ApiResponseDto.success("Planes obtenidos",
                PageResponseDto.from(planService.listAllPlans(pageable))));
    }

    @Operation(
            summary = "Actualizar plan (solo ADMIN)",
            description = """
                    Actualiza los límites y configuración de un plan existente. Los usuarios suscritos
                    verán los nuevos límites inmediatamente.

                    Si `isActive` o `requiredInvitedCode` se omiten, conservan su valor actual.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan actualizado"),
            @ApiResponse(responseCode = "400", description = "Validación fallida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe otro plan con ese nombre",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> updatePlan(
            @Parameter(description = "UUID del plan", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody PlanRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponseDto.success("Plan actualizado exitosamente",
                planService.updatePlan(id, requestDto)));
    }

    @Operation(
            summary = "Eliminar plan (solo ADMIN)",
            description = """
                    Elimina un plan de suscripción que no tenga usuarios.

                    Si tiene usuarios suscritos responde 409: desactívalo con `isActive=false` en su lugar.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan eliminado"),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Plan tiene usuarios suscritos y no puede eliminarse",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deletePlan(
            @Parameter(description = "UUID del plan", required = true)
            @PathVariable UUID id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponseDto.success("Plan eliminado exitosamente", null));
    }
}