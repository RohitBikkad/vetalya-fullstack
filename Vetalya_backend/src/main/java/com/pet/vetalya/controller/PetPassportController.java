package com.pet.vetalya.controller;

import com.pet.vetalya.dto.*;
import com.pet.vetalya.service.PetPassportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pets")
public class PetPassportController {

    private final PetPassportService passportService;

    public PetPassportController(PetPassportService passportService) {
        this.passportService = passportService;
    }

    /**
     * GET /api/v1/pets/{petId}/passport — Retrieve digital Pet Health Passport overview.
     */
    @GetMapping("/{petId}/passport")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PetPassportResponse> getPassport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId) {
        PetPassportResponse response = passportService.getOrCreatePassport(jwt.getSubject(), petId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/pets/{petId}/passport — Update Pet Health Passport details.
     */
    @PutMapping("/{petId}/passport")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<PetPassportResponse> updatePassport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId,
            @Valid @RequestBody PetPassportUpdateRequest request) {
        PetPassportResponse response = passportService.updatePassport(jwt.getSubject(), petId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/pets/{petId}/passport/timeline — Paginated medical timeline (encounters, vaccines, documents).
     */
    @GetMapping("/{petId}/passport/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PetTimelineItemDto>> getPetTimeline(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId) {
        List<PetTimelineItemDto> timeline = passportService.getPetTimeline(jwt.getSubject(), petId);
        return ResponseEntity.ok(timeline);
    }

    /**
     * GET /api/v1/pets/emergency/{qrHash} — Public unguessable emergency QR lookup for physical pet collar tag.
     */
    @GetMapping("/emergency/{qrHash}")
    public ResponseEntity<Map<String, Object>> getEmergencyProfile(@PathVariable String qrHash) {
        Map<String, Object> profile = passportService.getEmergencyProfileByQr(qrHash);
        return ResponseEntity.ok(profile);
    }

    /**
     * POST /api/v1/pets/{petId}/passport/share-requests — Initiate record sharing.
     */
    @GetMapping("/{petId}/passport/share-requests")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<List<RecordShareRequestDto.Response>> listShareRequests(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String petId) {
        return ResponseEntity.ok(passportService.listShareRequests(jwt.getSubject(), petId));
    }

    @PostMapping("/{petId}/passport/share-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecordShareRequestDto.Response> createShareRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId,
            @Valid @RequestBody RecordShareRequestDto.CreateRequest request) {
        RecordShareRequestDto.CreateRequest enriched = new RecordShareRequestDto.CreateRequest(
                petId,
                request.targetHospitalId(),
                request.scope(),
                request.expiryHours()
        );
        RecordShareRequestDto.Response res = passportService.createShareRequest(jwt.getSubject(), enriched);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * POST /api/v1/pets/share-requests/{requestId}/approve — Approve record sharing request.
     */
    @PostMapping("/share-requests/{requestId}/approve")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<RecordShareRequestDto.Response> approveShare(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String requestId) {
        RecordShareRequestDto.Response res = passportService.approveShareRequest(jwt.getSubject(), requestId);
        return ResponseEntity.ok(res);
    }

    /**
     * POST /api/v1/pets/share-requests/{requestId}/revoke — Revoke record sharing request.
     */
    @PostMapping("/share-requests/{requestId}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecordShareRequestDto.Response> revokeShare(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String requestId) {
        RecordShareRequestDto.Response res = passportService.revokeShareRequest(jwt.getSubject(), requestId);
        return ResponseEntity.ok(res);
    }

    /**
     * POST /api/v1/pets/{petId}/documents — Upload and attach medical document to pet.
     */
    @PostMapping("/{petId}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PetDocumentDto.Response> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId,
            @Valid @RequestBody PetDocumentDto.UploadRequest request) {
        PetDocumentDto.Response res = passportService.uploadDocument(jwt.getSubject(), petId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * GET /api/v1/pets/{petId}/documents — List all documents attached to pet.
     */
    @GetMapping("/{petId}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PetDocumentDto.Response>> listDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId) {
        List<PetDocumentDto.Response> res = passportService.listDocuments(jwt.getSubject(), petId);
        return ResponseEntity.ok(res);
    }
}
