-- ============================================================
-- V11: Clinical Encounters, Cryptographic Signing & Prescriptions
-- ============================================================

-- 1. clinical_encounter: Structured clinical SOAP encounters with signing
CREATE TABLE IF NOT EXISTS clinical_encounter (
    encounter_id            VARCHAR(26)     PRIMARY KEY,
    pet_id                  VARCHAR(26)     NOT NULL REFERENCES pet(pet_id) ON DELETE CASCADE,
    vet_user_id             VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    hospital_id             VARCHAR(26)     REFERENCES hospital(hospital_id),
    appointment_id          VARCHAR(26)     REFERENCES appointment(appointment_id),
    encounter_type          VARCHAR(50)     NOT NULL DEFAULT 'OPD_CONSULTATION', -- OPD_CONSULTATION, EMERGENCY, FOLLOW_UP, SURGERY_CONSULT
    chief_complaint         TEXT,
    presenting_history      TEXT,
    physical_examination    TEXT,
    temperature_f           NUMERIC(5,2),
    heart_rate_bpm          INT,
    respiratory_rate_bpm    INT,
    body_weight_kg          NUMERIC(6,2),
    body_condition_score    INT CHECK (body_condition_score BETWEEN 1 AND 9),
    assessment_notes        TEXT,
    diagnosis_primary       VARCHAR(255),
    differential_diagnosis  TEXT,
    treatment_plan          TEXT,
    diet_instructions       TEXT,
    follow_up_date          DATE,
    is_signed               BOOLEAN         NOT NULL DEFAULT FALSE,
    signed_at               TIMESTAMPTZ,
    digital_signature_hash  VARCHAR(64),
    is_passport_published   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_encounter_pet_id ON clinical_encounter(pet_id);
CREATE INDEX IF NOT EXISTS idx_encounter_vet_user_id ON clinical_encounter(vet_user_id);
CREATE INDEX IF NOT EXISTS idx_encounter_appointment_id ON clinical_encounter(appointment_id);
CREATE INDEX IF NOT EXISTS idx_encounter_signed ON clinical_encounter(is_signed);

-- 2. encounter_amendment: Auditable corrections for signed encounters
CREATE TABLE IF NOT EXISTS encounter_amendment (
    amendment_id            VARCHAR(26)     PRIMARY KEY,
    encounter_id            VARCHAR(26)     NOT NULL REFERENCES clinical_encounter(encounter_id) ON DELETE CASCADE,
    amended_by_vet_id       VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    amendment_reason        TEXT            NOT NULL,
    prior_snapshot_json     TEXT            NOT NULL,
    amended_content_json    TEXT            NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_amendment_encounter_id ON encounter_amendment(encounter_id);

-- 3. prescription: Digital prescriptions linked to encounters
CREATE TABLE IF NOT EXISTS prescription (
    prescription_id         VARCHAR(26)     PRIMARY KEY,
    encounter_id            VARCHAR(26)     REFERENCES clinical_encounter(encounter_id) ON DELETE SET NULL,
    pet_id                  VARCHAR(26)     NOT NULL REFERENCES pet(pet_id) ON DELETE CASCADE,
    vet_user_id             VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    rx_date                 DATE            NOT NULL DEFAULT CURRENT_DATE,
    instructions            TEXT,
    pdf_storage_url         VARCHAR(500),
    is_dispensed            BOOLEAN         NOT NULL DEFAULT FALSE,
    dispensed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prescription_pet_id ON prescription(pet_id);
CREATE INDEX IF NOT EXISTS idx_prescription_encounter_id ON prescription(encounter_id);
CREATE INDEX IF NOT EXISTS idx_prescription_vet_id ON prescription(vet_user_id);

-- 4. prescription_item: Itemized medications with Schedule H flags
CREATE TABLE IF NOT EXISTS prescription_item (
    item_id                 BIGSERIAL       PRIMARY KEY,
    prescription_id         VARCHAR(26)     NOT NULL REFERENCES prescription(prescription_id) ON DELETE CASCADE,
    medicine_name           VARCHAR(200)    NOT NULL,
    generic_composition     VARCHAR(200),
    dosage                  VARCHAR(100)    NOT NULL,
    frequency               VARCHAR(100)    NOT NULL, -- OD, BID, TID, QID, SOS, STAT
    duration                VARCHAR(100)    NOT NULL,
    instructions            VARCHAR(255),
    is_schedule_h           BOOLEAN         NOT NULL DEFAULT FALSE,
    quantity                INT             DEFAULT 1,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prescription_item_rx_id ON prescription_item(prescription_id);

