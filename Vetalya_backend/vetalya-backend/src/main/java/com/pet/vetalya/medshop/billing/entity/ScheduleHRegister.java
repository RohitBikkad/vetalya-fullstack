package com.pet.vetalya.medshop.billing.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_h_register", schema = "medshop")
public class ScheduleHRegister {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "invoice_id", length = 36, nullable = false)
    private String invoiceId;

    @Column(name = "invoice_item_id", length = 36, nullable = false)
    private String invoiceItemId;

    @Column(name = "medicine_id", length = 36, nullable = false)
    private String medicineId;

    @Column(name = "patient_name", length = 100, nullable = false)
    private String patientName;

    @Column(name = "patient_age")
    private Integer patientAge;

    @Column(name = "doctor_name", length = 100, nullable = false)
    private String doctorName;

    @Column(name = "doctor_registration_no", length = 50, nullable = false)
    private String doctorRegistrationNo;

    @Column(name = "prescription_date", nullable = false)
    private LocalDate prescriptionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ScheduleHRegister() {
        this.id = UUID.randomUUID().toString();
    }

    public ScheduleHRegister(String tenantId, String invoiceId, String invoiceItemId, String medicineId,
                             String patientName, Integer patientAge, String doctorName,
                             String doctorRegistrationNo, LocalDate prescriptionDate) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.invoiceId = invoiceId;
        this.invoiceItemId = invoiceItemId;
        this.medicineId = medicineId;
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.doctorName = doctorName;
        this.doctorRegistrationNo = doctorRegistrationNo;
        this.prescriptionDate = prescriptionDate;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getInvoiceItemId() { return invoiceItemId; }
    public void setInvoiceItemId(String invoiceItemId) { this.invoiceItemId = invoiceItemId; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Integer getPatientAge() { return patientAge; }
    public void setPatientAge(Integer patientAge) { this.patientAge = patientAge; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorRegistrationNo() { return doctorRegistrationNo; }
    public void setDoctorRegistrationNo(String doctorRegistrationNo) { this.doctorRegistrationNo = doctorRegistrationNo; }

    public LocalDate getPrescriptionDate() { return prescriptionDate; }
    public void setPrescriptionDate(LocalDate prescriptionDate) { this.prescriptionDate = prescriptionDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

