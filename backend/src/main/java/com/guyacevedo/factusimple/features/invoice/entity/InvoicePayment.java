package com.guyacevedo.factusimple.features.invoice.entity;

import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_payments")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePayment extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** DIAN payment form: 1 = cash (contado), 2 = credit. */
    @NotBlank
    @Column(name = "payment_form", nullable = false, length = 1)
    private String paymentForm;

    @NotBlank
    @Column(name = "payment_method_code", nullable = false, length = 4)
    private String paymentMethodCode;

    @Column(name = "reference_code", length = 50)
    private String referenceCode;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;
}