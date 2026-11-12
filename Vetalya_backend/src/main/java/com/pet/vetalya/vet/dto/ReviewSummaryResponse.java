package com.pet.vetalya.vet.dto;

import java.util.Map;

/**
 * Review summary with counts per star rating and overall average.
 */
public record ReviewSummaryResponse(
        Map<Integer, Long> starCounts,
        long totalReviews,
        double averageRating
) {}
