package com.pet.vetalya.medshop.customer.service;

import com.pet.vetalya.medshop.billing.repository.SalesInvoiceRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.ValidationPatterns;
import com.pet.vetalya.medshop.customer.dto.CustomerDtos.*;
import com.pet.vetalya.medshop.customer.entity.Customer;
import com.pet.vetalya.medshop.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final SalesInvoiceRepository invoiceRepo;

    public CustomerService(CustomerRepository customerRepo, SalesInvoiceRepository invoiceRepo) {
        this.customerRepo = customerRepo;
        this.invoiceRepo = invoiceRepo;
    }

    @Transactional
    public CustomerDto createCustomer(String tenantId, CreateCustomerRequest req) {
        validateCustomerDetails(req.phone(), req.gstin(), req.stateCode());

        Customer c = new Customer();
        c.setTenantId(tenantId);
        c.setName(req.name().trim());
        c.setPhone(req.phone().trim());
        c.setGstin(req.gstin() != null && !req.gstin().isBlank() ? req.gstin().trim().toUpperCase() : null);
        c.setAddress(req.address());
        c.setStateCode(req.stateCode() != null && !req.stateCode().isBlank() ? req.stateCode().trim() : null);

        c = customerRepo.save(c);
        return mapToDto(c);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDto> searchCustomers(String tenantId, String q, Pageable pageable) {
        return customerRepo.search(tenantId, q, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomer(String tenantId, String id) {
        Customer c = customerRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Customer not found"));
        return mapToDto(c);
    }

    @Transactional
    public CustomerDto updateCustomer(String tenantId, String id, UpdateCustomerRequest req) {
        Customer c = customerRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Customer not found"));

        validateCustomerDetails(req.phone(), req.gstin(), req.stateCode());

        c.setName(req.name().trim());
        c.setPhone(req.phone().trim());
        c.setGstin(req.gstin() != null && !req.gstin().isBlank() ? req.gstin().trim().toUpperCase() : null);
        c.setAddress(req.address());
        c.setStateCode(req.stateCode() != null && !req.stateCode().isBlank() ? req.stateCode().trim() : null);

        c = customerRepo.save(c);
        return mapToDto(c);
    }

    @Transactional
    public void deleteCustomer(String tenantId, String id) {
        Customer c = customerRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Customer not found"));

        if (invoiceRepo.existsByTenantIdAndCustomerId(tenantId, id)) {
            throw new MedShopException.ConflictException(
                    "Customer has sales invoices recorded. Cannot delete customer with billing history.");
        }

        customerRepo.delete(c);
    }

    private void validateCustomerDetails(String phone, String gstin, String stateCode) {
        if (!ValidationPatterns.isValidPhone(phone)) {
            throw new MedShopException.BadRequestException("Invalid phone number format: " + phone);
        }
        if (stateCode != null && !stateCode.isBlank() && !ValidationPatterns.isValidStateCode(stateCode)) {
            throw new MedShopException.BadRequestException("Invalid 2-digit Indian GST state code: " + stateCode);
        }
        if (gstin != null && !gstin.isBlank()) {
            String cleanGstin = gstin.trim().toUpperCase();
            if (!ValidationPatterns.isValidGstin(cleanGstin)) {
                throw new MedShopException.BadRequestException("Invalid GSTIN format: " + cleanGstin);
            }
            if (stateCode != null && !stateCode.isBlank() && !cleanGstin.startsWith(stateCode.trim())) {
                throw new MedShopException.BadRequestException("GSTIN state prefix must match state code: " + stateCode);
            }
        }
    }

    public CustomerDto mapToDto(Customer c) {
        return new CustomerDto(c.getId(), c.getName(), c.getPhone(), c.getGstin(), c.getAddress(), c.getStateCode());
    }
}

