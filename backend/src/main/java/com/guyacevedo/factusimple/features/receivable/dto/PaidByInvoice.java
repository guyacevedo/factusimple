package com.guyacevedo.factusimple.features.receivable.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** What was paid towards each invoice, from CustomerPaymentRepository.paidByInvoices. */
public record PaidByInvoice(UUID invoiceId, BigDecimal amount) {}
