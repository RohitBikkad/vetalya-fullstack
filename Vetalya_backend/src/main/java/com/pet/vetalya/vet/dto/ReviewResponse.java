package com.pet.vetalya.vet.dto;

import java.time.Instant;

/**
 * Response DTO for a review (read-only for vet).
 */
public record ReviewResponse(
        String reviewId,
        String hospitalId,
        int rating,
        String comment,
        String ownerFirstName,
        String ownerLastName,
        Instant createdAt
) {}
