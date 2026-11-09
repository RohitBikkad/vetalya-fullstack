package com.pet.vetalya.vet.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.vet.dto.ReminderResponse;
import com.pet.vetalya.vet.service.ReminderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only reminder endpoints for vets.
 */
@RestController
@RequestMapping("/api/vet/reminders")
@PreAuthorize("hasRole('VET')")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * GET /api/vet/reminders — Upcoming reminders (remind_at >= now), paginated.
     */
    @GetMapping
    public PageResponse<ReminderResponse> listUpcoming(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reminderService.listUpcoming(jwt.getSubject(), page, size);
    }
}
