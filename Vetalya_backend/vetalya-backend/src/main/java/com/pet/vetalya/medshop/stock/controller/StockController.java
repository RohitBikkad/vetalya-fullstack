package com.pet.vetalya.medshop.stock.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.stock.dto.StockDtos.*;
import com.pet.vetalya.medshop.stock.entity.StockTransaction;
import com.pet.vetalya.medshop.stock.service.StockService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/store/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/receive")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<BatchDto> receiveStock(
            @Valid @RequestBody StockReceiveRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        BatchDto dto = stockService.receiveStock(tenantId, userId, req, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/batches")
    public ResponseEntity<PageResponse<BatchDto>> getBatches(
            @RequestParam(required = false) String medicineId,
            @RequestParam(required = false) Integer expiringWithinDays,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) Boolean hasStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.ASC, "expiryDate"));
        String tenantId = TenantContext.requireTenantId();
        Page<BatchDto> result = stockService.getBatches(tenantId, medicineId, expiringWithinDays, expired, hasStock, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<BatchDto> getBatchById(@PathVariable String id) {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(stockService.getBatchById(tenantId, id));
    }

    @PutMapping("/batches/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<BatchDto> updateBatch(
            @PathVariable String id,
            @Valid @RequestBody UpdateBatchRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        return ResponseEntity.ok(stockService.updateBatch(tenantId, userId, id, req, request.getRemoteAddr()));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<BatchDto> adjustStock(
            @Valid @RequestBody StockAdjustRequest req,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        return ResponseEntity.ok(stockService.adjustStock(tenantId, userId, req, request.getRemoteAddr()));
    }

    @PostMapping("/write-off-expired")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<Map<String, Object>> writeOffExpired(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        String tenantId = TenantContext.requireTenantId();
        String userId = jwt.getSubject();
        int count = stockService.writeOffExpired(tenantId, userId, request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Expired batches written off successfully", "writtenOffCount", count));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<PageResponse<StockTransactionDto>> getTransactions(
            @RequestParam(required = false) String medicineId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) StockTransaction.Type type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String tenantId = TenantContext.requireTenantId();
        Page<StockTransactionDto> result = stockService.getTransactions(tenantId, medicineId, batchId, type, from, to, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<BatchDto>> getExpiringBatches(@RequestParam(defaultValue = "30") int days) {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(stockService.getExpiringBatches(tenantId, days));
    }

    @GetMapping("/valuation")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StockValuationResponse> getValuation() {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(stockService.getValuation(tenantId));
    }

    @GetMapping("/verify-ledger")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<LedgerMismatchDto>> verifyLedger() {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(stockService.verifyLedger(tenantId));
    }
}

