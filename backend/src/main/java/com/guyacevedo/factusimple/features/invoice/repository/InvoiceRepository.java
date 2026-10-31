package com.guyacevedo.factusimple.features.invoice.repository;

import com.guyacevedo.factusimple.features.invoice.entity.Invoice;
import com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every query is scoped to the owner (user_id). Payments and the frozen taxes
 * of each line load in batches (hibernate.default_batch_fetch_size); only the
 * customer and the lines are fetched up front.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsByCustomerId(UUID customerId);

    boolean existsByUserIdAndReferenceCode(UUID userId, String referenceCode);

    @EntityGraph(attributePaths = {"customer", "items"})
    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Locks the invoice (SELECT ... FOR UPDATE). Cancelling and paying towards it
     * both take this lock, so they cannot run over each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.user.id = :userId")
    Optional<Invoice> findForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * History with optional filters. The dates are a half-open range and never
     * null (the service fills in fixed bounds). paymentForm arrives as "" when it
     * is not filtered, so PostgreSQL never gets an untyped null text parameter.
     */
    @EntityGraph(attributePaths = "customer")
    @Query("""
            SELECT i FROM Invoice i
             WHERE i.user.id = :userId
               AND i.createdAt >= :from
               AND i.createdAt < :to
               AND (:status IS NULL OR i.status = :status)
               AND (:customerId IS NULL OR i.customer.id = :customerId)
               AND (:paymentForm = '' OR EXISTS (
                        SELECT 1 FROM InvoicePayment p
                         WHERE p.invoice = i AND p.paymentForm = :paymentForm))
               AND (:withBalance = false OR i.balanceDue > 0)
            """)
    Page<Invoice> search(@Param("userId") UUID userId,
                         @Param("status") InvoiceStatus status,
                         @Param("customerId") UUID customerId,
                         @Param("paymentForm") String paymentForm,
                         @Param("withBalance") boolean withBalance,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         Pageable pageable);

    /** Invoices the customer still owes on, oldest first: the order debt is collected in. */
    @Query("""
            SELECT i FROM Invoice i
             WHERE i.user.id = :userId
               AND i.customer.id = :customerId
               AND i.status <> com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus.CANCELLED
               AND i.balanceDue > 0
             ORDER BY i.createdAt ASC, i.id ASC
            """)
    List<Invoice> findWithBalance(@Param("customerId") UUID customerId, @Param("userId") UUID userId);

    /**
     * Same as findWithBalance, locking the rows so a payment can be spread over
     * them. Always locked in the same order, so two payments cannot deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM Invoice i
             WHERE i.user.id = :userId
               AND i.customer.id = :customerId
               AND i.status <> com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus.CANCELLED
               AND i.balanceDue > 0
             ORDER BY i.createdAt ASC, i.id ASC
            """)
    List<Invoice> findWithBalanceForUpdate(@Param("customerId") UUID customerId, @Param("userId") UUID userId);
}
