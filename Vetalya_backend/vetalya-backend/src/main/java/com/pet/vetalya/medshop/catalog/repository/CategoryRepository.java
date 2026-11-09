package com.pet.vetalya.medshop.catalog.repository;

import com.pet.vetalya.medshop.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    Optional<Category> findByTenantIdAndId(String tenantId, String id);

    List<Category> findByTenantId(String tenantId);

    List<Category> findByTenantIdAndParentId(String tenantId, String parentId);

    boolean existsByTenantIdAndName(String tenantId, String name);

    boolean existsByTenantIdAndParentId(String tenantId, String parentId);
}

