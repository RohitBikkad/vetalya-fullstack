-- ============================================================
-- V2: Tables for Vet API features
-- ============================================================

-- hospital: vet-owned hospitals / clinics
CREATE TABLE IF NOT EXISTS hospital (
    hospital_id     VARCHAR(26)     PRIMARY KEY,
    vet_user_id     VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id) ON DELETE CASCADE,
    name            VARCHAR(200)    NOT NULL,
    address1        VARCHAR(255)    NOT NULL,
    address2        VARCHAR(255),
    city            VARCHAR(100)    NOT NULL,
    state           VARCHAR(100)    NOT NULL,
    pincode         VARCHAR(6)      NOT NULL,
    country         VARCHAR(100)    NOT NULL DEFAULT 'INDIA',
    mobile          VARCHAR(20)     NOT NULL,
    email           VARCHAR(255),
    gst_number      VARCHAR(15),
    is_24x7         BOOLEAN         NOT NULL DEFAULT FALSE,
    open_time       TIME,
    close_time      TIME,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    version         INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hospital_vet_user_id ON hospital(vet_user_id);
CREATE INDEX IF NOT EXISTS idx_hospital_pincode ON hospital(pincode);

-- pet: pets belonging to pet_owners
CREATE TABLE IF NOT EXISTS pet (
    pet_id          VARCHAR(26)     PRIMARY KEY,
    owner_user_id   VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id) ON DELETE CASCADE,
    name            VARCHAR(100)    NOT NULL,
    species         VARCHAR(50)     NOT NULL,
    breed           VARCHAR(100),
    dob             DATE,
    gender          VARCHAR(10),
    photo_url       VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pet_owner_user_id ON pet(owner_user_id);

-- appointment: links pet_owner, pet, vet, and hospital
CREATE TABLE IF NOT EXISTS appointment (
    appointment_id  VARCHAR(26)     PRIMARY KEY,
    pet_id          VARCHAR(26)     NOT NULL REFERENCES pet(pet_id),
    owner_user_id   VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    vet_user_id     VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    hospital_id     VARCHAR(26)     NOT NULL REFERENCES hospital(hospital_id),
    appointment_at  TIMESTAMPTZ     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'BOOKED'
                    CHECK (status IN ('BOOKED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    reason          VARCHAR(500),
    visit_notes     TEXT,
    version         INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_appointment_vet_user_id ON appointment(vet_user_id);
CREATE INDEX IF NOT EXISTS idx_appointment_owner_user_id ON appointment(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_appointment_pet_id ON appointment(pet_id);
CREATE INDEX IF NOT EXISTS idx_appointment_hospital_id ON appointment(hospital_id);
CREATE INDEX IF NOT EXISTS idx_appointment_status ON appointment(status);
CREATE INDEX IF NOT EXISTS idx_appointment_appointment_at ON appointment(appointment_at);
-- composite index for vet dashboard queries (status + vet)
CREATE INDEX IF NOT EXISTS idx_appointment_vet_status ON appointment(vet_user_id, status);

-- reminder: linked to appointments
CREATE TABLE IF NOT EXISTS reminder (
    reminder_id     VARCHAR(26)     PRIMARY KEY,
    appointment_id  VARCHAR(26)     NOT NULL REFERENCES appointment(appointment_id) ON DELETE CASCADE,
    remind_at       TIMESTAMPTZ     NOT NULL,
    message         VARCHAR(500),
    is_sent         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reminder_appointment_id ON reminder(appointment_id);
CREATE INDEX IF NOT EXISTS idx_reminder_remind_at ON reminder(remind_at);

-- review: pet_owner reviews a vet at a hospital
CREATE TABLE IF NOT EXISTS review (
    review_id       VARCHAR(26)     PRIMARY KEY,
    appointment_id  VARCHAR(26)     NOT NULL REFERENCES appointment(appointment_id),
    vet_user_id     VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    hospital_id     VARCHAR(26)     NOT NULL REFERENCES hospital(hospital_id),
    owner_user_id   VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    rating          INTEGER         NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_vet_user_id ON review(vet_user_id);
CREATE INDEX IF NOT EXISTS idx_review_hospital_id ON review(hospital_id);
CREATE INDEX IF NOT EXISTS idx_review_rating ON review(rating);
-- Prevent duplicate reviews per appointment
CREATE UNIQUE INDEX IF NOT EXISTS uq_review_appointment ON review(appointment_id);

-- vaccination: vet records vaccination for a pet
CREATE TABLE IF NOT EXISTS vaccination (
    vaccination_id          VARCHAR(26)     PRIMARY KEY,
    pet_id                  VARCHAR(26)     NOT NULL REFERENCES pet(pet_id),
    vet_user_id             VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    vaccine_name            VARCHAR(200)    NOT NULL,
    batch_number            VARCHAR(100),
    last_vaccination_date   DATE            NOT NULL,
    next_vaccination_date   DATE,
    notes                   TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vaccination_pet_id ON vaccination(pet_id);
CREATE INDEX IF NOT EXISTS idx_vaccination_vet_user_id ON vaccination(vet_user_id);
CREATE INDEX IF NOT EXISTS idx_vaccination_next_date ON vaccination(next_vaccination_date);
