-- V5__medshop_base.sql
-- Base schema and tables for MedShop multi-tenant module

CREATE SCHEMA IF NOT EXISTS medshop;

-- Tenants (Pharmacy Stores)
CREATE TABLE IF NOT EXISTS medshop.tenants (
    id VARCHAR(36) PRIMARY KEY,
    shop_name VARCHAR(150) NOT NULL,
    gstin VARCHAR(15) UNIQUE,
    state_code CHAR(2) NOT NULL,
    address TEXT NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    plan VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_composition_dealer BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users (Staff & Owners)
CREATE TABLE IF NOT EXISTS medshop.users (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    address TEXT,
    profile_pic TEXT,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- OWNER, PHARMACIST, CASHIER
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50),
    last_name VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_medshop_users_tenant_id UNIQUE (tenant_id, id)
);

-- Audit Logs
CREATE TABLE IF NOT EXISTS medshop.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36),
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Categories
CREATE TABLE IF NOT EXISTS medshop.categories (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_medshop_categories_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_medshop_categories_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT fk_medshop_categories_parent FOREIGN KEY (tenant_id, parent_id)
        REFERENCES medshop.categories(tenant_id, id) ON DELETE RESTRICT
);

-- Suppliers
CREATE TABLE IF NOT EXISTS medshop.suppliers (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    gstin VARCHAR(15),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_medshop_suppliers_tenant_id UNIQUE (tenant_id, id)
);

-- Medicines
CREATE TABLE IF NOT EXISTS medshop.medicines (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    generic_name VARCHAR(150),
    hsn_code VARCHAR(10) NOT NULL,
    category_id VARCHAR(36),
    supplier_id VARCHAR(36),
    manufacturer VARCHAR(150),
    form VARCHAR(50),
    strength VARCHAR(50),
    mrp NUMERIC(12,2) NOT NULL,
    purchase_price NUMERIC(12,2) NOT NULL,
    gst_rate NUMERIC(5,2) NOT NULL,
    is_schedule_h BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_medshop_medicines_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_medshop_medicines_category FOREIGN KEY (tenant_id, category_id)
        REFERENCES medshop.categories(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_medshop_medicines_supplier FOREIGN KEY (tenant_id, supplier_id)
        REFERENCES medshop.suppliers(tenant_id, id) ON DELETE RESTRICT
);

-- Stock Batches
CREATE TABLE IF NOT EXISTS medshop.stock_batches (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    medicine_id VARCHAR(36) NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    manufacturing_date DATE,
    expiry_date DATE NOT NULL,
    quantity_received INT NOT NULL CHECK (quantity_received > 0),
    quantity_available INT NOT NULL CHECK (quantity_available >= 0 AND quantity_available <= quantity_received),
    purchase_price NUMERIC(12,2) NOT NULL,
    mrp NUMERIC(12,2) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_medshop_stock_batches_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_medshop_stock_batches_med_batch UNIQUE (tenant_id, medicine_id, batch_number),
    CONSTRAINT fk_medshop_stock_batches_med FOREIGN KEY (tenant_id, medicine_id)
        REFERENCES medshop.medicines(tenant_id, id) ON DELETE RESTRICT
);

-- Stock Transactions Ledger
CREATE TABLE IF NOT EXISTS medshop.stock_transactions (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    medicine_id VARCHAR(36) NOT NULL,
    batch_id VARCHAR(36) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL CHECK (transaction_type IN ('PURCHASE', 'SALE', 'SALE_RETURN', 'PURCHASE_RETURN', 'ADJUSTMENT', 'EXPIRY_WRITE_OFF', 'DAMAGED')),
    quantity INT NOT NULL CHECK (quantity <> 0),
    reason TEXT,
    performed_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_medshop_stock_tx_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_medshop_stock_tx_med FOREIGN KEY (tenant_id, medicine_id)
        REFERENCES medshop.medicines(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_medshop_stock_tx_batch FOREIGN KEY (tenant_id, batch_id)
        REFERENCES medshop.stock_batches(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_medshop_stock_tx_user FOREIGN KEY (tenant_id, performed_by)
        REFERENCES medshop.users(tenant_id, id) ON DELETE RESTRICT
);

-- Customers
CREATE TABLE IF NOT EXISTS medshop.customers (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    gstin VARCHAR(15),
    address TEXT,
    state_code CHAR(2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_medshop_customers_tenant_id UNIQUE (tenant_id, id)
);

-- Invoice Sequences (Per Financial Year)
CREATE TABLE IF NOT EXISTS medshop.invoice_sequences (
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    financial_year VARCHAR(7) NOT NULL,
    last_number BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, financial_year)
);

-- Sales Invoices
CREATE TABLE IF NOT EXISTS medshop.sales_invoices (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    invoice_number VARCHAR(16) NOT NULL,
    customer_id VARCHAR(36),
    created_by VARCHAR(36) NOT NULL,
    invoice_date DATE NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    cgst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(12,2) NOT NULL,
    payment_mode VARCHAR(20) NOT NULL CHECK (payment_mode IN ('CASH', 'UPI', 'CARD', 'CREDIT')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'FINALIZED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_medshop_sales_invoices_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_medshop_sales_invoices_num UNIQUE (tenant_id, invoice_number),
    CONSTRAINT fk_medshop_sales_invoices_cust FOREIGN KEY (tenant_id, customer_id)
        REFERENCES medshop.customers(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_medshop_sales_invoices_user FOREIGN KEY (tenant_id, created_by)
        REFERENCES medshop.users(tenant_id, id) ON DELETE RESTRICT
);

-- Invoice Items
CREATE TABLE IF NOT EXISTS medshop.invoice_items (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    invoice_id VARCHAR(36) NOT NULL,
    medicine_id VARCHAR(36) NOT NULL,
    batch_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL,
    taxable_value NUMERIC(12,2) NOT NULL,
    cgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    sgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    igst_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(12,2) NOT NULL,
    CONSTRAINT uq_medshop_invoice_items_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_medshop_invoice_items_inv FOREIGN KEY (tenant_id, invoice_id)
        REFERENCES medshop.sales_invoices(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_medshop_invoice_items_med FOREIGN KEY (tenant_id, medicine_id)
        REFERENCES medshop.medicines(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_medshop_invoice_items_batch FOREIGN KEY (tenant_id, batch_id)
        REFERENCES medshop.stock_batches(tenant_id, id) ON DELETE RESTRICT,
    CHECK (igst_rate = 0 OR (cgst_rate = 0 AND sgst_rate = 0))
);

-- GST Returns
CREATE TABLE IF NOT EXISTS medshop.gst_returns (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL REFERENCES medshop.tenants(id) ON DELETE CASCADE,
    return_month INT NOT NULL CHECK (return_month BETWEEN 1 AND 12),
    return_year INT NOT NULL,
    return_type VARCHAR(10) NOT NULL CHECK (return_type IN ('GSTR1', 'GSTR3B')),
    total_taxable_value NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_cgst NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_sgst NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_igst NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'GENERATED', 'FILED')),
    filed_ref_number VARCHAR(100),
    filed_at TIMESTAMPTZ,
    CONSTRAINT uq_medshop_gst_returns_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_medshop_gst_returns_period UNIQUE (tenant_id, return_year, return_month, return_type)
);

