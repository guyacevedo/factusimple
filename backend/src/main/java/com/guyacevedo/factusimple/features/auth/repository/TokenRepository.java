package com.guyacevedo.factusimple.features.auth.repository;

import com.guyacevedo.factusimple.features.auth.entity.Token;
import com.guyacevedo.factusimple.features.auth.entity.TokenType;
import com.guyacevedo.factusimple.features.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    // The entity field is tokenValue (column "value"). For REFRESH tokens it holds
    // the SHA-256 hash, never the token itself (see TokenService).
    @EntityGraph(attributePaths = "user")
    Optional<Token> findByTokenValue(String tokenValue);

    // Current valid token of a type, e.g. the user's Factus access token.
    Optional<Token> findFirstByUserAndTokenTypeAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            User user, TokenType tokenType, LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Token t SET t.revoked = true
            WHERE t.user = :user AND t.revoked = false AND t.tokenType IN :types
            """)
    int revokeByUserAndTypes(@Param("user") User user, @Param("types") Collection<TokenType> types);

    /** Closes every session of the user in FactuSimple (logout everywhere, password change). */
    default int revokeLocalTokensByUser(User user) {
        return revokeByUserAndTypes(user, EnumSet.of(TokenType.ACCESS, TokenType.REFRESH));
    }

    /** Forces a new Factus login, e.g. when the user changes their Factus credentials. */
    default int revokeFactusTokensByUser(User user) {
        return revokeByUserAndTypes(user, EnumSet.of(TokenType.FACTUS_ACCESS, TokenType.FACTUS_REFRESH));
    }

    default int revokeAllByUser(User user) {
        return revokeByUserAndTypes(user, EnumSet.allOf(TokenType.class));
    }

    /**
     * Deletes the user's sessions (password change or reset). Deleted, not
     * revoked: a revoked refresh token that comes back means theft and closes
     * every session, and an old device retrying after a password change is not theft.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Token t WHERE t.user = :user AND t.tokenType IN :types")
    int deleteByUserAndTypes(@Param("user") User user, @Param("types") Collection<TokenType> types);

    default int deleteLocalTokensByUser(User user) {
        return deleteByUserAndTypes(user, EnumSet.of(TokenType.ACCESS, TokenType.REFRESH));
    }

    // Revoked tokens are kept until they expire so a reused refresh token can
    // be detected; after that they are only clutter. Call it from a @Scheduled job.
    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}