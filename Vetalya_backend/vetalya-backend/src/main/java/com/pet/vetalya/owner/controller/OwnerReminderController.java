package com.pet.vetalya.owner.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.owner.dto.OwnerReminderResponse;
import com.pet.vetalya.owner.service.OwnerReminderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/reminders")
@PreAuthorize("hasRole('PET_OWNER')")
public class OwnerReminderController {

    private final OwnerReminderService reminderService;

    public OwnerReminderController(OwnerReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * GET /api/owner/reminders — Paginated upcoming reminders.
     */
    @GetMapping
    public PageResponse<OwnerReminderResponse> listUpcoming(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reminderService.listUpcoming(jwt.getSubject(), page, size);
    }
}
