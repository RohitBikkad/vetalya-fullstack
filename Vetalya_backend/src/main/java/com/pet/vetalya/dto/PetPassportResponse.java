package com.pet.vetalya.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PetPassportResponse(
        String passportId,
        String petId,
        String passportNumber,
        String name,
        String species,
        String breed,
        String gender,
        LocalDate dob,
        String age,
        BigDecimal weightKg,
        String color,
        String distinctiveMarks,
        String microchipNumber,
        String bloodGroup,
        String allergiesSummary,
        String chronicConditionsSummary,
        String dietaryNotes,
        String emergencyContactPhone,
        String publicEmergencyQrHash,
        String photoUrl,
        boolean isDeceased,
        LocalDate nextVaccinationDue,
        Instant nextAppointmentAt,
        List<PetIdentifierDto> identifiers,
        Instant createdAt,
        Instant updatedAt
) {
    public record PetIdentifierDto(
            Long id,
            String identifierType,
            String identifierValue,
            String issuingAuthority,
            boolean isVerified,
            Instant verifiedAt
    ) {}
}

