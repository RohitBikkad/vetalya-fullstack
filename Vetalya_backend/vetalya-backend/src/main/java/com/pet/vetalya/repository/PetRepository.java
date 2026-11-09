package com.pet.vetalya.repository;

import com.pet.vetalya.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, String> {

    Page<Pet> findAllByOwnerUserIdAndDeletedAtIsNull(String ownerUserId, Pageable pageable);

    List<Pet> findAllByOwnerUserIdAndDeletedAtIsNull(String ownerUserId);

    Optional<Pet> findByPetIdAndOwnerUserIdAndDeletedAtIsNull(String petId, String ownerUserId);

    long countByOwnerUserIdAndDeletedAtIsNull(String ownerUserId);

    boolean existsByRegistrationNoAndDeletedAtIsNull(String registrationNo);
}
