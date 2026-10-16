-- #############################################################################
-- V1. PLATFORM
-- #############################################################################
 
-- ============================================================================
-- PLANS TABLE
-- ============================================================================
CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    max_products INTEGER NOT NULL CHECK (max_products >= 1),
    max_customers INTEGER NOT NULL CHECK (max_customers >= 1),
    max_invoices INTEGER NOT NULL CHECK (max_invoices >= 1),
    required_invited_code BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(255),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_plan_name UNIQUE (name)
);
 
-- ============================================================================
-- USERS TABLE
-- ============================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    plan_id UUID NOT NULL REFERENCES plans(id),
    role VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(255),
    municipality_code VARCHAR(10),
    nit VARCHAR(20) NOT NULL,
    dv VARCHAR(2),
    legal_org_code VARCHAR(10),
    tribute_code VARCHAR(4),
    fiscal_responsibility VARCHAR(50),
    product_count INTEGER NOT NULL DEFAULT 0,
    customer_count INTEGER NOT NULL DEFAULT 0,
    invoice_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    blocked_at TIMESTAMP,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT ck_user_role CHECK (role IN ('ADMIN', 'ESTABLISHMENT'))
);
CREATE UNIQUE INDEX uk_user_email ON users(LOWER(email));
CREATE INDEX idx_user_plan_id ON users(plan_id);
 
-- ============================================================================
-- TOKENS TABLE
-- ============================================================================
CREATE TABLE tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    value VARCHAR(512) NOT NULL,
    type VARCHAR(20) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_token_value UNIQUE (value),
    CONSTRAINT ck_token_type CHECK (type IN
        ('ACCESS', 'REFRESH', 'FACTUS_ACCESS', 'FACTUS_REFRESH'))
);
CREATE INDEX idx_token_user_active ON tokens(user_id) WHERE revoked = FALSE;
CREATE INDEX idx_token_expires_at ON tokens(expires_at);
 
-- ============================================================================
-- RESET PASSWORDS TABLE
-- ============================================================================
CREATE TABLE reset_passwords (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(120) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_reset_password_user_unused ON reset_passwords(user_id) WHERE used = FALSE;
 
-- ============================================================================
-- LOGIN ATTEMPTS TABLE
-- ============================================================================
CREATE TABLE login_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(100) NOT NULL,
    successful BOOLEAN NOT NULL,
    ip VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_login_attempt_username_date ON login_attempts(username, created_at DESC);
 
-- ============================================================================
-- PARAMETERS TABLE
-- ============================================================================
CREATE TABLE parameters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    key VARCHAR(80) NOT NULL,
    value TEXT,
    description VARCHAR(255),
    encryption BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_parameter_key UNIQUE (key)
);