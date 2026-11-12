package com.pet.vetalya.medshop.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "medicines", schema = "medshop")
public class Medicine {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "generic_name", length = 150)
    private String genericName;

    @Column(name = "hsn_code", length = 10, nullable = false)
    private String hsnCode;

    @Column(name = "category_id", length = 36)
    private String categoryId;

    @Column(name = "supplier_id", length = 36)
    private String supplierId;

    @Column(name = "manufacturer", length = 150)
    private String manufacturer;

    @Column(name = "form", length = 50)
    private String form;

    @Column(name = "strength", length = 50)
    private String strength;

    @Column(name = "mrp", precision = 12, scale = 2, nullable = false)
    private BigDecimal mrp;

    @Column(name = "purchase_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal purchasePrice;

    @Column(name = "gst_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal gstRate;

    @Column(name = "is_schedule_h", nullable = false)
    private boolean scheduleH = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 10;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Medicine() {
        this.id = UUID.randomUUID().toString();
    }

    public Medicine(String id) {
        this.id = id;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public boolean isScheduleH() { return scheduleH; }
    public void setScheduleH(boolean scheduleH) { this.scheduleH = scheduleH; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

