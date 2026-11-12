package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "vaccination")
public class Vaccination {

    @Id
    @Column(name = "vaccination_id", length = 26)
    private String vaccinationId;

    @Column(name = "pet_id", length = 26, nullable = false)
    private String petId;

    @Column(name = "vet_user_id", length = 26, nullable = false)
    private String vetUserId;

    @Column(name = "vaccine_name", nullable = false, length = 200)
    private String vaccineName;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "last_vaccination_date", nullable = false)
    private LocalDate lastVaccinationDate;

    @Column(name = "next_vaccination_date")
    private LocalDate nextVaccinationDate;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
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

    public String getVaccinationId() { return vaccinationId; }
    public void setVaccinationId(String vaccinationId) { this.vaccinationId = vaccinationId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getVetUserId() { return vetUserId; }
    public void setVetUserId(String vetUserId) { this.vetUserId = vetUserId; }

    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getLastVaccinationDate() { return lastVaccinationDate; }
    public void setLastVaccinationDate(LocalDate lastVaccinationDate) { this.lastVaccinationDate = lastVaccinationDate; }

    public LocalDate getNextVaccinationDate() { return nextVaccinationDate; }
    public void setNextVaccinationDate(LocalDate nextVaccinationDate) { this.nextVaccinationDate = nextVaccinationDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Pet getPet() { return pet; }
}
