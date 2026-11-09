package com.pet.vetalya.vet.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for vet profile (includes read-only auth details and flags).
 */
public record VetProfileResponse(
        String userId,
        String firstName,
        String lastName,
        String registrationNo,
        LocalDate dob,
        String photoUrl,
        boolean verified,
        boolean profileComplete,
        // Read-only from user_auth
        String email,
        boolean emailVerified,
        String phone,
        boolean phoneVerified,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
