-- Review before applying to the confirmed staging project lihsmcgfxjuldjenjdti.
-- Spring Boot connects as the database owner, which bypasses ordinary RLS.
-- No rows are deleted; no existing policies are changed; owner access is retained.
-- Supabase anon/authenticated API clients will be denied unless an existing policy allows them.
DO $$
DECLARE t record;
BEGIN
  FOR t IN SELECT schemaname, tablename FROM pg_catalog.pg_tables
    WHERE schemaname IN ('public', 'medshop')
      AND tablename IN (
        'flyway_schema_history','user_auth','auth_provider','refresh_token','one_time_code',
        'vet','pet_owner','hospital','pet','appointment','reminder','review','vaccination',
        'audit_log','shedlock','otp_verification','pet_identifier','pet_health_passport',
        'record_share_request','pet_document','clinical_encounter','encounter_amendment',
        'prescription','prescription_item','appointment_slot','diagnostic_test_catalog',
        'diagnostic_order','diagnostic_order_item','clinic_staff_assignment',
        'clinic_operation_task','clinic_operation_event','tenants','users','audit_logs',
        'categories','suppliers','medicines','stock_batches','stock_transactions',
        'customers','invoice_sequences','sales_invoices','invoice_items','gst_returns',
        'auth_providers','refresh_tokens','auth_exchange_codes','schedule_h_register','idempotency_keys'
      )
  LOOP
    EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', t.schemaname, t.tablename);
  END LOOP;
END $$;
