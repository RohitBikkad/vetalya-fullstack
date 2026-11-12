package com.pet.vetalya.owner.dto;

import java.time.Instant;

public record OwnerReviewResponse(
        String reviewId,
        String appointmentId,
        String vetUserId,
        String vetName,
        String hospitalId,
        String hospitalName,
        int rating,
        String title,
        String comment,
        boolean canEdit,
        Instant createdAt,
        Instant updatedAt
) {}
