package com.pet.vetalya.medshop.catalog.repository;

import com.pet.vetalya.medshop.catalog.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

    Optional<Supplier> findByTenantIdAndId(String tenantId, String id);

    Page<Supplier> findByTenantId(String tenantId, Pageable pageable);

    boolean existsByTenantIdAndName(String tenantId, String name);
}

