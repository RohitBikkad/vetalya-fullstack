package com.pet.vetalya.medshop.gst.service;

import com.pet.vetalya.medshop.audit.service.StoreAuditService;
import com.pet.vetalya.medshop.billing.entity.InvoiceItem;
import com.pet.vetalya.medshop.billing.entity.SalesInvoice;
import com.pet.vetalya.medshop.billing.repository.InvoiceItemRepository;
import com.pet.vetalya.medshop.billing.repository.SalesInvoiceRepository;
import com.pet.vetalya.medshop.catalog.entity.Medicine;
import com.pet.vetalya.medshop.catalog.repository.MedicineRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.customer.entity.Customer;
import com.pet.vetalya.medshop.customer.repository.CustomerRepository;
import com.pet.vetalya.medshop.gst.dto.GstDtos.*;
import com.pet.vetalya.medshop.gst.entity.GstReturn;
import com.pet.vetalya.medshop.gst.repository.GstReturnRepository;
import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GstReturnService {

    private final GstReturnRepository gstReturnRepo;
    private final SalesInvoiceRepository salesInvoiceRepo;
    private final InvoiceItemRepository invoiceItemRepo;
    private final CustomerRepository customerRepo;
    private final MedicineRepository medicineRepo;
    private final TenantRepository tenantRepo;
    private final StoreAuditService auditService;

    public GstReturnService(GstReturnRepository gstReturnRepo,
                            SalesInvoiceRepository salesInvoiceRepo,
                            InvoiceItemRepository invoiceItemRepo,
                            CustomerRepository customerRepo,
                            MedicineRepository medicineRepo,
                            TenantRepository tenantRepo,
                            StoreAuditService auditService) {
        this.gstReturnRepo = gstReturnRepo;
        this.salesInvoiceRepo = salesInvoiceRepo;
        this.invoiceItemRepo = invoiceItemRepo;
        this.customerRepo = customerRepo;
        this.medicineRepo = medicineRepo;
        this.tenantRepo = tenantRepo;
        this.auditService = auditService;
    }

    @Transactional
    public GstReturnResponse generateReturn(GenerateGstReturnRequest request) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        Optional<GstReturn> existingOpt = gstReturnRepo.findByTenantIdAndReturnYearAndReturnMonthAndReturnType(
                tenantId, request.returnYear(), request.returnMonth(), request.returnType()
        );

        if (existingOpt.isPresent() && existingOpt.get().getStatus() == GstReturn.Status.FILED) {
            throw new MedShopException.ConflictException("Return for " + request.returnYear() + "-"
                    + request.returnMonth() + " (" + request.returnType() + ") has already been FILED and cannot be regenerated");
        }

        LocalDate startDate = LocalDate.of(request.returnYear(), request.returnMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<SalesInvoice> invoices = salesInvoiceRepo.findByTenantIdAndStatusAndInvoiceDateBetween(
                tenantId, SalesInvoice.Status.FINALIZED, startDate, endDate
        );

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (SalesInvoice inv : invoices) {
            totalTaxable = totalTaxable.add(inv.getSubtotal());
            totalCgst = totalCgst.add(inv.getCgstAmount());
            totalSgst = totalSgst.add(inv.getSgstAmount());
            totalIgst = totalIgst.add(inv.getIgstAmount());
        }

        GstReturn gstReturn = existingOpt.orElseGet(() -> {
            GstReturn gr = new GstReturn();
            gr.setTenantId(tenantId);
            gr.setReturnYear(request.returnYear());
            gr.setReturnMonth(request.returnMonth());
            gr.setReturnType(request.returnType());
            return gr;
        });

        gstReturn.setTotalTaxableValue(totalTaxable);
        gstReturn.setTotalCgst(totalCgst);
        gstReturn.setTotalSgst(totalSgst);
        gstReturn.setTotalIgst(totalIgst);
        gstReturn.setStatus(GstReturn.Status.GENERATED);

        gstReturn = gstReturnRepo.save(gstReturn);

        auditService.record(tenantId, userId, "GST_RETURN", gstReturn.getId(), "GENERATE", null,
                Map.of("year", request.returnYear(), "month", request.returnMonth(), "type", request.returnType().name()));

        return toResponse(gstReturn);
    }

    @Transactional
    public GstReturnResponse markFiled(String id, MarkFiledRequest request) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        GstReturn gstReturn = gstReturnRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("GST return not found: " + id));

        if (gstReturn.getStatus() == GstReturn.Status.FILED) {
            throw new MedShopException.BadRequestException("GST return is already marked as FILED");
        }

        gstReturn.setStatus(GstReturn.Status.FILED);
        gstReturn.setFiledRefNumber(request.filedRefNumber());
        gstReturn.setFiledAt(Instant.now());

        gstReturn = gstReturnRepo.save(gstReturn);

        auditService.record(tenantId, userId, "GST_RETURN", gstReturn.getId(), "FILE",
                Map.of("status", "GENERATED"),
                Map.of("status", "FILED", "arn", request.filedRefNumber()));

        return toResponse(gstReturn);
    }

    @Transactional(readOnly = true)
    public GstReturnResponse getReturn(String id) {
        String tenantId = TenantContext.getTenantId();
        GstReturn gr = gstReturnRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("GST return not found: " + id));
        return toResponse(gr);
    }

    @Transactional(readOnly = true)
    public Page<GstReturnResponse> listReturns(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return gstReturnRepo.findByTenantId(tenantId, pageable).map(r -> toResponse(r));
    }

    @Transactional(readOnly = true)
    public Gstr1ExportResponse exportGstr1Json(String id) {
        String tenantId = TenantContext.getTenantId();
        GstReturn gstReturn = gstReturnRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("GST return not found: " + id));

        if (gstReturn.getReturnType() != GstReturn.Type.GSTR1) {
            throw new MedShopException.BadRequestException("Only GSTR1 returns can be exported with B2B/B2CS breakdown");
        }

        LocalDate startDate = LocalDate.of(gstReturn.getReturnYear(), gstReturn.getReturnMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<SalesInvoice> invoices = salesInvoiceRepo.findByTenantIdAndStatusAndInvoiceDateBetween(
                tenantId, SalesInvoice.Status.FINALIZED, startDate, endDate
        );

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new MedShopException.NotFoundException("Tenant not found"));

        String defaultStateCode = tenant.getStateCode() != null ? tenant.getStateCode() : "99";

        List<Gstr1B2bInvoice> b2bList = new ArrayList<>();
        Map<String, B2csAggregator> b2csMap = new HashMap<>(); // key: pos + "_" + rate
        Map<String, HsnAggregator> hsnMap = new HashMap<>();   // key: hsnCode

        Map<String, Customer> customerCache = new HashMap<>();
        Map<String, Medicine> medicineCache = new HashMap<>();

        for (SalesInvoice inv : invoices) {
            Customer cust = null;
            if (inv.getCustomerId() != null) {
                cust = customerCache.computeIfAbsent(inv.getCustomerId(),
                        cid -> customerRepo.findByTenantIdAndId(tenantId, cid).orElse(null));
            }

            boolean isB2b = cust != null && cust.getGstin() != null && !cust.getGstin().isBlank();
            String pos = (cust != null && cust.getStateCode() != null && !cust.getStateCode().isBlank())
                    ? cust.getStateCode()
                    : defaultStateCode;

            if (isB2b) {
                b2bList.add(new Gstr1B2bInvoice(
                        inv.getInvoiceNumber(),
                        inv.getInvoiceDate(),
                        cust.getName(),
                        cust.getGstin(),
                        pos,
                        inv.getSubtotal(),
                        inv.getCgstAmount(),
                        inv.getSgstAmount(),
                        inv.getIgstAmount(),
                        inv.getTotalAmount()
                ));
            }

            List<InvoiceItem> items = invoiceItemRepo.findByTenantIdAndInvoiceId(tenantId, inv.getId());
            for (InvoiceItem item : items) {
                Medicine med = medicineCache.computeIfAbsent(item.getMedicineId(),
                        mid -> medicineRepo.findByTenantIdAndId(tenantId, mid).orElse(null));

                BigDecimal effectiveRate = item.getIgstRate().compareTo(BigDecimal.ZERO) > 0
                        ? item.getIgstRate()
                        : item.getCgstRate().add(item.getSgstRate());

                BigDecimal cgstAmt = item.getTaxableValue().multiply(item.getCgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal sgstAmt = item.getTaxableValue().multiply(item.getSgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal igstAmt = item.getTaxableValue().multiply(item.getIgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                if (!isB2b) {
                    String b2csKey = pos + "_" + effectiveRate.toPlainString();
                    B2csAggregator b2csAgg = b2csMap.computeIfAbsent(b2csKey, k -> new B2csAggregator(pos, effectiveRate));
                    b2csAgg.taxable = b2csAgg.taxable.add(item.getTaxableValue());
                    b2csAgg.cgst = b2csAgg.cgst.add(cgstAmt);
                    b2csAgg.sgst = b2csAgg.sgst.add(sgstAmt);
                    b2csAgg.igst = b2csAgg.igst.add(igstAmt);
                    b2csAgg.total = b2csAgg.total.add(item.getTotalAmount());
                }

                String hsnCode = (med != null && med.getHsnCode() != null && !med.getHsnCode().isBlank())
                        ? med.getHsnCode()
                        : "3004";
                String medName = med != null ? med.getName() : "Medicines";

                HsnAggregator hsnAgg = hsnMap.computeIfAbsent(hsnCode, k -> new HsnAggregator(hsnCode, medName));
                hsnAgg.qty += item.getQuantity();
                hsnAgg.totalValue = hsnAgg.totalValue.add(item.getTotalAmount());
                hsnAgg.taxable = hsnAgg.taxable.add(item.getTaxableValue());
                hsnAgg.cgst = hsnAgg.cgst.add(cgstAmt);
                hsnAgg.sgst = hsnAgg.sgst.add(sgstAmt);
                hsnAgg.igst = hsnAgg.igst.add(igstAmt);
            }
        }

        List<Gstr1B2csSummary> b2csList = b2csMap.values().stream()
                .map(a -> new Gstr1B2csSummary(a.pos, a.rate, a.taxable, a.cgst, a.sgst, a.igst, a.total))
                .sorted(Comparator.comparing(s -> s.placeOfSupply()))
                .collect(Collectors.toList());

        List<Gstr1HsnSummary> hsnList = hsnMap.values().stream()
                .map(a -> new Gstr1HsnSummary(a.hsnCode, a.medicineName, a.qty, a.totalValue, a.taxable, a.cgst, a.sgst, a.igst))
                .sorted(Comparator.comparing(s -> s.hsnCode()))
                .collect(Collectors.toList());

        return new Gstr1ExportResponse(toResponse(gstReturn), b2bList, b2csList, hsnList);
    }

    @Transactional(readOnly = true)
    public String exportGstr1Csv(String id) {
        Gstr1ExportResponse export = exportGstr1Json(id);
        StringBuilder sb = new StringBuilder();

        sb.append("# GSTR-1 SUMMARY\n");
        sb.append("Year,Month,Total Taxable,Total CGST,Total SGST,Total IGST,Status,ARN\n");
        GstReturnResponse d = export.returnDetails();
        sb.append(String.format("%d,%d,%.2f,%.2f,%.2f,%.2f,%s,%s\n\n",
                d.returnYear(), d.returnMonth(), d.totalTaxableValue(), d.totalCgst(), d.totalSgst(), d.totalIgst(),
                d.status(), d.filedRefNumber() != null ? d.filedRefNumber() : ""));

        sb.append("# B2B INVOICES (Customers with GSTIN)\n");
        sb.append("Invoice No,Date,Customer Name,Customer GSTIN,POS,Taxable,CGST,SGST,IGST,Total\n");
        for (Gstr1B2bInvoice b2b : export.b2bInvoices()) {
            sb.append(String.format("%s,%s,\"%s\",%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                    b2b.invoiceNumber(), b2b.invoiceDate(), b2b.customerName(), b2b.customerGstin(),
                    b2b.customerStateCode(), b2b.taxableValue(), b2b.cgstAmount(), b2b.sgstAmount(),
                    b2b.igstAmount(), b2b.totalAmount()));
        }
        sb.append("\n");

        sb.append("# B2CS SUMMARY (Walk-in / Consumer Sales)\n");
        sb.append("Place Of Supply,GST Rate,Taxable,CGST,SGST,IGST,Total\n");
        for (Gstr1B2csSummary b2cs : export.b2csSummary()) {
            sb.append(String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                    b2cs.placeOfSupply(), b2cs.gstRate(), b2cs.taxableValue(),
                    b2cs.cgstAmount(), b2cs.sgstAmount(), b2cs.igstAmount(), b2cs.totalAmount()));
        }
        sb.append("\n");

        sb.append("# HSN SUMMARY\n");
        sb.append("HSN Code,Description,Quantity,Total Value,Taxable,CGST,SGST,IGST\n");
        for (Gstr1HsnSummary hsn : export.hsnSummary()) {
            sb.append(String.format("%s,\"%s\",%d,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                    hsn.hsnCode(), hsn.medicineName(), hsn.totalQuantity(), hsn.totalValue(),
                    hsn.taxableValue(), hsn.cgstAmount(), hsn.sgstAmount(), hsn.igstAmount()));
        }

        return sb.toString();
    }

    private GstReturnResponse toResponse(GstReturn r) {
        return new GstReturnResponse(
                r.getId(),
                r.getTenantId(),
                r.getReturnMonth(),
                r.getReturnYear(),
                r.getReturnType(),
                r.getTotalTaxableValue(),
                r.getTotalCgst(),
                r.getTotalSgst(),
                r.getTotalIgst(),
                r.getStatus(),
                r.getFiledRefNumber(),
                r.getFiledAt()
        );
    }

    private static class B2csAggregator {
        String pos;
        BigDecimal rate;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        B2csAggregator(String pos, BigDecimal rate) {
            this.pos = pos;
            this.rate = rate;
        }
    }

    private static class HsnAggregator {
        String hsnCode;
        String medicineName;
        int qty = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        HsnAggregator(String hsnCode, String medicineName) {
            this.hsnCode = hsnCode;
            this.medicineName = medicineName;
        }
    }
}

