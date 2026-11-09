package com.pet.vetalya.owner.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PetDetailResponse(
        String petId,
        String name,
        String species,
        String gender,
        LocalDate dob,
        String age,
        String breed,
        String photoUrl,
        String registrationNo,
        LocalDate nextVaccinationDue,
        Instant nextAppointmentAt,
        Instant createdAt,
        Instant updatedAt
) {}
