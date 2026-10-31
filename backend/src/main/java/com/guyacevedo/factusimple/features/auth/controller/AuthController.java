package com.guyacevedo.factusimple.features.auth.controller;

import com.guyacevedo.factusimple.features.auth.dto.ActivateAccountRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.ForgotPasswordRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.LoginRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.LoginResponseDto;
import com.guyacevedo.factusimple.features.auth.dto.RefreshTokenRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.RegisterRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.ResetPasswordRequestDto;
import com.guyacevedo.factusimple.features.auth.service.AuthService;
import com.guyacevedo.factusimple.shared.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints (see SecurityConfig.PUBLIC_ENDPOINTS).
 *
 * Login protection lives in AuthService / LoginAttemptRecorder: a per-account
 * lock and a per-IP limit. There is no global rate limiter: a single limit for
 * all users lets anyone use it up and keep everyone else from logging in.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = """
        Registro, login, renovación de tokens, activación y recuperación de contraseña.
        Todos los endpoints son públicos (no requieren token).

        **Contraseñas:** mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula y 1 dígito.
        Se permite cualquier otro carácter (ñ, tildes, espacios, símbolos).
        """)
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Registrar nueva cuenta",
            description = """
                    Crea la cuenta del usuario con los datos fiscales de su establecimiento
                    y devuelve la sesión iniciada (mismos tokens que el login).

                    **Reglas de validación:**
                    - Email único en el sistema, sin distinguir mayúsculas
                    - El `planId` debe existir y estar activo
                    - Si el plan exige código de invitación (`requiredInvitedCode=true`), `inviteCode` es obligatorio
                    - `establishment.nit`: solo dígitos, sin puntos ni dígito de verificación
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada y sesión iniciada"),
            @ApiResponse(responseCode = "400", description = "Validación fallida, plan inactivo o código de invitación incorrecto",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "El plan no existe",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Email ya registrado (EMAIL_ALREADY_REGISTERED)",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request) {
        LoginResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Usuario registrado exitosamente", response));
    }

    @Operation(
            summary = "Iniciar sesión",
            description = """
                    Autentica al usuario y devuelve los tokens.

                    **Tokens:**
                    - `accessToken`: enviar en `Authorization: Bearer {token}`. Dura 30 minutos (`expiresIn` en segundos)
                    - `refreshToken`: usar en `POST /auth/refresh` para renovar la sesión. Dura 30 días y es de un solo uso

                    **Protección:**
                    - 10 contraseñas incorrectas seguidas bloquean la cuenta 15 minutos (403 `ACCOUNT_LOCKED`)
                    - 50 intentos fallidos desde una misma IP en 15 minutos bloquean esa IP (429 `TOO_MANY_LOGIN_ATTEMPTS`)
                    - Una cuenta con contraseña temporal debe activarse primero con `POST /auth/activate` (403 `PASSWORD_CHANGE_REQUIRED`)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "400", description = "Campos requeridos faltantes o formato inválido",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos (INVALID_CREDENTIALS no distingue cuál)",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Cuenta bloqueada (ACCOUNT_LOCKED), desactivada (ACCOUNT_DISABLED) o pendiente de activación (PASSWORD_CHANGE_REQUIRED)",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos desde esta IP",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {
        // Behind Nginx this is the client IP only with server.forward-headers-strategy=framework.
        LoginResponseDto response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponseDto.success("Login exitoso", response));
    }

    @Operation(
            summary = "Renovar la sesión",
            description = """
                    Cambia el `refreshToken` vigente por un par nuevo (`accessToken` + `refreshToken`).

                    Usar cuando un endpoint autenticado responda 401. El `refreshToken` usado deja de servir:
                    guardar siempre el nuevo. Si un `refreshToken` ya usado vuelve a llegar, se asume robado
                    y se cierran todas las sesiones del usuario.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión renovada"),
            @ApiResponse(responseCode = "401", description = "refreshToken inválido, ya usado, revocado o expirado. Hacer login de nuevo",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        LoginResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponseDto.success("Sesión renovada", response));
    }

    @Operation(
            summary = "Cerrar sesión",
            description = """
                    Cierra la sesión del `refreshToken` enviado en el body. Las sesiones del usuario
                    en otros dispositivos siguen abiertas.

                    El `accessToken` de esta sesión deja de renovarse y expira solo en máximo 30 minutos;
                    el frontend debe descartarlo al cerrar sesión. Responde 200 aunque el token ya no sea válido.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Sesión cerrada")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponseDto.success("Sesión cerrada", null));
    }

    @Operation(
            summary = "Activar cuenta",
            description = """
                    Para cuentas creadas con contraseña temporal (por ejemplo, por un administrador):
                    valida la contraseña temporal y establece la definitiva. Después, hacer login.

                    - `currentPassword`: la contraseña temporal recibida
                    - `newPassword`: la contraseña definitiva
                    - Cuenta con los mismos límites de intentos que el login
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta activada"),
            @ApiResponse(responseCode = "400", description = "Cuenta ya activa (ACCOUNT_ALREADY_ACTIVE) o nueva contraseña inválida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Email o contraseña temporal incorrectos",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Cuenta bloqueada o desactivada",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos desde esta IP",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/activate")
    public ResponseEntity<ApiResponseDto<Void>> activateAccount(
            @Valid @RequestBody ActivateAccountRequestDto request,
            HttpServletRequest httpRequest) {
        authService.activateAccount(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponseDto.success("Cuenta activada exitosamente", null));
    }

    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = """
                    Si el email está registrado y activo, envía a ese correo un enlace para restablecer
                    la contraseña. El enlace expira en **15 minutos** y solo funciona el último solicitado.

                    **Seguridad:** la respuesta es siempre la misma, exista o no el email, para no revelar
                    qué emails están registrados. El token nunca viaja en esta respuesta: solo por correo.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud recibida (siempre, exista o no el email)"),
            @ApiResponse(responseCode = "400", description = "Email con formato inválido",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDto<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponseDto.success(
                "Si el email está registrado, te enviamos un enlace para restablecer la contraseña", null));
    }

    @Operation(
            summary = "Restablecer contraseña",
            description = """
                    Establece una nueva contraseña con el token del enlace recibido por correo.

                    - El enlace es de un solo uso y expira a los 15 minutos
                    - Desbloquea la cuenta si estaba bloqueada por intentos fallidos
                    - Cierra todas las sesiones abiertas: después hay que hacer login con la nueva contraseña
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "Enlace inválido, usado o expirado (INVALID_RESET_TOKEN), o contraseña inválida",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDto<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponseDto.success("Contraseña actualizada exitosamente", null));
    }
}