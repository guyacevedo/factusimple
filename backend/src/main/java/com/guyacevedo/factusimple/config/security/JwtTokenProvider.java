package com.guyacevedo.factusimple.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates and verifies the application's own JWTs.
 *
 * Every token carries a "type" claim, and each check only accepts its own type:
 * a 30-day refresh token is rejected in the Authorization header, and a
 * short-lived access token cannot be used to refresh.
 *
 * Every token also carries a random id (jti), so two logins in the same second
 * never produce the same string (tokens.value is unique).
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private static final String ISSUER = "factusimple";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final int MIN_SECRET_BYTES = 32;

    private final Algorithm algorithm;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    /** What the JWT filter and the refresh flow need from a verified token. */
    public record TokenClaims(UUID userId, String email, Instant expiresAt) {}

    public JwtTokenProvider(
            // Empty default so a missing secret fails with the message below, not a placeholder error.
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.access-token-minutes:30}") long accessTokenMinutes,
            @Value("${app.jwt.refresh-token-days:30}") long refreshTokenDays) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret no configurado. Define app.jwt.secret (por ejemplo con la variable JWT_SECRET)");
        }
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret debe tener mínimo " + MIN_SECRET_BYTES + " bytes. Actual: " + key.length);
        }
        this.algorithm = Algorithm.HMAC256(key);
        this.accessTokenTtl = Duration.ofMinutes(accessTokenMinutes);
        this.refreshTokenTtl = Duration.ofDays(refreshTokenDays);
        log.info("JWT configurado: access token {} min, refresh token {} días", accessTokenMinutes, refreshTokenDays);
    }

    public String generateAccessToken(UUID userId, String email) {
        return generate(userId, email, TYPE_ACCESS, accessTokenTtl);
    }

    public String generateRefreshToken(UUID userId, String email) {
        return generate(userId, email, TYPE_REFRESH, refreshTokenTtl);
    }

    /** For the JWT filter: only access tokens are valid in the Authorization header. */
    public Optional<TokenClaims> parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    /** For POST /auth/refresh: only refresh tokens can be exchanged. */
    public Optional<TokenClaims> parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    /** Value for LoginResponseDto.expiresIn. */
    public long getAccessTokenExpiresInSeconds() {
        return accessTokenTtl.toSeconds();
    }

    private String generate(UUID userId, String email, String type, Duration ttl) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(email)
                .withClaim(CLAIM_USER_ID, userId.toString())
                .withClaim(CLAIM_TYPE, type)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(now)
                .withExpiresAt(now.plus(ttl))
                .sign(algorithm);
    }

    private Optional<TokenClaims> parse(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withClaim(CLAIM_TYPE, expectedType)
                    .build()
                    .verify(token);
            return Optional.of(new TokenClaims(
                    UUID.fromString(jwt.getClaim(CLAIM_USER_ID).asString()),
                    jwt.getSubject(),
                    jwt.getExpiresAtAsInstant()));
        } catch (JWTVerificationException | IllegalArgumentException | NullPointerException e) {
            log.debug("JWT rechazado ({}): {}", expectedType, e.getMessage());
            return Optional.empty();
        }
    }
}