package com.guyacevedo.factusimple.features.report.service;

import com.guyacevedo.factusimple.features.inventory.repository.ProductStockRepository;
import com.guyacevedo.factusimple.features.report.dto.DailySalesDto;
import com.guyacevedo.factusimple.features.report.dto.DayAmountRow;
import com.guyacevedo.factusimple.features.report.dto.InvoiceTotalsRow;
import com.guyacevedo.factusimple.features.report.dto.LastSaleRow;
import com.guyacevedo.factusimple.features.report.dto.PaymentFormTotalRow;
import com.guyacevedo.factusimple.features.report.dto.ProductAmountRow;
import com.guyacevedo.factusimple.features.report.dto.SalesSummaryDto;
import com.guyacevedo.factusimple.features.report.dto.SlowMovingProductDto;
import com.guyacevedo.factusimple.features.report.dto.StockOnHandRow;
import com.guyacevedo.factusimple.features.report.dto.TopProductDto;
import com.guyacevedo.factusimple.features.report.repository.ReportRepository;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dashboard figures. A period is [from, to] in whole days; when it is not given
 * it is the current month up to today.
 *
 * Each event counts on its own date: a sale on the invoice date, a cancellation
 * on the day it was cancelled (the credit note). Net = sold - cancelled. That
 * way a period that is already closed never changes when something happens
 * later, and the figures match the documents sent to the DIAN.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    /** DIAN payment forms. */
    private static final String CASH_SALE = "1";
    private static final String CREDIT_SALE = "2";

    /** One point per day in the chart: a longer period is a different report. */
    private static final int MAX_DAYS = 366;

    private final ReportRepository reportRepository;
    private final ProductStockRepository productStockRepository;

    @Transactional(readOnly = true)
    public SalesSummaryDto summary(UUID userId, LocalDate from, LocalDate to) {
        Period period = Period.of(from, to);
        InvoiceTotalsRow sold = reportRepository.invoiceTotals(userId, period.start(), period.end());
        InvoiceTotalsRow cancelled = reportRepository.cancelledTotals(userId, period.start(), period.end());
        Map<String, BigDecimal> byForm = reportRepository.totalsByPaymentForm(userId, period.start(), period.end())
                .stream()
                .collect(Collectors.toMap(PaymentFormTotalRow::paymentForm, PaymentFormTotalRow::amount));
        BigDecimal averageTicket = sold.invoices() == 0
                ? BigDecimal.ZERO
                : sold.total().divide(BigDecimal.valueOf(sold.invoices()), 2, RoundingMode.HALF_UP);

        return new SalesSummaryDto(
                period.from(), period.to(),
                sold.invoices(), sold.total(),
                cancelled.invoices(), cancelled.total(),
                sold.total().subtract(cancelled.total()),
                sold.subtotal().subtract(cancelled.subtotal()),
                sold.totalTaxes().subtract(cancelled.totalTaxes()),
                sold.totalDiscounts().subtract(cancelled.totalDiscounts()),
                byForm.getOrDefault(CASH_SALE, BigDecimal.ZERO),
                byForm.getOrDefault(CREDIT_SALE, BigDecimal.ZERO),
                averageTicket,
                reportRepository.receivablesPending(userId),
                reportRepository.receivablesOverdue(userId, LocalDate.now()),
                productStockRepository.countLowStockByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<DailySalesDto> salesByDay(UUID userId, LocalDate from, LocalDate to) {
        Period period = Period.of(from, to);
        return dailySeries(period.from(), period.to(),
                reportRepository.salesByDay(userId, period.start(), period.end()),
                reportRepository.cancelledByDay(userId, period.start(), period.end()));
    }

    @Transactional(readOnly = true)
    public List<TopProductDto> topProducts(UUID userId, LocalDate from, LocalDate to, int limit) {
        Period period = Period.of(from, to);
        List<ProductAmountRow> sold = reportRepository.soldByProduct(userId, period.start(), period.end(),
                PageRequest.of(0, Math.clamp(limit, 1, 50)));
        if (sold.isEmpty()) {
            return List.of();
        }
        List<String> names = sold.stream().map(ProductAmountRow::name).distinct().toList();
        return rankNetOfCancellations(sold,
                reportRepository.cancelledByProduct(userId, period.start(), period.end(), names));
    }

    /**
     * Products in stock that have not sold in the last {@code days} days. The
     * ones that never sold come first; then the ones with more money stuck in
     * stock, which is the purchase decision that cannot wait.
     */
    @Transactional(readOnly = true)
    public List<SlowMovingProductDto> slowMoving(UUID userId, int days) {
        return slowMoving(reportRepository.stockOnHand(userId), reportRepository.lastSaleByProduct(userId),
                Math.clamp(days, 1, 365), LocalDateTime.now());
    }

    // ── Pure functions, tested without a database ───────────────────────────

    /** One entry per day of [from, to], with zeros where nothing happened, so the chart has no gaps. */
    static List<DailySalesDto> dailySeries(LocalDate from, LocalDate to,
                                           List<DayAmountRow> sales, List<DayAmountRow> cancellations) {
        Map<LocalDate, DayAmountRow> soldByDay = sales.stream()
                .collect(Collectors.toMap(DayAmountRow::date, Function.identity()));
        Map<LocalDate, DayAmountRow> cancelledByDay = cancellations.stream()
                .collect(Collectors.toMap(DayAmountRow::date, Function.identity()));

        List<DailySalesDto> days = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            DayAmountRow sold = soldByDay.get(day);
            DayAmountRow cancelled = cancelledByDay.get(day);
            long invoices = sold == null ? 0 : sold.invoices();
            BigDecimal gross = sold == null ? BigDecimal.ZERO : sold.total();
            BigDecimal cancelledTotal = cancelled == null ? BigDecimal.ZERO : cancelled.total();
            days.add(new DailySalesDto(day, invoices, gross, cancelledTotal, gross.subtract(cancelledTotal)));
        }
        return days;
    }

    /**
     * Subtracts the cancellations of the period from what was sold, product by
     * product, and ranks by the net amount. A product that was only cancelled
     * in the period is not in the ranking: a negative row is not a best seller.
     */
    static List<TopProductDto> rankNetOfCancellations(List<ProductAmountRow> sold,
                                                      List<ProductAmountRow> cancelled) {
        Map<ProductKey, ProductAmountRow> cancelledByProduct = cancelled.stream()
                .collect(Collectors.toMap(ProductKey::of, Function.identity()));
        return sold.stream()
                .map(row -> {
                    ProductAmountRow back = cancelledByProduct.get(ProductKey.of(row));
                    BigDecimal quantityBack = back == null ? BigDecimal.ZERO : back.quantity();
                    BigDecimal totalBack = back == null ? BigDecimal.ZERO : back.total();
                    return new TopProductDto(row.productId(), row.name(), row.quantity(), quantityBack,
                            row.total().subtract(totalBack));
                })
                .sorted(Comparator.comparing(TopProductDto::netTotal).reversed())
                .toList();
    }

    static List<SlowMovingProductDto> slowMoving(List<StockOnHandRow> stock, List<LastSaleRow> lastSales,
                                                 int days, LocalDateTime now) {
        Map<UUID, LocalDateTime> lastSaleByProduct = lastSales.stream()
                .collect(Collectors.toMap(LastSaleRow::productId, LastSaleRow::lastSale));
        LocalDateTime cutoff = now.toLocalDate().minusDays(days).atStartOfDay();

        return stock.stream()
                .filter(row -> {
                    LocalDateTime lastSale = lastSaleByProduct.get(row.productId());
                    return lastSale == null || lastSale.isBefore(cutoff);
                })
                .map(row -> {
                    LocalDateTime lastSale = lastSaleByProduct.get(row.productId());
                    return new SlowMovingProductDto(row.productId(), row.sku(), row.name(), row.quantity(),
                            lastSale,
                            lastSale == null ? null : ChronoUnit.DAYS.between(lastSale, now),
                            row.quantity().multiply(row.costPrice()).setScale(2, RoundingMode.HALF_UP));
                })
                .sorted(Comparator.comparing(SlowMovingProductDto::neverSold).reversed()
                        .thenComparing(SlowMovingProductDto::idleCapital, Comparator.reverseOrder()))
                .toList();
    }

    /** Lines are grouped by product and frozen name; a deleted product has a null id. */
    private record ProductKey(UUID productId, String name) {

        static ProductKey of(ProductAmountRow row) {
            return new ProductKey(row.productId(), Objects.requireNonNull(row.name()));
        }
    }

    /** [from, to] in days, and the [start, end) instants the queries use. */
    record Period(LocalDate from, LocalDate to) {

        static Period of(LocalDate from, LocalDate to) {
            LocalDate end = to == null ? LocalDate.now() : to;
            LocalDate start = from == null ? end.withDayOfMonth(1) : from;
            if (start.isAfter(end)) {
                throw new BadRequestException("La fecha inicial no puede ser posterior a la final");
            }
            if (ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
                throw new BadRequestException("El periodo no puede superar " + MAX_DAYS + " días");
            }
            return new Period(start, end);
        }

        LocalDateTime start() {
            return from.atStartOfDay();
        }

        LocalDateTime end() {
            return to.plusDays(1).atStartOfDay();
        }
    }
}
