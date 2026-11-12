package com.pet.vetalya.owner.controller;

import com.pet.vetalya.owner.dto.OwnerVaccinationResponse;
import com.pet.vetalya.owner.service.OwnerVaccinationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('PET_OWNER')")
public class OwnerVaccinationController {

    private final OwnerVaccinationService vaccinationService;

    public OwnerVaccinationController(OwnerVaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    /**
     * GET /api/owner/pets/{petId}/vaccinations — Full vaccination history for my pet, newest first.
     */
    @GetMapping("/api/owner/pets/{petId}/vaccinations")
    public List<OwnerVaccinationResponse> getPetVaccinations(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId) {
        return vaccinationService.getPetVaccinations(jwt.getSubject(), petId);
    }

    /**
     * GET /api/owner/vaccinations/due?days=30 — Due vaccinations across all my pets within N days.
     */
    @GetMapping("/api/owner/vaccinations/due")
    public List<OwnerVaccinationResponse> getDueVaccinations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "30") Integer days) {
        return vaccinationService.getDueVaccinations(jwt.getSubject(), days);
    }
}
