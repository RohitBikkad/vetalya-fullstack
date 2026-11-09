package com.pet.vetalya.vet.controller;

import com.pet.vetalya.vet.dto.DashboardResponse;
import com.pet.vetalya.vet.service.AppointmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vet dashboard endpoint: GET /api/vet/dashboard.
 */
@RestController
@RequestMapping("/api/vet/dashboard")
@PreAuthorize("hasRole('VET')")
public class DashboardController {

    private final AppointmentService appointmentService;

    public DashboardController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public DashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt) {
        return appointmentService.getDashboard(jwt.getSubject());
    }
}
