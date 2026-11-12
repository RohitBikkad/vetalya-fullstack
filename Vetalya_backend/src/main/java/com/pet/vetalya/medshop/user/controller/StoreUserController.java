package com.pet.vetalya.medshop.user.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.user.dto.UserDtos.*;
import com.pet.vetalya.medshop.user.service.StoreUserService;
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
@RequestMapping("/api/store/users")
public class StoreUserController {

    private final StoreUserService userService;

    public StoreUserController(StoreUserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StoreUserResponse> createStaff(
            @Valid @RequestBody CreateStaffRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String currentUserId = jwt.getSubject();
        StoreUserResponse resp = userService.createStaff(tenantId, currentUserId, req, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PageResponse<StoreUserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String tenantId = TenantContext.requireTenantId();
        Page<StoreUserResponse> result = userService.getUsers(tenantId, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StoreUserResponse> getUser(@PathVariable String id) {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(userService.getUser(tenantId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StoreUserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest req) {

        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(userService.updateUser(tenantId, id, req));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StoreUserResponse> changeActive(
            @PathVariable String id,
            @Valid @RequestBody ChangeActiveRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String currentUserId = jwt.getSubject();
        return ResponseEntity.ok(userService.changeActive(tenantId, currentUserId, id, req.active(), request.getRemoteAddr()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StoreUserResponse> changeRole(
            @PathVariable String id,
            @Valid @RequestBody ChangeRoleRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String currentUserId = jwt.getSubject();
        return ResponseEntity.ok(userService.changeRole(tenantId, currentUserId, id, req.role(), request.getRemoteAddr()));
    }

    @PutMapping("/me")
    public ResponseEntity<StoreUserResponse> updateMe(
            @Valid @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = TenantContext.requireTenantId();
        String currentUserId = jwt.getSubject();
        return ResponseEntity.ok(userService.updateUser(tenantId, currentUserId, req));
    }

    @PostMapping("/me/profile-pic/upload-url")
    public ResponseEntity<PresignedUrlResponse> getProfilePicUploadUrl(
            @Valid @RequestBody PresignedUrlRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = TenantContext.requireTenantId();
        String currentUserId = jwt.getSubject();
        return ResponseEntity.ok(userService.generateProfilePicUploadUrl(tenantId, currentUserId, req.contentType()));
    }
}

