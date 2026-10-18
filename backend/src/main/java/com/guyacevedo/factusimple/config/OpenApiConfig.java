package com.guyacevedo.factusimple.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI(@Value("${app.openapi.server-url:}") String serverUrl) {
        OpenAPI openApi = new OpenAPI()
                .info(new Info()
                        .title("FactuSimple API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Guy Acevedo")
                                .email("guy.acevedoa@gmail.com"))
                        .description(DESCRIPTION))
                // Every operation needs the token unless publicEndpointsCustomizer() removes it.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("accessToken obtenido de `POST /auth/login`. Swagger lo envía como `Authorization: Bearer {token}`.")));

        // Without a fixed URL, springdoc uses the host, port and context path of the
        // request that opened Swagger, so "Try it out" works locally and behind Nginx
        // (with server.forward-headers-strategy=framework). Set app.openapi.server-url
        // only if a proxy rewrites the path in a way springdoc cannot see.
        if (!serverUrl.isBlank()) {
            openApi.servers(List.of(new Server().url(serverUrl)));
        }
        return openApi;
    }

    /**
     * Marks as "no token" exactly the routes SecurityConfig leaves public, so the
     * documentation and the real security cannot drift apart.
     */
    @Bean
    public OpenApiCustomizer publicEndpointsCustomizer() {
        PathPatternParser parser = new PathPatternParser();
        List<PathPattern> publicAnyMethod = Arrays.stream(SecurityConfig.PUBLIC_ENDPOINTS).map(parser::parse).toList();
        List<PathPattern> publicGet = Arrays.stream(SecurityConfig.PUBLIC_GET_ENDPOINTS).map(parser::parse).toList();

        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> {
                PathContainer container = PathContainer.parsePath(path);
                boolean anyMethodPublic = publicAnyMethod.stream().anyMatch(p -> p.matches(container));
                boolean getPublic = publicGet.stream().anyMatch(p -> p.matches(container));

                item.readOperationsMap().forEach((method, operation) -> {
                    if (anyMethodPublic || (getPublic && method == PathItem.HttpMethod.GET)) {
                        // An empty list overrides the global requirement: no lock in Swagger UI.
                        operation.setSecurity(List.of());
                    }
                });
            });
        };
    }

    private static final String DESCRIPTION = """
            API REST para facturación electrónica DIAN (Colombia) a través de Factus.
            Gestiona facturas, notas crédito, clientes y productos de cada establecimiento.
 
            ## Módulos
            - **auth** — registro, login, renovación de sesión, activación y recuperación de contraseña
            - **users** — cuenta del usuario, datos fiscales del establecimiento y cambio de plan
            - **plans** — planes de suscripción con límites de uso
            - **customers** — clientes del establecimiento
            - **products** — catálogo de productos con control de stock
            - **invoices** — facturas electrónicas DIAN
            - **credit-notes** — notas crédito asociadas a facturas validadas
            - **factus-codes** — códigos de referencia Factus (tipos de documento, impuestos, formas de pago)
            - **numbering-ranges** — rangos de numeración activos del establecimiento
 
            ## Autenticación
            Las rutas sin candado en esta documentación son públicas. El resto requieren:
````
            Authorization: Bearer <accessToken>
````
            - `POST /auth/login` devuelve `accessToken` (30 minutos) y `refreshToken` (30 días, un solo uso).
            - Ante un 401, renovar con `POST /auth/refresh` y guardar el par nuevo.
            - `POST /auth/logout` recibe el `refreshToken` en el body y cierra solo esa sesión.
 
            ## Roles
            | Rol | Acceso |
            |-----|--------|
            | **ADMIN** | Todo: usuarios, planes (incluidos los inactivos) |
            | **ESTABLISHMENT** | Sus propios recursos; puede cambiar su plan y su contraseña |
 
            ## Flujo principal de facturación
            1. `POST /auth/register` — crear la cuenta con los datos del establecimiento (devuelve la sesión iniciada)
            2. `GET /v1/numbering-ranges/sale` — obtener el rango de numeración activo
            3. `POST /v1/customers` — crear el cliente receptor
            4. `POST /v1/products` — crear los productos o servicios
            5. `POST /v1/invoices` — crear la factura en estado **PENDING**
            6. `POST /v1/invoices/{id}/sync` — enviar a Factus → **VALIDATED** con CUFE asignado
            7. `GET /v1/invoices/{id}/pdf` — descargar el PDF de la factura validada
 
            Las cuentas creadas con contraseña temporal deben pasar antes por `POST /auth/activate`.
 
            ## Estados de factura
````
            PENDING   → VALIDATED  (sync exitoso con Factus)
            PENDING   → ERROR      (fallo en el sync; reintentar con /sync)
            PENDING   → CANCELLED  (cancelada antes de validar)
            VALIDATED → solo se anula con una nota crédito
````
 
            ## Estructura de respuesta
            Todas las respuestas usan el envelope `ApiResponseDto`:
```json
            {
              "success": true,
              "message": "Mensaje descriptivo",
              "errorCode": null,
              "data": { },
              "timestamp": "2026-01-15T10:30:00Z"
            }
```
            En errores: `success=false`, `data=null` (o el detalle de validación) y `errorCode` con el código.
 
            ## Códigos de error
            | HTTP | errorCode | Causa |
            |------|-----------|-------|
            | 400 | VALIDATION_ERROR | Campos inválidos; `data` trae los errores por campo |
            | 400 | BAD_REQUEST_BODY / INVALID_PARAMETER | JSON mal formado o parámetro con formato inválido (ej. UUID) |
            | 401 | UNAUTHORIZED | Sin token, token expirado o inválido: renovar o iniciar sesión |
            | 401 | INVALID_CREDENTIALS | Email o contraseña incorrectos |
            | 403 | ACCESS_DENIED | El rol no tiene permiso para esta acción |
            | 403 | ACCOUNT_LOCKED / ACCOUNT_DISABLED / PASSWORD_CHANGE_REQUIRED | Estado de la cuenta impide el login |
            | 404 | RESOURCE_NOT_FOUND / NOT_FOUND | El recurso o la ruta no existe, o no pertenece al usuario |
            | 405 | METHOD_NOT_ALLOWED | Método HTTP no soportado en esa ruta |
            | 409 | CONFLICT / DUPLICATE_RESOURCE / EMAIL_ALREADY_REGISTERED | Duplicado o estado incompatible |
            | 422 | UNPROCESSABLE_ENTITY | Regla de negocio, por ejemplo stock insuficiente |
            | 429 | TOO_MANY_LOGIN_ATTEMPTS | Demasiados logins fallidos desde la misma IP |
            | 500 | INTERNAL_SERVER_ERROR | Error interno no controlado |
 
            ## Límites del plan
            Cada usuario tiene un plan con `maxProducts`, `maxCustomers` y `maxInvoices`.
            Los contadores se actualizan de forma atómica, así que dos peticiones simultáneas no pueden superar el límite.
            """;
}