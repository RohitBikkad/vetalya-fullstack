package com.pet.vetalya.repository;

import com.pet.vetalya.entity.EncounterAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EncounterAmendmentRepository extends JpaRepository<EncounterAmendment, String> {

    List<EncounterAmendment> findAllByEncounterIdOrderByCreatedAtDesc(String encounterId);
}

