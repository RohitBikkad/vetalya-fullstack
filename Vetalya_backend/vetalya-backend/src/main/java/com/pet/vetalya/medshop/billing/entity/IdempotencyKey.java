package com.pet.vetalya.medshop.billing.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "idempotency_keys", schema = "medshop")
@IdClass(IdempotencyKey.IdempotencyKeyId.class)
public class IdempotencyKey {

    @Id
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Id
    @Column(name = "idempotency_key", length = 100, nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Column(name = "invoice_id", length = 36, nullable = false)
    private String invoiceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public IdempotencyKey() {}

    public IdempotencyKey(String tenantId, String idempotencyKey, String requestHash, String invoiceId) {
        this.tenantId = tenantId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.invoiceId = invoiceId;
        this.createdAt = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static class IdempotencyKeyId implements Serializable {
        private String tenantId;
        private String idempotencyKey;

        public IdempotencyKeyId() {}

        public IdempotencyKeyId(String tenantId, String idempotencyKey) {
            this.tenantId = tenantId;
            this.idempotencyKey = idempotencyKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IdempotencyKeyId that = (IdempotencyKeyId) o;
            return Objects.equals(tenantId, that.tenantId) && Objects.equals(idempotencyKey, that.idempotencyKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, idempotencyKey);
        }
    }
}

