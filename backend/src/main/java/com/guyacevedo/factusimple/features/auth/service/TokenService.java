package com.guyacevedo.factusimple.features.auth.service;

import com.guyacevedo.factusimple.config.security.JwtTokenProvider;
import com.guyacevedo.factusimple.features.auth.entity.Token;
import com.guyacevedo.factusimple.features.auth.entity.TokenType;
import com.guyacevedo.factusimple.features.auth.repository.TokenRepository;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.shared.exception.UnauthorizedException;
import com.guyacevedo.factusimple.shared.util.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Sessions: issues, rotates and revokes the application's tokens.
 *
 * - Access tokens are not stored: they are short (30 min) and the JWT filter
 *   only checks their signature, type and expiry.
 * - Refresh tokens are stored as a SHA-256 hash: a copy of the database
 *   does not give anyone a working token.
 * - Each refresh revokes the token used and issues a new pair (rotation).
 *   If a revoked refresh token comes back, it was probably stolen, so every
 *   session of that user is closed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private static final String INVALID_SESSION = "Sesión inválida o expirada. Inicia sesión de nuevo";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRepository tokenRepository;

    /** Tokens for LoginResponseDto. userId lets the caller load the user for the response. */
    public record IssuedTokens(UUID userId, String accessToken, String refreshToken, long expiresInSeconds) {}

    /** Login, account activation: a new session for the user. */
    @Transactional
    public IssuedTokens issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        LocalDateTime expiresAt = jwtTokenProvider.parseRefreshToken(refreshToken)
                .map(claims -> LocalDateTime.ofInstant(claims.expiresAt(), ZoneId.systemDefault()))
                .orElseThrow(() -> new IllegalStateException("No se pudo leer el refresh token recién creado"));

        tokenRepository.save(Token.builder()
                .user(user)
                .tokenType(TokenType.REFRESH)
                .tokenValue(Hashing.sha256Hex(refreshToken))
                .expiresAt(expiresAt)
                .build());

        return new IssuedTokens(user.getId(), accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds());
    }

    /**
     * POST /auth/refresh. noRollbackFor keeps the "close every session" revocation
     * when a reused token is detected, even though an exception is thrown after it.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public IssuedTokens refresh(String refreshToken) {
        if (jwtTokenProvider.parseRefreshToken(refreshToken).isEmpty()) {
            throw new UnauthorizedException(INVALID_SESSION);
        }

        Token stored = tokenRepository.findByTokenValue(Hashing.sha256Hex(refreshToken))
                .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));
        User user = stored.getUser();

        if (Boolean.TRUE.equals(stored.getRevoked())) {
            int closed = tokenRepository.revokeLocalTokensByUser(user);
            log.warn("Refresh token revocado reutilizado para user id={}. Sesiones cerradas: {}", user.getId(), closed);
            throw new UnauthorizedException(INVALID_SESSION);
        }
        if (stored.isExpired()) {
            throw new UnauthorizedException(INVALID_SESSION);
        }
        // Only a disabled account ends the session here. The login lock (blocked_at)
        // stops password guessing; it must not close a session that is already open,
        // or anyone could log a user out by typing wrong passwords for their email.
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("Tu cuenta está desactivada");
        }

        stored.setRevoked(true);
        return issueTokens(user);
    }

    /**
     * Logout of this session. The row is deleted, not revoked: "revoked" is kept
     * only for rotated tokens, where a reuse means theft. If it were revoked, a
     * refresh already in flight when the user logs out would look like theft and
     * close the user's sessions on their other devices too.
     * Unknown tokens are ignored.
     */
    @Transactional
    public void logout(String refreshToken) {
        tokenRepository.findByTokenValue(Hashing.sha256Hex(refreshToken))
                .ifPresent(tokenRepository::delete);
    }

    /**
     * Password change or reset: every session ends. Deleted for the same reason
     * as logout, so an old device retrying does not close the new session.
     * Open access tokens expire on their own (30 min).
     */
    @Transactional
    public void revokeAllSessions(User user) {
        tokenRepository.deleteLocalTokensByUser(user);
    }
}