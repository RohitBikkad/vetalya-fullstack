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
    private final com.pet.vetalya.clinic.ClinicalAccess access;
    private final com.pet.vetalya.clinic.ClinicWorkspaceService clinics;

    public AppointmentSlotController(AppointmentSlotService slotService, com.pet.vetalya.clinic.ClinicalAccess access, com.pet.vetalya.clinic.ClinicWorkspaceService clinics) {
        this.slotService = slotService;
        this.access=access;
        this.clinics=clinics;
    }

    /**
     * POST /api/v1/slots/generate — Generate bulk slots for a veterinarian schedule.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<List<SlotResponse>> generateSlots(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody GenerateSlotsRequest request) {
        if (request.hospitalId() != null) clinics.requireAccess(jwt.getSubject(),request.hospitalId());
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
    public ResponseEntity<BookingReceipt> bookSlot(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookSlotRequest request) {
        Appointment booked = slotService.bookSlotAtomically(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BookingReceipt(
                booked.getAppointmentId(), booked.getPetId(), booked.getHospitalId(), booked.getSlotId(),
                booked.getAppointmentAt(), booked.getStatus(), booked.getQueueNumber()));
    }

    public record BookingReceipt(String appointmentId, String petId, String hospitalId, String slotId,
                                 java.time.Instant appointmentAt, String status, Integer queueNumber) {}

    /**
     * POST /api/v1/slots/appointments/{appointmentId}/check-in — Check in patient for queue token.
     */
    @PostMapping("/appointments/{appointmentId}/check-in")
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<Void> checkIn(@AuthenticationPrincipal Jwt jwt, @PathVariable String appointmentId) {
        access.appointmentVet(jwt.getSubject(),appointmentId);
        slotService.checkInAppointment(appointmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/slots/appointments/{appointmentId}/start-consultation — Mark consultation started.
     */
    @PostMapping("/appointments/{appointmentId}/start-consultation")
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<Void> startConsultation(@AuthenticationPrincipal Jwt jwt, @PathVariable String appointmentId) {
        access.appointmentVet(jwt.getSubject(),appointmentId);
        slotService.startConsultation(appointmentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/slots/appointments/{appointmentId}/end-consultation — Mark consultation completed.
     */
    @PostMapping("/appointments/{appointmentId}/end-consultation")
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<Void> endConsultation(@AuthenticationPrincipal Jwt jwt, @PathVariable String appointmentId) {
        access.appointmentVet(jwt.getSubject(),appointmentId);
        slotService.endConsultation(appointmentId);
        return ResponseEntity.noContent().build();
    }
}
