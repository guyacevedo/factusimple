package com.guyacevedo.factusimple.features.report.dto;

import java.math.BigDecimal;

/** Sold amount per DIAN payment form (1 = cash, 2 = credit). */
public record PaymentFormTotalRow(String paymentForm, BigDecimal amount) {}
