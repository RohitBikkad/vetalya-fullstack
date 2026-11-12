-- Explicit clinic access: no global staff-role inference or invented memberships.
ALTER TABLE user_auth ALTER COLUMN role TYPE VARCHAR(40);
ALTER TABLE user_auth DROP CONSTRAINT IF EXISTS user_auth_role_check;
ALTER TABLE user_auth ADD CONSTRAINT user_auth_role_check CHECK (role IN (
 'ROLE_VET','ROLE_PET_OWNER','ROLE_PLATFORM_ADMIN','ROLE_CLINIC_ADMIN',
 'ROLE_VET_NURSE','ROLE_LAB_TECHNICIAN','ROLE_RECEPTIONIST','ROLE_PHARMACIST',
 'ROLE_ACCOUNTANT','ROLE_CLINIC_STAFF','ROLE_CLINIC_MANAGER'));
CREATE TABLE clinic_staff_assignment (
 hospital_id VARCHAR(26) NOT NULL REFERENCES hospital(hospital_id),
 user_id VARCHAR(26) NOT NULL REFERENCES user_auth(user_id),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 department VARCHAR(100) NOT NULL DEFAULT 'General',
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 PRIMARY KEY (hospital_id,user_id)
);
CREATE INDEX idx_clinic_staff_user ON clinic_staff_assignment(user_id,active);
CREATE TABLE clinic_operation_task (
 task_id VARCHAR(26) PRIMARY KEY,
 hospital_id VARCHAR(26) NOT NULL REFERENCES hospital(hospital_id),
 title VARCHAR(200) NOT NULL,
 assigned_role VARCHAR(40) NOT NULL,
 priority VARCHAR(10) NOT NULL CHECK (priority IN ('ROUTINE','URGENT')),
 status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','DONE')),
 created_by VARCHAR(26) NOT NULL REFERENCES user_auth(user_id),
 completed_by VARCHAR(26) REFERENCES user_auth(user_id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 completed_at TIMESTAMPTZ
);
CREATE INDEX idx_clinic_task_hospital ON clinic_operation_task(hospital_id,status);
CREATE TABLE clinic_operation_event (
 event_id VARCHAR(26) PRIMARY KEY,
 hospital_id VARCHAR(26) NOT NULL REFERENCES hospital(hospital_id),
 actor_id VARCHAR(26) NOT NULL REFERENCES user_auth(user_id),
 action VARCHAR(50) NOT NULL,
 target_id VARCHAR(26) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE appointment DROP CONSTRAINT IF EXISTS appointment_status_check;
ALTER TABLE appointment ADD CONSTRAINT appointment_status_check CHECK (status IN ('BOOKED','CONFIRMED','IN_CONSULTATION','COMPLETED','CANCELLED','NO_SHOW'));
