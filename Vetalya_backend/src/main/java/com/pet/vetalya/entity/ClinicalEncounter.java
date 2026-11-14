package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "clinical_encounter")
public class ClinicalEncounter {

    @Id
    @Column(name = "encounter_id", length = 26)
    private String encounterId;

    @Column(name = "pet_id", length = 26, nullable = false)
    private String petId;

    @Column(name = "vet_user_id", length = 26, nullable = false)
    private String vetUserId;

    @Column(name = "hospital_id", length = 26)
    private String hospitalId;

    @Column(name = "appointment_id", length = 26)
    private String appointmentId;

    @Column(name = "encounter_type", length = 50, nullable = false)
    private String encounterType = "OPD_CONSULTATION";

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "presenting_history", columnDefinition = "TEXT")
    private String presentingHistory;

    @Column(name = "physical_examination", columnDefinition = "TEXT")
    private String physicalExamination;

    @Column(name = "temperature_f", precision = 5, scale = 2)
    private BigDecimal temperatureF;

    @Column(name = "heart_rate_bpm")
    private Integer heartRateBpm;

    @Column(name = "respiratory_rate_bpm")
    private Integer respiratoryRateBpm;

    @Column(name = "body_weight_kg", precision = 6, scale = 2)
    private BigDecimal bodyWeightKg;

    @Column(name = "body_condition_score")
    private Integer bodyConditionScore;

    @Column(name = "assessment_notes", columnDefinition = "TEXT")
    private String assessmentNotes;

    @Column(name = "diagnosis_primary", length = 255)
    private String diagnosisPrimary;

    @Column(name = "differential_diagnosis", columnDefinition = "TEXT")
    private String differentialDiagnosis;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(name = "diet_instructions", columnDefinition = "TEXT")
    private String dietInstructions;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "is_signed", nullable = false)
    private boolean isSigned = false;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "digital_signature_hash", length = 64)
    private String digitalSignatureHash;

    @Column(name = "is_passport_published", nullable = false)
    private boolean isPassportPublished = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", insertable = false, updatable = false)
    private Appointment appointment;

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

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getVetUserId() { return vetUserId; }
    public void setVetUserId(String vetUserId) { this.vetUserId = vetUserId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getEncounterType() { return encounterType; }
    public void setEncounterType(String encounterType) { this.encounterType = encounterType; }

    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }

    public String getPresentingHistory() { return presentingHistory; }
    public void setPresentingHistory(String presentingHistory) { this.presentingHistory = presentingHistory; }

    public String getPhysicalExamination() { return physicalExamination; }
    public void setPhysicalExamination(String physicalExamination) { this.physicalExamination = physicalExamination; }

    public BigDecimal getTemperatureF() { return temperatureF; }
    public void setTemperatureF(BigDecimal temperatureF) { this.temperatureF = temperatureF; }

    public Integer getHeartRateBpm() { return heartRateBpm; }
    public void setHeartRateBpm(Integer heartRateBpm) { this.heartRateBpm = heartRateBpm; }

    public Integer getRespiratoryRateBpm() { return respiratoryRateBpm; }
    public void setRespiratoryRateBpm(Integer respiratoryRateBpm) { this.respiratoryRateBpm = respiratoryRateBpm; }

    public BigDecimal getBodyWeightKg() { return bodyWeightKg; }
    public void setBodyWeightKg(BigDecimal bodyWeightKg) { this.bodyWeightKg = bodyWeightKg; }

    public Integer getBodyConditionScore() { return bodyConditionScore; }
    public void setBodyConditionScore(Integer bodyConditionScore) { this.bodyConditionScore = bodyConditionScore; }

    public String getAssessmentNotes() { return assessmentNotes; }
    public void setAssessmentNotes(String assessmentNotes) { this.assessmentNotes = assessmentNotes; }

    public String getDiagnosisPrimary() { return diagnosisPrimary; }
    public void setDiagnosisPrimary(String diagnosisPrimary) { this.diagnosisPrimary = diagnosisPrimary; }

    public String getDifferentialDiagnosis() { return differentialDiagnosis; }
    public void setDifferentialDiagnosis(String differentialDiagnosis) { this.differentialDiagnosis = differentialDiagnosis; }

    public String getTreatmentPlan() { return treatmentPlan; }
    public void setTreatmentPlan(String treatmentPlan) { this.treatmentPlan = treatmentPlan; }

    public String getDietInstructions() { return dietInstructions; }
    public void setDietInstructions(String dietInstructions) { this.dietInstructions = dietInstructions; }

    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public boolean isSigned() { return isSigned; }
    public void setSigned(boolean signed) { isSigned = signed; }

    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }

    public String getDigitalSignatureHash() { return digitalSignatureHash; }
    public void setDigitalSignatureHash(String digitalSignatureHash) { this.digitalSignatureHash = digitalSignatureHash; }

    public boolean isPassportPublished() { return isPassportPublished; }
    public void setPassportPublished(boolean passportPublished) { isPassportPublished = passportPublished; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
}
