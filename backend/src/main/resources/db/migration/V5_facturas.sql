-- #############################################################################
-- V5. BILLING
-- #############################################################################

-- ============================================================================
-- INVOICES TABLE
-- ============================================================================
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    reference_code VARCHAR(100) NOT NULL,
    document_type VARCHAR(4),
    operation_type VARCHAR(20),
    send_email BOOLEAN NOT NULL DEFAULT FALSE,
    observation TEXT,
    cash_rounding NUMERIC(12, 2),
    numbering_range_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    subtotal NUMERIC(16, 2) NOT NULL DEFAULT 0.00,
    total_taxes NUMERIC(16, 2) NOT NULL DEFAULT 0.00,
    total_holding_taxes NUMERIC(16, 2) NOT NULL DEFAULT 0.00,
    total_discounts NUMERIC(16, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(16, 2) NOT NULL DEFAULT 0.00,
    cufe VARCHAR(200),
    xml_url TEXT,
    factus_number VARCHAR(50),
    factus_error TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_invoice_user_reference_code UNIQUE (user_id, reference_code)
);
CREATE INDEX idx_invoice_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoice_reference_code ON invoices(reference_code);
CREATE INDEX idx_invoice_status ON invoices(status);
CREATE INDEX idx_invoice_user_status ON invoices(user_id, status);
CREATE INDEX idx_invoice_user_customer ON invoices(user_id, customer_id);
CREATE INDEX idx_invoice_user_created ON invoices(user_id, created_at);

-- ============================================================================
-- INVOICE_ITEMS TABLE
-- ============================================================================
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    code_reference VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    discount_rate NUMERIC(5, 2),
    note TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_invoice_item_invoice_id ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_item_product_id ON invoice_items(product_id);

-- ============================================================================
-- INVOICE_PAYMENTS TABLE
-- ============================================================================
CREATE TABLE invoice_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    payment_form VARCHAR(1) NOT NULL,
    payment_method_code VARCHAR(4) NOT NULL,
    reference_code VARCHAR(50),
    amount NUMERIC(16, 2) NOT NULL,
    due_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_invoice_payment_invoice_id ON invoice_payments(invoice_id);

-- ============================================================================
-- ALLOWANCE_CHARGES TABLE
-- ============================================================================
CREATE TABLE allowance_charges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    concept_type VARCHAR(4) NOT NULL,
    is_surcharge BOOLEAN NOT NULL,
    reason VARCHAR(255) NOT NULL,
    base_amount NUMERIC(16, 2) NOT NULL,
    amount NUMERIC(16, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_allowance_charge_invoice_id ON allowance_charges(invoice_id);

-- ============================================================================
-- CREDIT_NOTES TABLE
-- ============================================================================
CREATE TABLE credit_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE RESTRICT,
    numbering_range_id INTEGER NOT NULL,
    reference_code VARCHAR(100) NOT NULL,
    correction_concept_code VARCHAR(100) NOT NULL,
    customization_id VARCHAR(2) DEFAULT '20',
    observation TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    factus_number VARCHAR(50),
    cufe VARCHAR(200),
    xml_url TEXT,
    factus_error TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_credit_note_user_reference_code UNIQUE (user_id, reference_code)
);
CREATE INDEX idx_credit_note_invoice_id ON credit_notes(invoice_id);
CREATE INDEX idx_credit_note_customer_id ON credit_notes(customer_id);
CREATE INDEX idx_credit_note_status ON credit_notes(status);