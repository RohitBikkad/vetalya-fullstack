package com.pet.vetalya.clinic;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClinicalAccess {
    private final JdbcTemplate db;
    private final ClinicWorkspaceService clinics;

    public ClinicalAccess(JdbcTemplate db, ClinicWorkspaceService clinics) {
        this.db = db;
        this.clinics = clinics;
    }

    public void requireVeterinarian(String userId, String hospitalId) {
        if (!"ROLE_VET".equals(clinics.role(userId))) throw denied();
        if (hospitalId != null) clinics.requireAccess(userId, hospitalId);
    }

    public List<String> veterinarianHospitalIds(String userId) {
        requireVeterinarian(userId, null);
        return clinics.hospitals(userId).stream()
                .map(hospital -> (String) hospital.get("hospitalId"))
                .toList();
    }

    public void appointmentVet(String userId, String appointmentId) {
        var matches = db.query("""
            SELECT hospital_id FROM appointment WHERE appointment_id=? AND vet_user_id=?
            """, (row, index) -> row.getString("hospital_id"), appointmentId, userId);
        if (matches.isEmpty()) throw denied();
        requireVeterinarian(userId, matches.getFirst());
    }

    public void diagnosticRead(String userId, String orderId) {
        checkRead(userId, db.query("""
            SELECT d.hospital_id, p.owner_user_id=? AND d.status='COMPLETED' AS owner_access
            FROM diagnostic_order d JOIN pet p ON p.pet_id=d.pet_id
            WHERE d.order_id=? AND (d.vet_user_id=? OR (p.owner_user_id=? AND d.status='COMPLETED'))
            """, (row, index) -> new RecordScope(row.getString("hospital_id"), row.getBoolean("owner_access")),
            userId, orderId, userId, userId));
    }

    public void encounterRead(String userId, String encounterId) {
        checkRead(userId, db.query("""
            SELECT e.hospital_id, p.owner_user_id=? AND e.is_signed AS owner_access
            FROM clinical_encounter e JOIN pet p ON p.pet_id=e.pet_id
            WHERE e.encounter_id=? AND (e.vet_user_id=? OR (p.owner_user_id=? AND e.is_signed))
            """, (row, index) -> new RecordScope(row.getString("hospital_id"), row.getBoolean("owner_access")),
            userId, encounterId, userId, userId));
    }

    private void checkRead(String userId, List<RecordScope> matches) {
        if (matches.isEmpty()) throw denied();
        RecordScope scope = matches.getFirst();
        if (!scope.ownerAccess()) requireVeterinarian(userId, scope.hospitalId());
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("Clinical record is outside your care scope");
    }

    private record RecordScope(String hospitalId, boolean ownerAccess) {}
}
