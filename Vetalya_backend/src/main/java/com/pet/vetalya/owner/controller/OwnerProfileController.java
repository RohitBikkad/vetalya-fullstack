package com.pet.vetalya.owner.controller;

import com.pet.vetalya.owner.dto.OwnerProfileRequest;
import com.pet.vetalya.owner.dto.OwnerProfileResponse;
import com.pet.vetalya.owner.service.OwnerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('PET_OWNER')")
public class OwnerProfileController {

    private final OwnerProfileService ownerProfileService;

    public OwnerProfileController(OwnerProfileService ownerProfileService) {
        this.ownerProfileService = ownerProfileService;
    }

    /**
     * POST /api/owner/profile — Create or fill profile (first time after OAuth signup).
     */
    @PostMapping("/profile")
    public ResponseEntity<OwnerProfileResponse> createOrFillProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OwnerProfileRequest request) {
        OwnerProfileResponse response = ownerProfileService.createOrFillProfile(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/owner/profile — Get my profile + completion flag + email/phone.
     */
    @GetMapping("/profile")
    public OwnerProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ownerProfileService.getProfile(jwt.getSubject());
    }

    /**
     * PUT /api/owner/profile — Update my profile.
     */
    @PutMapping("/profile")
    public OwnerProfileResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OwnerProfileRequest request) {
        return ownerProfileService.updateProfile(jwt.getSubject(), request);
    }

    /**
     * DELETE /api/owner/account — Soft delete my account.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        ownerProfileService.deleteAccount(jwt.getSubject());
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }
}
