package com.pet.vetalya.owner.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AppointmentBookingRequest(
        @NotBlank(message = "Vet user ID is required")
        @Size(max = 26, message = "Vet user ID must be at most 26 characters")
        String vetUserId,

        @Size(max = 26, message = "Hospital ID must be at most 26 characters")
        String hospitalId,

        @NotBlank(message = "Pet ID is required")
        @Size(max = 26, message = "Pet ID must be at most 26 characters")
        String petId,

        @NotNull(message = "Appointment date and time is required")
        @Future(message = "Appointment time must be in the future")
        Instant appointmentAt,

        @Size(max = 1000, message = "Notes must be at most 1000 characters")
        String notes
) {}
