package com.pet.vetalya.medshop.stock.repository;

import com.pet.vetalya.medshop.stock.entity.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, String> {

    @Query("""
        SELECT t FROM StockTransaction t
        WHERE t.tenantId = :tenantId
          AND (:medicineId IS NULL OR t.medicineId = :medicineId)
          AND (:batchId IS NULL OR t.batchId = :batchId)
          AND (:type IS NULL OR t.transactionType = :type)
          AND (:from IS NULL OR t.createdAt >= :from)
          AND (:to IS NULL OR t.createdAt <= :to)
        ORDER BY t.createdAt DESC
    """)
    Page<StockTransaction> findFiltered(
            @Param("tenantId") String tenantId,
            @Param("medicineId") String medicineId,
            @Param("batchId") String batchId,
            @Param("type") StockTransaction.Type type,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    List<StockTransaction> findByTenantIdAndBatchId(String tenantId, String batchId);

    long countByTenantIdAndBatchIdAndTransactionTypeNot(String tenantId, String batchId, StockTransaction.Type type);
}

