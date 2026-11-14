package com.pet.vetalya.dto;

import jakarta.validation.constraints.NotBlank;

public record BookSlotRequest(
        @NotBlank(message = "slotId is required")
        String slotId,

        @NotBlank(message = "petId is required")
        String petId,

        String reason,

        String appointmentType
) {}
