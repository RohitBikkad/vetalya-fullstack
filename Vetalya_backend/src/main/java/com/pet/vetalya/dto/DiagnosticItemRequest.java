package com.pet.vetalya.dto;

import jakarta.validation.constraints.NotBlank;

public record DiagnosticItemRequest(
        @NotBlank(message = "testCode is required")
        String testCode,

        @NotBlank(message = "testName is required")
        String testName,

        @NotBlank(message = "category is required")
        String category
) {}
