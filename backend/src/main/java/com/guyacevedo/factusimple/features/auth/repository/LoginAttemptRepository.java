package com.guyacevedo.factusimple.features.auth.repository;

import com.guyacevedo.factusimple.features.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Failed attempts from one IP in a time window (uses idx_login_attempt_ip_date, V8).
     *
     * The per-account lock protects one account, but does not stop someone from
     * trying one common password against hundreds of different emails.
     * Counting by IP covers that case.
     */
    @Query("""
            SELECT COUNT(a) FROM LoginAttempt a
            WHERE a.ip = :ip AND a.successful = false AND a.createdAt > :since
            """)
    long countFailedByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}