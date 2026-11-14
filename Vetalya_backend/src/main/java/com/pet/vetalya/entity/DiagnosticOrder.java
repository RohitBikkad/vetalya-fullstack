package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diagnostic_order")
public class DiagnosticOrder {

    @Id
    @Column(name = "order_id", length = 26)
    private String orderId;

    @Column(name = "order_number", length = 50, unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "pet_id", length = 26, nullable = false)
    private String petId;

    @Column(name = "vet_user_id", length = 26, nullable = false)
    private String vetUserId;

    @Column(name = "hospital_id", length = 26)
    private String hospitalId;

    @Column(name = "encounter_id", length = 26)
    private String encounterId;

    @Column(length = 30, nullable = false)
    private String status = "ORDERED";

    @Column(length = 20, nullable = false)
    private String priority = "ROUTINE";

    @Column(name = "clinical_notes")
    private String clinicalNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiagnosticOrderItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "ORDERED";
        if (priority == null) priority = "ROUTINE";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addItem(DiagnosticOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getVetUserId() { return vetUserId; }
    public void setVetUserId(String vetUserId) { this.vetUserId = vetUserId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public List<DiagnosticOrderItem> getItems() { return items; }
    public void setItems(List<DiagnosticOrderItem> items) { this.items = items; }
}
