package com.pet.vetalya.medshop.gst.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gst_returns", schema = "medshop")
public class GstReturn {

    public enum Type {
        GSTR1, GSTR3B
    }

    public enum Status {
        DRAFT, GENERATED, FILED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "return_month", nullable = false)
    private int returnMonth;

    @Column(name = "return_year", nullable = false)
    private int returnYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", length = 10, nullable = false)
    private Type returnType;

    @Column(name = "total_taxable_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalTaxableValue = BigDecimal.ZERO;

    @Column(name = "total_cgst", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalCgst = BigDecimal.ZERO;

    @Column(name = "total_sgst", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalSgst = BigDecimal.ZERO;

    @Column(name = "total_igst", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalIgst = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.GENERATED;

    @Column(name = "filed_ref_number", length = 100)
    private String filedRefNumber;

    @Column(name = "filed_at")
    private Instant filedAt;

    public GstReturn() {
        this.id = UUID.randomUUID().toString();
    }

    public GstReturn(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public int getReturnMonth() { return returnMonth; }
    public void setReturnMonth(int returnMonth) { this.returnMonth = returnMonth; }

    public int getReturnYear() { return returnYear; }
    public void setReturnYear(int returnYear) { this.returnYear = returnYear; }

    public Type getReturnType() { return returnType; }
    public void setReturnType(Type returnType) { this.returnType = returnType; }

    public BigDecimal getTotalTaxableValue() { return totalTaxableValue; }
    public void setTotalTaxableValue(BigDecimal totalTaxableValue) { this.totalTaxableValue = totalTaxableValue; }

    public BigDecimal getTotalCgst() { return totalCgst; }
    public void setTotalCgst(BigDecimal totalCgst) { this.totalCgst = totalCgst; }

    public BigDecimal getTotalSgst() { return totalSgst; }
    public void setTotalSgst(BigDecimal totalSgst) { this.totalSgst = totalSgst; }

    public BigDecimal getTotalIgst() { return totalIgst; }
    public void setTotalIgst(BigDecimal totalIgst) { this.totalIgst = totalIgst; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getFiledRefNumber() { return filedRefNumber; }
    public void setFiledRefNumber(String filedRefNumber) { this.filedRefNumber = filedRefNumber; }

    public Instant getFiledAt() { return filedAt; }
    public void setFiledAt(Instant filedAt) { this.filedAt = filedAt; }
}

