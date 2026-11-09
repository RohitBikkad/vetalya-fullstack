-- ============================================================
-- V4: Security Audit Log and Distributed Scheduler Lock
-- ============================================================

-- audit_log: immutable security and business event record
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    event_type      VARCHAR(50)     NOT NULL,
    user_id         VARCHAR(26)     NULL, -- Deliberately no foreign key so audit records survive account deletion
    role            VARCHAR(20)     NULL,
    provider        VARCHAR(10)     NULL CHECK (provider IN ('GOOGLE', 'APPLE', 'PHONE', NULL)),
    outcome         VARCHAR(20)     NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    reason          VARCHAR(100)    NULL,
    ip_address      VARCHAR(45)     NULL,
    client_type     VARCHAR(10)     NULL,
    device_id       VARCHAR(255)    NULL,
    user_agent      VARCHAR(255)    NULL,
    correlation_id  VARCHAR(64)     NULL,
    details         JSONB           NULL, -- IDs and non-personal metadata only
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Fast queries by user history, event filtering, and time-range retention pruning
CREATE INDEX IF NOT EXISTS idx_audit_log_user_created ON audit_log(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_event_created ON audit_log(event_type, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);

-- shedlock: distributed lock table for scheduled jobs across multiple application instances
CREATE TABLE IF NOT EXISTS shedlock (
    name            VARCHAR(64)     PRIMARY KEY,
    lock_until      TIMESTAMPTZ     NOT NULL,
    locked_at       TIMESTAMPTZ     NOT NULL,
    locked_by       VARCHAR(255)    NOT NULL
);

