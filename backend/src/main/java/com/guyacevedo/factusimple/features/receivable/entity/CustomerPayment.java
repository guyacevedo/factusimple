package com.guyacevedo.factusimple.features.receivable.entity;

import com.guyacevedo.factusimple.features.customer.entity.Customer;
import com.guyacevedo.factusimple.features.invoice.entity.Invoice;
import com.guyacevedo.factusimple.features.user.entity.User;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A payment towards a credit invoice (abono). A payment to the customer's
 * overall debt is stored as one row per invoice it settled, so every row says
 * exactly whose balance it lowered (see V9).
 */
@Entity
@Table(name = "customer_payments")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPayment extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    /** DIAN payment method code, the same catalog as invoice_payments (10 = cash, ...). */
    @Column(name = "payment_method_code", nullable = false, length = 4)
    private String paymentMethodCode;

    @Column(name = "reference_code", length = 50)
    private String referenceCode;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
