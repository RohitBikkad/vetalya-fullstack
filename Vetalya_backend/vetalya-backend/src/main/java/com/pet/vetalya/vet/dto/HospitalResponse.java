package com.pet.vetalya.vet.dto;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Response DTO for a hospital.
 */
public record HospitalResponse(
        String hospitalId,
        String name,
        String address1,
        String address2,
        String city,
        String state,
        String pincode,
        String country,
        String mobile,
        String email,
        String gstNumber,
        boolean is24x7,
        LocalTime openTime,
        LocalTime closeTime,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
