package com.pet.vetalya.owner.dto;

import java.time.Instant;

public record SlotResponse(
        Instant slotStart,
        Instant slotEnd,
        boolean available
) {}
