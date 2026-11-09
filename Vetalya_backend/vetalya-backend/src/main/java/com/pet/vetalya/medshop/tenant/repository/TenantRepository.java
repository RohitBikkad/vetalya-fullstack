package com.pet.vetalya.medshop.tenant.repository;

import com.pet.vetalya.medshop.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findByGstin(String gstin);

    boolean existsByGstin(String gstin);
}

