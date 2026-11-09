package com.pet.vetalya.owner.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AppointmentRescheduleRequest(
        @NotNull(message = "New appointment date and time is required")
        @Future(message = "Appointment time must be in the future")
        Instant appointmentAt
) {}
