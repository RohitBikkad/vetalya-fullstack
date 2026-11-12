package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescription")
public class Prescription {

    @Id
    @Column(name = "prescription_id", length = 26)
    private String prescriptionId;

    @Column(name = "encounter_id", length = 26)
    private String encounterId;

    @Column(name = "pet_id", length = 26, nullable = false)
    private String petId;

    @Column(name = "vet_user_id", length = 26, nullable = false)
    private String vetUserId;

    @Column(name = "rx_date", nullable = false)
    private LocalDate rxDate = LocalDate.now();

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "pdf_storage_url", length = 500)
    private String pdfStorageUrl;

    @Column(name = "is_dispensed", nullable = false)
    private boolean isDispensed = false;

    @Column(name = "dispensed_at")
    private Instant dispensedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();

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

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getVetUserId() { return vetUserId; }
    public void setVetUserId(String vetUserId) { this.vetUserId = vetUserId; }

    public LocalDate getRxDate() { return rxDate; }
    public void setRxDate(LocalDate rxDate) { this.rxDate = rxDate; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getPdfStorageUrl() { return pdfStorageUrl; }
    public void setPdfStorageUrl(String pdfStorageUrl) { this.pdfStorageUrl = pdfStorageUrl; }

    public boolean isDispensed() { return isDispensed; }
    public void setDispensed(boolean dispensed) { isDispensed = dispensed; }

    public Instant getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(Instant dispensedAt) { this.dispensedAt = dispensedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }

    public List<PrescriptionItem> getItems() { return items; }
    public void setItems(List<PrescriptionItem> items) { this.items = items; }
}

