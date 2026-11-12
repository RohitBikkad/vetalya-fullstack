package com.pet.vetalya.audit.repository;

import com.pet.vetalya.audit.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a.id FROM AuditLog a WHERE a.createdAt < :cutoff ORDER BY a.createdAt ASC")
    List<Long> findIdsOlderThan(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.id IN :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    long countByEventTypeAndOutcome(String eventType, String outcome);
}

