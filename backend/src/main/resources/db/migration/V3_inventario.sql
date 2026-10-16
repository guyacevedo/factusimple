-- #############################################################################
-- V3. INVENTORY
-- #############################################################################

-- ============================================================================
-- PRODUCT_STOCKS TABLE
-- ============================================================================
CREATE TABLE product_stocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity NUMERIC(12, 3) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_product_stock_product_location UNIQUE (product_id, location),
    CONSTRAINT ck_product_stock_non_negative CHECK (quantity >= 0)
);

-- ============================================================================
-- STOCK_ADJUSTMENTS TABLE
-- ============================================================================
CREATE TABLE stock_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    product_id UUID NOT NULL REFERENCES products(id),
    previous_quantity NUMERIC(12, 3) NOT NULL,
    new_quantity NUMERIC(12, 3) NOT NULL,
    difference NUMERIC(12, 3) NOT NULL,
    reason VARCHAR(30) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT ck_stock_adjustment_quantities CHECK (
        previous_quantity >= 0 AND new_quantity >= 0),
    CONSTRAINT ck_stock_adjustment_difference CHECK (
        difference = new_quantity - previous_quantity),
    CONSTRAINT ck_stock_adjustment_reason CHECK (reason IN
        ('PHYSICAL_COUNT', 'SHRINKAGE', 'DAMAGE', 'EXPIRATION', 'RECEIPT', 'RETURN', 'CORRECTION'))
);
CREATE INDEX idx_stock_adjustment_product_date ON stock_adjustments(product_id, created_at DESC);