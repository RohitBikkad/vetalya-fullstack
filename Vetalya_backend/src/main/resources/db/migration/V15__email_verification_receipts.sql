-- Existing in-flight codes expire when moving from plaintext to hashed OTP storage.
ALTER TABLE otp_verification ALTER COLUMN otp_code TYPE VARCHAR(100);
ALTER TABLE otp_verification ADD COLUMN verification_token_hash VARCHAR(64);
UPDATE otp_verification SET expires_at = LEAST(expires_at, CURRENT_TIMESTAMP), attempts = 5;
CREATE INDEX IF NOT EXISTS idx_otp_target_purpose_created ON otp_verification (target, purpose, created_at DESC);
