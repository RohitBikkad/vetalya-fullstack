-- ============================================================
-- V3: Schema additions and indexes for Pet Owner API
-- ============================================================

-- 1. pet table changes: soft delete and registration number
ALTER TABLE pet ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE pet ADD COLUMN IF NOT EXISTS registration_no VARCHAR(50) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pet_registration_no 
    ON pet(registration_no) 
    WHERE registration_no IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_pet_owner_active 
    ON pet(owner_user_id) 
    WHERE deleted_at IS NULL;

-- 2. review table changes: add title and updated_at
ALTER TABLE review ADD COLUMN IF NOT EXISTS title VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_review_owner_user_id ON review(owner_user_id);

-- 3. reminder table changes: add user_id, pet_id, sent_at
ALTER TABLE reminder ADD COLUMN IF NOT EXISTS user_id VARCHAR(26) REFERENCES user_auth(user_id) ON DELETE CASCADE;
ALTER TABLE reminder ADD COLUMN IF NOT EXISTS pet_id VARCHAR(26) REFERENCES pet(pet_id) ON DELETE SET NULL;
ALTER TABLE reminder ADD COLUMN IF NOT EXISTS sent_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_reminder_user_id ON reminder(user_id);
CREATE INDEX IF NOT EXISTS idx_reminder_user_remind_at ON reminder(user_id, remind_at);

-- 4. appointment table: allow nullable hospital_id, add partial unique index for slot collision
ALTER TABLE appointment ALTER COLUMN hospital_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_appointment_vet_slot 
    ON appointment(vet_user_id, appointment_at) 
    WHERE status <> 'CANCELLED';

CREATE INDEX IF NOT EXISTS idx_appointment_owner_at ON appointment(owner_user_id, appointment_at);
CREATE INDEX IF NOT EXISTS idx_appointment_pet_non_cancelled ON appointment(pet_id, appointment_at) WHERE status <> 'CANCELLED';
