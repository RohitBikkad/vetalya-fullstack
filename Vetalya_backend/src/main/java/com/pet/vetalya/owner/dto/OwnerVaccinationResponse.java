package com.pet.vetalya.owner.dto;

import java.time.Instant;
import java.time.LocalDate;

public record OwnerVaccinationResponse(
        String vaccinationId,
        String petId,
        String petName,
        String vaccineName,
        String batchNumber,
        LocalDate lastVaccinationDate,
        LocalDate nextVaccinationDate,
        String notes,
        String administeringVetName,
        Instant createdAt
) {}
