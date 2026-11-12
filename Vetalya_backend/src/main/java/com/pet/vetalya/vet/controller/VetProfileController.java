package com.pet.vetalya.vet.controller;

import com.pet.vetalya.vet.dto.VetProfileRequest;
import com.pet.vetalya.vet.dto.VetProfileResponse;
import com.pet.vetalya.vet.service.VetProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Vet profile and account management endpoints.
 */
@RestController
@RequestMapping("/api/vet")
@PreAuthorize("hasRole('VET')")
public class VetProfileController {

    private final VetProfileService vetProfileService;

    public VetProfileController(VetProfileService vetProfileService) {
        this.vetProfileService = vetProfileService;
    }

    /**
     * POST /api/vet/profile — Create/fill vet profile (first time after OAuth login).
     */
    @PostMapping("/profile")
    public ResponseEntity<VetProfileResponse> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VetProfileRequest request) {
        VetProfileResponse response = vetProfileService.createProfile(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/vet/profile — Get vet profile with auth details and flags.
     */
    @GetMapping("/profile")
    public VetProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return vetProfileService.getProfile(jwt.getSubject());
    }

    /**
     * PUT /api/vet/profile — Update vet profile.
     * Registration number is only changeable if is_verified = false.
     */
    @PutMapping("/profile")
    public VetProfileResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VetProfileRequest request) {
        return vetProfileService.updateProfile(jwt.getSubject(), request);
    }

    /**
     * DELETE /api/vet/account — Soft delete vet account.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        vetProfileService.deleteAccount(jwt.getSubject());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}
