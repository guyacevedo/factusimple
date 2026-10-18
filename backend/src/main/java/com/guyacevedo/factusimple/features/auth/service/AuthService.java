package com.guyacevedo.factusimple.features.auth.service;

import com.guyacevedo.factusimple.features.auth.dto.ActivateAccountRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.EstablishmentRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.ForgotPasswordRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.LoginRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.LoginResponseDto;
import com.guyacevedo.factusimple.features.auth.dto.RefreshTokenRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.RegisterRequestDto;
import com.guyacevedo.factusimple.features.auth.dto.ResetPasswordRequestDto;
import com.guyacevedo.factusimple.features.auth.entity.ResetPassword;
import com.guyacevedo.factusimple.features.auth.repository.ResetPasswordRepository;
import com.guyacevedo.factusimple.features.plan.entity.Plan;
import com.guyacevedo.factusimple.features.plan.repository.PlanRepository;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.features.user.entity.UserRole;
import com.guyacevedo.factusimple.features.user.mapper.UserMapper;
import com.guyacevedo.factusimple.features.user.repository.UserRepository;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import com.guyacevedo.factusimple.shared.exception.ConflictException;
import com.guyacevedo.factusimple.shared.exception.ForbiddenException;
import com.guyacevedo.factusimple.shared.exception.ResourceNotFoundException;
import com.guyacevedo.factusimple.shared.exception.TooManyRequestsException;
import com.guyacevedo.factusimple.shared.exception.UnauthorizedException;
import com.guyacevedo.factusimple.shared.util.Hashing;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Account and session flows. Sessions (JWT, refresh, revocation) live in
 * TokenService; login attempts and the lock live in LoginAttemptRecorder.
 * This class decides who may get a session.
 *
 * Factus is not called here: logging in to FactuSimple must not depend on
 * Factus being up. The Factus token is obtained when it is needed (sending an
 * invoice), by the Factus integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);
    private static final String INVALID_CREDENTIALS = "Email o contraseña incorrectos";
    private static final String INVALID_RESET_TOKEN = "El enlace de recuperación no es válido o ya expiró";

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final ResetPasswordRepository resetPasswordRepository;
    private final LoginAttemptRecorder loginAttemptRecorder;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final PasswordResetMailer passwordResetMailer;

    private final SecureRandom secureRandom = new SecureRandom();

    // Compared against when the email does not exist, so a login takes the same
    // time either way and the response time does not reveal registered emails.
    private String dummyPasswordHash;

    @PostConstruct
    void initDummyHash() {
        dummyPasswordHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @Transactional
    public LoginResponseDto register(RegisterRequestDto request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("El email ya está registrado", "EMAIL_ALREADY_REGISTERED");
        }

        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.planId()));
        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new BadRequestException("Este plan no está disponible");
        }
        checkInviteCode(plan, request.inviteCode());

        EstablishmentRequestDto establishment = request.establishment();
        User user = userRepository.save(User.builder()
                .plan(plan)
                .role(UserRole.ESTABLISHMENT)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(request.phone())
                .companyName(establishment.companyName().trim())
                .nit(establishment.nit())
                .dv(establishment.dv())
                .legalOrgCode(establishment.legalOrgCode())
                .tributeCode(establishment.tributeCode())
                .fiscalResponsibility(establishment.fiscalResponsibility())
                .address(establishment.address())
                .municipalityCode(establishment.municipalityCode())
                .build());

        log.info("Usuario registrado: id={}", user.getId());
        return buildLoginResponse(user, tokenService.issueTokens(user));
    }

    // ── Login ────────────────────────────────────────────────────────────────

    /**
     * @param ip client IP (request.getRemoteAddr()), for the per-IP limit.
     *
     * Not @Transactional on purpose. LoginAttemptRecorder writes in REQUIRES_NEW;
     * inside an outer transaction every failed login would hold two pool
     * connections at once, and as many simultaneous failed logins as the pool
     * size would block each other until timeout. Here each step (reads, the
     * recorder, issueTokens) uses its own short transaction, one connection at a time.
     */
    public LoginResponseDto login(LoginRequestDto request, String ip) {
        User user = authenticate(request.email(), request.password(), ip);

        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new ForbiddenException(
                    "Debes activar tu cuenta y definir tu contraseña antes de iniciar sesión",
                    "PASSWORD_CHANGE_REQUIRED");
        }

        log.info("Login: user id={}", user.getId());
        return buildLoginResponse(user, tokenService.issueTokens(user));
    }

    // ── Session ──────────────────────────────────────────────────────────────

    // noRollbackFor: a reused (stolen) refresh token closes every session of the
    // user and then throws; that revocation must be committed.
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResponseDto refreshToken(RefreshTokenRequestDto request) {
        TokenService.IssuedTokens tokens = tokenService.refresh(request.refreshToken());
        User user = userRepository.findById(tokens.userId())
                .orElseThrow(() -> new UnauthorizedException("Sesión inválida o expirada. Inicia sesión de nuevo"));
        return buildLoginResponse(user, tokens);
    }

    /** Ends this session only; the user's other devices stay logged in. */
    @Transactional
    public void logout(String refreshToken) {
        tokenService.logout(refreshToken);
    }

    // ── Account activation ───────────────────────────────────────────────────

    /**
     * For accounts created with a temporary password (must_change_password = true).
     * It does not touch is_active: a disabled account stays disabled, so a user
     * the admin turned off cannot turn themselves back on through this endpoint.
     *
     * Not @Transactional, for the same reason as login(). The password is written
     * with a targeted UPDATE, which has its own transaction.
     */
    public void activateAccount(ActivateAccountRequestDto request, String ip) {
        User user = authenticate(request.email(), request.currentPassword(), ip);

        if (!Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new BadRequestException("La cuenta ya está activa", "ACCOUNT_ALREADY_ACTIVE");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("La nueva contraseña no puede ser igual a la actual");
        }

        userRepository.updatePasswordAndClearMustChange(user.getId(), passwordEncoder.encode(request.newPassword()));
        log.info("Cuenta activada: user id={}", user.getId());
    }

    // ── Password recovery ────────────────────────────────────────────────────

    /**
     * Always ends the same way, whether or not the email exists, so it cannot be
     * used to find out which emails are registered. The token goes only by email.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(user -> Boolean.TRUE.equals(user.getActive()));
        if (found.isEmpty()) {
            return;
        }

        User user = found.get();
        resetPasswordRepository.invalidateAllByUser(user);

        String rawToken = generateSecureToken();
        resetPasswordRepository.save(ResetPassword.builder()
                .user(user)
                .tokenHash(Hashing.sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().plus(RESET_TOKEN_TTL))
                .build());

        try {
            passwordResetMailer.send(user, rawToken);
        } catch (RuntimeException e) {
            // Same response for the client; the failure is for us to see.
            log.error("No se pudo enviar el correo de recuperación a user id={}", user.getId(), e);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadRequestException(INVALID_RESET_TOKEN, "INVALID_RESET_TOKEN"));

        ResetPassword reset = resetPasswordRepository
                .findByUserAndTokenHash(user, Hashing.sha256Hex(request.resetToken()))
                .filter(ResetPassword::isCurrent)
                .orElseThrow(() -> new BadRequestException(INVALID_RESET_TOKEN, "INVALID_RESET_TOKEN"));

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("La nueva contraseña no puede ser igual a la actual");
        }

        // Conditional UPDATE: of two simultaneous requests with the same link, only one gets 1.
        if (resetPasswordRepository.markUsed(reset.getId()) == 0) {
            throw new BadRequestException(INVALID_RESET_TOKEN, "INVALID_RESET_TOKEN");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        // Whoever proves they own the email gets back in, even if the account was locked.
        user.setFailedLoginAttempts(0);
        user.setBlockedAt(null);
        // Sessions opened with the old password stop working.
        tokenService.revokeAllSessions(user);

        log.info("Contraseña restablecida: user id={}", user.getId());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Shared by login and activation, so both count failed attempts and respect
     * the locks. Every failure path answers the same message, except the lock:
     * the real owner needs to know why they cannot get in, and an attacker
     * gains nothing because they cannot keep trying anyway. The account state
     * (disabled) is only revealed to someone who already knows the password.
     */
    private User authenticate(String rawEmail, String rawPassword, String ip) {
        String email = normalizeEmail(rawEmail);

        if (loginAttemptRecorder.tooManyFailuresFromIp(ip)) {
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos desde tu conexión. Intenta de nuevo en unos minutos",
                    "TOO_MANY_LOGIN_ATTEMPTS");
        }

        Optional<User> found = userRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            loginAttemptRecorder.recordRejected(email, ip);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        User user = found.get();
        if (user.isLocked()) {
            loginAttemptRecorder.recordRejected(email, ip);
            throw new ForbiddenException(
                    "Cuenta bloqueada por demasiados intentos fallidos. Intenta de nuevo en "
                            + User.LOCK_DURATION.toMinutes() + " minutos",
                    "ACCOUNT_LOCKED");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            loginAttemptRecorder.recordFailure(user.getId(), email, ip);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            loginAttemptRecorder.recordRejected(email, ip);
            throw new ForbiddenException("Tu cuenta está desactivada", "ACCOUNT_DISABLED");
        }

        loginAttemptRecorder.recordSuccess(user.getId(), email, ip);
        return user;
    }

    private static void checkInviteCode(Plan plan, String inviteCode) {
        if (!Boolean.TRUE.equals(plan.getRequiredInvitedCode())) {
            return;
        }
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new BadRequestException("Este plan requiere un código de invitación");
        }
        if (!inviteCode.trim().equals(plan.getCode())) {
            throw new BadRequestException("El código de invitación es incorrecto");
        }
    }

    private LoginResponseDto buildLoginResponse(User user, TokenService.IssuedTokens tokens) {
        return new LoginResponseDto(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresInSeconds(),
                userMapper.toDto(user));
    }

    // Emails are stored lowercase, so the unique LOWER(email) index and the
    // stored value always agree.
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}