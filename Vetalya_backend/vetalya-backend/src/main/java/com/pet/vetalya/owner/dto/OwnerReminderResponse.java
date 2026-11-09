package com.pet.vetalya.owner.dto;

import java.time.Instant;

public record OwnerReminderResponse(
        String reminderId,
        String appointmentId,
        Instant remindAt,
        String message,
        String petId,
        Instant createdAt
) {}
