package com.guyacevedo.factusimple.features.auth.service;

import com.guyacevedo.factusimple.features.auth.entity.LoginAttempt;
import com.guyacevedo.factusimple.features.auth.repository.LoginAttemptRepository;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What must be written about a login attempt even when the attempt fails.
 *
 * Each method runs in its own transaction (REQUIRES_NEW): the login throws on
 * failure, and in the same transaction the exception would roll back the
 * counter and the lock would never trigger. It is a separate bean because
 * REQUIRES_NEW only works through the proxy, not on calls inside AuthService.
 *
 * Counter and lock are conditional UPDATEs in the database, not read-then-save,
 * so parallel attempts cannot slip between the read and the write.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptRecorder {

    static final int MAX_FAILED_PER_IP = 50;
    static final Duration IP_WINDOW = Duration.ofMinutes(15);

    private final LoginAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean tooManyFailuresFromIp(String ip) {
        if (ip == null) {
            return false;
        }
        LocalDateTime since = LocalDateTime.now().minus(IP_WINDOW);
        return attemptRepository.countFailedByIpSince(ip, since) >= MAX_FAILED_PER_IP;
    }

    /** Attempt that does not change the counter (unknown email, locked or disabled account). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejected(String username, String ip) {
        save(username, false, ip);
    }

    /** Wrong password: records it, raises the counter and locks at the threshold. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId, String username, String ip) {
        save(username, false, ip);

        LocalDateTime now = LocalDateTime.now();
        userRepository.registerFailedLogin(userId, now.minus(User.LOCK_DURATION));
        if (userRepository.lockIfThresholdReached(userId, User.MAX_FAILED_LOGIN_ATTEMPTS, now) > 0) {
            log.warn("Cuenta bloqueada {} min por intentos fallidos: user id={}",
                    User.LOCK_DURATION.toMinutes(), userId);
        }
    }

    /** Correct password: old failures stop counting, so they cannot lock out the real owner later. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId, String username, String ip) {
        save(username, true, ip);
        userRepository.resetFailedLogins(userId);
    }

    private void save(String username, boolean successful, String ip) {
        attemptRepository.save(LoginAttempt.builder()
                .username(username)
                .successful(successful)
                .ip(ip)
                .build());
    }
}