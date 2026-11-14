package com.pet.vetalya.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EncounterAmendmentRequest(
        @NotBlank(message = "Amendment justification reason is mandatory")
        String amendmentReason,

        String chiefComplaint,
        String physicalExamination,
        BigDecimal temperatureF,
        Integer heartRateBpm,
        Integer respiratoryRateBpm,
        BigDecimal bodyWeightKg,
        String assessmentNotes,
        String diagnosisPrimary,
        String differentialDiagnosis,
        String treatmentPlan,
        String dietInstructions,
        LocalDate followUpDate,
        List<EncounterRequest.PrescriptionItemRequest> medications
) {}
