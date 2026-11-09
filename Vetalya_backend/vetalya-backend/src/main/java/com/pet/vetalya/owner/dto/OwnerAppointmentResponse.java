package com.pet.vetalya.owner.dto;

import java.time.Instant;
import java.time.LocalDate;

public record OwnerAppointmentResponse(
        String appointmentId,
        Instant appointmentAt,
        String status,
        String reason,
        String notes,
        String visitNotes,
        int version,
        Instant createdAt,
        Instant updatedAt,
        VetSummary vet,
        HospitalSummary hospital,
        PetSummary pet
) {
    public record VetSummary(
            String userId,
            String firstName,
            String lastName,
            String photoUrl
    ) {}

    public record HospitalSummary(
            String hospitalId,
            String name,
            String address1,
            String address2,
            String city,
            String state,
            String pincode,
            String mobile
    ) {}

    public record PetSummary(
            String petId,
            String name,
            String species,
            String breed,
            String gender,
            LocalDate dob,
            String photoUrl
    ) {}
}
