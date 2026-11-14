package com.pet.vetalya.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record GenerateSlotsRequest(
        String hospitalId,
        LocalDate fromDate,
        LocalDate toDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        String slotType,
        Integer capacity
) {}
