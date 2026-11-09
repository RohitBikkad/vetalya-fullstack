package com.pet.vetalya.vet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request DTO for creating / updating vet profile.
 */
public record VetProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @Size(max = 50, message = "Registration number must be at most 50 characters")
        String registrationNo,

        LocalDate dob,

        @Size(max = 500, message = "Photo URL must be at most 500 characters")
        String photoUrl
) {}
