package com.pet.vetalya.medshop.stock.repository;

import com.pet.vetalya.medshop.stock.entity.StockBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, String> {

    Optional<StockBatch> findByTenantIdAndId(String tenantId, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBatch b WHERE b.tenantId = :tenantId AND b.id = :id")
    Optional<StockBatch> findForUpdateByTenantIdAndId(@Param("tenantId") String tenantId, @Param("id") String id);

    Optional<StockBatch> findByTenantIdAndMedicineIdAndBatchNumber(String tenantId, String medicineId, String batchNumber);

    List<StockBatch> findByTenantIdAndMedicineId(String tenantId, String medicineId);

    // FEFO: First Expiry First Out, only non-expired with shelf-life >= minShelfLifeDate
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM StockBatch b
        WHERE b.tenantId = :tenantId
          AND b.medicineId = :medicineId
          AND b.quantityAvailable > 0
          AND b.expiryDate >= :minShelfLifeDate
        ORDER BY b.expiryDate ASC, b.id ASC
    """)
    List<StockBatch> findEligibleBatchesForSale(
            @Param("tenantId") String tenantId,
            @Param("medicineId") String medicineId,
            @Param("minShelfLifeDate") LocalDate minShelfLifeDate
    );

    @Query("""
        SELECT b FROM StockBatch b
        WHERE b.tenantId = :tenantId
          AND (:medicineId IS NULL OR b.medicineId = :medicineId)
          AND (:minExpiry IS NULL OR b.expiryDate >= :minExpiry)
          AND (:maxExpiry IS NULL OR b.expiryDate <= :maxExpiry)
          AND (:hasStock IS NULL OR (:hasStock = true AND b.quantityAvailable > 0) OR (:hasStock = false AND b.quantityAvailable = 0))
    """)
    Page<StockBatch> findBatchesFiltered(
            @Param("tenantId") String tenantId,
            @Param("medicineId") String medicineId,
            @Param("minExpiry") LocalDate minExpiry,
            @Param("maxExpiry") LocalDate maxExpiry,
            @Param("hasStock") Boolean hasStock,
            Pageable pageable
    );

    @Query("""
        SELECT b FROM StockBatch b
        WHERE b.tenantId = :tenantId
          AND b.expiryDate < :today
          AND b.quantityAvailable > 0
    """)
    List<StockBatch> findExpiredBatchesWithStock(@Param("tenantId") String tenantId, @Param("today") LocalDate today);

    @Query("""
        SELECT b FROM StockBatch b
        WHERE b.tenantId = :tenantId
          AND b.expiryDate >= :today
          AND b.expiryDate <= :cutoffDate
          AND b.quantityAvailable > 0
        ORDER BY b.expiryDate ASC
    """)
    List<StockBatch> findExpiringBatches(
            @Param("tenantId") String tenantId,
            @Param("today") LocalDate today,
            @Param("cutoffDate") LocalDate cutoffDate
    );

    // Valuation projection interface
    interface CategoryValuationProjection {
        String getCategoryName();
        BigDecimal getTotalPurchaseValue();
        BigDecimal getTotalMrpValue();
    }

    @Query(value = """
        SELECT COALESCE(c.name, 'Uncategorized') AS categoryName,
               COALESCE(SUM(b.quantity_available * b.purchase_price), 0) AS totalPurchaseValue,
               COALESCE(SUM(b.quantity_available * b.mrp), 0) AS totalMrpValue
        FROM medshop.stock_batches b
        JOIN medshop.medicines m ON b.medicine_id = m.id AND b.tenant_id = m.tenant_id
        LEFT JOIN medshop.categories c ON m.category_id = c.id AND m.tenant_id = c.tenant_id
        WHERE b.tenant_id = :tenantId AND b.quantity_available > 0
        GROUP BY c.name
    """, nativeQuery = true)
    List<CategoryValuationProjection> getValuationByCategory(@Param("tenantId") String tenantId);

    // Ledger mismatch projection interface
    interface LedgerMismatchProjection {
        String getBatchId();
        String getMedicineId();
        String getBatchNumber();
        int getQuantityReceived();
        int getQuantityAvailable();
        int getLedgerSum();
        int getExpectedAvailable();
    }

    @Query(value = """
        SELECT b.id AS batchId,
               b.medicine_id AS medicineId,
               b.batch_number AS batchNumber,
               b.quantity_received AS quantityReceived,
               b.quantity_available AS quantityAvailable,
               COALESCE(SUM(t.quantity), 0) AS ledgerSum,
               (b.quantity_received + COALESCE(SUM(CASE WHEN t.transaction_type <> 'PURCHASE' THEN t.quantity ELSE 0 END), 0)) AS expectedAvailable
        FROM medshop.stock_batches b
        LEFT JOIN medshop.stock_transactions t ON b.id = t.batch_id AND b.tenant_id = t.tenant_id
        WHERE b.tenant_id = :tenantId
        GROUP BY b.id, b.medicine_id, b.batch_number, b.quantity_received, b.quantity_available
        HAVING b.quantity_available <> (b.quantity_received + COALESCE(SUM(CASE WHEN t.transaction_type <> 'PURCHASE' THEN t.quantity ELSE 0 END), 0))
    """, nativeQuery = true)
    List<LedgerMismatchProjection> findLedgerMismatches(@Param("tenantId") String tenantId);

    boolean existsByTenantIdAndMedicineId(String tenantId, String medicineId);
}
