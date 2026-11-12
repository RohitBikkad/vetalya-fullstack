package com.pet.vetalya.medshop.gst.controller;

import com.pet.vetalya.medshop.gst.dto.GstDtos.*;
import com.pet.vetalya.medshop.gst.service.GstReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store/gst/returns")
@Tag(name = "MedShop GST Compliance", description = "Endpoints for GSTR-1 and GSTR-3B tax return aggregation, JSON/CSV exports, and filing freeze")
public class GstReturnController {

    private final GstReturnService gstReturnService;

    public GstReturnController(GstReturnService gstReturnService) {
        this.gstReturnService = gstReturnService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Aggregate sales invoices for month/year and compute GSTR-1/GSTR-3B liability")
    public ResponseEntity<GstReturnResponse> generateReturn(@Valid @RequestBody GenerateGstReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gstReturnService.generateReturn(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "List generated and filed GST returns")
    public ResponseEntity<Page<GstReturnResponse>> listReturns(
            @PageableDefault(size = 20, sort = "returnYear", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(gstReturnService.listReturns(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Get return summary by ID")
    public ResponseEntity<GstReturnResponse> getReturn(@PathVariable("id") String id) {
        return ResponseEntity.ok(gstReturnService.getReturn(id));
    }

    @PostMapping("/{id}/mark-filed")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Record tax filing reference (ARN) and permanently freeze the period from invoice cancellations")
    public ResponseEntity<GstReturnResponse> markFiled(
            @PathVariable("id") String id,
            @Valid @RequestBody MarkFiledRequest request
    ) {
        return ResponseEntity.ok(gstReturnService.markFiled(id, request));
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Export GSTR-1 in JSON or CSV format with B2B, B2CS, and HSN breakdowns")
    public ResponseEntity<?> exportReturn(
            @PathVariable("id") String id,
            @RequestParam(value = "format", defaultValue = "json") String format
    ) {
        if ("csv".equalsIgnoreCase(format)) {
            String csvContent = gstReturnService.exportGstr1Csv(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "GSTR1-" + id + ".csv");
            return new ResponseEntity<>(csvContent, headers, HttpStatus.OK);
        } else {
            Gstr1ExportResponse jsonResponse = gstReturnService.exportGstr1Json(id);
            return ResponseEntity.ok(jsonResponse);
        }
    }
}

