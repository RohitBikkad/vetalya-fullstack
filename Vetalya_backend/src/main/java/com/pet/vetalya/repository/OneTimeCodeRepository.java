package com.pet.vetalya.repository;

import com.pet.vetalya.entity.OneTimeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, Long> {
    Optional<OneTimeCode> findByCodeHash(String codeHash);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM OneTimeCode c WHERE c.codeHash = :codeHash")
    Optional<OneTimeCode> findByCodeHashForUpdate(String codeHash);
}
