package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pet_health_passport")
public class PetHealthPassport {

    @Id
    @Column(name = "passport_id", length = 26)
    private String passportId;

    @Column(name = "pet_id", length = 26, nullable = false, unique = true)
    private String petId;

    @Column(name = "passport_number", length = 30, nullable = false, unique = true)
    private String passportNumber;

    @Column(name = "blood_group", length = 20)
    private String bloodGroup;

    @Column(name = "allergies_summary", columnDefinition = "TEXT")
    private String allergiesSummary;

    @Column(name = "chronic_conditions_summary", columnDefinition = "TEXT")
    private String chronicConditionsSummary;

    @Column(name = "dietary_notes", columnDefinition = "TEXT")
    private String dietaryNotes;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "public_emergency_qr_hash", length = 64, unique = true)
    private String publicEmergencyQrHash;

    @Column(name = "qr_revoked", nullable = false)
    private boolean qrRevoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

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

    public String getPassportId() { return passportId; }
    public void setPassportId(String passportId) { this.passportId = passportId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getAllergiesSummary() { return allergiesSummary; }
    public void setAllergiesSummary(String allergiesSummary) { this.allergiesSummary = allergiesSummary; }

    public String getChronicConditionsSummary() { return chronicConditionsSummary; }
    public void setChronicConditionsSummary(String chronicConditionsSummary) { this.chronicConditionsSummary = chronicConditionsSummary; }

    public String getDietaryNotes() { return dietaryNotes; }
    public void setDietaryNotes(String dietaryNotes) { this.dietaryNotes = dietaryNotes; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getPublicEmergencyQrHash() { return publicEmergencyQrHash; }
    public void setPublicEmergencyQrHash(String publicEmergencyQrHash) { this.publicEmergencyQrHash = publicEmergencyQrHash; }

    public boolean isQrRevoked() { return qrRevoked; }
    public void setQrRevoked(boolean qrRevoked) { this.qrRevoked = qrRevoked; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }
}
