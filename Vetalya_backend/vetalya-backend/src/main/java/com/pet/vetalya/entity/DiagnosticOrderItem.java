package com.pet.vetalya.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "diagnostic_order_item")
public class DiagnosticOrderItem {

    @Id
    @Column(name = "item_id", length = 26)
    private String itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private DiagnosticOrder order;

    @Column(name = "test_code", length = 50, nullable = false)
    private String testCode;

    @Column(name = "test_name", length = 200, nullable = false)
    private String testName;

    @Column(length = 50, nullable = false)
    private String category;

    @Column(length = 30, nullable = false)
    private String status = "PENDING";

    @Column(name = "result_value", columnDefinition = "TEXT")
    private String resultValue;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(length = 50)
    private String unit;

    @Column(length = 20)
    private String flag = "NORMAL";

    @Column(name = "report_file_url", columnDefinition = "TEXT")
    private String reportFileUrl;

    @Column(columnDefinition = "TEXT")
    private String interpretation;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "PENDING";
        if (flag == null) flag = "NORMAL";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public DiagnosticOrder getOrder() { return order; }
    public void setOrder(DiagnosticOrder order) { this.order = order; }

    public String getTestCode() { return testCode; }
    public void setTestCode(String testCode) { this.testCode = testCode; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResultValue() { return resultValue; }
    public void setResultValue(String resultValue) { this.resultValue = resultValue; }

    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    public String getReportFileUrl() { return reportFileUrl; }
    public void setReportFileUrl(String reportFileUrl) { this.reportFileUrl = reportFileUrl; }

    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

