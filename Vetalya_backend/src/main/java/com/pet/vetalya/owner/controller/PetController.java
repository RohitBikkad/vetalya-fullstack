package com.pet.vetalya.owner.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.owner.dto.PetDetailResponse;
import com.pet.vetalya.owner.dto.PetRequest;
import com.pet.vetalya.owner.dto.PetResponse;
import com.pet.vetalya.owner.service.PetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner/pets")
@PreAuthorize("hasRole('PET_OWNER')")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    /**
     * POST /api/owner/pets — Register a new pet.
     */
    @PostMapping
    public ResponseEntity<PetResponse> createPet(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PetRequest request) {
        PetResponse response = petService.createPet(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/owner/pets — Paginated list of my active pets.
     */
    @GetMapping
    public PageResponse<PetResponse> listPets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return petService.listPets(jwt.getSubject(), page, size, sort, direction);
    }

    /**
     * GET /api/owner/pets/{petId} — Pet details with next vaccination and appointment.
     */
    @GetMapping("/{petId}")
    public PetDetailResponse getPet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId) {
        return petService.getPet(jwt.getSubject(), petId);
    }

    /**
     * PUT /api/owner/pets/{petId} — Update pet details.
     */
    @PutMapping("/{petId}")
    public PetResponse updatePet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId,
            @Valid @RequestBody PetRequest request) {
        return petService.updatePet(jwt.getSubject(), petId, request);
    }

    /**
     * DELETE /api/owner/pets/{petId} — Soft delete pet.
     */
    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> deletePet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String petId) {
        petService.deletePet(jwt.getSubject(), petId);
        return ResponseEntity.noContent().build();
    }
}
