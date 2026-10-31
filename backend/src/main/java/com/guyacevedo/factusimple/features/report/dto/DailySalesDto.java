package com.guyacevedo.factusimple.features.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** invoices and grossTotal by the invoice date; cancelledTotal by the date of the cancellation. */
public record DailySalesDto(LocalDate date, long invoices, BigDecimal grossTotal, BigDecimal cancelledTotal,
                            BigDecimal netTotal) {}
