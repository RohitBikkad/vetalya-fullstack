package com.pet.vetalya.medshop.audit.repository;

import com.pet.vetalya.medshop.audit.entity.StoreAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface StoreAuditLogRepository extends JpaRepository<StoreAuditLog, Long> {

    @Query("""
        SELECT a FROM StoreAuditLog a
        WHERE a.tenantId = :tenantId
          AND (:entityType IS NULL OR a.entityType = :entityType)
          AND (:entityId IS NULL OR a.entityId = :entityId)
          AND (:userId IS NULL OR a.userId = :userId)
          AND (:from IS NULL OR a.createdAt >= :from)
          AND (:to IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
    """)
    Page<StoreAuditLog> findFiltered(
            @Param("tenantId") String tenantId,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("userId") String userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}

