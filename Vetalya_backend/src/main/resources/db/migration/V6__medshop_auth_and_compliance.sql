-- V6__medshop_auth_and_compliance.sql
-- Auth enhancements, compliance (Schedule H), idempotency, and query indexes for MedShop

-- 1. Support social-only users and token invalidation
ALTER TABLE medshop.users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE medshop.users ADD COLUMN IF NOT EXISTS token_version INT NOT NULL DEFAULT 0;

-- 2. Social Auth Providers (Google & Apple)
CREATE TABLE IF NOT EXISTS medshop.auth_providers (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GOOGLE', 'APPLE')),
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_auth_providers_provider_sub UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_auth_providers_user FOREIGN KEY (tenant_id, user_id)
        REFERENCES medshop.users(tenant_id, id) ON DELETE CASCADE
);

-- 3. Refresh Tokens with Family Reuse Detection
CREATE TABLE IF NOT EXISTS medshop.refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) UNIQUE NOT NULL,
    family_id UUID NOT NULL,
    client_type VARCHAR(10), -- web, android, ios
    device_id VARCHAR(100),
    ip_address VARCHAR(45),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (tenant_id, user_id)
        REFERENCES medshop.users(tenant_id, id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON medshop.refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family ON medshop.refresh_tokens (family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked ON medshop.refresh_tokens (tenant_id, user_id, revoked);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry ON medshop.refresh_tokens (expires_at);

-- 4. Auth Exchange Codes (One-time codes for Google/Apple redirect flow across instances)
CREATE TABLE IF NOT EXISTS medshop.auth_exchange_codes (
    code_hash VARCHAR(64) PRIMARY KEY,
    payload JSONB NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_exchange_codes_expiry ON medshop.auth_exchange_codes (expires_at);

-- 5. Schedule H Drug Register (Mandatory patient & doctor records under Indian Drugs & Cosmetics Rules)
CREATE TABLE IF NOT EXISTS medshop.schedule_h_register (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    invoice_id VARCHAR(36) NOT NULL,
    invoice_item_id VARCHAR(36) NOT NULL,
    medicine_id VARCHAR(36) NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    patient_age INT,
    doctor_name VARCHAR(100) NOT NULL,
    doctor_registration_no VARCHAR(50) NOT NULL,
    prescription_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_h_inv FOREIGN KEY (tenant_id, invoice_id)
        REFERENCES medshop.sales_invoices(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_schedule_h_item FOREIGN KEY (tenant_id, invoice_item_id)
        REFERENCES medshop.invoice_items(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_schedule_h_med FOREIGN KEY (tenant_id, medicine_id)
        REFERENCES medshop.medicines(tenant_id, id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_schedule_h_tenant_date ON medshop.schedule_h_register (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_schedule_h_tenant_med ON medshop.schedule_h_register (tenant_id, medicine_id);

-- 6. Billing Idempotency Keys (Prevent double-invoicing from retries or double-clicks)
CREATE TABLE IF NOT EXISTS medshop.idempotency_keys (
    tenant_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    invoice_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, idempotency_key),
    CONSTRAINT fk_idempotency_invoice FOREIGN KEY (tenant_id, invoice_id)
        REFERENCES medshop.sales_invoices(tenant_id, id) ON DELETE CASCADE
);

-- 7. Low-Stock Reorder Level Threshold
ALTER TABLE medshop.medicines ADD COLUMN IF NOT EXISTS reorder_level INT NOT NULL DEFAULT 10;

-- 8. Query and Reporting Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_sales_invoices_query ON medshop.sales_invoices (tenant_id, status, invoice_date);
CREATE INDEX IF NOT EXISTS idx_stock_tx_query ON medshop.stock_transactions (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_batches_expiry ON medshop.stock_batches (tenant_id, medicine_id, expiry_date);
CREATE INDEX IF NOT EXISTS idx_medicines_name ON medshop.medicines (tenant_id, name);
CREATE INDEX IF NOT EXISTS idx_medicines_generic_name ON medshop.medicines (tenant_id, generic_name);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_entity ON medshop.audit_logs (tenant_id, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created ON medshop.audit_logs (tenant_id, created_at DESC);

