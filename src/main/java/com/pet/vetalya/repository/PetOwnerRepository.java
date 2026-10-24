package com.pet.vetalya.repository;

import com.pet.vetalya.entity.PetOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetOwnerRepository extends JpaRepository<PetOwner, String> {
}
