package com.pet.vetalya.medshop.auth.repository;

import com.pet.vetalya.medshop.auth.entity.AuthExchangeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AuthExchangeCodeRepository extends JpaRepository<AuthExchangeCode, String> {

    Optional<AuthExchangeCode> findByCodeHash(String codeHash);

    @Modifying
    @Query("DELETE FROM AuthExchangeCode c WHERE c.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}

