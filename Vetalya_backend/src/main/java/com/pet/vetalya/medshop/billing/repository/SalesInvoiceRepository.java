package com.pet.vetalya.medshop.billing.repository;

import com.pet.vetalya.medshop.billing.entity.SalesInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, String> {

    Optional<SalesInvoice> findByTenantIdAndId(String tenantId, String id);

    Optional<SalesInvoice> findByTenantIdAndInvoiceNumber(String tenantId, String invoiceNumber);

    @Query("""
        SELECT i FROM SalesInvoice i
        WHERE i.tenantId = :tenantId
          AND (:from IS NULL OR i.invoiceDate >= :from)
          AND (:to IS NULL OR i.invoiceDate <= :to)
          AND (:status IS NULL OR i.status = :status)
          AND (:customerId IS NULL OR i.customerId = :customerId)
          AND (:paymentMode IS NULL OR i.paymentMode = :paymentMode)
          AND (:createdBy IS NULL OR i.createdBy = :createdBy)
          AND (:invoiceNumber IS NULL OR i.invoiceNumber LIKE CONCAT('%', :invoiceNumber, '%'))
        ORDER BY i.invoiceDate DESC, i.createdAt DESC
    """)
    Page<SalesInvoice> findFiltered(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") SalesInvoice.Status status,
            @Param("customerId") String customerId,
            @Param("paymentMode") SalesInvoice.PaymentMode paymentMode,
            @Param("createdBy") String createdBy,
            @Param("invoiceNumber") String invoiceNumber,
            Pageable pageable
    );

    boolean existsByTenantIdAndCustomerId(String tenantId, String customerId);

    boolean existsByTenantIdAndStatusAndInvoiceDateBetween(
            String tenantId, SalesInvoice.Status status, LocalDate from, LocalDate to
    );

    List<SalesInvoice> findByTenantIdAndStatusAndInvoiceDateBetween(
            String tenantId, SalesInvoice.Status status, LocalDate from, LocalDate to
    );

    // Dashboard metrics
    @Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0)
        FROM SalesInvoice i
        WHERE i.tenantId = :tenantId AND i.status = 'FINALIZED' AND i.invoiceDate = :date
    """)
    BigDecimal getTodaySalesAmount(@Param("tenantId") String tenantId, @Param("date") LocalDate date);

    @Query("""
        SELECT COUNT(i)
        FROM SalesInvoice i
        WHERE i.tenantId = :tenantId AND i.status = 'FINALIZED' AND i.invoiceDate = :date
    """)
    long getTodayInvoiceCount(@Param("tenantId") String tenantId, @Param("date") LocalDate date);

    @Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0)
        FROM SalesInvoice i
        WHERE i.tenantId = :tenantId AND i.status = 'FINALIZED'
          AND i.invoiceDate >= :startDate AND i.invoiceDate <= :endDate
    """)
    BigDecimal getSalesAmountBetween(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Report Projections
    interface TopMedicineProjection {
        String getMedicineId();
        String getMedicineName();
        long getTotalQuantitySold();
        BigDecimal getTotalRevenue();
    }

    @Query(value = """
        SELECT m.id AS medicineId,
               m.name AS medicineName,
               COALESCE(SUM(it.quantity), 0) AS totalQuantitySold,
               COALESCE(SUM(it.total_amount), 0) AS totalRevenue
        FROM medshop.sales_invoices i
        JOIN medshop.invoice_items it ON i.id = it.invoice_id AND i.tenant_id = it.tenant_id
        JOIN medshop.medicines m ON it.medicine_id = m.id AND it.tenant_id = m.tenant_id
        WHERE i.tenant_id = :tenantId AND i.status = 'FINALIZED'
          AND i.invoice_date >= :from AND i.invoice_date <= :to
        GROUP BY m.id, m.name
        ORDER BY totalRevenue DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<TopMedicineProjection> getTopMedicines(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("limit") int limit
    );

    interface ProfitReportProjection {
        BigDecimal getTotalRevenue();
        BigDecimal getTotalCost();
        BigDecimal getGrossProfit();
    }

    @Query(value = """
        SELECT COALESCE(SUM(it.taxable_value), 0) AS totalRevenue,
               COALESCE(SUM(it.quantity * b.purchase_price), 0) AS totalCost,
               (COALESCE(SUM(it.taxable_value), 0) - COALESCE(SUM(it.quantity * b.purchase_price), 0)) AS grossProfit
        FROM medshop.sales_invoices i
        JOIN medshop.invoice_items it ON i.id = it.invoice_id AND i.tenant_id = it.tenant_id
        JOIN medshop.stock_batches b ON it.batch_id = b.id AND it.tenant_id = b.tenant_id
        WHERE i.tenant_id = :tenantId AND i.status = 'FINALIZED'
          AND i.invoice_date >= :from AND i.invoice_date <= :to
    """, nativeQuery = true)
    ProfitReportProjection getProfitReport(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    interface SalesGroupedProjection {
        String getGroupKey();
        long getInvoiceCount();
        BigDecimal getTotalTaxable();
        BigDecimal getTotalTax();
        BigDecimal getTotalAmount();
    }

    @Query(value = """
        SELECT i.payment_mode AS groupKey,
               COUNT(i.id) AS invoiceCount,
               COALESCE(SUM(i.subtotal), 0) AS totalTaxable,
               COALESCE(SUM(i.cgst_amount + i.sgst_amount + i.igst_amount), 0) AS totalTax,
               COALESCE(SUM(i.total_amount), 0) AS totalAmount
        FROM medshop.sales_invoices i
        WHERE i.tenant_id = :tenantId AND i.status = 'FINALIZED'
          AND i.invoice_date >= :from AND i.invoice_date <= :to
        GROUP BY i.payment_mode
    """, nativeQuery = true)
    List<SalesGroupedProjection> getSalesGroupedByPaymentMode(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query(value = """
        SELECT CAST(i.invoice_date AS TEXT) AS groupKey,
               COUNT(i.id) AS invoiceCount,
               COALESCE(SUM(i.subtotal), 0) AS totalTaxable,
               COALESCE(SUM(i.cgst_amount + i.sgst_amount + i.igst_amount), 0) AS totalTax,
               COALESCE(SUM(i.total_amount), 0) AS totalAmount
        FROM medshop.sales_invoices i
        WHERE i.tenant_id = :tenantId AND i.status = 'FINALIZED'
          AND i.invoice_date >= :from AND i.invoice_date <= :to
        GROUP BY i.invoice_date
        ORDER BY i.invoice_date ASC
    """, nativeQuery = true)
    List<SalesGroupedProjection> getSalesGroupedByDay(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}

