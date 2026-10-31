-- #############################################################################
-- V2. CATALOG
-- #############################################################################

-- ============================================================================
-- CATEGORIES TABLE
-- ============================================================================
-- Each establishment manages its own categories.
-- "sort_order" replaces "order", which is a reserved word in PostgreSQL.
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE UNIQUE INDEX uk_category_user_name ON categories(user_id, LOWER(name));

-- ============================================================================
-- PRODUCTS TABLE
-- ============================================================================
-- Quantities use 3 decimals so weight sales (e.g. 0.375 kg) are exact and
-- match product_stocks and invoice_items.
-- unit_measure_code values match the UnitMeasurement enum in Java.
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id),
    sku VARCHAR(50) NOT NULL,
    barcode VARCHAR(60),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    retail_price NUMERIC(12, 2) NOT NULL,
    wholesale_price NUMERIC(12, 2) NOT NULL,
    cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    min_stock_wholesale NUMERIC(12, 3) NOT NULL DEFAULT 1,
    min_stock NUMERIC(12, 3) NOT NULL DEFAULT 0,
    unit_measure_code VARCHAR(10) NOT NULL DEFAULT '94',
    standard_code VARCHAR(10),
    sale_type VARCHAR(20) NOT NULL DEFAULT 'BY_UNIT',
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT ck_product_sale_type CHECK (sale_type IN ('BY_UNIT', 'BY_WEIGHT', 'BY_VOLUME')),
    CONSTRAINT ck_product_unit CHECK (unit_measure_code IN
        ('94', 'SET', 'DZN', 'DPR', 'KGM', 'LBR', 'GRM', 'ONZ', 'LTR', 'MLT', 'GLL')),
    CONSTRAINT ck_product_prices CHECK (
        retail_price >= 0 AND wholesale_price >= 0 AND cost_price >= 0),
    CONSTRAINT ck_product_quantities CHECK (
        min_stock_wholesale > 0 AND min_stock >= 0),
    -- ONZ is an ounce of weight, so it only belongs to BY_WEIGHT.
    CONSTRAINT ck_product_unit_consistency CHECK (
        (sale_type = 'BY_UNIT' AND unit_measure_code IN ('94', 'SET', 'DZN', 'DPR'))
        OR
        (sale_type = 'BY_WEIGHT' AND unit_measure_code IN ('KGM', 'LBR', 'GRM', 'ONZ'))
        OR
        (sale_type = 'BY_VOLUME' AND unit_measure_code IN ('LTR', 'MLT', 'GLL'))
    )
);
-- SKU and barcode are unique per establishment, not across the platform.
CREATE UNIQUE INDEX uk_product_user_sku ON products(user_id, UPPER(sku));
CREATE UNIQUE INDEX uk_product_user_barcode ON products(user_id, barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_product_category ON products(category_id) WHERE is_active = TRUE;
CREATE INDEX idx_product_user_name ON products(user_id, LOWER(name));

-- ============================================================================
-- PRODUCT_TAXES TABLE
-- ============================================================================
CREATE TABLE product_taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tax_code VARCHAR(4) NOT NULL,
    tax_rate NUMERIC(5, 2) NOT NULL,
    is_excluded BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_product_tax_product_id ON product_taxes(product_id);

-- ============================================================================
-- PRODUCT_WITHHOLDING_TAXES TABLE
-- ============================================================================
CREATE TABLE product_withholding_taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    withholding_tax_code VARCHAR(4) NOT NULL,
    withholding_tax_rate NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_product_withholding_tax_product_id ON product_withholding_taxes(product_id);