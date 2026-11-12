package com.pet.vetalya.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EncounterRequest(
        @NotBlank(message = "Pet ID is required")
        String petId,

        String hospitalId,

        String appointmentId,

        String encounterType,

        String chiefComplaint,

        String presentingHistory,

        String physicalExamination,

        BigDecimal temperatureF,

        Integer heartRateBpm,

        Integer respiratoryRateBpm,

        BigDecimal bodyWeightKg,

        Integer bodyConditionScore,

        String assessmentNotes,

        @NotBlank(message = "Primary diagnosis is required")
        String diagnosisPrimary,

        String differentialDiagnosis,

        String treatmentPlan,

        String dietInstructions,

        LocalDate followUpDate,

        List<PrescriptionItemRequest> medications
) {
    public record PrescriptionItemRequest(
            String medicineName,
            String genericComposition,
            String dosage,
            String frequency,
            String duration,
            String instructions,
            boolean isScheduleH,
            Integer quantity
    ) {}
}

