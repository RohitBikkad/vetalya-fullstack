package com.pet.vetalya.dto;

import java.time.Instant;

public record PetTimelineItemDto(
        String id,
        String eventType, // CONSULTATION, VACCINATION, DIAGNOSTIC, PRESCRIPTION, DOCUMENT_UPLOAD
        String title,
        String subtitle,
        String description,
        String clinicOrDoctor,
        String verificationStatus, // VERIFIED_BY_VET, OWNER_UPLOADED, PENDING
        String downloadUrl,
        Instant eventTimestamp
) {}

