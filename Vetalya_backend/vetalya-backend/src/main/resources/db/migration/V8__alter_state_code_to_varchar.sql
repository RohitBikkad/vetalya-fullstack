-- Migration V8: Align state_code column type to VARCHAR(2) for Hibernate schema validation
ALTER TABLE IF EXISTS medshop.tenants ALTER COLUMN state_code TYPE VARCHAR(2);
ALTER TABLE IF EXISTS medshop.customers ALTER COLUMN state_code TYPE VARCHAR(2);

