package com.pet.vetalya.medshop.customer.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.medshop.billing.entity.SalesInvoice;
import com.pet.vetalya.medshop.billing.repository.SalesInvoiceRepository;
import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.customer.dto.CustomerDtos.*;
import com.pet.vetalya.medshop.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final SalesInvoiceRepository invoiceRepo;

    public CustomerController(CustomerService customerService, SalesInvoiceRepository invoiceRepo) {
        this.customerService = customerService;
        this.invoiceRepo = invoiceRepo;
    }

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CreateCustomerRequest req) {
        String tenantId = TenantContext.requireTenantId();
        CustomerDto dto = customerService.createCustomer(tenantId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CustomerDto>> searchCustomers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.ASC, "name"));
        String tenantId = TenantContext.requireTenantId();
        Page<CustomerDto> result = customerService.searchCustomers(tenantId, q, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable String id) {
        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(customerService.getCustomer(tenantId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerRequest req) {

        String tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(customerService.updateCustomer(tenantId, id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'PHARMACIST')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        String tenantId = TenantContext.requireTenantId();
        customerService.deleteCustomer(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/invoices")
    public ResponseEntity<PageResponse<SalesInvoice>> getCustomerInvoices(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "invoiceDate"));
        String tenantId = TenantContext.requireTenantId();
        Page<SalesInvoice> result = invoiceRepo.findFiltered(tenantId, null, null, null, id, null, null, null, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }
}

