package com.pet.vetalya.owner.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PetResponse(
        String petId,
        String name,
        String species,
        String gender,
        LocalDate dob,
        String age,
        String breed,
        String photoUrl,
        String registrationNo,
        Instant createdAt,
        Instant updatedAt
) {}
