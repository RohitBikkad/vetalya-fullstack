package com.pet.vetalya.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "diagnostic_test_catalog")
public class DiagnosticTestCatalog {

    @Id
    @Column(name = "test_id", length = 26)
    private String testId;

    @Column(name = "test_code", length = 50, unique = true, nullable = false)
    private String testCode;

    @Column(name = "test_name", length = 200, nullable = false)
    private String testName;

    @Column(length = 50, nullable = false)
    private String category;

    @Column(name = "sample_type", length = 50)
    private String sampleType;

    @Column(name = "tat_hours")
    private Integer tatHours = 24;

    @Column(name = "base_price_paisa", nullable = false)
    private Long basePricePaisa = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = testId; }

    public String getTestCode() { return testCode; }
    public void setTestCode(String testCode) { this.testCode = testCode; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSampleType() { return sampleType; }
    public void setSampleType(String sampleType) { this.sampleType = sampleType; }

    public Integer getTatHours() { return tatHours; }
    public void setTatHours(Integer tatHours) { this.tatHours = tatHours; }

    public Long getBasePricePaisa() { return basePricePaisa; }
    public void setBasePricePaisa(Long basePricePaisa) { this.basePricePaisa = basePricePaisa; }

    public Instant getCreatedAt() { return createdAt; }
}

