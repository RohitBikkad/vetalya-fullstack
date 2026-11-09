package com.pet.vetalya.medshop.tenant.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "medshop")
public class Tenant {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "shop_name", length = 150, nullable = false)
    private String shopName;

    @Column(name = "gstin", length = 15, unique = true)
    private String gstin;

    @Column(name = "state_code", length = 2, nullable = false)
    private String stateCode;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "plan", length = 50, nullable = false)
    private String plan = "STANDARD";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_composition_dealer", nullable = false)
    private boolean compositionDealer = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Tenant() {
        this.id = UUID.randomUUID().toString();
    }

    public Tenant(String id) {
        this.id = id;
    }

    public Tenant(String id, String shopName, String gstin, String stateCode, String address,
                  String phone, String email, String plan, boolean compositionDealer) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.shopName = shopName;
        this.gstin = gstin;
        this.stateCode = stateCode;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.plan = plan;
        this.compositionDealer = compositionDealer;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isCompositionDealer() { return compositionDealer; }
    public void setCompositionDealer(boolean compositionDealer) { this.compositionDealer = compositionDealer; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

