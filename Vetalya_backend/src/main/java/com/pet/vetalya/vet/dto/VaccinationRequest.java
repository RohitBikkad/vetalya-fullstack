package com.pet.vetalya.vet.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request DTO for creating / updating a vaccination record.
 */
public record VaccinationRequest(
        @NotBlank(message = "Vaccine name is required")
        @Size(max = 200, message = "Vaccine name must be at most 200 characters")
        String vaccineName,

        @Size(max = 100, message = "Batch number must be at most 100 characters")
        String batchNumber,

        @NotNull(message = "Last vaccination date is required")
        LocalDate lastVaccinationDate,

        LocalDate nextVaccinationDate,

        String notes
) {}
