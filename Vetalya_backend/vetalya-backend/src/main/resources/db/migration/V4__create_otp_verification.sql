-- ============================================================
-- V4: OTP Verification table for mobile & email verification
-- ============================================================

CREATE TABLE IF NOT EXISTS otp_verification (
    id              BIGSERIAL       PRIMARY KEY,
    target          VARCHAR(255)    NOT NULL,
    otp_code        VARCHAR(10)     NOT NULL,
    purpose         VARCHAR(30)     NOT NULL,
    attempts        INTEGER         NOT NULL DEFAULT 0,
    verified        BOOLEAN         NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_otp_target ON otp_verification(target);
CREATE INDEX IF NOT EXISTS idx_otp_target_purpose ON otp_verification(target, purpose);
