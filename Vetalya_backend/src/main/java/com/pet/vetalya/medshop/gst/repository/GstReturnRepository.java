package com.pet.vetalya.medshop.gst.repository;

import com.pet.vetalya.medshop.gst.entity.GstReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GstReturnRepository extends JpaRepository<GstReturn, String> {

    Optional<GstReturn> findByTenantIdAndId(String tenantId, String id);

    Page<GstReturn> findByTenantId(String tenantId, Pageable pageable);

    Optional<GstReturn> findByTenantIdAndReturnYearAndReturnMonthAndReturnType(
            String tenantId, int returnYear, int returnMonth, GstReturn.Type returnType
    );

    boolean existsByTenantIdAndReturnYearAndReturnMonthAndStatus(
            String tenantId, int returnYear, int returnMonth, GstReturn.Status status
    );
}

