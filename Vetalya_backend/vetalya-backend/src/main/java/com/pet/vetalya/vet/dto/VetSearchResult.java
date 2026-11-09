package com.pet.vetalya.vet.dto;

import java.util.List;

/**
 * Public vet search result DTO (no sensitive data).
 */
public record VetSearchResult(
        String vetUserId,
        String firstName,
        String lastName,
        String photoUrl,
        double averageRating,
        long reviewCount,
        List<HospitalSummary> hospitals
) {
    public record HospitalSummary(
            String hospitalId,
            String name,
            String address1,
            String address2,
            String city,
            String state,
            String pincode,
            String country,
            String mobile,
            boolean is24x7,
            java.time.LocalTime openTime,
            java.time.LocalTime closeTime
    ) {}
}
