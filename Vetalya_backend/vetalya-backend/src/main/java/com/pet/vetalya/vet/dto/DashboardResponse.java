package com.pet.vetalya.vet.dto;

import java.util.Map;

/**
 * Dashboard response with aggregated counts and ratings.
 */
public record DashboardResponse(
        long todayAppointments,
        long upcoming7Days,
        long pendingRequests,
        double averageRating,
        long reviewCount,
        Map<String, Long> statusCounts
) {}
