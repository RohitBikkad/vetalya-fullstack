package com.pet.vetalya.dto;

import java.time.Instant;

public record DiagnosticOrderItemResponse(
        String itemId,
        String testCode,
        String testName,
        String category,
        String status,
        String resultValue,
        String referenceRange,
        String unit,
        String flag,
        String reportFileUrl,
        String interpretation,
        String verifiedBy,
        Instant verifiedAt
) {}

