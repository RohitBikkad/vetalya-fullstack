package com.pet.vetalya.medshop.auth.repository;

import com.pet.vetalya.medshop.auth.entity.StoreRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("storeRefreshTokenRepository")
public interface StoreRefreshTokenRepository extends JpaRepository<StoreRefreshToken, String> {

    Optional<StoreRefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE StoreRefreshToken r SET r.revoked = true WHERE r.familyId = :familyId")
    void revokeFamily(@Param("familyId") UUID familyId);

    @Modifying
    @Query("UPDATE StoreRefreshToken r SET r.revoked = true WHERE r.tenantId = :tenantId AND r.userId = :userId")
    void revokeAllForUser(@Param("tenantId") String tenantId, @Param("userId") String userId);
}

