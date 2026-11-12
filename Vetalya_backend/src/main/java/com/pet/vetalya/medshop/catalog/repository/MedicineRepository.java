package com.pet.vetalya.medshop.catalog.repository;

import com.pet.vetalya.medshop.catalog.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, String> {

    Optional<Medicine> findByTenantIdAndId(String tenantId, String id);

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.tenantId = :tenantId
          AND (:q IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:categoryId IS NULL OR m.categoryId = :categoryId)
          AND (:supplierId IS NULL OR m.supplierId = :supplierId)
          AND (:form IS NULL OR m.form = :form)
          AND (:scheduleH IS NULL OR m.scheduleH = :scheduleH)
          AND (:active IS NULL OR m.active = :active)
    """)
    Page<Medicine> findFiltered(
            @Param("tenantId") String tenantId,
            @Param("q") String q,
            @Param("categoryId") String categoryId,
            @Param("supplierId") String supplierId,
            @Param("form") String form,
            @Param("scheduleH") Boolean scheduleH,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.tenantId = :tenantId AND m.active = true
          AND (
            COALESCE((
              SELECT SUM(b.quantityAvailable)
              FROM StockBatch b
              WHERE b.tenantId = :tenantId
                AND b.medicineId = m.id
                AND b.expiryDate >= :today
            ), 0) < m.reorderLevel
          )
    """)
    List<Medicine> findLowStockMedicines(@Param("tenantId") String tenantId, @Param("today") LocalDate today);

    @Query("""
        SELECT COALESCE(SUM(b.quantityAvailable), 0)
        FROM StockBatch b
        WHERE b.tenantId = :tenantId
          AND b.medicineId = :medicineId
          AND b.expiryDate >= :today
    """)
    int getAvailableStock(@Param("tenantId") String tenantId, @Param("medicineId") String medicineId, @Param("today") LocalDate today);

    @Query("""
        SELECT MIN(b.expiryDate)
        FROM StockBatch b
        WHERE b.tenantId = :tenantId
          AND b.medicineId = :medicineId
          AND b.expiryDate >= :today
          AND b.quantityAvailable > 0
    """)
    LocalDate getNearestExpiry(@Param("tenantId") String tenantId, @Param("medicineId") String medicineId, @Param("today") LocalDate today);

    boolean existsByTenantIdAndCategoryId(String tenantId, String categoryId);

    boolean existsByTenantIdAndSupplierId(String tenantId, String supplierId);
}

