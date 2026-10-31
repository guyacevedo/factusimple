package com.guyacevedo.factusimple.features.user.controller;

import com.guyacevedo.factusimple.config.security.CustomUserDetails;
import com.guyacevedo.factusimple.features.user.dto.ChangePasswordRequestDto;
import com.guyacevedo.factusimple.features.user.dto.ChangePlanRequestDto;
import com.guyacevedo.factusimple.features.user.dto.UpdateProfileRequestDto;
import com.guyacevedo.factusimple.features.user.dto.UserResponseDto;
import com.guyacevedo.factusimple.features.user.service.UserService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = """
        Gestión de usuarios del sistema.

        **Permisos por endpoint:**
        - `GET /v1/users` — solo ADMIN (lista todos los usuarios)
        - `GET /v1/users/me` — cualquier usuario autenticado (su cuenta, plan y datos fiscales)
        - `PUT /v1/users/me/establishment` — solo ESTABLISHMENT (actualiza sus datos y los del establecimiento)
        - `PATCH /v1/users/me/password` — cualquier usuario autenticado (cambia su propia contraseña)
        - `PATCH /v1/users/me/plan` — solo ESTABLISHMENT (cambia el plan del establishment)

        La respuesta `UserResponseDto` incluye los contadores de uso actual del plan
        (`productCount`, `customerCount`, `invoiceCount`) versus los límites máximos.
        """)
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Listar todos los usuarios (solo ADMIN)",
            description = "Retorna todos los usuarios registrados, del más reciente al más antiguo, con su plan y contadores de uso.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de usuarios"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<PageResponseDto<UserResponseDto>>> listUsers(
            @Parameter(description = "Número de página (0-indexed)", example = "0", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (máximo 100)", example = "10", schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "10") int size) {

        // Clamped so page=-1 or size=0 return a valid page instead of a 500.
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserResponseDto> usersPage = userService.listUsers(pageable);
        return ResponseEntity.ok(ApiResponseDto.success("Usuarios obtenidos", PageResponseDto.from(usersPage)));
    }

    @Operation(
            summary = "Obtener mi cuenta",
            description = "Retorna el usuario autenticado con su plan, contadores de uso y datos fiscales del establecimiento.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta del usuario"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> getProfile(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponseDto.success("Cuenta obtenida", userService.getProfile(principal.getUserId())));
    }

    @Operation(
            summary = "Actualizar mis datos y los del establecimiento (solo ESTABLISHMENT)",
            description = """
                    Actualiza nombre, teléfono y los datos fiscales del establecimiento.

                    El **NIT y el DV** solo se pueden cambiar mientras el establecimiento no haya emitido
                    facturas: identifican al emisor y cambiarlos alteraría las facturas ya creadas.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos actualizados"),
            @ApiResponse(responseCode = "400", description = "Validación fallida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ESTABLISHMENT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Cambio de NIT o DV con facturas ya emitidas",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PutMapping("/me/establishment")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequestDto request) {
        return ResponseEntity.ok(ApiResponseDto.success("Datos actualizados exitosamente",
                userService.updateProfile(principal.getUserId(), request)));
    }

    @Operation(
            summary = "Cambiar contraseña propia",
            description = """
                    Permite al usuario autenticado cambiar su propia contraseña.

                    **Requisitos:**
                    - `currentPassword`: contraseña actual correcta
                    - `newPassword`: entre 8 y 72 caracteres, al menos 1 mayúscula, 1 minúscula y 1 dígito
                    - La nueva contraseña no puede ser igual a la actual
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta o nueva contraseña inválida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDto<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        userService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponseDto.success("Contraseña actualizada exitosamente", null));
    }

    @Operation(
            summary = "Cambiar plan de suscripción (solo ESTABLISHMENT)",
            description = """
                    Cambia el plan del establishment del usuario autenticado.

                    **Requisitos:**
                    - `planId`: UUID del nuevo plan (debe existir y estar activo)
                    - `inviteCode`: requerido si el nuevo plan tiene `requiredInvitedCode=true`
                    - El uso actual (productos, clientes, facturas) no puede superar los límites del nuevo plan

                    **Nota:** Los contadores actuales de uso no se resetean.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Plan inactivo, código de invitación inválido o uso actual por encima de los límites",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Se requiere rol ESTABLISHMENT",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Plan no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PatchMapping("/me/plan")
    @PreAuthorize("hasRole('ESTABLISHMENT')")
    public ResponseEntity<ApiResponseDto<Void>> changePlan(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePlanRequestDto request) {
        userService.changePlan(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponseDto.success("Plan actualizado exitosamente", null));
    }
}