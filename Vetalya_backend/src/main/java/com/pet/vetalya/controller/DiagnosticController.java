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
    private final com.pet.vetalya.clinic.ClinicalAccess access;

    public DiagnosticController(DiagnosticService diagnosticService, com.pet.vetalya.clinic.ClinicalAccess access) {
        this.access=access;
        this.diagnosticService = diagnosticService;
    }

    /**
     * POST /api/v1/diagnostics/orders — Order diagnostic tests.
     */
    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('ROLE_VET')")
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
    public ResponseEntity<DiagnosticOrderResponse> getOrderById(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderId) {
        access.diagnosticRead(jwt.getSubject(),orderId);
        DiagnosticOrderResponse response = diagnosticService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/diagnostics/pet/{petId} — Get diagnostic history for a pet.
     */
    @GetMapping("/pet/{petId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiagnosticOrderResponse>> getOrdersForPet(@AuthenticationPrincipal Jwt jwt, @PathVariable String petId) {
        List<DiagnosticOrderResponse> response = diagnosticService.getOrdersForPet(petId).stream().filter(o -> { try { access.diagnosticRead(jwt.getSubject(),o.orderId()); return true; } catch (org.springframework.security.access.AccessDeniedException ex) { return false; } }).toList();
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/diagnostics/items/{itemId}/results — Enter and verify diagnostic result.
     */
    @PutMapping("/items/{itemId}/results")
    @PreAuthorize("hasAuthority('ROLE_VET')")
    public ResponseEntity<DiagnosticOrderResponse> updateItemResult(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId,
            @Valid @RequestBody DiagnosticResultUpdateRequest request) {
        access.diagnosticWrite(jwt.getSubject(),itemId);
        var attributed = new DiagnosticResultUpdateRequest(request.resultValue(),request.referenceRange(),request.unit(),request.flag(),request.interpretation(),request.reportFileUrl(),jwt.getSubject());
        DiagnosticOrderResponse response = diagnosticService.updateItemResult(itemId, attributed);
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

