package com.pet.vetalya.controller;

import com.pet.vetalya.dto.DiagnosticOrderRequest;
import com.pet.vetalya.dto.DiagnosticOrderResponse;
import com.pet.vetalya.dto.DiagnosticResultUpdateRequest;
import com.pet.vetalya.entity.DiagnosticTestCatalog;
import com.pet.vetalya.service.DiagnosticService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diagnostics")
public class DiagnosticController {

    private final DiagnosticService diagnosticService;

    public DiagnosticController(DiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    /**
     * POST /api/v1/diagnostics/orders — Order diagnostic tests.
     */
    @PostMapping("/orders")
    @PreAuthorize("hasAnyAuthority('ROLE_VET', 'ROLE_CLINIC_ADMIN')")
    public ResponseEntity<DiagnosticOrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DiagnosticOrderRequest request) {
        DiagnosticOrderResponse response = diagnosticService.createOrder(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/diagnostics/orders/{orderId} — Get diagnostic order by ID.
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DiagnosticOrderResponse> getOrderById(@PathVariable String orderId) {
        DiagnosticOrderResponse response = diagnosticService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/diagnostics/pet/{petId} — Get diagnostic history for a pet.
     */
    @GetMapping("/pet/{petId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiagnosticOrderResponse>> getOrdersForPet(@PathVariable String petId) {
        List<DiagnosticOrderResponse> response = diagnosticService.getOrdersForPet(petId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/diagnostics/items/{itemId}/results — Enter and verify diagnostic result.
     */
    @PutMapping("/items/{itemId}/results")
    @PreAuthorize("hasAnyAuthority('ROLE_VET', 'ROLE_CLINIC_ADMIN', 'ROLE_LAB_TECHNICIAN')")
    public ResponseEntity<DiagnosticOrderResponse> updateItemResult(
            @PathVariable String itemId,
            @Valid @RequestBody DiagnosticResultUpdateRequest request) {
        DiagnosticOrderResponse response = diagnosticService.updateItemResult(itemId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/diagnostics/catalog — Retrieve diagnostic test catalog.
     */
    @GetMapping("/catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiagnosticTestCatalog>> getCatalog(
            @RequestParam(required = false) String category) {
        List<DiagnosticTestCatalog> catalog = diagnosticService.getCatalog(category);
        return ResponseEntity.ok(catalog);
    }
}

