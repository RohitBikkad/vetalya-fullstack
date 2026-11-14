package com.pet.vetalya.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PetPassportUpdateRequest(
        @Size(max = 20, message = "Blood group cannot exceed 20 characters")
        String bloodGroup,

        @Size(max = 1000, message = "Allergies summary cannot exceed 1000 characters")
        String allergiesSummary,

        @Size(max = 1000, message = "Chronic conditions summary cannot exceed 1000 characters")
        String chronicConditionsSummary,

        @Size(max = 1000, message = "Dietary notes cannot exceed 1000 characters")
        String dietaryNotes,

        @Size(max = 20, message = "Emergency contact phone cannot exceed 20 characters")
        String emergencyContactPhone,

        BigDecimal weightKg,

        @Size(max = 100, message = "Color cannot exceed 100 characters")
        String color,

        @Size(max = 255, message = "Distinctive marks cannot exceed 255 characters")
        String distinctiveMarks,

        @Size(max = 50, message = "Microchip number cannot exceed 50 characters")
        String microchipNumber
) {}
