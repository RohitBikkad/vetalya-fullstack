package com.pet.vetalya.vet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding / updating visit notes on an appointment.
 */
public record VisitNotesRequest(
        @NotBlank(message = "Visit notes cannot be blank")
        @Size(max = 5000, message = "Visit notes must be at most 5000 characters")
        String visitNotes
) {}
