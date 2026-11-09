package com.pet.vetalya.vet.controller;

import com.pet.vetalya.vet.dto.VaccinationRequest;
import com.pet.vetalya.vet.dto.VaccinationResponse;
import com.pet.vetalya.vet.service.VaccinationService;
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

/**
 * Vaccination endpoints for vets: manage vaccinations on pets they have appointments with.
 */
@RestController
@PreAuthorize("hasRole('VET')")
public class VaccinationController {

    private final VaccinationService vaccinationService;

    public VaccinationController(VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    /**
     * POST /api/vet/pets/{petId}/vaccinations — Allowed only if vet has at least one non-cancelled appointment for this pet.
     */
    @PostMapping("/api/vet/pets/{petId}/vaccinations")
    public ResponseEntity<VaccinationResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId,
            @Valid @RequestBody VaccinationRequest request) {
        VaccinationResponse response = vaccinationService.create(jwt.getSubject(), petId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/vet/pets/{petId}/vaccinations — Returns full history for pet, newest first.
     */
    @GetMapping("/api/vet/pets/{petId}/vaccinations")
    public List<VaccinationResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId) {
        return vaccinationService.listForPet(jwt.getSubject(), petId);
    }

    /**
     * PUT /api/vet/vaccinations/{vaccinationId} — Only vet who created record can update.
     */
    @PutMapping("/api/vet/vaccinations/{vaccinationId}")
    public VaccinationResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String vaccinationId,
            @Valid @RequestBody VaccinationRequest request) {
        return vaccinationService.update(jwt.getSubject(), vaccinationId, request);
    }

    /**
     * DELETE /api/vet/vaccinations/{vaccinationId} — Only vet who created record can delete.
     */
    @DeleteMapping("/api/vet/vaccinations/{vaccinationId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String vaccinationId) {
        vaccinationService.delete(jwt.getSubject(), vaccinationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/vet/vaccinations/due?from=&to= — Records I created whose next_vaccination_date falls in range.
     */
    @GetMapping("/api/vet/vaccinations/due")
    public List<VaccinationResponse> findDue(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return vaccinationService.findDue(jwt.getSubject(), from, to);
    }
}
