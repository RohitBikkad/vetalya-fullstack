package com.pet.vetalya.owner.dto;

import jakarta.validation.constraints.Size;

public record AppointmentCancelRequest(
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {}
