package com.guyacevedo.factusimple.features.auth.service;

import com.guyacevedo.factusimple.features.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Development only: writes the reset link to the log instead of sending it.
 * Active with the "dev" or "local" profile, never in production.
 */
@Component
@Profile({"dev", "local"})
@Slf4j
public class LoggingPasswordResetMailer implements PasswordResetMailer {

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void send(User user, String rawToken) {
        String link = frontendUrl + "/reset-password"
                + "?email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8)
                + "&token=" + rawToken;
        log.warn("[DEV] Enlace de recuperación de contraseña para user id={}: {}", user.getId(), link);
    }
}