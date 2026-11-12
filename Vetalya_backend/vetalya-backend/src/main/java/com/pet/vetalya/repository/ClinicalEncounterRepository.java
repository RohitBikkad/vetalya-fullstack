package com.pet.vetalya.repository;

import com.pet.vetalya.entity.ClinicalEncounter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClinicalEncounterRepository extends JpaRepository<ClinicalEncounter, String> {

    List<ClinicalEncounter> findAllByPetIdOrderByCreatedAtDesc(String petId);

    Page<ClinicalEncounter> findAllByPetIdOrderByCreatedAtDesc(String petId, Pageable pageable);

    List<ClinicalEncounter> findAllByPetIdAndIsSignedIsTrueOrderBySignedAtDesc(String petId);

    List<ClinicalEncounter> findAllByVetUserIdOrderByCreatedAtDesc(String vetUserId);

    Optional<ClinicalEncounter> findByAppointmentId(String appointmentId);
}

