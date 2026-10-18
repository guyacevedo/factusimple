package com.guyacevedo.factusimple.features.invoice.entity;

import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "allowance_charges")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowanceCharge extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @NotBlank
    @Column(name = "concept_type", nullable = false, length = 4)
    private String conceptType;

    @NotNull
    @Column(name = "is_surcharge", nullable = false)
    private Boolean surcharge; // false = descuento, true = recargo

    @NotBlank
    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @NotNull
    @Column(name = "base_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal baseAmount;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;
}