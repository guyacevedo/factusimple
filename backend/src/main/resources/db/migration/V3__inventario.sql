-- #############################################################################
-- V3. INVENTORY
-- One stock row per product. Every change that is not a sale is recorded in
-- stock_adjustments with its reason.
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
    -- Also serves lookups by product_id, so no separate index is needed.
    CONSTRAINT uk_product_stock_product UNIQUE (product_id),
    -- Last line of defense. The application deducts with a conditional UPDATE
    -- that already prevents going negative.
    CONSTRAINT ck_product_stock_non_negative CHECK (quantity >= 0)
);

-- ============================================================================
-- STOCK_ADJUSTMENTS TABLE
-- ============================================================================
CREATE TABLE stock_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
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