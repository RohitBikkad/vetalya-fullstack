package com.pet.vetalya.medshop.stock.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_batches", schema = "medshop")
public class StockBatch {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "medicine_id", length = 36, nullable = false)
    private String medicineId;

    @Column(name = "batch_number", length = 50, nullable = false)
    private String batchNumber;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @Column(name = "purchase_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal purchasePrice;

    @Column(name = "mrp", precision = 12, scale = 2, nullable = false)
    private BigDecimal mrp;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();

    public StockBatch() {
        this.id = UUID.randomUUID().toString();
    }

    public StockBatch(String id) {
        this.id = id;
    }

    public StockBatch(String tenantId, String medicineId, String batchNumber,
                      LocalDate manufacturingDate, LocalDate expiryDate,
                      int quantityReceived, int quantityAvailable,
                      BigDecimal purchasePrice, BigDecimal mrp) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.medicineId = medicineId;
        this.batchNumber = batchNumber;
        this.manufacturingDate = manufacturingDate;
        this.expiryDate = expiryDate;
        this.quantityReceived = quantityReceived;
        this.quantityAvailable = quantityAvailable;
        this.purchasePrice = purchasePrice;
        this.mrp = mrp;
        this.receivedAt = Instant.now();
    }

    public boolean isExpired(LocalDate asOfDate) {
        return expiryDate != null && expiryDate.isBefore(asOfDate);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = quantityReceived; }

    public int getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(int quantityAvailable) { this.quantityAvailable = quantityAvailable; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}

