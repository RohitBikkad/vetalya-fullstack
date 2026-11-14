package com.pet.vetalya.repository;

import com.pet.vetalya.entity.ClinicalEncounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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

    boolean existsByEncounterIdAndPetIdAndHospitalIdAndVetUserId(
            String encounterId, String petId, String hospitalId, String vetUserId);

    @Query("SELECT e.encounterId FROM ClinicalEncounter e WHERE e.appointmentId = :appointmentId")
    Optional<String> findEncounterIdByAppointmentId(String appointmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ClinicalEncounter e WHERE e.encounterId = :encounterId")
    Optional<ClinicalEncounter> findByIdForUpdate(String encounterId);
}
