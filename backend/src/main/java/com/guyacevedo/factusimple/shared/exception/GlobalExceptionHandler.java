package com.guyacevedo.factusimple.shared.exception;

import com.guyacevedo.factusimple.shared.dto.ApiResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Every error leaves the API as an ApiResponseDto.
 *
 * Extends ResponseEntityExceptionHandler so Spring MVC's own errors keep their
 * real status (404 unknown route, 405 wrong method, 415 wrong content type,
 * 400 bad parameter...) instead of falling into the catch-all as a 500.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ── Application errors ───────────────────────────────────────────────────

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponseDto<?>> handleApiException(ApiException ex) {
        HttpStatus status = Objects.requireNonNullElse(
                HttpStatus.resolve(ex.getStatusCode()), HttpStatus.INTERNAL_SERVER_ERROR);
        if (status.is5xxServerError()) {
            log.error("Error API {}: {}", status.value(), ex.getMessage(), ex);
        } else {
            log.warn("Error API {}: {}", status.value(), ex.getMessage());
        }
        return error(status, ex.getMessage(), ex.getErrorCode());
    }

    // ── Security ─────────────────────────────────────────────────────────────

    // @PreAuthorize throws AccessDeniedException for anonymous users too.
    // They get 401 so the frontend refreshes the token or sends them to login;
    // only a logged-in user without the role gets 403.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDto<?>> handleAccessDenied(AccessDeniedException ex) {
        if (isAnonymous()) {
            return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para continuar", "UNAUTHORIZED");
        }
        log.warn("Acceso denegado: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción", "ACCESS_DENIED");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponseDto<?>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Credenciales inválidas");
        return error(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos", "INVALID_CREDENTIALS");
    }

    // Raised by AuthenticationManager when CustomUserDetails is locked (blocked_at set).
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponseDto<?>> handleLocked(LockedException ex) {
        log.warn("Login de cuenta bloqueada");
        return error(HttpStatus.FORBIDDEN,
                "Tu cuenta está bloqueada por intentos fallidos. Intenta más tarde o contacta al administrador",
                "ACCOUNT_LOCKED");
    }

    // Raised by AuthenticationManager when CustomUserDetails is disabled (is_active = false).
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponseDto<?>> handleDisabled(DisabledException ex) {
        log.warn("Login de cuenta desactivada");
        return error(HttpStatus.FORBIDDEN, "Tu cuenta está desactivada", "ACCOUNT_DISABLED");
    }

    // Raised by SecurityConfig's entry point: protected route with no token,
    // or with an expired or invalid one. 401 tells the frontend to refresh or log in.
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ApiResponseDto<?>> handleInsufficientAuthentication(InsufficientAuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para continuar", "UNAUTHORIZED");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseDto<?>> handleAuthentication(AuthenticationException ex) {
        log.warn("Autenticación fallida: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "No fue posible autenticarte", "AUTHENTICATION_FAILED");
    }

    // ── Validation ───────────────────────────────────────────────────────────

    // @Valid @RequestBody
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> addError(errors, e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> addError(errors, e.getObjectName(), e.getDefaultMessage()));
        log.warn("Error de validación: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponseDto.error("Errores de validación", "VALIDATION_ERROR", errors));
    }

    // Constraints on @RequestParam / @PathVariable with @Validated
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDto<?>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, List<String>> errors = violations(ex);
        log.warn("Error de validación: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponseDto.error("Errores de validación", "VALIDATION_ERROR", errors));
    }

    // Entity constraints (@NotBlank on entities) are checked at flush/commit,
    // so they arrive wrapped in a TransactionSystemException.
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponseDto<?>> handleTransactionSystem(TransactionSystemException ex) {
        if (ex.getMostSpecificCause() instanceof ConstraintViolationException violation) {
            return handleConstraintViolation(violation);
        }
        return handleUnexpected(ex);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("Cuerpo de solicitud inválido: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponseDto.error("El cuerpo de la solicitud tiene formato inválido", "BAD_REQUEST_BODY"));
    }

    // e.g. /v1/plans/abc where a UUID is expected
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String param = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        log.warn("Parámetro con formato inválido: {} = {}", param, ex.getValue());
        return ResponseEntity.badRequest()
                .body(ApiResponseDto.error("El parámetro '" + param + "' tiene un formato inválido", "INVALID_PARAMETER"));
    }

    // ── Database ─────────────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDto<?>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String sqlState = sqlState(ex);
        if ("23505".equals(sqlState)) { // unique_violation
            log.warn("Registro duplicado: {}", ex.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT, "Ya existe un registro con esos datos", "DUPLICATE_RESOURCE");
        }
        if ("23503".equals(sqlState)) { // foreign_key_violation
            log.warn("Violación de llave foránea: {}", ex.getMostSpecificCause().getMessage());
            return error(HttpStatus.CONFLICT,
                    "La operación no es posible porque hay datos relacionados", "RELATED_DATA_CONFLICT");
        }
        // NOT NULL (23502) or CHECK (23514): data the application should have
        // validated before reaching the database. Logged as error to fix it.
        log.error("Restricción de BD no validada en la aplicación (SQLState {})", sqlState, ex);
        return error(HttpStatus.BAD_REQUEST,
                "Los datos enviados no cumplen las reglas del sistema", "DATA_CONSTRAINT_VIOLATION");
    }

    // ── Spring MVC errors without a specific handler above ───────────────────
    // 404 unknown route, 405 wrong method, 415 wrong content type, missing parameter...

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = Objects.requireNonNullElse(
                HttpStatus.resolve(statusCode.value()), HttpStatus.INTERNAL_SERVER_ERROR);
        if (status.is5xxServerError()) {
            log.error("Error de Spring MVC {}", status.value(), ex);
        } else {
            log.warn("{} {}: {}", status.value(), ex.getClass().getSimpleName(), ex.getMessage());
        }
        return ResponseEntity.status(status).headers(headers)
                .body(ApiResponseDto.error(messageFor(status), status.name()));
    }

    // ── Anything else is a bug: 500 without internal details ─────────────────
    // There is no IllegalArgumentException handler on purpose: those come from
    // bugs or bad data in our own code, and should not reach the client as a 400.

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<?>> handleUnexpected(Exception ex) {
        log.error("Error inesperado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", "INTERNAL_SERVER_ERROR");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static ResponseEntity<ApiResponseDto<?>> error(HttpStatus status, String message, String errorCode) {
        return ResponseEntity.status(status).body(ApiResponseDto.error(message, errorCode));
    }

    private static boolean isAnonymous() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken;
    }

    private static void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
    }

    // "changePlan.request.planId" -> "planId"
    private static Map<String, List<String>> violations(ConstraintViolationException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = null;
            for (Path.Node node : violation.getPropertyPath()) {
                field = node.getName();
            }
            addError(errors, field != null ? field : "_global", violation.getMessage());
        }
        return errors;
    }

    private static String sqlState(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
    }

    private static String messageFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido para este recurso";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado. Usa application/json";
            case NOT_ACCEPTABLE -> "Formato de respuesta no disponible";
            case PAYLOAD_TOO_LARGE -> "La solicitud es demasiado grande";
            case BAD_REQUEST -> "Solicitud inválida";
            default -> status.is5xxServerError()
                    ? "Error interno del servidor"
                    : "No fue posible procesar la solicitud";
        };
    }
}