package com.guyacevedo.factusimple.features.product.entity;

import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_withholding_taxes")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWithholdingTax extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank
    @Column(name = "withholding_tax_code", nullable = false, length = 4)
    private String withholdingTaxCode;

    @NotNull
    @Column(name = "withholding_tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal withholdingTaxRate;
}