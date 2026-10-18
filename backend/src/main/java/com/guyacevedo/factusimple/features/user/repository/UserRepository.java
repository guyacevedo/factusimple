package com.guyacevedo.factusimple.features.user.repository;

import com.guyacevedo.factusimple.features.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // The email is unique ignoring case (uk_user_email on LOWER(email)),
    // so lookups ignore case too: "Guy@x.co" and "guy@x.co" are the same user.
    @EntityGraph(attributePaths = "plan")
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPlanId(UUID planId);

    @Override
    @EntityGraph(attributePaths = "plan")
    Optional<User> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "plan")
    Page<User> findAll(Pageable pageable);

    // ── Plan usage counters ─────────────────────────────────────────────────
    // Each increment compares against the user's current plan in the same
    // statement, so two requests at once cannot both slip past the limit.
    // They return 1 when the counter went up and 0 when the limit is reached.
    // flushAutomatically writes pending changes before the update;
    // clearAutomatically drops the cached User so the next read sees the new value.
    // Bulk updates skip @Version and updated_at, which is fine for counters.

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE User u SET u.productCount = u.productCount + 1
            WHERE u.id = :userId
              AND u.productCount < (SELECT p.maxProducts FROM Plan p WHERE p = u.plan)
            """)
    int incrementProductCountIfBelowLimit(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE User u SET u.customerCount = u.customerCount + 1
            WHERE u.id = :userId
              AND u.customerCount < (SELECT p.maxCustomers FROM Plan p WHERE p = u.plan)
            """)
    int incrementCustomerCountIfBelowLimit(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE User u SET u.invoiceCount = u.invoiceCount + 1
            WHERE u.id = :userId
              AND u.invoiceCount < (SELECT p.maxInvoices FROM Plan p WHERE p = u.plan)
            """)
    int incrementInvoiceCountIfBelowLimit(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.productCount = u.productCount - 1 WHERE u.id = :userId AND u.productCount > 0")
    int decrementProductCount(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.customerCount = u.customerCount - 1 WHERE u.id = :userId AND u.customerCount > 0")
    int decrementCustomerCount(@Param("userId") UUID userId);

    /** Used by account activation, which runs without an outer transaction. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.password = :passwordHash, u.mustChangePassword = false WHERE u.id = :userId")
    int updatePasswordAndClearMustChange(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);

    // ── Failed logins (used by LoginAttemptRecorder) ────────────────────────
    // Conditional UPDATEs so parallel attempts cannot race on the counter.

    /** +1 to the counter; if a previous lock already expired, starts again from 1. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE User u SET
                u.failedLoginAttempts = CASE
                    WHEN u.blockedAt IS NOT NULL AND u.blockedAt < :lockExpiredBefore THEN 1
                    ELSE u.failedLoginAttempts + 1 END,
                u.blockedAt = CASE
                    WHEN u.blockedAt IS NOT NULL AND u.blockedAt < :lockExpiredBefore THEN NULL
                    ELSE u.blockedAt END
            WHERE u.id = :userId
            """)
    int registerFailedLogin(@Param("userId") UUID userId,
                            @Param("lockExpiredBefore") LocalDateTime lockExpiredBefore);

    /** Sets blocked_at once the counter reaches the limit. Returns 1 only the moment it locks. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE User u SET u.blockedAt = :now
            WHERE u.id = :userId AND u.blockedAt IS NULL AND u.failedLoginAttempts >= :maxAttempts
            """)
    int lockIfThresholdReached(@Param("userId") UUID userId,
                               @Param("maxAttempts") int maxAttempts,
                               @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE User u SET u.failedLoginAttempts = 0, u.blockedAt = NULL
            WHERE u.id = :userId AND (u.failedLoginAttempts > 0 OR u.blockedAt IS NOT NULL)
            """)
    int resetFailedLogins(@Param("userId") UUID userId);

    // Only to undo an increment when the invoice could not be created.
    // An invoice already sent to the DIAN keeps counting even if it is cancelled.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.invoiceCount = u.invoiceCount - 1 WHERE u.id = :userId AND u.invoiceCount > 0")
    int decrementInvoiceCount(@Param("userId") UUID userId);
}