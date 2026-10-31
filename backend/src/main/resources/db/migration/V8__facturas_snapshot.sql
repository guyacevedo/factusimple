-- #############################################################################
-- V8. BILLING SNAPSHOT AND CREDIT BALANCE
-- A PENDING invoice is sent to the DIAN later, so it must keep exactly what was
-- sold: taxes, unit and amounts are copied into the line instead of being read
-- from the product, whose taxes may change or which may be deleted
-- (invoice_items.product_id is ON DELETE SET NULL).
-- #############################################################################

-- ============================================================================
-- INVOICE_ITEMS: frozen unit and amounts
-- ============================================================================
-- Prices include taxes: total is what the customer pays for the line and
-- base_amount + tax_amount = total. Withholding taxes are informed apart and do
-- not lower the total.
ALTER TABLE invoice_items
    ADD COLUMN unit_measure_code VARCHAR(10),
    ADD COLUMN standard_code VARCHAR(10),
    ADD COLUMN discount_amount NUMERIC(16, 2) NOT NULL DEFAULT 0,
    ADD COLUMN base_amount NUMERIC(16, 2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_amount NUMERIC(16, 2) NOT NULL DEFAULT 0,
    ADD COLUMN withholding_tax_amount NUMERIC(16, 2) NOT NULL DEFAULT 0,
    ADD COLUMN total NUMERIC(16, 2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_invoice_item_amounts CHECK (
        quantity > 0 AND unit_price >= 0 AND discount_amount >= 0 AND base_amount >= 0
        AND tax_amount >= 0 AND withholding_tax_amount >= 0 AND total >= 0),
    ADD CONSTRAINT ck_invoice_item_discount_rate CHECK (
        discount_rate IS NULL OR discount_rate BETWEEN 0 AND 100);

-- ============================================================================
-- INVOICE_ITEM_TAXES TABLE
-- ============================================================================
CREATE TABLE invoice_item_taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    invoice_item_id UUID NOT NULL REFERENCES invoice_items(id) ON DELETE CASCADE,
    tax_code VARCHAR(4) NOT NULL,
    tax_rate NUMERIC(5, 2) NOT NULL,
    is_excluded BOOLEAN NOT NULL DEFAULT FALSE,
    base_amount NUMERIC(16, 2) NOT NULL,
    amount NUMERIC(16, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT ck_invoice_item_tax_amounts CHECK (base_amount >= 0 AND amount >= 0)
);
CREATE INDEX idx_invoice_item_tax_item_id ON invoice_item_taxes(invoice_item_id);

-- ============================================================================
-- INVOICE_ITEM_WITHHOLDING_TAXES TABLE
-- ============================================================================
CREATE TABLE invoice_item_withholding_taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    invoice_item_id UUID NOT NULL REFERENCES invoice_items(id) ON DELETE CASCADE,
    withholding_tax_code VARCHAR(4) NOT NULL,
    withholding_tax_rate NUMERIC(5, 2) NOT NULL,
    base_amount NUMERIC(16, 2) NOT NULL,
    amount NUMERIC(16, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT ck_invoice_item_wh_tax_amounts CHECK (base_amount >= 0 AND amount >= 0)
);
CREATE INDEX idx_invoice_item_wh_tax_item_id ON invoice_item_withholding_taxes(invoice_item_id);

-- ============================================================================
-- INVOICES: balance due
-- ============================================================================
-- What the customer still owes on a credit invoice. Credit payments lower it;
-- cancelling the invoice releases whatever is left. Always 0 for cash sales.
ALTER TABLE invoices
    ADD COLUMN balance_due NUMERIC(16, 2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_invoice_balance_due CHECK (balance_due >= 0 AND balance_due <= total);
CREATE INDEX idx_invoice_customer_balance ON invoices(customer_id, created_at) WHERE balance_due > 0;

-- ============================================================================
-- INVOICE_PAYMENTS: missing checks
-- ============================================================================
-- payment_form is the DIAN code: 1 = cash (contado), 2 = credit.
ALTER TABLE invoice_payments
    ADD CONSTRAINT ck_invoice_payment_form CHECK (payment_form IN ('1', '2')),
    ADD CONSTRAINT ck_invoice_payment_amount CHECK (amount > 0);
