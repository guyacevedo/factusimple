-- #############################################################################
-- V6. POS
-- #############################################################################

-- ============================================================================
-- HELD_SALES TABLE
-- ============================================================================
-- Cart put on hold to serve another customer. It does not touch inventory:
-- stock is only deducted at checkout, because a held sale may never close.
CREATE TABLE held_sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(120) NOT NULL,
    content JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_held_sale_user_created ON held_sales(user_id, created_at DESC);