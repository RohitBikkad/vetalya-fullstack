package com.pet.vetalya.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DiagnosticOrderRequest(
        @NotBlank(message = "petId is required")
        String petId,

        String hospitalId,

        String encounterId,

        String priority,

        String clinicalNotes,

        @NotEmpty(message = "At least one diagnostic item is required")
        List<DiagnosticItemRequest> items
) {}

