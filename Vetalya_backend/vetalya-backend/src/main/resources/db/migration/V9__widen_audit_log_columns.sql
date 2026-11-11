-- ============================================================
-- V9: Widen audit_log columns to prevent truncation errors
-- ============================================================

ALTER TABLE audit_log ALTER COLUMN role TYPE VARCHAR(100);
ALTER TABLE audit_log ALTER COLUMN client_type TYPE VARCHAR(50);
ALTER TABLE audit_log ALTER COLUMN provider TYPE VARCHAR(50);

-- Drop restrictive check constraint so JWT, GOOGLE, APPLE, OTP, FAST2SMS are all supported
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_provider_check;
