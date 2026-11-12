-- ============================================================
-- V13: Diagnostic Orders, Test Catalog, and Laboratory Results
-- ============================================================

-- 1. diagnostic_test_catalog: Master list of veterinary diagnostic tests
CREATE TABLE IF NOT EXISTS diagnostic_test_catalog (
    test_id             VARCHAR(26)     PRIMARY KEY,
    test_code           VARCHAR(50)     UNIQUE NOT NULL,
    test_name           VARCHAR(200)    NOT NULL,
    category            VARCHAR(50)     NOT NULL, -- HEMATOLOGY, BIOCHEMISTRY, SEROLOGY, RADIOLOGY, ULTRASOUND, MICROBIOLOGY, URINALYSIS
    sample_type         VARCHAR(50),              -- WHOLE_BLOOD_EDTA, SERUM, URINE, FECES, TISSUE_BIOPSY, SWAB, NONE_IMAGING
    tat_hours           INT             DEFAULT 24,
    base_price_paisa    BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dx_catalog_code ON diagnostic_test_catalog(test_code);
CREATE INDEX IF NOT EXISTS idx_dx_catalog_category ON diagnostic_test_catalog(category);

-- Seed standard veterinary diagnostic tests
INSERT INTO diagnostic_test_catalog (test_id, test_code, test_name, category, sample_type, tat_hours, base_price_paisa)
VALUES
    ('01ARZ3NDEKTSV4RRFFQ69G5DX1', 'CBC_CANINE', 'Complete Blood Count (Canine/Feline)', 'HEMATOLOGY', 'WHOLE_BLOOD_EDTA', 6, 75000),
    ('01ARZ3NDEKTSV4RRFFQ69G5DX2', 'BIOCHEM_LFT_KFT', 'Comprehensive Liver & Kidney Profile', 'BIOCHEMISTRY', 'SERUM', 12, 160000),
    ('01ARZ3NDEKTSV4RRFFQ69G5DX3', 'PARVO_RAPID', 'Canine Parvovirus (CPV) Antigen Rapid Test', 'SEROLOGY', 'FECES', 1, 95000),
    ('01ARZ3NDEKTSV4RRFFQ69G5DX4', 'RABIES_TITRE', 'Rabies Neutralizing Antibody Titre (FAVN/RFFIT)', 'SEROLOGY', 'SERUM', 72, 450000),
    ('01ARZ3NDEKTSV4RRFFQ69G5DX5', 'XRAY_CHEST_2V', 'Digital Radiography (Chest 2 Views)', 'RADIOLOGY', 'NONE_IMAGING', 2, 120000),
    ('01ARZ3NDEKTSV4RRFFQ69G5DX6', 'USG_ABDOMEN', 'Complete Abdominal Ultrasonography', 'ULTRASOUND', 'NONE_IMAGING', 4, 180000),
    ('01ARZ3NDEKTSV4RRFFQ69G5DX7', 'URINALYSIS_FULL', 'Urinalysis with Microscopic Examination', 'URINALYSIS', 'URINE', 4, 60000)
ON CONFLICT (test_code) DO NOTHING;

-- 2. diagnostic_order: Clinical laboratory requisition
CREATE TABLE IF NOT EXISTS diagnostic_order (
    order_id            VARCHAR(26)     PRIMARY KEY,
    order_number        VARCHAR(50)     UNIQUE NOT NULL,
    pet_id              VARCHAR(26)     NOT NULL REFERENCES pet(pet_id) ON DELETE CASCADE,
    vet_user_id         VARCHAR(26)     NOT NULL REFERENCES user_auth(user_id),
    hospital_id         VARCHAR(26)     REFERENCES hospital(hospital_id),
    encounter_id        VARCHAR(26)     REFERENCES clinical_encounter(encounter_id),
    status              VARCHAR(30)     NOT NULL DEFAULT 'ORDERED'
                        CHECK (status IN ('ORDERED', 'SAMPLE_COLLECTED', 'PROCESSING', 'COMPLETED', 'CANCELLED')),
    priority            VARCHAR(20)     NOT NULL DEFAULT 'ROUTINE'
                        CHECK (priority IN ('ROUTINE', 'URGENT', 'STAT')),
    clinical_notes      TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dx_order_pet_id ON diagnostic_order(pet_id);
CREATE INDEX IF NOT EXISTS idx_dx_order_vet_id ON diagnostic_order(vet_user_id);
CREATE INDEX IF NOT EXISTS idx_dx_order_status ON diagnostic_order(status);
CREATE INDEX IF NOT EXISTS idx_dx_order_encounter ON diagnostic_order(encounter_id);

-- 3. diagnostic_order_item: Individual test parameters & results
CREATE TABLE IF NOT EXISTS diagnostic_order_item (
    item_id             VARCHAR(26)     PRIMARY KEY,
    order_id            VARCHAR(26)     NOT NULL REFERENCES diagnostic_order(order_id) ON DELETE CASCADE,
    test_code           VARCHAR(50)     NOT NULL,
    test_name           VARCHAR(200)    NOT NULL,
    category            VARCHAR(50)     NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'SAMPLE_COLLECTED', 'PROCESSING', 'COMPLETED', 'CANCELLED')),
    result_value        TEXT,
    reference_range     VARCHAR(100),
    unit                VARCHAR(50),
    flag                VARCHAR(20)     DEFAULT 'NORMAL'
                        CHECK (flag IN ('NORMAL', 'HIGH', 'LOW', 'CRITICAL', 'INCONCLUSIVE')),
    report_file_url     TEXT,
    interpretation      TEXT,
    verified_by         VARCHAR(100),
    verified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dx_item_order_id ON diagnostic_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_dx_item_status ON diagnostic_order_item(status);

