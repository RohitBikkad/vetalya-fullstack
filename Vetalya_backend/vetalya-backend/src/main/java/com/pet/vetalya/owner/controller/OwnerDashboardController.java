package com.pet.vetalya.owner.controller;

import com.pet.vetalya.owner.dto.OwnerDashboardResponse;
import com.pet.vetalya.owner.service.OwnerDashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/dashboard")
@PreAuthorize("hasRole('PET_OWNER')")
public class OwnerDashboardController {

    private final OwnerDashboardService dashboardService;

    public OwnerDashboardController(OwnerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/owner/dashboard — Owner metrics: pet count, next 3 appointments, pending count, vax due, reviewable count.
     */
    @GetMapping
    public OwnerDashboardResponse getDashboard(@AuthenticationPrincipal Jwt jwt) {
        return dashboardService.getDashboard(jwt.getSubject());
    }
}
