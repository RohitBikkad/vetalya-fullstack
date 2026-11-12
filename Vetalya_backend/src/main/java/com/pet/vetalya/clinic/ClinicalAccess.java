package com.pet.vetalya.clinic;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ClinicalAccess {
    private final JdbcTemplate db;
    public ClinicalAccess(JdbcTemplate db) { this.db=db; }
    public void appointmentVet(String userId,String appointmentId) {
        require(db.queryForObject("SELECT COUNT(*) FROM appointment WHERE appointment_id=? AND vet_user_id=?", Integer.class,appointmentId,userId));
    }
    public void diagnosticRead(String userId,String orderId) {
        require(db.queryForObject("SELECT COUNT(*) FROM diagnostic_order d JOIN pet p ON p.pet_id=d.pet_id WHERE d.order_id=? AND (d.vet_user_id=? OR (p.owner_user_id=? AND d.status='COMPLETED'))",Integer.class,orderId,userId,userId));
    }
    public void diagnosticWrite(String userId,String itemId) {
        require(db.queryForObject("SELECT COUNT(*) FROM diagnostic_order_item i JOIN diagnostic_order d ON d.order_id=i.order_id WHERE i.item_id=? AND d.vet_user_id=?",Integer.class,itemId,userId));
    }
    public void encounterRead(String userId,String encounterId) {
        require(db.queryForObject("SELECT COUNT(*) FROM clinical_encounter e JOIN pet p ON p.pet_id=e.pet_id WHERE e.encounter_id=? AND (e.vet_user_id=? OR (p.owner_user_id=? AND e.is_signed=TRUE))",Integer.class,encounterId,userId,userId));
    }
    private void require(Integer count) { if(count==null || count==0) throw new AccessDeniedException("Clinical record is outside your care scope"); }
}
