package com.pet.vetalya.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class RecordShareRequestDto {

    public record CreateRequest(
            @NotBlank(message = "Pet ID is required")
            String petId,

            String targetHospitalId,

            @Pattern(regexp = "^(FULL_TIMELINE|VACCINES_ONLY|EMERGENCY_SUMMARY)$",
                     message = "Scope must be FULL_TIMELINE, VACCINES_ONLY, or EMERGENCY_SUMMARY")
            String scope,

            Integer expiryHours
    ) {}

    public record Response(
            String requestId,
            String petId,
            String petName,
            String requesterUserId,
            String targetHospitalId,
            String scope,
            String status,
            Instant expiresAt,
            Instant approvedAt,
            Instant revokedAt,
            Instant createdAt
    ) {}
}
