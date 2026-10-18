package com.guyacevedo.factusimple.features.auth.service;

import com.guyacevedo.factusimple.features.auth.repository.LoginAttemptRepository;
import com.guyacevedo.factusimple.features.auth.repository.ResetPasswordRepository;
import com.guyacevedo.factusimple.features.auth.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Deletes expired auth data once a day, so the tables do not grow forever.
 *
 * Runs at night (Colombia time), when there is no traffic. Missing a day does
 * not matter: the next run deletes both days.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredAuthDataCleanup {

    // Kept for a while to investigate attacks; then they are only clutter.
    private static final Duration LOGIN_ATTEMPTS_RETENTION = Duration.ofDays(30);

    private final TokenRepository tokenRepository;
    private final ResetPasswordRepository resetPasswordRepository;
    private final LoginAttemptRepository loginAttemptRepository;

    @Scheduled(cron = "0 20 3 * * *", zone = "America/Bogota")
    @Transactional
    public void purge() {
        LocalDateTime now = LocalDateTime.now();
        int tokens = tokenRepository.deleteExpired(now);
        int resets = resetPasswordRepository.deleteExpired(now);
        int attempts = loginAttemptRepository.deleteOlderThan(now.minus(LOGIN_ATTEMPTS_RETENTION));
        if (tokens + resets + attempts > 0) {
            log.info("Limpieza de auth: {} tokens, {} enlaces de recuperación, {} intentos de login eliminados",
                    tokens, resets, attempts);
        }
    }
}