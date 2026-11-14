package com.pet.vetalya.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EncounterResponse(
        String encounterId,
        String petId,
        String petName,
        String vetUserId,
        String vetName,
        String hospitalId,
        String hospitalName,
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
        String diagnosisPrimary,
        String differentialDiagnosis,
        String treatmentPlan,
        String dietInstructions,
        LocalDate followUpDate,
        boolean isSigned,
        Instant signedAt,
        String digitalSignatureHash,
        boolean isPassportPublished,
        PrescriptionResponse prescription,
        List<AmendmentResponse> amendments,
        Instant createdAt,
        Instant updatedAt
) {
    public record PrescriptionResponse(
            String prescriptionId,
            LocalDate rxDate,
            String instructions,
            String pdfStorageUrl,
            boolean isDispensed,
            List<PrescriptionItemDto> items
    ) {}

    public record PrescriptionItemDto(
            Long itemId,
            String medicineName,
            String genericComposition,
            String dosage,
            String frequency,
            String duration,
            String instructions,
            boolean isScheduleH,
            Integer quantity
    ) {}

    public record AmendmentResponse(
            String amendmentId,
            String amendedByVetId,
            String amendmentReason,
            Instant createdAt
    ) {}
}
