package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pet_document")
public class PetDocument {

    @Id
    @Column(name = "document_id", length = 26)
    private String documentId;

    @Column(name = "pet_id", length = 26, nullable = false)
    private String petId;

    @Column(name = "uploaded_by_user_id", length = 26, nullable = false)
    private String uploadedByUserId;

    @Column(nullable = false, length = 50)
    private String category; // VACCINATION_CERTIFICATE, PRESCRIPTION, LAB_REPORT, XRAY, ULTRASOUND, SURGERY_REPORT, DISCHARGE_SUMMARY, INSURANCE_DOC, ADOPTION_DOC, MICROCHIP_CERT, OTHER

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(length = 500)
    private String description;

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus = "OWNER_UPLOADED"; // VERIFIED_BY_VET, OWNER_UPLOADED, PENDING_VERIFICATION

    @Column(name = "verified_by_vet_id", length = 26)
    private String verifiedByVetId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

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

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(String uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerifiedByVetId() { return verifiedByVetId; }
    public void setVerifiedByVetId(String verifiedByVetId) { this.verifiedByVetId = verifiedByVetId; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }
}

