package com.pet.vetalya.medshop.audit.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.medshop.audit.entity.StoreAuditLog;
import com.pet.vetalya.medshop.audit.repository.StoreAuditLogRepository;
import com.pet.vetalya.medshop.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/store/audit-logs")
@PreAuthorize("hasRole('OWNER')")
public class StoreAuditController {

    private final StoreAuditLogRepository auditRepo;

    public StoreAuditController(StoreAuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @GetMapping
    public ResponseEntity<PageResponse<StoreAuditLog>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String tenantId = TenantContext.requireTenantId();

        Page<StoreAuditLog> result = auditRepo.findFiltered(tenantId, entityType, entityId, userId, from, to, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }
}

