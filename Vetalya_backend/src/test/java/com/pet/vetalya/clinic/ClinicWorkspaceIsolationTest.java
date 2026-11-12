package com.pet.vetalya.clinic;

import com.pet.vetalya.common.IdGenerator;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

class ClinicWorkspaceIsolationTest {
    JdbcTemplate db;
    ClinicWorkspaceService service;
    @BeforeEach void setup() {
        db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa",""));
        db.execute("CREATE TABLE user_auth(user_id VARCHAR PRIMARY KEY,role VARCHAR,status VARCHAR,email VARCHAR)");
        db.execute("CREATE TABLE hospital(hospital_id VARCHAR PRIMARY KEY,vet_user_id VARCHAR,name VARCHAR,city VARCHAR,mobile VARCHAR,address1 VARCHAR,is_active BOOLEAN)");
        db.execute("CREATE TABLE clinic_staff_assignment(hospital_id VARCHAR,user_id VARCHAR,active BOOLEAN,department VARCHAR,updated_at TIMESTAMP DEFAULT NOW())");
        db.execute("CREATE TABLE clinic_operation_task(task_id VARCHAR PRIMARY KEY,hospital_id VARCHAR,title VARCHAR,assigned_role VARCHAR,priority VARCHAR,status VARCHAR DEFAULT 'OPEN',created_by VARCHAR,completed_by VARCHAR,created_at TIMESTAMP DEFAULT NOW(),completed_at TIMESTAMP)");
        db.execute("CREATE TABLE clinic_operation_event(event_id VARCHAR,hospital_id VARCHAR,actor_id VARCHAR,action VARCHAR,target_id VARCHAR)");
        db.execute("CREATE TABLE appointment(appointment_id VARCHAR,hospital_id VARCHAR,status VARCHAR,check_in_time TIMESTAMP,updated_at TIMESTAMP,version INT DEFAULT 0)");
        db.update("INSERT INTO user_auth VALUES ('vet','ROLE_VET','ACTIVE','vet@example.test'),('reception','ROLE_RECEPTIONIST','ACTIVE','r@example.test'),('nurse','ROLE_VET_NURSE','ACTIVE','n@example.test'),('admin','ROLE_CLINIC_ADMIN','ACTIVE','a@example.test'),('owner','ROLE_PET_OWNER','ACTIVE','o@example.test')");
        db.update("INSERT INTO hospital VALUES ('a','vet','A','Pune','','',TRUE),('b','other','B','Pune','','',TRUE)");
        db.update("INSERT INTO clinic_staff_assignment(hospital_id,user_id,active,department) VALUES ('a','reception',TRUE,'Reception'),('a','nurse',TRUE,'Nursing'),('a','admin',TRUE,'Administration')");
        db.execute("CREATE TABLE pet(pet_id VARCHAR,name VARCHAR,species VARCHAR,breed VARCHAR)");
        db.execute("CREATE TABLE vet(user_id VARCHAR,first_name VARCHAR,last_name VARCHAR)");
        db.execute("ALTER TABLE appointment ADD pet_id VARCHAR");
        db.execute("ALTER TABLE appointment ADD vet_user_id VARCHAR");
        db.execute("ALTER TABLE appointment ADD appointment_at TIMESTAMP WITH TIME ZONE");
        db.execute("ALTER TABLE appointment ADD queue_number INT");
        db.execute("ALTER TABLE appointment ADD consultation_start_time TIMESTAMP");
        db.execute("ALTER TABLE appointment ADD consultation_end_time TIMESTAMP");
        db.update("INSERT INTO user_auth VALUES ('finance','ROLE_ACCOUNTANT','ACTIVE','f@example.test')");
        db.update("INSERT INTO clinic_staff_assignment(hospital_id,user_id,active,department) VALUES ('a','finance',TRUE,'Finance')");
        service=new ClinicWorkspaceService(db,new IdGenerator());
    }
    @Test void roleDoesNotGrantAnotherHospitalsAccess() {
        assertEquals(1,service.hospitals("admin").size());
        assertThrows(AccessDeniedException.class,()->service.requireAccess("admin","b"));
        assertThrows(AccessDeniedException.class,()->service.staff("admin","b"));
        assertThrows(AccessDeniedException.class,()->service.hospitals("owner"));
    }
    @Test void revokedMembershipFailsImmediately() {
        service.requireAccess("reception","a");
        db.update("UPDATE clinic_staff_assignment SET active=FALSE WHERE user_id='reception'");
        assertThrows(AccessDeniedException.class,()->service.requireAccess("reception","a"));
    }
    @Test void onlyHospitalManagersCanManageStaff() {
        service.requireManager("vet","a");
        service.requireManager("admin","a");
        assertThrows(AccessDeniedException.class,()->service.requireManager("nurse","a"));
        assertThrows(AccessDeniedException.class,()->service.assign("admin","b","r@example.test","Reception",true));
    }
    @Test void tasksEnforceHospitalAndRoleAndRecordActor() {
        service.createTask("admin","a","Prepare room","ROLE_VET_NURSE","URGENT");
        String id=db.queryForObject("SELECT task_id FROM clinic_operation_task",String.class);
        assertThrows(ResponseStatusException.class,()->service.completeTask("reception","a",id));
        assertThrows(AccessDeniedException.class,()->service.completeTask("admin","b",id));
        service.completeTask("nurse","a",id);
        assertEquals("nurse",db.queryForObject("SELECT completed_by FROM clinic_operation_task",String.class));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM clinic_operation_event",Integer.class));
        assertThrows(ResponseStatusException.class,()->service.completeTask("nurse","a",id));
        assertThrows(AccessDeniedException.class,()->service.createTask("nurse","a","Other role","ROLE_PHARMACIST","ROUTINE"));
    }
    @Test void dashboardScopesDateAndHospitalWithoutClinicalNotes() {
        db.update("INSERT INTO pet VALUES ('pet','Patient A','DOG','Labrador')");
        db.update("INSERT INTO appointment(appointment_id,hospital_id,status,pet_id,vet_user_id,appointment_at) VALUES ('today','a','BOOKED','pet','vet','2026-11-12 05:00:00+00'),('other-clinic','b','BOOKED','pet','vet','2026-11-12 05:00:00+00'),('tomorrow','a','BOOKED','pet','vet','2026-11-13 05:00:00+00')");
        var result=service.dashboard("reception","a",java.time.LocalDate.of(2026,11,12));
        var queue=(java.util.List<?>)result.get("queue");
        assertEquals(1,queue.size());
        var patient=(java.util.Map<?,?>)queue.getFirst();
        assertEquals("today",patient.get("appointmentId"));
        assertFalse(patient.containsKey("visitNotes"));
        assertEquals(false,result.get("canManageStaff"));
    }
    @Test void financialStaffCannotReadPatientQueue() {
        var result=service.dashboard("finance","a",java.time.LocalDate.of(2026,11,12));
        assertEquals(false,result.get("queueAccess"));
        assertEquals(java.util.List.of(),result.get("queue"));
        assertEquals(false,result.get("canCheckIn"));
    }
    @Test void checkInRejectsWrongHospitalRoleAndRepeatedArrival() {
        db.update("INSERT INTO appointment(appointment_id,hospital_id,status) VALUES ('one','a','BOOKED'),('two','b','BOOKED')");
        assertThrows(AccessDeniedException.class,()->service.checkIn("nurse","a","one"));
        assertThrows(ResponseStatusException.class,()->service.checkIn("reception","a","two"));
        service.checkIn("reception","a","one");
        assertEquals("CONFIRMED",db.queryForObject("SELECT status FROM appointment WHERE appointment_id='one'",String.class));
        assertNotNull(db.queryForObject("SELECT check_in_time FROM appointment WHERE appointment_id='one'",Object.class));
        assertThrows(ResponseStatusException.class,()->service.checkIn("reception","a","one"));
    }
}
