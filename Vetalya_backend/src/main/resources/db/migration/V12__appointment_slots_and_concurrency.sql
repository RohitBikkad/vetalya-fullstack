-- ============================================================
-- V12: Appointment Slots, Concurrency Control & Queue Management
-- ============================================================

-- 1. appointment_slot: Pre-generated slots with pessimistic concurrency controls
CREATE TABLE IF NOT EXISTS appointment_slot (
    slot_id             VARCHAR(26)     PRIMARY KEY,
    hospital_id         VARCHAR(26)     REFERENCES hospital(hospital_id),
    vet_user_id         VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    slot_date           DATE            NOT NULL,
    start_time          TIME            NOT NULL,
    end_time            TIME            NOT NULL,
    slot_type           VARCHAR(30)     NOT NULL DEFAULT 'OPD_CONSULTATION'
                        CHECK (slot_type IN ('OPD_CONSULTATION', 'VACCINATION', 'SURGERY', 'EMERGENCY', 'TELECONSULTATION', 'HOME_VISIT')),
    capacity            INT             NOT NULL DEFAULT 1,
    booked_count        INT             NOT NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE'
                        CHECK (status IN ('AVAILABLE', 'BOOKED', 'BLOCKED', 'CANCELLED')),
    version             INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vet_slot UNIQUE (vet_user_id, slot_date, start_time)
);

CREATE INDEX IF NOT EXISTS idx_slot_vet_date ON appointment_slot(vet_user_id, slot_date);
CREATE INDEX IF NOT EXISTS idx_slot_status ON appointment_slot(status);
CREATE INDEX IF NOT EXISTS idx_slot_hospital_date ON appointment_slot(hospital_id, slot_date);

-- 2. Extend appointment with queue, workflow timestamps and slot linkage
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS slot_id VARCHAR(26) REFERENCES appointment_slot(slot_id);
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS queue_number INT;
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS check_in_time TIMESTAMPTZ;
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS consultation_start_time TIMESTAMPTZ;
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS consultation_end_time TIMESTAMPTZ;
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS appointment_type VARCHAR(30) DEFAULT 'OPD_CONSULTATION';

CREATE INDEX IF NOT EXISTS idx_appointment_slot_id ON appointment(slot_id);
CREATE INDEX IF NOT EXISTS idx_appointment_queue ON appointment(vet_user_id, appointment_at, queue_number);

