package com.pet.vetalya.clinic;

import com.pet.vetalya.common.IdGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class ClinicWorkspaceService {
    public static final Set<String> STAFF_ROLES = Set.of("ROLE_VET", "ROLE_VET_NURSE", "ROLE_LAB_TECHNICIAN",
        "ROLE_RECEPTIONIST", "ROLE_PHARMACIST", "ROLE_ACCOUNTANT", "ROLE_CLINIC_STAFF", "ROLE_CLINIC_ADMIN", "ROLE_CLINIC_MANAGER");
    private final JdbcTemplate db;
    private final IdGenerator ids;
    public ClinicWorkspaceService(JdbcTemplate db, IdGenerator ids) { this.db = db; this.ids = ids; }

    // The database identity, rather than request parameters, controls the effective role.
    public String role(String userId) {
        var roles = db.queryForList("SELECT role FROM user_auth WHERE user_id=? AND status='ACTIVE'", String.class, userId);
        if (roles.isEmpty() || !(STAFF_ROLES.contains(roles.getFirst()) || "ROLE_PLATFORM_ADMIN".equals(roles.getFirst())))
            throw new AccessDeniedException("Active hospital staff account required");
        return roles.getFirst();
    }
    public List<Map<String,Object>> hospitals(String userId) {
        role(userId);
        return db.queryForList("""
            SELECT h.hospital_id AS "hospitalId", h.name, h.city, h.mobile, h.address1,
                   h.vet_user_id AS "ownerId"
            FROM hospital h WHERE h.is_active=TRUE AND COALESCE(
              (SELECT s.active FROM clinic_staff_assignment s WHERE s.hospital_id=h.hospital_id AND s.user_id=?),
              h.vet_user_id=?)
            ORDER BY h.name
            """, userId,userId);
    }
    public void requireAccess(String userId, String hospitalId) {
        role(userId);
        Integer count = db.queryForObject("""
            SELECT COUNT(*) FROM hospital h WHERE h.hospital_id=? AND h.is_active=TRUE
              AND COALESCE(
                (SELECT s.active FROM clinic_staff_assignment s WHERE s.hospital_id=h.hospital_id AND s.user_id=?),
                h.vet_user_id=?)
            """, Integer.class, hospitalId,userId,userId);
        if (count == null || count == 0) throw new AccessDeniedException("No active assignment to this hospital");
    }
    public void requireManager(String userId, String hospitalId) {
        String role = role(userId);
        if ("ROLE_PLATFORM_ADMIN".equals(role)) return; // Explicit provisioning endpoint only; never dashboard data.
        requireAccess(userId,hospitalId);
        Integer owner = db.queryForObject("SELECT COUNT(*) FROM hospital WHERE hospital_id=? AND vet_user_id=?", Integer.class,hospitalId,userId);
        if (!Set.of("ROLE_CLINIC_ADMIN","ROLE_CLINIC_MANAGER").contains(role) && (owner == null || owner == 0))
            throw new AccessDeniedException("Hospital administrator required");
    }
    @Transactional(readOnly=true)
    public Map<String,Object> dashboard(String userId, String hospitalId, LocalDate date) {
        requireAccess(userId,hospitalId);
        String role = role(userId);
        boolean queueAccess = Set.of("ROLE_VET","ROLE_VET_NURSE","ROLE_RECEPTIONIST","ROLE_CLINIC_STAFF","ROLE_CLINIC_ADMIN","ROLE_CLINIC_MANAGER").contains(role);
        var start = Timestamp.from(date.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant());
        var end = Timestamp.from(date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant());
        List<Map<String,Object>> queue = queueAccess ? db.queryForList("""
            SELECT a.appointment_id AS "appointmentId", a.pet_id AS "petId", p.name AS "petName", p.species, p.breed,
              a.appointment_at AS "appointmentAt", a.status, a.queue_number AS "queueNumber",
              a.check_in_time AS "checkInTime", a.consultation_start_time AS "consultationStartTime",
              a.consultation_end_time AS "consultationEndTime", a.vet_user_id AS "vetUserId",
              TRIM(CONCAT(COALESCE(v.first_name,''),' ',COALESCE(v.last_name,''))) AS "vetName"
            FROM appointment a JOIN pet p ON p.pet_id=a.pet_id LEFT JOIN vet v ON v.user_id=a.vet_user_id
            WHERE a.hospital_id=? AND a.appointment_at>=? AND a.appointment_at<? ORDER BY a.appointment_at,a.appointment_id
            """, hospitalId,start,end) : List.of();
        var tasks = db.queryForList("""
            SELECT task_id AS "taskId", title, assigned_role AS "assignedRole", priority,status,
              created_at AS "createdAt",completed_by AS "completedBy" FROM clinic_operation_task
            WHERE hospital_id=? AND (assigned_role=? OR created_by=? OR ? IN ('ROLE_CLINIC_ADMIN','ROLE_CLINIC_MANAGER'))
            ORDER BY CASE WHEN status='OPEN' THEN 0 ELSE 1 END,created_at DESC LIMIT 200
            """, hospitalId,role,userId,role);
        var result = new LinkedHashMap<String,Object>();
        result.put("role",role); result.put("date",date.toString()); result.put("queue",queue); result.put("tasks",tasks);
        result.put("queueAccess",queueAccess);
        result.put("canManageStaff", canManage(userId,hospitalId));
        result.put("canCheckIn", Set.of("ROLE_RECEPTIONIST","ROLE_CLINIC_ADMIN","ROLE_CLINIC_MANAGER","ROLE_VET").contains(role));
        return result;
    }
    private boolean canManage(String userId,String hospitalId) {
        try { requireManager(userId,hospitalId); return true; } catch (AccessDeniedException e) { return false; }
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> staff(String userId,String hospitalId) {
        requireManager(userId,hospitalId);
        return db.queryForList("""
            SELECT u.user_id AS "userId",u.email,u.role,u.status,s.active,s.department,
              s.updated_at AS "updatedAt" FROM clinic_staff_assignment s JOIN user_auth u ON u.user_id=s.user_id
            WHERE s.hospital_id=? ORDER BY u.role,u.email
            """,hospitalId);
    }
    @Transactional
    public void assign(String userId,String hospitalId,String email,String department,boolean active) {
        requireManager(userId,hospitalId);
        var users = db.queryForList("SELECT user_id,role FROM user_auth WHERE LOWER(email)=LOWER(?) AND status='ACTIVE'",email.trim());
        if (users.size()!=1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"One existing active staff account with this email is required");
        var target=users.getFirst();
        if (!STAFF_ROLES.contains(String.valueOf(target.get("role")))) throw new AccessDeniedException("Only hospital staff accounts may be assigned");
        if (userId.equals(target.get("user_id"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ask another administrator to change your assignment");
        db.update("""
            INSERT INTO clinic_staff_assignment(hospital_id,user_id,department,active) VALUES (?,?,?,?)
            ON CONFLICT (hospital_id,user_id) DO UPDATE SET department=EXCLUDED.department,active=EXCLUDED.active,updated_at=NOW()
            """,hospitalId,target.get("user_id"),department.trim(),active);
        event(userId,hospitalId,active?"STAFF_ACCESS_GRANTED":"STAFF_ACCESS_REVOKED",String.valueOf(target.get("user_id")));
    }
    @Transactional
    public void createTask(String userId,String hospitalId,String title,String assignedRole,String priority) {
        requireAccess(userId,hospitalId);
        if (!STAFF_ROLES.contains(assignedRole)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown staff role");
        if (!assignedRole.equals(role(userId)) && !canManage(userId,hospitalId)) throw new AccessDeniedException("Only administrators can assign other roles");
        String id=ids.newId();
        db.update("INSERT INTO clinic_operation_task(task_id,hospital_id,title,assigned_role,priority,created_by) VALUES (?,?,?,?,?,?)",
            id,hospitalId,title.trim(),assignedRole,priority,userId);
        event(userId,hospitalId,"TASK_CREATED",id);
    }
    @Transactional
    public void completeTask(String userId,String hospitalId,String taskId) {
        requireAccess(userId,hospitalId);
        int changed=db.update("""
            UPDATE clinic_operation_task SET status='DONE',completed_by=?,completed_at=NOW()
            WHERE task_id=? AND hospital_id=? AND status='OPEN' AND (assigned_role=? OR ?=TRUE)
            """,userId,taskId,hospitalId,role(userId),canManage(userId,hospitalId));
        if(changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Task is unavailable, already completed, or assigned to another role");
        event(userId,hospitalId,"TASK_COMPLETED",taskId);
    }
    @Transactional
    public void checkIn(String userId,String hospitalId,String appointmentId) {
        requireAccess(userId,hospitalId);
        if (!Set.of("ROLE_VET","ROLE_RECEPTIONIST","ROLE_CLINIC_ADMIN","ROLE_CLINIC_MANAGER").contains(role(userId)))
            throw new AccessDeniedException("Check-in permission required");
        int changed=db.update("""
            UPDATE appointment SET check_in_time=NOW(),status='CONFIRMED',updated_at=NOW(),version=version+1
            WHERE appointment_id=? AND hospital_id=? AND status IN ('BOOKED','CONFIRMED') AND check_in_time IS NULL
            """,appointmentId,hospitalId);
        if(changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Appointment cannot be checked in");
        event(userId,hospitalId,"PATIENT_CHECKED_IN",appointmentId);
    }
    private void event(String userId,String hospitalId,String action,String targetId) {
        db.update("INSERT INTO clinic_operation_event(event_id,hospital_id,actor_id,action,target_id) VALUES (?,?,?,?,?)",
            ids.newId(),hospitalId,userId,action,targetId);
    }
}
