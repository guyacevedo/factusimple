package com.guyacevedo.factusimple.features.report.service;

import com.guyacevedo.factusimple.features.report.dto.DailySalesDto;
import com.guyacevedo.factusimple.features.report.dto.DayAmountRow;
import com.guyacevedo.factusimple.features.report.dto.LastSaleRow;
import com.guyacevedo.factusimple.features.report.dto.ProductAmountRow;
import com.guyacevedo.factusimple.features.report.dto.SlowMovingProductDto;
import com.guyacevedo.factusimple.features.report.dto.StockOnHandRow;
import com.guyacevedo.factusimple.features.report.dto.TopProductDto;
import com.guyacevedo.factusimple.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportServiceTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_2 = LocalDate.of(2026, 10, 2);
    private static final LocalDate OCT_3 = LocalDate.of(2026, 10, 3);
    private static final LocalDate OCT_4 = LocalDate.of(2026, 10, 4);

    @Test
    void dailySeriesFillsEmptyDaysAndCountsCancellationsOnTheirOwnDay() {
        // Sold 50 000 on the 2nd, cancelled on the 3rd: the 2nd keeps its sale, the 3rd goes negative.
        List<DailySalesDto> days = ReportService.dailySeries(OCT_1, OCT_4,
                List.of(new DayAmountRow(OCT_2, 3, money("50000"))),
                List.of(new DayAmountRow(OCT_3, 1, money("20000"))));

        assertEquals(4, days.size());
        assertEquals(new DailySalesDto(OCT_1, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), days.get(0));
        assertMoney("50000", days.get(1).netTotal());
        assertMoney("0", days.get(1).cancelledTotal());
        assertMoney("-20000", days.get(2).netTotal());
        assertEquals(0, days.get(2).invoices());
        assertEquals(OCT_4, days.getLast().date());
    }

    @Test
    void rankingSubtractsCancellationsAndReordersByNet() {
        UUID rice = UUID.randomUUID();
        UUID sugar = UUID.randomUUID();
        List<ProductAmountRow> sold = List.of(
                new ProductAmountRow(rice, "Arroz", money("10"), money("50000")),
                new ProductAmountRow(sugar, "Azúcar", money("8"), money("40000")));
        List<ProductAmountRow> cancelled = List.of(
                new ProductAmountRow(rice, "Arroz", money("6"), money("30000")));

        List<TopProductDto> ranking = ReportService.rankNetOfCancellations(sold, cancelled);

        assertEquals("Azúcar", ranking.get(0).name());
        assertMoney("40000", ranking.get(0).netTotal());
        assertEquals("Arroz", ranking.get(1).name());
        assertMoney("6", ranking.get(1).quantityCancelled());
        assertMoney("20000", ranking.get(1).netTotal());
    }

    @Test
    void slowMovingPutsNeverSoldFirstThenTheMostIdleCapital() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 31, 12, 0);
        UUID neverSold = UUID.randomUUID();
        UUID oldCheap = UUID.randomUUID();
        UUID oldExpensive = UUID.randomUUID();
        UUID soldToday = UUID.randomUUID();
        List<StockOnHandRow> stock = List.of(
                new StockOnHandRow(oldCheap, "A", "Barato", money("10"), money("100")),
                new StockOnHandRow(soldToday, "B", "Rota", money("5"), money("1000")),
                new StockOnHandRow(oldExpensive, "C", "Caro", money("2"), money("50000")),
                new StockOnHandRow(neverSold, "D", "Nunca", money("1"), money("10")));
        List<LastSaleRow> lastSales = List.of(
                new LastSaleRow(oldCheap, now.minusDays(40)),
                new LastSaleRow(oldExpensive, now.minusDays(45)),
                new LastSaleRow(soldToday, now.minusHours(1)));

        List<SlowMovingProductDto> slow = ReportService.slowMoving(stock, lastSales, 30, now);

        assertEquals(List.of("Nunca", "Caro", "Barato"), slow.stream().map(SlowMovingProductDto::name).toList());
        assertNull(slow.getFirst().lastSale());
        assertNull(slow.getFirst().daysWithoutSales());
        assertEquals(45L, slow.get(1).daysWithoutSales());
        assertMoney("100000", slow.get(1).idleCapital());
    }

    @Test
    void periodDefaultsToTheCurrentMonthUpToToday() {
        ReportService.Period period = ReportService.Period.of(null, null);

        assertEquals(LocalDate.now().withDayOfMonth(1), period.from());
        assertEquals(LocalDate.now(), period.to());
        assertEquals(LocalDate.now().plusDays(1).atStartOfDay(), period.end());
    }

    @Test
    void periodIsWholeDaysHalfOpenAtTheEnd() {
        ReportService.Period period = ReportService.Period.of(OCT_1, OCT_4);

        assertEquals(OCT_1.atStartOfDay(), period.start());
        assertEquals(LocalDate.of(2026, 10, 5).atStartOfDay(), period.end());
    }

    @Test
    void invertedOrTooLongPeriodsAreRejected() {
        assertThrows(BadRequestException.class, () -> ReportService.Period.of(OCT_4, OCT_1));
        assertThrows(BadRequestException.class, () -> ReportService.Period.of(OCT_1, OCT_1.plusDays(366)));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }
}
