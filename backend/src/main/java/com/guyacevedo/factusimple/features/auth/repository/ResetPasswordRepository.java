package com.guyacevedo.factusimple.features.auth.repository;

import com.guyacevedo.factusimple.features.auth.entity.ResetPassword;
import com.guyacevedo.factusimple.features.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResetPasswordRepository extends JpaRepository<ResetPassword, UUID> {

    // token_hash holds the SHA-256 of the token sent by email, never the token itself.
    Optional<ResetPassword> findByUserAndTokenHash(User user, String tokenHash);

    /**
     * Marks the link as used only if it was not used yet. Returns 0 if another
     * request got there first, so two simultaneous requests with the same link
     * cannot change the password twice.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ResetPassword r SET r.used = true WHERE r.id = :id AND r.used = false")
    int markUsed(@Param("id") UUID id);

    /** A new request invalidates the previous links: only the latest email works. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ResetPassword r SET r.used = true WHERE r.user = :user AND r.used = false")
    int invalidateAllByUser(@Param("user") User user);

    // Call it from a @Scheduled job together with TokenRepository.deleteExpired.
    @Modifying
    @Query("DELETE FROM ResetPassword r WHERE r.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}