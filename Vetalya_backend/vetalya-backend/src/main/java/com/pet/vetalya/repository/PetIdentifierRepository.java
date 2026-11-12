package com.pet.vetalya.repository;

import com.pet.vetalya.entity.PetIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PetIdentifierRepository extends JpaRepository<PetIdentifier, Long> {

    List<PetIdentifier> findAllByPetId(String petId);

    Optional<PetIdentifier> findByIdentifierValue(String identifierValue);

    boolean existsByIdentifierValue(String identifierValue);
}

