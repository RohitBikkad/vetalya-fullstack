package com.pet.vetalya.owner.controller;

import com.pet.vetalya.owner.dto.SlotResponse;
import com.pet.vetalya.owner.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vets")
@PreAuthorize("isAuthenticated()")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * GET /api/vets/{vetUserId}/availability?date=&hospitalId= — Free slots for vet on given date.
     */
    @GetMapping("/{vetUserId}/availability")
    public List<SlotResponse> getAvailability(
            @PathVariable String vetUserId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String hospitalId) {
        return availabilityService.getFreeSlots(vetUserId, date, hospitalId);
    }
}
