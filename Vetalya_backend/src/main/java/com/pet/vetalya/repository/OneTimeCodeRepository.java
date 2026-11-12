package com.pet.vetalya.repository;

import com.pet.vetalya.entity.OneTimeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, Long> {
    Optional<OneTimeCode> findByCodeHash(String codeHash);
}
