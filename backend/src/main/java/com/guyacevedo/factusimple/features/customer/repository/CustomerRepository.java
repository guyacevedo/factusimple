package com.guyacevedo.factusimple.features.customer.repository;

import com.guyacevedo.factusimple.features.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Every query is scoped to the owner (user_id). The identification is unique
 * per user (uk_customer_user_identification), compared exactly.
 * Derived queries use the Java field names: the field is "active", not "isActive".
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndIdentification(UUID userId, String identification);

    boolean existsByUserIdAndIdentificationAndIdNot(UUID userId, String identification, UUID id);

    Page<Customer> findAllByUserIdAndActiveTrue(UUID userId, Pageable pageable);

    Page<Customer> findAllByUserId(UUID userId, Pageable pageable);

    // ── Credit ──────────────────────────────────────────────────────────────
    // One statement each, like the plan counters in UserRepository: two credit
    // sales at once cannot both fit in the last of the customer's credit limit.
    // Bulk updates skip @Version and auditing, so version and updated_at are set here.

    /** Adds a credit sale to the balance only if it fits the limit. @return 1 if it fit, 0 if not */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Customer c
               SET c.creditBalance = c.creditBalance + :amount,
                   c.version = c.version + 1,
                   c.updatedAt = LOCAL DATETIME
             WHERE c.id = :customerId
               AND c.creditBalance + :amount <= c.creditLimit
            """)
    int reserveCredit(@Param("customerId") UUID customerId, @Param("amount") BigDecimal amount);

    /** Lowers the balance after a payment or a cancellation. @return 1 if done, 0 if it would go negative */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Customer c
               SET c.creditBalance = c.creditBalance - :amount,
                   c.version = c.version + 1,
                   c.updatedAt = LOCAL DATETIME
             WHERE c.id = :customerId
               AND c.creditBalance >= :amount
            """)
    int releaseCredit(@Param("customerId") UUID customerId, @Param("amount") BigDecimal amount);
}
