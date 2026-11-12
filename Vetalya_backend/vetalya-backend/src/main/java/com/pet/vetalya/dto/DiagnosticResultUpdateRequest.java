package com.pet.vetalya.dto;

import jakarta.validation.constraints.NotBlank;

public record DiagnosticResultUpdateRequest(
        @NotBlank(message = "resultValue is required")
        String resultValue,

        String referenceRange,

        String unit,

        String flag,

        String interpretation,

        String reportFileUrl,

        String verifiedBy
) {}

