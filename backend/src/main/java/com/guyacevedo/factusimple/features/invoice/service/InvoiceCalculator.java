package com.guyacevedo.factusimple.features.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Amounts of an invoice, without Spring or JPA so it can be tested on its own.
 *
 * Catalog prices include taxes: what the customer pays for a line is fixed and
 * the base is worked out backwards from it. The last tax of the line absorbs the
 * rounding cent, so base + taxes always equals what was charged. Withholding
 * taxes are computed on the base and informed apart; they do not lower the total.
 * Every amount is rounded to 2 decimals, HALF_UP.
 */
public final class InvoiceCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private InvoiceCalculator() {
    }

    public record TaxInput(String code, BigDecimal rate, boolean excluded) {}

    public record WithholdingTaxInput(String code, BigDecimal rate) {}

    public record LineInput(
            BigDecimal quantity,
            BigDecimal retailPrice,
            BigDecimal wholesalePrice,
            BigDecimal minStockWholesale,
            BigDecimal discountRate,
            List<TaxInput> taxes,
            List<WithholdingTaxInput> withholdingTaxes) {}

    public record TaxLine(String code, BigDecimal rate, boolean excluded, BigDecimal baseAmount, BigDecimal amount) {}

    public record WithholdingTaxLine(String code, BigDecimal rate, BigDecimal baseAmount, BigDecimal amount) {}

    public record LineCalculation(
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal discountAmount,
            BigDecimal baseAmount,
            BigDecimal taxAmount,
            BigDecimal withholdingTaxAmount,
            BigDecimal total,
            List<TaxLine> taxes,
            List<WithholdingTaxLine> withholdingTaxes) {}

    public record InvoiceTotals(
            BigDecimal subtotal,
            BigDecimal totalDiscounts,
            BigDecimal totalTaxes,
            BigDecimal totalHoldingTaxes,
            BigDecimal total) {}

    public static LineCalculation calculateLine(LineInput line) {
        // Wholesale price from the minimum wholesale quantity on.
        BigDecimal unitPrice = line.quantity().compareTo(line.minStockWholesale()) >= 0
                ? line.wholesalePrice()
                : line.retailPrice();
        BigDecimal discountRate = line.discountRate() == null ? BigDecimal.ZERO : line.discountRate();

        BigDecimal gross = round(line.quantity().multiply(unitPrice));
        BigDecimal discount = round(gross.multiply(discountRate).divide(HUNDRED, 10, ROUNDING));
        BigDecimal net = gross.subtract(discount);

        BigDecimal appliedRates = line.taxes().stream()
                .filter(tax -> !tax.excluded())
                .map(TaxInput::rate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // net = base * (1 + rates / 100)  =>  base = net * 100 / (100 + rates)
        BigDecimal base = net.multiply(HUNDRED).divide(HUNDRED.add(appliedRates), SCALE, ROUNDING);
        BigDecimal taxTotal = net.subtract(base);

        List<TaxLine> taxes = splitTaxes(line.taxes(), base, taxTotal);
        List<WithholdingTaxLine> withholdingTaxes = line.withholdingTaxes().stream()
                .map(tax -> new WithholdingTaxLine(tax.code(), tax.rate(), base, percentOf(base, tax.rate())))
                .toList();
        BigDecimal withholdingTotal = withholdingTaxes.stream()
                .map(WithholdingTaxLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LineCalculation(unitPrice, discountRate, discount, base, taxTotal, withholdingTotal, net,
                taxes, withholdingTaxes);
    }

    public static InvoiceTotals totals(List<LineCalculation> lines) {
        BigDecimal subtotal = sum(lines, LineCalculation::baseAmount);
        BigDecimal totalTaxes = sum(lines, LineCalculation::taxAmount);
        return new InvoiceTotals(
                subtotal,
                sum(lines, LineCalculation::discountAmount),
                totalTaxes,
                sum(lines, LineCalculation::withholdingTaxAmount),
                subtotal.add(totalTaxes));
    }

    /** Each applied tax gets base * rate; the last one takes what is left of taxTotal. */
    private static List<TaxLine> splitTaxes(List<TaxInput> inputs, BigDecimal base, BigDecimal taxTotal) {
        int lastApplied = -1;
        for (int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).excluded()) {
                lastApplied = i;
            }
        }

        List<TaxLine> taxes = new ArrayList<>(inputs.size());
        BigDecimal assigned = BigDecimal.ZERO;
        for (int i = 0; i < inputs.size(); i++) {
            TaxInput tax = inputs.get(i);
            BigDecimal amount;
            if (tax.excluded()) {
                amount = BigDecimal.ZERO.setScale(SCALE);
            } else if (i == lastApplied) {
                amount = taxTotal.subtract(assigned);
            } else {
                amount = percentOf(base, tax.rate());
                assigned = assigned.add(amount);
            }
            taxes.add(new TaxLine(tax.code(), tax.rate(), tax.excluded(), base, amount));
        }
        return taxes;
    }

    private static BigDecimal percentOf(BigDecimal amount, BigDecimal rate) {
        return round(amount.multiply(rate).divide(HUNDRED, 10, ROUNDING));
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    private static BigDecimal sum(List<LineCalculation> lines,
                                  Function<LineCalculation, BigDecimal> amount) {
        return lines.stream().map(amount).reduce(BigDecimal.ZERO.setScale(SCALE), BigDecimal::add);
    }
}
