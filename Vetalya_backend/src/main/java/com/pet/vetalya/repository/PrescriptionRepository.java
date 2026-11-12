package com.pet.vetalya.repository;

import com.pet.vetalya.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, String> {

    List<Prescription> findAllByPetIdOrderByCreatedAtDesc(String petId);

    Page<Prescription> findAllByPetIdOrderByCreatedAtDesc(String petId, Pageable pageable);

    Optional<Prescription> findByEncounterId(String encounterId);
}

