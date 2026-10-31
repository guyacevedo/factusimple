package com.guyacevedo.factusimple.features.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of a grouped query: how many invoices and for how much. */
public record DayAmountRow(LocalDate date, long invoices, BigDecimal total) {}
