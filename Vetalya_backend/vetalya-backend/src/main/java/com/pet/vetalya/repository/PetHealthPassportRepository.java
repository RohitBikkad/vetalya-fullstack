package com.pet.vetalya.repository;

import com.pet.vetalya.entity.PetHealthPassport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PetHealthPassportRepository extends JpaRepository<PetHealthPassport, String> {

    Optional<PetHealthPassport> findByPetId(String petId);

    Optional<PetHealthPassport> findByPassportNumber(String passportNumber);

    Optional<PetHealthPassport> findByPublicEmergencyQrHashAndQrRevokedIsFalse(String qrHash);

    boolean existsByPassportNumber(String passportNumber);
}

