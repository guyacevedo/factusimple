package com.guyacevedo.factusimple.features.receivable.repository;

import com.guyacevedo.factusimple.features.receivable.dto.PaidByInvoice;
import com.guyacevedo.factusimple.features.receivable.entity.CustomerPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, UUID> {

    /** History with optional filters. Customer and invoice are fetched because the DTO prints their name and code. */
    @EntityGraph(attributePaths = {"customer", "invoice"})
    @Query("""
            SELECT p FROM CustomerPayment p
             WHERE p.user.id = :userId
               AND (:customerId IS NULL OR p.customer.id = :customerId)
               AND (:invoiceId IS NULL OR p.invoice.id = :invoiceId)
            """)
    Page<CustomerPayment> search(@Param("userId") UUID userId,
                                 @Param("customerId") UUID customerId,
                                 @Param("invoiceId") UUID invoiceId,
                                 Pageable pageable);

    /**
     * What the customer has paid towards one invoice. Cancelling uses it to say
     * how much money goes back; it cannot be worked out from the balance alone.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CustomerPayment p WHERE p.invoice.id = :invoiceId")
    BigDecimal totalPaidByInvoice(@Param("invoiceId") UUID invoiceId);

    /**
     * The same for a page of invoices in one query instead of one per row.
     * Invoices without payments are not returned; the caller uses 0 for them.
     */
    @Query("""
            SELECT new com.guyacevedo.factusimple.features.receivable.dto.PaidByInvoice(
                       p.invoice.id, COALESCE(SUM(p.amount), 0))
              FROM CustomerPayment p
             WHERE p.invoice.id IN :invoiceIds
             GROUP BY p.invoice.id
            """)
    List<PaidByInvoice> paidByInvoices(@Param("invoiceIds") Collection<UUID> invoiceIds);
}
