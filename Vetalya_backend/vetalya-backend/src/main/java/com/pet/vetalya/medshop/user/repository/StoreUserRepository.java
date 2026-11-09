package com.pet.vetalya.medshop.user.repository;

import com.pet.vetalya.medshop.user.entity.StoreUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreUserRepository extends JpaRepository<StoreUser, String> {

    Optional<StoreUser> findByUsername(String username);

    Optional<StoreUser> findByEmail(String email);

    Optional<StoreUser> findByPhone(String phone);

    Optional<StoreUser> findByTenantIdAndId(String tenantId, String id);

    Page<StoreUser> findByTenantId(String tenantId, Pageable pageable);

    long countByTenantIdAndRoleAndActiveTrue(String tenantId, String role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}

