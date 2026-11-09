package com.pet.vetalya.medshop.billing.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "invoice_sequences", schema = "medshop")
@IdClass(InvoiceSequence.InvoiceSequenceId.class)
public class InvoiceSequence {

    @Id
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Id
    @Column(name = "financial_year", length = 7, nullable = false)
    private String financialYear;

    @Column(name = "last_number", nullable = false)
    private long lastNumber = 0;

    public InvoiceSequence() {}

    public InvoiceSequence(String tenantId, String financialYear, long lastNumber) {
        this.tenantId = tenantId;
        this.financialYear = financialYear;
        this.lastNumber = lastNumber;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getFinancialYear() { return financialYear; }
    public void setFinancialYear(String financialYear) { this.financialYear = financialYear; }

    public long getLastNumber() { return lastNumber; }
    public void setLastNumber(long lastNumber) { this.lastNumber = lastNumber; }

    public static class InvoiceSequenceId implements Serializable {
        private String tenantId;
        private String financialYear;

        public InvoiceSequenceId() {}

        public InvoiceSequenceId(String tenantId, String financialYear) {
            this.tenantId = tenantId;
            this.financialYear = financialYear;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvoiceSequenceId that = (InvoiceSequenceId) o;
            return Objects.equals(tenantId, that.tenantId) && Objects.equals(financialYear, that.financialYear);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, financialYear);
        }
    }
}

