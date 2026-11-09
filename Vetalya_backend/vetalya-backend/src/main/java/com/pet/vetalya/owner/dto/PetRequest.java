package com.pet.vetalya.owner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PetRequest(
        @NotBlank(message = "Pet name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Species is required")
        @Pattern(regexp = "^(DOG|CAT|BIRD|RABBIT|OTHER)$",
                 message = "Species must be one of: DOG, CAT, BIRD, RABBIT, OTHER")
        String species,

        @NotBlank(message = "Gender is required")
        @Pattern(regexp = "^(MALE|FEMALE|UNKNOWN)$",
                 message = "Gender must be one of: MALE, FEMALE, UNKNOWN")
        String gender,

        @PastOrPresent(message = "Date of birth cannot be in the future")
        LocalDate dob,

        @Size(max = 100, message = "Breed must be at most 100 characters")
        String breed,

        @Size(max = 500, message = "Photo URL must be at most 500 characters")
        String photoUrl,

        @Pattern(regexp = "^PET-\\d{4}-[A-Z0-9]{6}$",
                 message = "Registration number must follow format PET-<year>-<6 alphanumeric> (e.g. PET-2026-A1B2C3)")
        String registrationNo
) {}
