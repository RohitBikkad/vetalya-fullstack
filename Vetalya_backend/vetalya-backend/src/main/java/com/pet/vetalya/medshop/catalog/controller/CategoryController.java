package com.pet.vetalya.medshop.catalog.controller;

import com.pet.vetalya.medshop.catalog.dto.CatalogDtos.*;
import com.pet.vetalya.medshop.catalog.service.CatalogService;
import com.pet.vetalya.medshop.common.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/store/categories")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<CategoryDto> createCategory(
            @Valid @RequestBody CreateCategoryRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        CategoryDto dto = catalogService.createCategory(tenantId, userId, req, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "false") boolean tree) {

        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(catalogService.getCategories(tenantId, tree));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable String id) {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(catalogService.getCategory(tenantId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody UpdateCategoryRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        return ResponseEntity.ok(catalogService.updateCategory(tenantId, userId, id, req, request.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        catalogService.deleteCategory(tenantId, userId, id, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}

