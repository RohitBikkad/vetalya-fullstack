package com.pet.vetalya.medshop.report.controller;

import com.pet.vetalya.medshop.report.dto.ReportDtos.*;
import com.pet.vetalya.medshop.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/store")
@Tag(name = "MedShop Reports & Analytics", description = "Endpoints for business dashboard, sales trends, top medicines, expiry forecasts, and owner profit analysis")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Real-time store dashboard: sales, invoice count, low stock alert, expiry alerts")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(reportService.getDashboard());
    }

    @GetMapping("/reports/sales")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Periodic sales summary report grouped by day or payment mode")
    public ResponseEntity<List<SalesSummaryReportItem>> getSalesReport(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "groupBy", defaultValue = "day") String groupBy
    ) {
        return ResponseEntity.ok(reportService.getSalesReport(from, to, groupBy));
    }

    @GetMapping("/reports/top-medicines")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Top selling medicines by quantity and revenue")
    public ResponseEntity<List<TopMedicineReportItem>> getTopMedicines(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(reportService.getTopMedicinesReport(from, to, limit));
    }

    @GetMapping("/reports/profit")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Owner gross profit report (Revenue - COGS) with margin percentage")
    public ResponseEntity<ProfitReportResponse> getProfitReport(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportService.getProfitReport(from, to));
    }

    @GetMapping("/reports/expiry")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Batch expiry forecast report (30/60/90 days) with inventory valuation")
    public ResponseEntity<List<ExpiringBatchReportItem>> getExpiryReport(
            @RequestParam(value = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(reportService.getExpiryReport(days));
    }
}

