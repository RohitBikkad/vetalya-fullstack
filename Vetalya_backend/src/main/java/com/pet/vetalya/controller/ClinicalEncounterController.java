package com.pet.vetalya.controller;

import com.pet.vetalya.dto.EncounterAmendmentRequest;
import com.pet.vetalya.dto.EncounterRequest;
import com.pet.vetalya.dto.EncounterResponse;
import com.pet.vetalya.service.ClinicalEncounterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/encounters")
public class ClinicalEncounterController {

    private final ClinicalEncounterService encounterService;
    private final com.pet.vetalya.clinic.ClinicalAccess access;

    public ClinicalEncounterController(ClinicalEncounterService encounterService, com.pet.vetalya.clinic.ClinicalAccess access) {
        this.access=access;
        this.encounterService = encounterService;
    }

    /**
     * POST /api/v1/encounters — Start or draft a new clinical encounter.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<EncounterResponse> createEncounter(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EncounterRequest request) {
        EncounterResponse response = encounterService.createEncounter(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/encounters/{encounterId} — Get full encounter details.
     */
    @GetMapping("/{encounterId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EncounterResponse> getEncounter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String encounterId) {
        access.encounterRead(jwt.getSubject(),encounterId);
        EncounterResponse response = encounterService.getEncounter(encounterId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/encounters/{encounterId} — Update draft encounter before signing.
     */
    @PutMapping("/{encounterId}")
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<EncounterResponse> updateDraftEncounter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String encounterId,
            @Valid @RequestBody EncounterRequest request) {
        EncounterResponse response = encounterService.updateDraftEncounter(jwt.getSubject(), encounterId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/encounters/{encounterId}/sign — Cryptographically sign and lock encounter.
     */
    @PostMapping("/{encounterId}/sign")
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<EncounterResponse> signEncounter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String encounterId) {
        EncounterResponse response = encounterService.signEncounter(jwt.getSubject(), encounterId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/encounters/{encounterId}/amend — Create auditable amendment to signed record.
     */
    @PostMapping("/{encounterId}/amend")
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<EncounterResponse> amendEncounter(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String encounterId,
            @Valid @RequestBody EncounterAmendmentRequest request) {
        EncounterResponse response = encounterService.amendEncounter(jwt.getSubject(), encounterId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/encounters/pet/{petId} — List all encounters for a pet.
     */
    @GetMapping("/pet/{petId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EncounterResponse>> listPetEncounters(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId) {
        List<EncounterResponse> list = encounterService.listPetEncounters(petId).stream().filter(e -> { try { access.encounterRead(jwt.getSubject(),e.encounterId()); return true; } catch (org.springframework.security.access.AccessDeniedException ex) { return false; } }).toList();
        return ResponseEntity.ok(list);
    }
}

