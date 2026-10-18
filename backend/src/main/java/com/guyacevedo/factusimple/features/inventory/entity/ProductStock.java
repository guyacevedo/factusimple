package com.guyacevedo.factusimple.features.inventory.entity;

import com.guyacevedo.factusimple.features.product.entity.Product;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_stocks")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStock extends BaseEntity {

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Builder.Default
    @NotNull
    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    /** True when stock is at or below the product's minimum (loads the product). */
    public boolean belowMinimumQuantity() {
        return product != null
                && quantity.compareTo(product.getMinStock()) <= 0;
    }
}