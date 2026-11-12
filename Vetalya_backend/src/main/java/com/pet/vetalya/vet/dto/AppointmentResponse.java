package com.pet.vetalya.vet.dto;

import java.time.Instant;

/**
 * Response DTO for an appointment (vet side), including pet and owner summaries.
 */
public record AppointmentResponse(
        String appointmentId,
        String hospitalId,
        Instant appointmentAt,
        String status,
        String reason,
        String visitNotes,
        int version,
        PetSummary pet,
        OwnerSummary owner,
        Instant createdAt,
        Instant updatedAt
) {
    public record PetSummary(
            String petId,
            String name,
            String species,
            String breed,
            String photoUrl
    ) {}

    public record OwnerSummary(
            String ownerUserId,
            String firstName,
            String lastName
    ) {}
}
