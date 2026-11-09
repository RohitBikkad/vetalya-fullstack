package com.pet.vetalya.vet.dto;

import java.time.Instant;

/**
 * Response DTO for a reminder (read-only for vet).
 */
public record ReminderResponse(
        String reminderId,
        String appointmentId,
        Instant remindAt,
        String message,
        boolean sent,
        Instant createdAt
) {}
