package com.pet.vetalya.medshop.billing.repository;

import com.pet.vetalya.medshop.billing.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKey.IdempotencyKeyId> {

    Optional<IdempotencyKey> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
}

