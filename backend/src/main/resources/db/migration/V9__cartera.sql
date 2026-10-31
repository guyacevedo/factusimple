-- #############################################################################
-- V9. RECEIVABLES (customer payments towards credit invoices)
-- #############################################################################

-- ============================================================================
-- CUSTOMER_PAYMENTS TABLE
-- ============================================================================
-- All debt comes from a credit invoice, so every payment points to the invoice
-- whose balance_due it lowered. A payment towards the customer's overall debt
-- is stored as one row per invoice it settles, oldest invoice first.
-- payment_method_code uses the same DIAN codes as invoice_payments.
CREATE TABLE customer_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE RESTRICT,
    amount NUMERIC(16, 2) NOT NULL,
    payment_method_code VARCHAR(4) NOT NULL,
    reference_code VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT ck_customer_payment_amount CHECK (amount > 0)
);
CREATE INDEX idx_customer_payment_customer ON customer_payments(customer_id, created_at DESC);
CREATE INDEX idx_customer_payment_invoice ON customer_payments(invoice_id);
