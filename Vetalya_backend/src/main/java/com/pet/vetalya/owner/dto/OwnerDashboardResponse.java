package com.pet.vetalya.owner.dto;

import java.util.List;

public record OwnerDashboardResponse(
        long petCount,
        List<OwnerAppointmentResponse> upcomingAppointments,
        long pendingAppointmentsCount,
        long vaccinationsDueCount,
        long reviewableAppointmentsCount
) {}
