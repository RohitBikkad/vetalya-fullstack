package com.pet.vetalya.vet.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.vet.dto.*;
import com.pet.vetalya.vet.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Appointment endpoints for vet side: list, get, status transitions, visit notes, dashboard.
 */
@RestController
@RequestMapping("/api/vet/appointments")
@PreAuthorize("hasRole('VET')")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * GET /api/vet/appointments — List appointments with optional filters and pagination.
     */
    @GetMapping
    public PageResponse<AppointmentResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appointmentAt") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return appointmentService.listAppointments(
                jwt.getSubject(), status, hospitalId, from, to, page, size, sort, direction);
    }

    /**
     * GET /api/vet/appointments/{appointmentId} — Get appointment details.
     */
    @GetMapping("/{appointmentId}")
    public AppointmentResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String appointmentId) {
        return appointmentService.getAppointment(jwt.getSubject(), appointmentId);
    }

    /**
     * PATCH /api/vet/appointments/{appointmentId}/status — Update appointment status.
     */
    @PatchMapping("/{appointmentId}/status")
    public AppointmentResponse updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentStatusRequest request) {
        return appointmentService.updateStatus(jwt.getSubject(), appointmentId, request.status(), request.notes());
    }

    /**
     * PUT /api/vet/appointments/{appointmentId}/notes — Update visit notes.
     */
    @PutMapping("/{appointmentId}/notes")
    public AppointmentResponse updateVisitNotes(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String appointmentId,
            @Valid @RequestBody VisitNotesRequest request) {
        return appointmentService.updateVisitNotes(jwt.getSubject(), appointmentId, request.visitNotes());
    }

    /**
     * GET /api/vet/appointments/dashboard — Dashboard with aggregated counts.
     */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt) {
        return appointmentService.getDashboard(jwt.getSubject());
    }
}
