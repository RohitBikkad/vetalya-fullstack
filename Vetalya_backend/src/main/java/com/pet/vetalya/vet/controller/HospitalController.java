package com.pet.vetalya.vet.controller;

import com.pet.vetalya.vet.dto.HospitalRequest;
import com.pet.vetalya.vet.dto.HospitalResponse;
import com.pet.vetalya.vet.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Hospital CRUD endpoints for vets.
 */
@RestController
@RequestMapping("/api/vet/hospitals")
@PreAuthorize("hasRole('VET')")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    public ResponseEntity<HospitalResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HospitalRequest request) {
        HospitalResponse response = hospitalService.create(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<HospitalResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return hospitalService.listOwn(jwt.getSubject());
    }

    @GetMapping("/{hospitalId}")
    public HospitalResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String hospitalId) {
        return hospitalService.getOwn(jwt.getSubject(), hospitalId);
    }

    @PutMapping("/{hospitalId}")
    public HospitalResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String hospitalId,
            @Valid @RequestBody HospitalRequest request) {
        return hospitalService.update(jwt.getSubject(), hospitalId, request);
    }

    @DeleteMapping("/{hospitalId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String hospitalId) {
        hospitalService.delete(jwt.getSubject(), hospitalId);
        return ResponseEntity.noContent().build();
    }
}
