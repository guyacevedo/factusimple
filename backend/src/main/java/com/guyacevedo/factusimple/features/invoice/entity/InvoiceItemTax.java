package com.guyacevedo.factusimple.features.invoice.entity;

import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** A tax of the product copied into the sold line, with what it amounted to. */
@Entity
@Table(name = "invoice_item_taxes")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemTax extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_item_id", nullable = false)
    private InvoiceItem invoiceItem;

    @Column(name = "tax_code", nullable = false, length = 4)
    private String taxCode;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Builder.Default
    @Column(name = "is_excluded", nullable = false)
    private Boolean excluded = false;

    @Column(name = "base_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;
}
