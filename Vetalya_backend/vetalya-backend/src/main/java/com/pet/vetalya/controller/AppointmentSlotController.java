package com.pet.vetalya.controller;

import com.pet.vetalya.dto.BookSlotRequest;
import com.pet.vetalya.dto.GenerateSlotsRequest;
import com.pet.vetalya.dto.SlotResponse;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.service.AppointmentSlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/slots")
public class AppointmentSlotController {

    private final AppointmentSlotService slotService;

    public AppointmentSlotController(AppointmentSlotService slotService) {
        this.slotService = slotService;
    }

    /**
     * POST /api/v1/slots/generate — Generate bulk slots for a veterinarian schedule.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('ROLE_VET', 'ROLE_CLINIC_ADMIN')")
    public ResponseEntity<List<SlotResponse>> generateSlots(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody GenerateSlotsRequest request) {
        List<SlotResponse> slots = slotService.generateSlots(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(slots);
    }

    /**
     * GET /api/v1/slots/available — Retrieve available slots for booking.
     */
    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SlotResponse>> getAvailableSlots(
            @RequestParam String vetUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<SlotResponse> slots = slotService.getAvailableSlots(vetUserId, fromDate, toDate);
        return ResponseEntity.ok(slots);
    }

    /**
     * POST /api/v1/slots/book — Pessimistically locked atomic booking.
     */
    @PostMapping("/book")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Appointment> bookSlot(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookSlotRequest request) {
        Appointment booked = slotService.bookSlotAtomically(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booked);
    }

    /**
     * POST /api/v1/slots/appointments/{appointmentId}/check-in — Check in patient for queue token.
     */
    @PostMapping("/appointments/{appointmentId}/check-in")
    @PreAuthorize("hasAnyAuthority('ROLE_VET', 'ROLE_CLINIC_ADMIN', 'ROLE_RECEPTIONIST')")
    public ResponseEntity<Void> checkIn(@PathVariable String appointmentId) {
        slotService.checkInAppointment(appointmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/slots/appointments/{appointmentId}/start-consultation — Mark consultation started.
     */
    @PostMapping("/appointments/{appointmentId}/start-consultation")
    @PreAuthorize("hasAnyAuthority('ROLE_VET', 'ROLE_CLINIC_ADMIN')")
    public ResponseEntity<Void> startConsultation(@PathVariable String appointmentId) {
        slotService.startConsultation(appointmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/slots/appointments/{appointmentId}/end-consultation — Mark consultation completed.
     */
    @PostMapping("/appointments/{appointmentId}/end-consultation")
    @PreAuthorize("hasAnyAuthority('ROLE_VET', 'ROLE_CLINIC_ADMIN')")
    public ResponseEntity<Void> endConsultation(@PathVariable String appointmentId) {
        slotService.endConsultation(appointmentId);
        return ResponseEntity.noContent().build();
    }
}

