package com.pet.vetalya.medshop.stock.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_transactions", schema = "medshop")
public class StockTransaction {

    public enum Type {
        PURCHASE,
        SALE,
        SALE_RETURN,
        PURCHASE_RETURN,
        ADJUSTMENT,
        EXPIRY_WRITE_OFF,
        DAMAGED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "medicine_id", length = 36, nullable = false)
    private String medicineId;

    @Column(name = "batch_id", length = 36, nullable = false)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 30, nullable = false)
    private Type transactionType;

    @Column(name = "quantity", nullable = false)
    private int quantity; // Positive for IN, Negative for OUT

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "performed_by", length = 36, nullable = false)
    private String performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StockTransaction() {
        this.id = UUID.randomUUID().toString();
    }

    public StockTransaction(String tenantId, String medicineId, String batchId,
                            Type transactionType, int quantity, String reason, String performedBy) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.medicineId = medicineId;
        this.batchId = batchId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.reason = reason;
        this.performedBy = performedBy;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public Type getTransactionType() { return transactionType; }
    public void setTransactionType(Type transactionType) { this.transactionType = transactionType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

