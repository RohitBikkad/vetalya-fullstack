package com.pet.vetalya.dto;

import java.time.Instant;
import java.util.List;

public record DiagnosticOrderResponse(
        String orderId,
        String orderNumber,
        String petId,
        String vetUserId,
        String hospitalId,
        String encounterId,
        String status,
        String priority,
        String clinicalNotes,
        List<DiagnosticOrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}

