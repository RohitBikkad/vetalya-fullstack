package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pet")
public class Pet {

    @Id
    @Column(name = "pet_id", length = 26)
    private String petId;

    @Column(name = "owner_user_id", length = 26, nullable = false)
    private String ownerUserId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String species;

    @Column(length = 100)
    private String breed;

    private LocalDate dob;

    @Column(length = 10)
    private String gender;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "registration_no", length = 50)
    private String registrationNo;

    @Column(name = "passport_number", length = 30, unique = true)
    private String passportNumber;

    @Column(length = 100)
    private String color;

    @Column(name = "distinctive_marks", length = 255)
    private String distinctiveMarks;

    @Column(name = "microchip_number", length = 50)
    private String microchipNumber;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private java.math.BigDecimal weightKg;

    @Column(name = "sterilization_status", length = 20)
    private String sterilizationStatus = "UNKNOWN";

    @Column(name = "is_deceased", nullable = false)
    private boolean isDeceased = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDistinctiveMarks() { return distinctiveMarks; }
    public void setDistinctiveMarks(String distinctiveMarks) { this.distinctiveMarks = distinctiveMarks; }

    public String getMicrochipNumber() { return microchipNumber; }
    public void setMicrochipNumber(String microchipNumber) { this.microchipNumber = microchipNumber; }

    public java.math.BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(java.math.BigDecimal weightKg) { this.weightKg = weightKg; }

    public String getSterilizationStatus() { return sterilizationStatus; }
    public void setSterilizationStatus(String sterilizationStatus) { this.sterilizationStatus = sterilizationStatus; }

    public boolean isDeceased() { return isDeceased; }
    public void setDeceased(boolean deceased) { isDeceased = deceased; }
}
