package com.pet.vetalya.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotResponse(
        String slotId,
        String hospitalId,
        String vetUserId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        String slotType,
        int capacity,
        int bookedCount,
        String status
) {}

