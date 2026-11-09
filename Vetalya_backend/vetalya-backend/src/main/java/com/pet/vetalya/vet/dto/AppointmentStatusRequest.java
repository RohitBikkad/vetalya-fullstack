package com.pet.vetalya.vet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for patching appointment status.
 */
public record AppointmentStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "^(CONFIRMED|COMPLETED|CANCELLED|NO_SHOW)$",
                 message = "Status must be one of: CONFIRMED, COMPLETED, CANCELLED, NO_SHOW")
        String status,

        String notes
) {}
