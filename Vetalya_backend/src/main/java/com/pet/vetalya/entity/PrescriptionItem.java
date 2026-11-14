package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "prescription_item")
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(name = "prescription_id", length = 26, nullable = false)
    private String prescriptionId;

    @Column(name = "medicine_name", nullable = false, length = 200)
    private String medicineName;

    @Column(name = "generic_composition", length = 200)
    private String genericComposition;

    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(nullable = false, length = 100)
    private String frequency; // OD, BID, TID, QID, SOS, STAT

    @Column(nullable = false, length = 100)
    private String duration;

    @Column(length = 255)
    private String instructions;

    @Column(name = "is_schedule_h", nullable = false)
    private boolean isScheduleH = false;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", insertable = false, updatable = false)
    private Prescription prescription;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // ── Getters / Setters ──

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getGenericComposition() { return genericComposition; }
    public void setGenericComposition(String genericComposition) { this.genericComposition = genericComposition; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isScheduleH() { return isScheduleH; }
    public void setScheduleH(boolean scheduleH) { isScheduleH = scheduleH; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Instant getCreatedAt() { return createdAt; }

    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription prescription) { this.prescription = prescription; }
}
