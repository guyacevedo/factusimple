package com.guyacevedo.factusimple.features.invoice.service;

import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.InvoiceTotals;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.LineCalculation;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.LineInput;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.TaxInput;
import com.guyacevedo.factusimple.features.invoice.service.InvoiceCalculator.WithholdingTaxInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceCalculatorTest {

    private static final TaxInput IVA_19 = new TaxInput("01", new BigDecimal("19.00"), false);

    @Test
    void priceWithIvaIncludedIsSplitIntoBaseAndTax() {
        LineCalculation line = InvoiceCalculator.calculateLine(
                line("1", "11900", "11000", "12", null, List.of(IVA_19), List.of()));

        assertMoney("10000.00", line.baseAmount());
        assertMoney("1900.00", line.taxAmount());
        assertMoney("11900.00", line.total());
        assertMoney("1900.00", line.taxes().getFirst().amount());
    }

    @Test
    void discountIsTakenFromWhatTheCustomerPays() {
        LineCalculation line = InvoiceCalculator.calculateLine(
                line("2", "11900", "11000", "12", "10", List.of(IVA_19), List.of()));

        // 2 x 11 900 = 23 800, minus 10 % = 21 420 paid
        assertMoney("2380.00", line.discountAmount());
        assertMoney("21420.00", line.total());
        assertMoney("18000.00", line.baseAmount());
        assertMoney("3420.00", line.taxAmount());
    }

    @Test
    void wholesalePriceAppliesFromTheMinimumQuantity() {
        LineCalculation belowMinimum = InvoiceCalculator.calculateLine(
                line("11", "1000", "800", "12", null, List.of(), List.of()));
        LineCalculation atMinimum = InvoiceCalculator.calculateLine(
                line("12", "1000", "800", "12", null, List.of(), List.of()));

        assertMoney("1000.00", belowMinimum.unitPrice());
        assertMoney("800.00", atMinimum.unitPrice());
        assertMoney("9600.00", atMinimum.total());
    }

    @Test
    void excludedTaxAmountsToZeroAndDoesNotReduceTheBase() {
        TaxInput excludedIva = new TaxInput("01", new BigDecimal("19.00"), true);
        LineCalculation line = InvoiceCalculator.calculateLine(
                line("1", "5000", "5000", "1", null, List.of(excludedIva), List.of()));

        assertMoney("5000.00", line.baseAmount());
        assertMoney("0.00", line.taxAmount());
        assertMoney("0.00", line.taxes().getFirst().amount());
    }

    @Test
    void lastTaxAbsorbsTheRoundingCentSoBasePlusTaxesEqualsTotal() {
        TaxInput inc8 = new TaxInput("04", new BigDecimal("8.00"), false);
        LineCalculation line = InvoiceCalculator.calculateLine(
                line("1", "10001", "10001", "1", null, List.of(IVA_19, inc8), List.of()));

        BigDecimal taxes = line.taxes().stream().map(InvoiceCalculator.TaxLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMoney("10001.00", line.total());
        assertMoney(line.total().toPlainString(), line.baseAmount().add(taxes));
        assertMoney(line.taxAmount().toPlainString(), taxes);
    }

    @Test
    void withholdingIsComputedOnTheBaseAndDoesNotLowerTheTotal() {
        WithholdingTaxInput reteRenta = new WithholdingTaxInput("06", new BigDecimal("2.50"));
        LineCalculation line = InvoiceCalculator.calculateLine(
                line("1", "11900", "11900", "1", null, List.of(IVA_19), List.of(reteRenta)));

        assertMoney("250.00", line.withholdingTaxAmount());
        assertMoney("11900.00", line.total());
    }

    @Test
    void totalsAddUpTheLines() {
        LineCalculation first = InvoiceCalculator.calculateLine(
                line("1", "11900", "11900", "1", null, List.of(IVA_19), List.of()));
        LineCalculation second = InvoiceCalculator.calculateLine(
                line("2", "5000", "5000", "10", "10", List.of(), List.of()));

        InvoiceTotals totals = InvoiceCalculator.totals(List.of(first, second));

        assertMoney("19000.00", totals.subtotal());
        assertMoney("1000.00", totals.totalDiscounts());
        assertMoney("1900.00", totals.totalTaxes());
        assertMoney("20900.00", totals.total());
    }

    private static LineInput line(String quantity, String retail, String wholesale, String minWholesale,
                                  String discountRate, List<TaxInput> taxes,
                                  List<WithholdingTaxInput> withholdingTaxes) {
        return new LineInput(new BigDecimal(quantity), new BigDecimal(retail), new BigDecimal(wholesale),
                new BigDecimal(minWholesale), discountRate == null ? null : new BigDecimal(discountRate),
                taxes, withholdingTaxes);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }
}
