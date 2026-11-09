package com.pet.vetalya.medshop.catalog.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.medshop.catalog.dto.CatalogDtos.*;
import com.pet.vetalya.medshop.catalog.service.CatalogService;
import com.pet.vetalya.medshop.common.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store/suppliers")
public class SupplierController {

    private final CatalogService catalogService;

    public SupplierController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<SupplierDto> createSupplier(
            @Valid @RequestBody CreateSupplierRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        SupplierDto dto = catalogService.createSupplier(tenantId, userId, req, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<PageResponse<SupplierDto>> getSuppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.ASC, "name"));
        String tenantId = TenantContext.requireTenantId();
        Page<SupplierDto> result = catalogService.getSuppliers(tenantId, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<SupplierDto> getSupplier(@PathVariable String id) {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(catalogService.getSupplier(tenantId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<SupplierDto> updateSupplier(
            @PathVariable String id,
            @Valid @RequestBody UpdateSupplierRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        return ResponseEntity.ok(catalogService.updateSupplier(tenantId, userId, id, req, request.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        catalogService.deleteSupplier(tenantId, userId, id, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}

