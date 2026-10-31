package com.guyacevedo.factusimple.features.report.repository;

import com.guyacevedo.factusimple.features.invoice.entity.Invoice;
import com.guyacevedo.factusimple.features.report.dto.DayAmountRow;
import com.guyacevedo.factusimple.features.report.dto.InvoiceTotalsRow;
import com.guyacevedo.factusimple.features.report.dto.LastSaleRow;
import com.guyacevedo.factusimple.features.report.dto.PaymentFormTotalRow;
import com.guyacevedo.factusimple.features.report.dto.ProductAmountRow;
import com.guyacevedo.factusimple.features.report.dto.StockOnHandRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Read-only aggregates for the dashboard, scoped to the owner (user_id).
 * Extends Repository and not JpaRepository so nothing can be written from here.
 *
 * Each event counts on its own date, over half-open periods [from, to):
 * sales by createdAt whatever their current status, cancellations by
 * cancelledAt. Cancelling today a sale of last week lowers today, not last
 * week: a period that was already closed never changes.
 */
@org.springframework.stereotype.Repository
public interface ReportRepository extends Repository<Invoice, UUID> {

    // ── Sales (by invoice date) ─────────────────────────────────────────────

    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.InvoiceTotalsRow(
                       COUNT(i), COALESCE(SUM(i.total), 0), COALESCE(SUM(i.subtotal), 0),
                       COALESCE(SUM(i.totalTaxes), 0), COALESCE(SUM(i.totalDiscounts), 0))
              FROM Invoice i
             WHERE i.user.id = :userId
               AND i.createdAt >= :from AND i.createdAt < :to
            """)
    InvoiceTotalsRow invoiceTotals(@Param("userId") UUID userId,
                                   @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** What was sold on credit that day stays the same even if it is paid or cancelled later. */
    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.PaymentFormTotalRow(
                       p.paymentForm, COALESCE(SUM(p.amount), 0))
              FROM InvoicePayment p JOIN p.invoice i
             WHERE i.user.id = :userId
               AND i.createdAt >= :from AND i.createdAt < :to
             GROUP BY p.paymentForm
            """)
    List<PaymentFormTotalRow> totalsByPaymentForm(@Param("userId") UUID userId,
                                                  @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Only days with sales; ReportService fills in the rest. */
    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.DayAmountRow(
                       CAST(i.createdAt AS LocalDate), COUNT(i), COALESCE(SUM(i.total), 0))
              FROM Invoice i
             WHERE i.user.id = :userId
               AND i.createdAt >= :from AND i.createdAt < :to
             GROUP BY CAST(i.createdAt AS LocalDate)
            """)
    List<DayAmountRow> salesByDay(@Param("userId") UUID userId,
                                  @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Best sellers by amount sold. By amount and not by quantity: kilos and
     * units do not add up. Grouped by the name frozen in the line, so deleted
     * products still show. The Pageable only carries the limit.
     */
    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.ProductAmountRow(
                       it.product.id, it.name, SUM(it.quantity), SUM(it.total))
              FROM InvoiceItem it JOIN it.invoice i
             WHERE i.user.id = :userId
               AND i.createdAt >= :from AND i.createdAt < :to
             GROUP BY it.product.id, it.name
             ORDER BY SUM(it.total) DESC
            """)
    List<ProductAmountRow> soldByProduct(@Param("userId") UUID userId,
                                         @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                         Pageable limit);

    // ── Cancellations (by cancellation date) ────────────────────────────────

    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.InvoiceTotalsRow(
                       COUNT(i), COALESCE(SUM(i.total), 0), COALESCE(SUM(i.subtotal), 0),
                       COALESCE(SUM(i.totalTaxes), 0), COALESCE(SUM(i.totalDiscounts), 0))
              FROM Invoice i
             WHERE i.user.id = :userId
               AND i.cancelledAt >= :from AND i.cancelledAt < :to
            """)
    InvoiceTotalsRow cancelledTotals(@Param("userId") UUID userId,
                                     @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.DayAmountRow(
                       CAST(i.cancelledAt AS LocalDate), COUNT(i), COALESCE(SUM(i.total), 0))
              FROM Invoice i
             WHERE i.user.id = :userId
               AND i.cancelledAt >= :from AND i.cancelledAt < :to
             GROUP BY CAST(i.cancelledAt AS LocalDate)
            """)
    List<DayAmountRow> cancelledByDay(@Param("userId") UUID userId,
                                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** The lines cancelled in the period, for the products of the ranking. */
    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.ProductAmountRow(
                       it.product.id, it.name, SUM(it.quantity), SUM(it.total))
              FROM InvoiceItem it JOIN it.invoice i
             WHERE i.user.id = :userId
               AND i.cancelledAt >= :from AND i.cancelledAt < :to
               AND it.name IN :names
             GROUP BY it.product.id, it.name
            """)
    List<ProductAmountRow> cancelledByProduct(@Param("userId") UUID userId,
                                              @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                              @Param("names") Collection<String> names);

    // ── As of today ─────────────────────────────────────────────────────────

    /** What credit customers still owe (not bound to a period). */
    @Query("""
            SELECT COALESCE(SUM(i.balanceDue), 0) FROM Invoice i
             WHERE i.user.id = :userId
               AND i.status <> com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus.CANCELLED
               AND i.balanceDue > 0
            """)
    BigDecimal receivablesPending(@Param("userId") UUID userId);

    /** The part of it whose due date already passed. */
    @Query("""
            SELECT COALESCE(SUM(i.balanceDue), 0) FROM Invoice i
             WHERE i.user.id = :userId
               AND i.status <> com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus.CANCELLED
               AND i.balanceDue > 0
               AND EXISTS (SELECT 1 FROM InvoicePayment p WHERE p.invoice = i AND p.dueDate < :today)
            """)
    BigDecimal receivablesOverdue(@Param("userId") UUID userId, @Param("today") LocalDate today);

    // ── Slow-moving stock ───────────────────────────────────────────────────

    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.StockOnHandRow(
                       p.id, p.sku, p.name, s.quantity, p.costPrice)
              FROM ProductStock s JOIN s.product p
             WHERE p.user.id = :userId
               AND p.active = true
               AND s.quantity > 0
            """)
    List<StockOnHandRow> stockOnHand(@Param("userId") UUID userId);

    /** A cancelled sale did not move the product, so it does not count as its last sale. */
    @Query("""
            SELECT new com.guyacevedo.factusimple.features.report.dto.LastSaleRow(
                       it.product.id, MAX(i.createdAt))
              FROM InvoiceItem it JOIN it.invoice i
             WHERE i.user.id = :userId
               AND it.product IS NOT NULL
               AND i.status <> com.guyacevedo.factusimple.features.invoice.entity.InvoiceStatus.CANCELLED
             GROUP BY it.product.id
            """)
    List<LastSaleRow> lastSaleByProduct(@Param("userId") UUID userId);
}
