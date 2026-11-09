package com.pet.vetalya.medshop.tenant.controller;

import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.tenant.dto.TenantDtos.*;
import com.pet.vetalya.medshop.tenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store/tenant")
@PreAuthorize("hasRole('OWNER')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<TenantResponse> getTenant() {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(tenantService.getTenant(tenantId));
    }

    @PutMapping
    public ResponseEntity<TenantResponse> updateTenant(
            @Valid @RequestBody UpdateTenantRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        return ResponseEntity.ok(tenantService.updateTenant(tenantId, userId, req, request.getRemoteAddr()));
    }
}

