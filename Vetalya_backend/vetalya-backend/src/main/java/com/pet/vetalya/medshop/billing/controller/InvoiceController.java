package com.pet.vetalya.medshop.billing.controller;

import com.pet.vetalya.medshop.billing.dto.BillingDtos.*;
import com.pet.vetalya.medshop.billing.entity.SalesInvoice;
import com.pet.vetalya.medshop.billing.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/store")
@Tag(name = "MedShop Billing & Invoices", description = "Endpoints for point-of-sale billing, FEFO batch allocation, cancellation, PDF generation, and Schedule H register")
public class InvoiceController {

    private final BillingService billingService;

    public InvoiceController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Generate a new GST sales invoice with FEFO stock allocation")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        InvoiceResponse response = billingService.createInvoice(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get invoice details by ID")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable("id") String id) {
        return ResponseEntity.ok(billingService.getInvoice(id));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "List invoices with multi-criteria filtering")
    public ResponseEntity<Page<InvoiceResponse>> listInvoices(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "status", required = false) SalesInvoice.Status status,
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "paymentMode", required = false) SalesInvoice.PaymentMode paymentMode,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @RequestParam(value = "invoiceNumber", required = false) String invoiceNumber,
            @PageableDefault(size = 20, sort = "invoiceDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(billingService.listInvoices(from, to, status, customerId, paymentMode, createdBy, invoiceNumber, pageable));
    }

    @PostMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Cancel an invoice and restore stock batches (Blocked if GST return is filed)")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable("id") String id,
            @Valid @RequestBody CancelInvoiceRequest request
    ) {
        return ResponseEntity.ok(billingService.cancelInvoice(id, request));
    }

    @GetMapping(value = "/invoices/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Download invoice tax invoice PDF")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable("id") String id) {
        byte[] pdfBytes = billingService.getInvoicePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Invoice-" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/schedule-h-register")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "View regulatory Schedule H dispensation register")
    public ResponseEntity<Page<ScheduleHRegisterResponse>> listScheduleHRegister(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "medicineId", required = false) String medicineId,
            @PageableDefault(size = 50, sort = "prescriptionDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(billingService.listScheduleHRegister(from, to, medicineId, pageable));
    }
}

