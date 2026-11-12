package com.pet.vetalya.medshop.auth.repository;

import com.pet.vetalya.medshop.auth.entity.StoreAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("storeAuthProviderRepository")
public interface StoreAuthProviderRepository extends JpaRepository<StoreAuthProvider, Long> {

    Optional<StoreAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<StoreAuthProvider> findByTenantIdAndUserId(String tenantId, String userId);

    Optional<StoreAuthProvider> findByTenantIdAndUserIdAndProvider(String tenantId, String userId, String provider);

    long countByTenantIdAndUserId(String tenantId, String userId);

    void deleteByTenantIdAndUserIdAndProvider(String tenantId, String userId, String provider);
}

