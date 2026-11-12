package com.pet.vetalya.medshop.customer.repository;

import com.pet.vetalya.medshop.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    Optional<Customer> findByTenantIdAndId(String tenantId, String id);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.tenantId = :tenantId
          AND (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR c.phone LIKE CONCAT('%', :q, '%'))
    """)
    Page<Customer> search(@Param("tenantId") String tenantId, @Param("q") String q, Pageable pageable);

    boolean existsByTenantIdAndPhone(String tenantId, String phone);
}

