package com.guyacevedo.factusimple.features.auth.dto;

import com.guyacevedo.factusimple.features.user.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de login exitoso con tokens JWT y datos del usuario")
public record LoginResponseDto(

    @Schema(description = """
            Token de acceso JWT. Usar en el header Authorization de todas las llamadas autenticadas:
            `Authorization: Bearer {accessToken}`
            Expira según configuración del servidor (ver expiresIn).
            """,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken,

    @Schema(description = """
            Token de renovación. Usar en POST /auth/refresh para obtener un nuevo accessToken sin re-login.
            Tiene vida útil de 30 días. Si expira, realizar login nuevamente.
            """,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...")
    String refreshToken,

    @Schema(description = "Segundos hasta que el accessToken expire. Renovar antes de este tiempo con POST /auth/refresh.",
            example = "86400")
    Long expiresIn,

    @Schema(description = "Datos del usuario autenticado incluyendo plan, contadores de uso y estado")
    UserResponseDto user
) {}
