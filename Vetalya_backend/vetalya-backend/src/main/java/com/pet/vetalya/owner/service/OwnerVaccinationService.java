package com.pet.vetalya.owner.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.entity.Vaccination;
import com.pet.vetalya.owner.dto.OwnerVaccinationResponse;
import com.pet.vetalya.repository.PetRepository;
import com.pet.vetalya.repository.VaccinationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OwnerVaccinationService {

    private final VaccinationRepository vaccinationRepo;
    private final PetRepository petRepo;

    public OwnerVaccinationService(VaccinationRepository vaccinationRepo,
                                  PetRepository petRepo) {
        this.vaccinationRepo = vaccinationRepo;
        this.petRepo = petRepo;
    }

    @Transactional(readOnly = true)
    public List<OwnerVaccinationResponse> getPetVaccinations(String ownerUserId, String petId) {
        Pet pet = petRepo.findByPetIdAndOwnerUserIdAndDeletedAtIsNull(petId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        List<Object[]> rows = vaccinationRepo.findHistoryForPetWithVetName(petId);
        List<OwnerVaccinationResponse> results = new ArrayList<>();

        for (Object[] row : rows) {
            Vaccination v = (Vaccination) row[0];
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            String vetName = firstName != null ? (firstName + " " + (lastName != null ? lastName : "")).trim() : "Unknown Vet";

            results.add(new OwnerVaccinationResponse(
                    v.getVaccinationId(),
                    petId,
                    pet.getName(),
                    v.getVaccineName(),
                    v.getBatchNumber(),
                    v.getLastVaccinationDate(),
                    v.getNextVaccinationDate(),
                    v.getNotes(),
                    vetName,
                    v.getCreatedAt()
            ));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<OwnerVaccinationResponse> getDueVaccinations(String ownerUserId, Integer days) {
        int boundedDays = (days == null || days <= 0) ? 30 : Math.min(days, 365);
        LocalDate today = LocalDate.now();
        LocalDate toDate = today.plusDays(boundedDays);

        List<Object[]> rows = vaccinationRepo.findDueForOwner(ownerUserId, today, toDate);
        List<OwnerVaccinationResponse> results = new ArrayList<>();

        for (Object[] row : rows) {
            Vaccination v = (Vaccination) row[0];
            String petName = (String) row[1];
            String firstName = (String) row[2];
            String lastName = (String) row[3];
            String vetName = firstName != null ? (firstName + " " + (lastName != null ? lastName : "")).trim() : "Unknown Vet";

            results.add(new OwnerVaccinationResponse(
                    v.getVaccinationId(),
                    v.getPetId(),
                    petName,
                    v.getVaccineName(),
                    v.getBatchNumber(),
                    v.getLastVaccinationDate(),
                    v.getNextVaccinationDate(),
                    v.getNotes(),
                    vetName,
                    v.getCreatedAt()
            ));
        }

        return results;
    }
}
