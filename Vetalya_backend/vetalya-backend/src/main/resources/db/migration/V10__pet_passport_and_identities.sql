-- ============================================================
-- V10: Digital Pet Health Passport, Identifiers & Record Sharing
-- ============================================================

-- 1. Enhance existing 'pet' table with passport fields
ALTER TABLE pet ADD COLUMN IF NOT EXISTS passport_number VARCHAR(30) UNIQUE;
ALTER TABLE pet ADD COLUMN IF NOT EXISTS color VARCHAR(100);
ALTER TABLE pet ADD COLUMN IF NOT EXISTS distinctive_marks VARCHAR(255);
ALTER TABLE pet ADD COLUMN IF NOT EXISTS microchip_number VARCHAR(50);
ALTER TABLE pet ADD COLUMN IF NOT EXISTS weight_kg NUMERIC(6,2);
ALTER TABLE pet ADD COLUMN IF NOT EXISTS sterilization_status VARCHAR(20) DEFAULT 'UNKNOWN';
ALTER TABLE pet ADD COLUMN IF NOT EXISTS is_deceased BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_pet_microchip_number ON pet(microchip_number);
CREATE INDEX IF NOT EXISTS idx_pet_passport_number ON pet(passport_number);

-- 2. pet_identifier: Multi-tag support (Microchips, RFID ear tags, Pashu Aadhaar)
CREATE TABLE IF NOT EXISTS pet_identifier (
    id                  BIGSERIAL       PRIMARY KEY,
    pet_id              VARCHAR(26)     NOT NULL REFERENCES pet(pet_id) ON DELETE CASCADE,
    identifier_type     VARCHAR(50)     NOT NULL, -- MICROCHIP, RFID_TAG, REGISTRATION_TAG, PASHU_AADHAAR
    identifier_value    VARCHAR(100)    NOT NULL,
    issuing_authority   VARCHAR(150),
    verified_by_vet_id  VARCHAR(26)     REFERENCES user_auth(user_id),
    verified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pet_identifier_pet_id ON pet_identifier(pet_id);
CREATE INDEX IF NOT EXISTS idx_pet_identifier_val ON pet_identifier(identifier_value);

-- 3. pet_health_passport: Lifetime digital health passport
CREATE TABLE IF NOT EXISTS pet_health_passport (
    passport_id                 VARCHAR(26)     PRIMARY KEY,
    pet_id                      VARCHAR(26)     NOT NULL UNIQUE REFERENCES pet(pet_id) ON DELETE CASCADE,
    passport_number             VARCHAR(30)     NOT NULL UNIQUE,
    blood_group                 VARCHAR(20),
    allergies_summary           TEXT,
    chronic_conditions_summary  TEXT,
    dietary_notes               TEXT,
    emergency_contact_phone     VARCHAR(20),
    public_emergency_qr_hash    VARCHAR(64)     UNIQUE,
    qr_revoked                  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_health_passport_pet_id ON pet_health_passport(pet_id);
CREATE INDEX IF NOT EXISTS idx_health_passport_qr ON pet_health_passport(public_emergency_qr_hash);

-- 4. record_share_request: Multi-clinic consent-gated record sharing
CREATE TABLE IF NOT EXISTS record_share_request (
    request_id          VARCHAR(26)     PRIMARY KEY,
    pet_id              VARCHAR(26)     NOT NULL REFERENCES pet(pet_id) ON DELETE CASCADE,
    requester_user_id   VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    target_hospital_id  VARCHAR(26)     REFERENCES hospital(hospital_id),
    scope               VARCHAR(50)     NOT NULL DEFAULT 'FULL_TIMELINE', -- FULL_TIMELINE, VACCINES_ONLY, EMERGENCY_SUMMARY
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED', 'EXPIRED')),
    expires_at          TIMESTAMPTZ,
    approved_at         TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_record_share_pet_id ON record_share_request(pet_id);
CREATE INDEX IF NOT EXISTS idx_record_share_requester ON record_share_request(requester_user_id);

-- 5. pet_document: Category-tagged document vault
CREATE TABLE IF NOT EXISTS pet_document (
    document_id             VARCHAR(26)     PRIMARY KEY,
    pet_id                  VARCHAR(26)     NOT NULL REFERENCES pet(pet_id) ON DELETE CASCADE,
    uploaded_by_user_id     VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    category                VARCHAR(50)     NOT NULL, -- VACCINATION_CERTIFICATE, PRESCRIPTION, LAB_REPORT, XRAY, ULTRASOUND, SURGERY_REPORT, DISCHARGE_SUMMARY, INSURANCE_DOC, ADOPTION_DOC, MICROCHIP_CERT, OTHER
    file_name               VARCHAR(255)    NOT NULL,
    file_type               VARCHAR(100)    NOT NULL,
    file_size_bytes         BIGINT          NOT NULL,
    storage_path            VARCHAR(500)    NOT NULL,
    description             VARCHAR(500),
    document_date           DATE,
    verification_status     VARCHAR(20)     NOT NULL DEFAULT 'OWNER_UPLOADED' CHECK (verification_status IN ('VERIFIED_BY_VET', 'OWNER_UPLOADED', 'PENDING_VERIFICATION')),
    verified_by_vet_id      VARCHAR(26)     REFERENCES user_auth(user_id),
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pet_document_pet_id ON pet_document(pet_id);
CREATE INDEX IF NOT EXISTS idx_pet_document_category ON pet_document(category);

-- 6. Backfill existing pets with persistent passport numbers and health passport records
UPDATE pet
SET passport_number = 'VET-' || TO_CHAR(created_at, 'YYYY') || '-' || UPPER(SUBSTRING(pet_id FROM GREATEST(1, LENGTH(pet_id) - 5)))
WHERE passport_number IS NULL;

INSERT INTO pet_health_passport (passport_id, pet_id, passport_number, created_at, updated_at)
SELECT pet_id, pet_id, passport_number, created_at, updated_at
FROM pet
ON CONFLICT (pet_id) DO NOTHING;

