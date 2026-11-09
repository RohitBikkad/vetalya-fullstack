package com.pet.vetalya.vet.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for a vaccination record.
 */
public record VaccinationResponse(
        String vaccinationId,
        String petId,
        String vetUserId,
        String vaccineName,
        String batchNumber,
        LocalDate lastVaccinationDate,
        LocalDate nextVaccinationDate,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
