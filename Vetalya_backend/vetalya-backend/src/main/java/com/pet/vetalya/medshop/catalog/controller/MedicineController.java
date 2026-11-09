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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/store/medicines")
public class MedicineController {

    private final CatalogService catalogService;

    public MedicineController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<MedicineFullDto> createMedicine(
            @Valid @RequestBody CreateMedicineRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        MedicineFullDto dto = catalogService.createMedicine(tenantId, userId, req, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<PageResponse<?>> getMedicines(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String form,
            @RequestParam(required = false) Boolean scheduleH,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.ASC, "name"));
        String tenantId = TenantContext.requireTenantId();

        boolean isCashier = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CASHIER")) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_OWNER") || a.getAuthority().equals("ROLE_PHARMACIST"));

        Page<?> result = catalogService.getMedicines(tenantId, q, categoryId, supplierId, form, scheduleH, active, pageable, isCashier);
        return ResponseEntity.ok(PageResponse.of((Page<Object>) result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicineById(@PathVariable String id, Authentication authentication) {
        String tenantId = TenantContext.requireTenantId();
        boolean isCashier = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CASHIER")) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_OWNER") || a.getAuthority().equals("ROLE_PHARMACIST"));

        return ResponseEntity.ok(catalogService.getMedicineById(tenantId, id, isCashier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<MedicineFullDto> updateMedicine(
            @PathVariable String id,
            @Valid @RequestBody UpdateMedicineRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        return ResponseEntity.ok(catalogService.updateMedicine(tenantId, userId, id, req, request.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<Void> deleteMedicine(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        catalogService.deleteMedicine(tenantId, userId, id, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<List<MedicineFullDto>> getLowStockMedicines() {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(catalogService.getLowStockMedicines(tenantId));
    }
}

