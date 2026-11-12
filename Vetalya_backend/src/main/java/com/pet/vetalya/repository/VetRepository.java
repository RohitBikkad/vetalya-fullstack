package com.pet.vetalya.repository;

import com.pet.vetalya.entity.Vet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VetRepository extends JpaRepository<Vet, String> {
    long countByVerifiedTrue();
    long countByVerifiedFalse();
}
