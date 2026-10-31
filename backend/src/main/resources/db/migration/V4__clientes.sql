-- #############################################################################
-- V4. CUSTOMERS
-- #############################################################################

-- ============================================================================
-- CUSTOMERS TABLE
-- ============================================================================
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id_type_code VARCHAR(4) NOT NULL,
    identification VARCHAR(30) NOT NULL,
    dv VARCHAR(2),
    legal_org_code VARCHAR(10),
    company VARCHAR(255),
    names VARCHAR(255),
    trade_name VARCHAR(255),
    address VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    municipality_code VARCHAR(10),
    tribute_code VARCHAR(4),
    fiscal_responsibility VARCHAR(50),
    credit_limit NUMERIC(15, 2) NOT NULL DEFAULT 0,
    credit_balance NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_customer_user_identification UNIQUE (user_id, identification),
    CONSTRAINT ck_customer_credit CHECK (credit_limit >= 0 AND credit_balance >= 0)
);
CREATE INDEX idx_customer_identification ON customers(identification);
CREATE INDEX idx_customer_user_active ON customers(user_id, is_active);