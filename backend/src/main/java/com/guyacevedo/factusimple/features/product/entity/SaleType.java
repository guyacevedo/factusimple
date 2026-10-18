package com.guyacevedo.factusimple.features.product.entity;

import java.math.BigDecimal;

public enum SaleType {
    BY_UNIT,
    BY_WEIGHT,
    BY_VOLUME;

    public boolean allowsDecimals() {
        return this == BY_WEIGHT || this == BY_VOLUME;
    }

    public boolean validQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            return false;
        }
        if (this == BY_UNIT) {
            return quantity.stripTrailingZeros().scale() <= 0;
        }
        return true;
    }
}