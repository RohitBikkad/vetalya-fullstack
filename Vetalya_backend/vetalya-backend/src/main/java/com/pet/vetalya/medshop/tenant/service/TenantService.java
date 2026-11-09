package com.pet.vetalya.medshop.tenant.service;

import com.pet.vetalya.medshop.audit.service.StoreAuditService;
import com.pet.vetalya.medshop.billing.entity.SalesInvoice;
import com.pet.vetalya.medshop.billing.repository.SalesInvoiceRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.ValidationPatterns;
import com.pet.vetalya.medshop.gst.entity.GstReturn;
import com.pet.vetalya.medshop.gst.repository.GstReturnRepository;
import com.pet.vetalya.medshop.tenant.dto.TenantDtos.*;
import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class TenantService {

    private final TenantRepository tenantRepo;
    private final SalesInvoiceRepository invoiceRepo;
    private final GstReturnRepository gstReturnRepo;
    private final StoreAuditService auditService;
    private final ZoneId zoneId;

    public TenantService(
            TenantRepository tenantRepo,
            SalesInvoiceRepository invoiceRepo,
            GstReturnRepository gstReturnRepo,
            StoreAuditService auditService,
            @Value("${app.zone:Asia/Kolkata}") String zone) {
        this.tenantRepo = tenantRepo;
        this.invoiceRepo = invoiceRepo;
        this.gstReturnRepo = gstReturnRepo;
        this.auditService = auditService;
        this.zoneId = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(String tenantId) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new MedShopException.NotFoundException("Tenant not found"));
        return mapToDto(tenant);
    }

    @Transactional
    public TenantResponse updateTenant(String tenantId, String userId, UpdateTenantRequest req, String ipAddress) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new MedShopException.NotFoundException("Tenant not found"));

        if (!ValidationPatterns.isValidStateCode(req.stateCode())) {
            throw new MedShopException.BadRequestException("Invalid 2-digit Indian GST state code: " + req.stateCode());
        }

        String cleanGstin = req.gstin() != null && !req.gstin().isBlank() ? req.gstin().trim().toUpperCase() : null;
        if (cleanGstin != null) {
            if (!ValidationPatterns.isValidGstin(cleanGstin)) {
                throw new MedShopException.BadRequestException("Invalid GSTIN format: " + cleanGstin);
            }
            if (!cleanGstin.startsWith(req.stateCode().trim())) {
                throw new MedShopException.BadRequestException("GSTIN state prefix must match state code: " + req.stateCode());
            }
        }

        boolean gstinChanged = !Objects.equals(tenant.getGstin(), cleanGstin);
        boolean stateChanged = !Objects.equals(tenant.getStateCode(), req.stateCode().trim());
        boolean compositionChanged = req.isCompositionDealer() != null
                && tenant.isCompositionDealer() != req.isCompositionDealer();

        if (gstinChanged || stateChanged || compositionChanged) {
            LocalDate today = LocalDate.now(zoneId);
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

            // Check if any finalized invoices exist in the current unfiled GST period
            boolean hasFinalizedInvoices = invoiceRepo.existsByTenantIdAndStatusAndInvoiceDateBetween(
                    tenantId, SalesInvoice.Status.FINALIZED, startOfMonth, endOfMonth);

            boolean isFiled = gstReturnRepo.existsByTenantIdAndReturnYearAndReturnMonthAndStatus(
                    tenantId, today.getYear(), today.getMonthValue(), GstReturn.Status.FILED);

            if (hasFinalizedInvoices && !isFiled) {
                throw new MedShopException.ConflictException(
                        "Cannot modify GSTIN, state code, or composition flag while unfiled finalized invoices exist in the current tax period.");
            }
        }

        TenantResponse oldVal = mapToDto(tenant);

        tenant.setShopName(req.shopName().trim());
        tenant.setAddress(req.address().trim());
        tenant.setPhone(req.phone().trim());
        tenant.setEmail(req.email().trim());
        tenant.setGstin(cleanGstin);
        tenant.setStateCode(req.stateCode().trim());
        if (req.isCompositionDealer() != null) {
            tenant.setCompositionDealer(req.isCompositionDealer());
        }

        tenant = tenantRepo.save(tenant);
        TenantResponse newVal = mapToDto(tenant);

        if (gstinChanged || stateChanged || compositionChanged) {
            auditService.record(tenantId, userId, "TENANT", tenantId, "UPDATE_TAX_PROFILE", oldVal, newVal, ipAddress);
        }

        return newVal;
    }

    private TenantResponse mapToDto(Tenant t) {
        return new TenantResponse(
                t.getId(), t.getShopName(), t.getGstin(), t.getStateCode(),
                t.getAddress(), t.getPhone(), t.getEmail(), t.getPlan(),
                t.isActive(), t.isCompositionDealer()
        );
    }
}

