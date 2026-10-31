package com.guyacevedo.factusimple.features.inventory.entity;

import com.guyacevedo.factusimple.features.product.entity.Product;
import com.guyacevedo.factusimple.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_adjustments")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustment extends BaseEntity {

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "previous_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal previousQuantity;

    @Column(name = "new_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal newQuantity;

    @Setter(AccessLevel.NONE)
    @Column(name = "difference", nullable = false, precision = 12, scale = 3)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private AdjustmentReason reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    @PreUpdate
    void computeDifference() {
        if (previousQuantity != null && newQuantity != null) {
            difference = newQuantity.subtract(previousQuantity);
        }
    }
}