package com.pet.vetalya.owner.dto;

import java.time.Instant;

public record OwnerProfileResponse(
        String userId,
        String firstName,
        String lastName,
        String address1,
        String address2,
        String pincode,
        String city,
        String state,
        String country,
        boolean profileComplete,
        String email,
        boolean emailVerified,
        String phone,
        boolean phoneVerified,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
