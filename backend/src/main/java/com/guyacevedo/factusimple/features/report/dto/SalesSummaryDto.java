package com.guyacevedo.factusimple.features.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Each event counts on its own date: sales by the invoice date, cancellations
 * by the date they were cancelled (the credit note). A closed period never
 * changes. Receivables and stock are as of today, not of the period.
 */
public record SalesSummaryDto(
    LocalDate from,
    LocalDate to,
    @Schema(description = "Facturas emitidas en el periodo, incluidas las que luego se cancelaron") long invoiceCount,
    @Schema(description = "Total facturado en el periodo") BigDecimal grossTotal,
    @Schema(description = "Facturas canceladas en el periodo (por fecha de cancelación)") long cancelledCount,
    @Schema(description = "Total de esas cancelaciones") BigDecimal cancelledTotal,
    @Schema(description = "Facturado menos cancelado") BigDecimal netTotal,
    @Schema(description = "Base gravable neta") BigDecimal subtotal,
    @Schema(description = "Impuestos netos") BigDecimal totalTaxes,
    @Schema(description = "Descuentos netos") BigDecimal totalDiscounts,
    @Schema(description = "Facturado de contado en el periodo") BigDecimal cashSalesTotal,
    @Schema(description = "Facturado a crédito en el periodo") BigDecimal creditSalesTotal,
    @Schema(description = "Facturado / facturas emitidas") BigDecimal averageTicket,
    @Schema(description = "Saldo pendiente de todas las ventas a crédito, a hoy") BigDecimal receivablesPending,
    @Schema(description = "Parte del saldo pendiente con fecha de vencimiento ya pasada") BigDecimal receivablesOverdue,
    @Schema(description = "Productos activos en su stock mínimo o por debajo, a hoy") long lowStockProducts
) {}
