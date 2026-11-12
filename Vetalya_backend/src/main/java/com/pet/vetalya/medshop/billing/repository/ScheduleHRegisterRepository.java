package com.pet.vetalya.medshop.billing.repository;

import com.pet.vetalya.medshop.billing.entity.ScheduleHRegister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ScheduleHRegisterRepository extends JpaRepository<ScheduleHRegister, String> {

    Optional<ScheduleHRegister> findByTenantIdAndId(String tenantId, String id);

    @Query("""
        SELECT s FROM ScheduleHRegister s
        WHERE s.tenantId = :tenantId
          AND (:from IS NULL OR s.prescriptionDate >= :from)
          AND (:to IS NULL OR s.prescriptionDate <= :to)
          AND (:medicineId IS NULL OR s.medicineId = :medicineId)
        ORDER BY s.prescriptionDate DESC, s.createdAt DESC
    """)
    Page<ScheduleHRegister> findFiltered(
            @Param("tenantId") String tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("medicineId") String medicineId,
            Pageable pageable
    );
}

