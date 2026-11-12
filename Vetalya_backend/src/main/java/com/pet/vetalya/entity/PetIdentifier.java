package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pet_identifier")
public class PetIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", length = 26, nullable = false)
    private String petId;

    @Column(name = "identifier_type", length = 50, nullable = false)
    private String identifierType; // MICROCHIP, RFID_TAG, REGISTRATION_TAG, PASHU_AADHAAR

    @Column(name = "identifier_value", length = 100, nullable = false)
    private String identifierValue;

    @Column(name = "issuing_authority", length = 150)
    private String issuingAuthority;

    @Column(name = "verified_by_vet_id", length = 26)
    private String verifiedByVetId;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // ── Getters / Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getIdentifierType() { return identifierType; }
    public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }

    public String getIdentifierValue() { return identifierValue; }
    public void setIdentifierValue(String identifierValue) { this.identifierValue = identifierValue; }

    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }

    public String getVerifiedByVetId() { return verifiedByVetId; }
    public void setVerifiedByVetId(String verifiedByVetId) { this.verifiedByVetId = verifiedByVetId; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public Instant getCreatedAt() { return createdAt; }

    public Pet getPet() { return pet; }
    public void setPet(Pet pet) { this.pet = pet; }
}

