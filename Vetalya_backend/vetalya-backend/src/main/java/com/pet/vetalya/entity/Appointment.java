package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @Column(name = "appointment_id", length = 26)
    private String appointmentId;

    @Column(name = "pet_id", length = 26, nullable = false)
    private String petId;

    @Column(name = "owner_user_id", length = 26, nullable = false)
    private String ownerUserId;

    @Column(name = "vet_user_id", length = 26, nullable = false)
    private String vetUserId;

    @Column(name = "hospital_id", length = 26)
    private String hospitalId;

    @Column(name = "appointment_at", nullable = false)
    private Instant appointmentAt;

    @Column(nullable = false, length = 20)
    private String status = "BOOKED";

    @Column(length = 500)
    private String reason;

    @Column(name = "visit_notes")
    private String visitNotes;

    @Column(name = "slot_id", length = 26)
    private String slotId;

    @Column(name = "queue_number")
    private Integer queueNumber;

    @Column(name = "check_in_time")
    private Instant checkInTime;

    @Column(name = "consultation_start_time")
    private Instant consultationStartTime;

    @Column(name = "consultation_end_time")
    private Instant consultationEndTime;

    @Column(name = "appointment_type", length = 30)
    private String appointmentType = "OPD_CONSULTATION";

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Relationships (lazy for performance) ──

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private PetOwner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", insertable = false, updatable = false)
    private Hospital hospital;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Getters / Setters ──

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getVetUserId() { return vetUserId; }
    public void setVetUserId(String vetUserId) { this.vetUserId = vetUserId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public Instant getAppointmentAt() { return appointmentAt; }
    public void setAppointmentAt(Instant appointmentAt) { this.appointmentAt = appointmentAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getVisitNotes() { return visitNotes; }
    public void setVisitNotes(String visitNotes) { this.visitNotes = visitNotes; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public Integer getQueueNumber() { return queueNumber; }
    public void setQueueNumber(Integer queueNumber) { this.queueNumber = queueNumber; }

    public Instant getCheckInTime() { return checkInTime; }
    public void setCheckInTime(Instant checkInTime) { this.checkInTime = checkInTime; }

    public Instant getConsultationStartTime() { return consultationStartTime; }
    public void setConsultationStartTime(Instant consultationStartTime) { this.consultationStartTime = consultationStartTime; }

    public Instant getConsultationEndTime() { return consultationEndTime; }
    public void setConsultationEndTime(Instant consultationEndTime) { this.consultationEndTime = consultationEndTime; }

    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Pet getPet() { return pet; }
    public PetOwner getOwner() { return owner; }
    public Hospital getHospital() { return hospital; }
}
