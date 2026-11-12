package com.pet.vetalya.owner.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.owner.dto.AppointmentBookingRequest;
import com.pet.vetalya.owner.dto.AppointmentCancelRequest;
import com.pet.vetalya.owner.dto.AppointmentRescheduleRequest;
import com.pet.vetalya.owner.dto.OwnerAppointmentResponse;
import com.pet.vetalya.owner.service.OwnerAppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner/appointments")
@PreAuthorize("hasRole('PET_OWNER')")
public class OwnerAppointmentController {

    private final OwnerAppointmentService appointmentService;

    public OwnerAppointmentController(OwnerAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * POST /api/owner/appointments — Book a new appointment.
     */
    @PostMapping
    public ResponseEntity<OwnerAppointmentResponse> bookAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AppointmentBookingRequest request) {
        OwnerAppointmentResponse response = appointmentService.bookAppointment(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/owner/appointments — List my appointments with optional filters.
     */
    @GetMapping
    public PageResponse<OwnerAppointmentResponse> listAppointments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appointmentAt") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return appointmentService.listAppointments(
                jwt.getSubject(), status, petId, from, to, page, size, sort, direction);
    }

    /**
     * GET /api/owner/appointments/{id} — Appointment details.
     */
    @GetMapping("/{id}")
    public OwnerAppointmentResponse getAppointment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        return appointmentService.getAppointment(jwt.getSubject(), id);
    }

    /**
     * PUT /api/owner/appointments/{id}/reschedule — Reschedule appointment.
     */
    @PutMapping("/{id}/reschedule")
    public OwnerAppointmentResponse reschedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody AppointmentRescheduleRequest request) {
        return appointmentService.reschedule(jwt.getSubject(), id, request);
    }

    /**
     * PATCH /api/owner/appointments/{id}/cancel — Cancel appointment.
     */
    @PatchMapping("/{id}/cancel")
    public OwnerAppointmentResponse cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody(required = false) AppointmentCancelRequest request) {
        return appointmentService.cancel(jwt.getSubject(), id, request);
    }
}
