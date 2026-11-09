package com.pet.vetalya.medshop.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.medshop.audit.service.StoreAuditService;
import com.pet.vetalya.medshop.billing.calculation.GstTaxCalculator;
import com.pet.vetalya.medshop.billing.dto.BillingDtos.*;
import com.pet.vetalya.medshop.billing.entity.*;
import com.pet.vetalya.medshop.billing.pdf.InvoicePdfGenerator;
import com.pet.vetalya.medshop.billing.repository.*;
import com.pet.vetalya.medshop.catalog.entity.Medicine;
import com.pet.vetalya.medshop.catalog.repository.MedicineRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.customer.entity.Customer;
import com.pet.vetalya.medshop.customer.repository.CustomerRepository;
import com.pet.vetalya.medshop.gst.entity.GstReturn;
import com.pet.vetalya.medshop.gst.repository.GstReturnRepository;
import com.pet.vetalya.medshop.stock.entity.StockBatch;
import com.pet.vetalya.medshop.stock.entity.StockTransaction;
import com.pet.vetalya.medshop.stock.repository.StockBatchRepository;
import com.pet.vetalya.medshop.stock.repository.StockTransactionRepository;
import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final SalesInvoiceRepository salesInvoiceRepo;
    private final InvoiceItemRepository invoiceItemRepo;
    private final InvoiceSequenceRepository invoiceSequenceRepo;
    private final IdempotencyKeyRepository idempotencyKeyRepo;
    private final ScheduleHRegisterRepository scheduleHRegisterRepo;
    private final GstReturnRepository gstReturnRepo;
    private final StockBatchRepository stockBatchRepo;
    private final StockTransactionRepository stockTransactionRepo;
    private final MedicineRepository medicineRepo;
    private final CustomerRepository customerRepo;
    private final TenantRepository tenantRepo;
    private final StoreAuditService auditService;
    private final InvoicePdfGenerator pdfGenerator;
    private final ObjectMapper objectMapper;

    @Value("${app.zone:Asia/Kolkata}")
    private String zoneId;

    @Value("${app.billing.min-shelf-life-days:30}")
    private int minShelfLifeDays;

    public BillingService(SalesInvoiceRepository salesInvoiceRepo,
                          InvoiceItemRepository invoiceItemRepo,
                          InvoiceSequenceRepository invoiceSequenceRepo,
                          IdempotencyKeyRepository idempotencyKeyRepo,
                          ScheduleHRegisterRepository scheduleHRegisterRepo,
                          GstReturnRepository gstReturnRepo,
                          StockBatchRepository stockBatchRepo,
                          StockTransactionRepository stockTransactionRepo,
                          MedicineRepository medicineRepo,
                          CustomerRepository customerRepo,
                          TenantRepository tenantRepo,
                          StoreAuditService auditService,
                          InvoicePdfGenerator pdfGenerator,
                          ObjectMapper objectMapper) {
        this.salesInvoiceRepo = salesInvoiceRepo;
        this.invoiceItemRepo = invoiceItemRepo;
        this.invoiceSequenceRepo = invoiceSequenceRepo;
        this.idempotencyKeyRepo = idempotencyKeyRepo;
        this.scheduleHRegisterRepo = scheduleHRegisterRepo;
        this.gstReturnRepo = gstReturnRepo;
        this.stockBatchRepo = stockBatchRepo;
        this.stockTransactionRepo = stockTransactionRepo;
        this.medicineRepo = medicineRepo;
        this.customerRepo = customerRepo;
        this.tenantRepo = tenantRepo;
        this.auditService = auditService;
        this.pdfGenerator = pdfGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, String idempotencyKey) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        // 1. Idempotency Check
        String requestHash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestHash = hashRequest(request);
            Optional<IdempotencyKey> existingKeyOpt = idempotencyKeyRepo.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
            if (existingKeyOpt.isPresent()) {
                IdempotencyKey existingKey = existingKeyOpt.get();
                if (existingKey.getRequestHash().equals(requestHash)) {
                    log.info("Idempotent request replayed for key: {}", idempotencyKey);
                    return getInvoice(existingKey.getInvoiceId());
                } else {
                    throw new MedShopException.ConflictException("Idempotency key reused with different request payload");
                }
            }
        }

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new MedShopException.NotFoundException("Tenant not found"));

        if (!tenant.isActive()) {
            throw new MedShopException.ForbiddenException("Tenant account is suspended");
        }

        Customer customer = null;
        if (request.customerId() != null && !request.customerId().isBlank()) {
            customer = customerRepo.findByTenantIdAndId(tenantId, request.customerId())
                    .orElseThrow(() -> new MedShopException.NotFoundException("Customer not found: " + request.customerId()));
        }

        LocalDate today = LocalDate.now(ZoneId.of(zoneId));
        String financialYear = calculateFinancialYear(today);

        // 2. Atomic Sequential Invoice Number
        InvoiceSequence sequence = invoiceSequenceRepo.findWithLock(tenantId, financialYear)
                .orElseGet(() -> new InvoiceSequence(tenantId, financialYear, 0L));

        long nextNumber = sequence.getLastNumber() + 1L;
        sequence.setLastNumber(nextNumber);
        invoiceSequenceRepo.saveAndFlush(sequence);

        String fyShort = financialYear.substring(2); // e.g. 2026-27 -> 26-27
        String invoiceNumber = String.format("INV/%s/%06d", fyShort, nextNumber);

        // 3. Tax Parameters
        boolean isComposition = tenant.isCompositionDealer();
        String tenantStateCode = tenant.getStateCode() != null ? tenant.getStateCode().trim() : "99";
        String customerStateCode = customer != null && customer.getStateCode() != null && !customer.getStateCode().isBlank()
                ? customer.getStateCode().trim()
                : tenantStateCode;

        boolean isIntraState = tenantStateCode.equalsIgnoreCase(customerStateCode);

        // 4. Batch Allocation (FEFO or explicit), Stock Decrement & Item Calculation
        List<InvoiceItem> itemsToSave = new ArrayList<>();
        List<ScheduleHRegister> scheduleHToSave = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal cgstTotal = BigDecimal.ZERO;
        BigDecimal sgstTotal = BigDecimal.ZERO;
        BigDecimal igstTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        String invoiceId = UUID.randomUUID().toString();

        for (CreateInvoiceItemRequest itemReq : request.items()) {
            Medicine medicine = medicineRepo.findByTenantIdAndId(tenantId, itemReq.medicineId())
                    .orElseThrow(() -> new MedShopException.NotFoundException("Medicine not found: " + itemReq.medicineId()));

            if (!medicine.isActive()) {
                throw new MedShopException.BadRequestException("Medicine is inactive: " + medicine.getName());
            }

            // Schedule H verification
            if (medicine.isScheduleH()) {
                if (request.doctorName() == null || request.doctorName().isBlank() ||
                    request.doctorRegistrationNo() == null || request.doctorRegistrationNo().isBlank()) {
                    throw new MedShopException.BadRequestException(
                            "Doctor name and registration number are mandatory for Schedule H medicine: " + medicine.getName());
                }
                String patientName = customer != null ? customer.getName() : request.customerName();
                if (patientName == null || patientName.isBlank()) {
                    throw new MedShopException.BadRequestException(
                            "Patient/Customer name is mandatory for Schedule H medicine: " + medicine.getName());
                }
            }

            BigDecimal effectiveGstRate = isComposition ? BigDecimal.ZERO : medicine.getGstRate();

            if (itemReq.batchId() != null && !itemReq.batchId().isBlank()) {
                // Explicit Batch Selection
                StockBatch batch = stockBatchRepo.findForUpdateByTenantIdAndId(tenantId, itemReq.batchId())
                        .orElseThrow(() -> new MedShopException.NotFoundException("Batch not found: " + itemReq.batchId()));

                if (!batch.getMedicineId().equals(medicine.getId())) {
                    throw new MedShopException.BadRequestException("Batch " + batch.getBatchNumber() + " does not belong to medicine " + medicine.getName());
                }
                if (batch.getExpiryDate().isBefore(today)) {
                    throw new MedShopException.BadRequestException("Batch " + batch.getBatchNumber() + " is expired (" + batch.getExpiryDate() + ")");
                }
                if (batch.getQuantityAvailable() < itemReq.quantity()) {
                    throw new MedShopException.BadRequestException("Insufficient stock in batch " + batch.getBatchNumber()
                            + ". Available: " + batch.getQuantityAvailable() + ", Requested: " + itemReq.quantity());
                }

                BigDecimal unitPrice = itemReq.unitPrice() != null ? itemReq.unitPrice() : batch.getMrp();
                if (unitPrice.compareTo(batch.getMrp()) > 0) {
                    throw new MedShopException.BadRequestException("Selling price (" + unitPrice + ") cannot exceed MRP (" + batch.getMrp() + ")");
                }

                if (itemReq.discountPercent() != null && itemReq.discountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discountFactor = BigDecimal.ONE.subtract(itemReq.discountPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                    unitPrice = unitPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
                }

                batch.setQuantityAvailable(batch.getQuantityAvailable() - itemReq.quantity());
                stockBatchRepo.save(batch);

                StockTransaction txn = new StockTransaction(
                        tenantId, medicine.getId(), batch.getId(),
                        StockTransaction.Type.SALE, -itemReq.quantity(),
                        "Invoice " + invoiceNumber, userId
                );
                stockTransactionRepo.save(txn);

                GstTaxCalculator.RowTaxCalculation tax = GstTaxCalculator.calculateRow(
                        unitPrice, itemReq.quantity(), effectiveGstRate, isComposition, isIntraState);

                InvoiceItem invoiceItem = new InvoiceItem();
                invoiceItem.setTenantId(tenantId);
                invoiceItem.setInvoiceId(invoiceId);
                invoiceItem.setMedicineId(medicine.getId());
                invoiceItem.setBatchId(batch.getId());
                invoiceItem.setQuantity(itemReq.quantity());
                invoiceItem.setUnitPrice(unitPrice);
                invoiceItem.setTaxableValue(tax.taxableValue());
                invoiceItem.setCgstRate(tax.cgstRate());
                invoiceItem.setSgstRate(tax.sgstRate());
                invoiceItem.setIgstRate(tax.igstRate());
                invoiceItem.setTotalAmount(tax.totalAmount());
                itemsToSave.add(invoiceItem);

                if (medicine.isScheduleH()) {
                    String patient = customer != null ? customer.getName() : request.customerName();
                    LocalDate presDate = request.prescriptionDate() != null ? request.prescriptionDate() : today;
                    ScheduleHRegister sch = new ScheduleHRegister(
                            tenantId, invoiceId, invoiceItem.getId(), medicine.getId(),
                            patient, request.patientAge(), request.doctorName(),
                            request.doctorRegistrationNo(), presDate
                    );
                    scheduleHToSave.add(sch);
                }

                subtotal = subtotal.add(tax.taxableValue());
                cgstTotal = cgstTotal.add(tax.cgstAmount());
                sgstTotal = sgstTotal.add(tax.sgstAmount());
                igstTotal = igstTotal.add(tax.igstAmount());
                grandTotal = grandTotal.add(tax.totalAmount());

            } else {
                // FEFO Allocation: batches with shelf life >= minShelfLifeDays
                LocalDate minShelfLifeDate = today.plusDays(minShelfLifeDays);
                List<StockBatch> eligibleBatches = stockBatchRepo.findEligibleBatchesForSale(tenantId, medicine.getId(), minShelfLifeDate);

                int totalAvailable = eligibleBatches.stream().mapToInt(StockBatch::getQuantityAvailable).sum();
                if (totalAvailable < itemReq.quantity()) {
                    throw new MedShopException.BadRequestException("Insufficient stock for medicine: " + medicine.getName()
                            + ". Available: " + totalAvailable + ", Requested: " + itemReq.quantity());
                }

                int remainingNeeded = itemReq.quantity();
                for (StockBatch batch : eligibleBatches) {
                    if (remainingNeeded <= 0) break;
                    int take = Math.min(remainingNeeded, batch.getQuantityAvailable());

                    BigDecimal unitPrice = itemReq.unitPrice() != null ? itemReq.unitPrice() : batch.getMrp();
                    if (unitPrice.compareTo(batch.getMrp()) > 0) {
                        throw new MedShopException.BadRequestException("Selling price (" + unitPrice + ") cannot exceed MRP (" + batch.getMrp() + ")");
                    }

                    if (itemReq.discountPercent() != null && itemReq.discountPercent().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal discountFactor = BigDecimal.ONE.subtract(itemReq.discountPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                        unitPrice = unitPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
                    }

                    batch.setQuantityAvailable(batch.getQuantityAvailable() - take);
                    stockBatchRepo.save(batch);

                    StockTransaction txn = new StockTransaction(
                            tenantId, medicine.getId(), batch.getId(),
                            StockTransaction.Type.SALE, -take,
                            "Invoice " + invoiceNumber, userId
                    );
                    stockTransactionRepo.save(txn);

                    GstTaxCalculator.RowTaxCalculation tax = GstTaxCalculator.calculateRow(
                            unitPrice, take, effectiveGstRate, isComposition, isIntraState);

                    InvoiceItem invoiceItem = new InvoiceItem();
                    invoiceItem.setTenantId(tenantId);
                    invoiceItem.setInvoiceId(invoiceId);
                    invoiceItem.setMedicineId(medicine.getId());
                    invoiceItem.setBatchId(batch.getId());
                    invoiceItem.setQuantity(take);
                    invoiceItem.setUnitPrice(unitPrice);
                    invoiceItem.setTaxableValue(tax.taxableValue());
                    invoiceItem.setCgstRate(tax.cgstRate());
                    invoiceItem.setSgstRate(tax.sgstRate());
                    invoiceItem.setIgstRate(tax.igstRate());
                    invoiceItem.setTotalAmount(tax.totalAmount());
                    itemsToSave.add(invoiceItem);

                    if (medicine.isScheduleH()) {
                        String patient = customer != null ? customer.getName() : request.customerName();
                        LocalDate presDate = request.prescriptionDate() != null ? request.prescriptionDate() : today;
                        ScheduleHRegister sch = new ScheduleHRegister(
                                tenantId, invoiceId, invoiceItem.getId(), medicine.getId(),
                                patient, request.patientAge(), request.doctorName(),
                                request.doctorRegistrationNo(), presDate
                        );
                        scheduleHToSave.add(sch);
                    }

                    subtotal = subtotal.add(tax.taxableValue());
                    cgstTotal = cgstTotal.add(tax.cgstAmount());
                    sgstTotal = sgstTotal.add(tax.sgstAmount());
                    igstTotal = igstTotal.add(tax.igstAmount());
                    grandTotal = grandTotal.add(tax.totalAmount());

                    remainingNeeded -= take;
                }
            }
        }

        // 5. Persist Invoice & Children
        SalesInvoice invoice = new SalesInvoice(invoiceId);
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomerId(customer != null ? customer.getId() : null);
        invoice.setCreatedBy(userId);
        invoice.setInvoiceDate(today);
        invoice.setSubtotal(subtotal);
        invoice.setCgstAmount(cgstTotal);
        invoice.setSgstAmount(sgstTotal);
        invoice.setIgstAmount(igstTotal);
        invoice.setTotalAmount(grandTotal);
        invoice.setPaymentMode(request.paymentMode());
        invoice.setStatus(SalesInvoice.Status.FINALIZED);

        salesInvoiceRepo.save(invoice);
        invoiceItemRepo.saveAll(itemsToSave);
        if (!scheduleHToSave.isEmpty()) {
            scheduleHRegisterRepo.saveAll(scheduleHToSave);
        }

        // 6. Record Idempotency Key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey keyRecord = new IdempotencyKey(tenantId, idempotencyKey, requestHash, invoice.getId());
            idempotencyKeyRepo.save(keyRecord);
        }

        auditService.record(tenantId, userId, "INVOICE", invoice.getId(), "CREATE", null,
                Map.of("invoiceNumber", invoiceNumber, "totalAmount", grandTotal, "itemsCount", itemsToSave.size()));

        return toInvoiceResponse(invoice, itemsToSave, customer);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(String id) {
        String tenantId = TenantContext.getTenantId();
        SalesInvoice invoice = salesInvoiceRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Invoice not found: " + id));

        List<InvoiceItem> items = invoiceItemRepo.findByTenantIdAndInvoiceId(tenantId, id);
        Customer customer = invoice.getCustomerId() != null
                ? customerRepo.findByTenantIdAndId(tenantId, invoice.getCustomerId()).orElse(null)
                : null;

        return toInvoiceResponse(invoice, items, customer);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(LocalDate from, LocalDate to, SalesInvoice.Status status,
                                              String customerId, SalesInvoice.PaymentMode paymentMode,
                                              String createdBy, String invoiceNumber, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<SalesInvoice> page = salesInvoiceRepo.findFiltered(tenantId, from, to, status, customerId, paymentMode, createdBy, invoiceNumber, pageable);

        Map<String, Customer> customerCache = new HashMap<>();
        return page.map(inv -> {
            List<InvoiceItem> items = invoiceItemRepo.findByTenantIdAndInvoiceId(tenantId, inv.getId());
            Customer cust = null;
            if (inv.getCustomerId() != null) {
                cust = customerCache.computeIfAbsent(inv.getCustomerId(), cid -> customerRepo.findByTenantIdAndId(tenantId, cid).orElse(null));
            }
            return toInvoiceResponse(inv, items, cust);
        });
    }

    @Transactional
    public InvoiceResponse cancelInvoice(String id, CancelInvoiceRequest request) {
        String tenantId = TenantContext.getTenantId();
        String userId = TenantContext.getUserId();

        SalesInvoice invoice = salesInvoiceRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Invoice not found: " + id));

        if (invoice.getStatus() == SalesInvoice.Status.CANCELLED) {
            throw new MedShopException.BadRequestException("Invoice is already cancelled");
        }

        // GST Returns lock check: cannot cancel if GST return for that month/year is FILED
        boolean isFiled = gstReturnRepo.existsByTenantIdAndReturnYearAndReturnMonthAndStatus(
                tenantId, invoice.getInvoiceDate().getYear(), invoice.getInvoiceDate().getMonthValue(), GstReturn.Status.FILED);

        if (isFiled) {
            throw new MedShopException.ConflictException("Cannot cancel invoice for a tax period whose GST return has already been filed");
        }

        invoice.setStatus(SalesInvoice.Status.CANCELLED);
        salesInvoiceRepo.save(invoice);

        // Reverse stock allocations
        List<InvoiceItem> items = invoiceItemRepo.findByTenantIdAndInvoiceId(tenantId, id);
        for (InvoiceItem item : items) {
            StockBatch batch = stockBatchRepo.findForUpdateByTenantIdAndId(tenantId, item.getBatchId())
                    .orElse(null);

            if (batch != null) {
                batch.setQuantityAvailable(batch.getQuantityAvailable() + item.getQuantity());
                stockBatchRepo.save(batch);

                StockTransaction txn = new StockTransaction(
                        tenantId, item.getMedicineId(), item.getBatchId(),
                        StockTransaction.Type.SALE_RETURN, item.getQuantity(),
                        "Invoice cancelled: " + request.reason(), userId
                );
                stockTransactionRepo.save(txn);
            }
        }

        auditService.record(tenantId, userId, "INVOICE", invoice.getId(), "CANCEL",
                Map.of("status", "FINALIZED"), Map.of("status", "CANCELLED", "reason", request.reason()));

        Customer customer = invoice.getCustomerId() != null
                ? customerRepo.findByTenantIdAndId(tenantId, invoice.getCustomerId()).orElse(null)
                : null;

        return toInvoiceResponse(invoice, items, customer);
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(String id) {
        String tenantId = TenantContext.getTenantId();
        SalesInvoice invoice = salesInvoiceRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Invoice not found: " + id));

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new MedShopException.NotFoundException("Tenant not found"));

        Customer customer = invoice.getCustomerId() != null
                ? customerRepo.findByTenantIdAndId(tenantId, invoice.getCustomerId()).orElse(null)
                : null;

        List<InvoiceItem> items = invoiceItemRepo.findByTenantIdAndInvoiceId(tenantId, id);
        List<InvoiceItemResponse> itemResponses = buildItemResponses(tenantId, items);

        TenantPdfInfo tenantInfo = new TenantPdfInfo(
                tenant.getShopName(), tenant.getAddress(), tenant.getPhone(),
                tenant.getEmail(), tenant.getGstin(), tenant.getStateCode(), tenant.isCompositionDealer()
        );

        CustomerPdfInfo customerInfo = customer != null
                ? new CustomerPdfInfo(customer.getName(), customer.getPhone(), customer.getGstin(), customer.getStateCode())
                : null;

        InvoiceDetailsResponse details = new InvoiceDetailsResponse(
                tenantInfo, customerInfo, invoice.getInvoiceNumber(), invoice.getInvoiceDate(),
                invoice.getPaymentMode(), invoice.getStatus(), itemResponses,
                invoice.getSubtotal(), invoice.getCgstAmount(), invoice.getSgstAmount(),
                invoice.getIgstAmount(), invoice.getTotalAmount()
        );

        return pdfGenerator.generateInvoicePdf(details);
    }

    @Transactional(readOnly = true)
    public Page<ScheduleHRegisterResponse> listScheduleHRegister(LocalDate from, LocalDate to, String medicineId, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<ScheduleHRegister> page = scheduleHRegisterRepo.findFiltered(tenantId, from, to, medicineId, pageable);

        Map<String, String> medNames = new HashMap<>();
        Map<String, String> invNumbers = new HashMap<>();

        return page.map(s -> {
            String medName = medNames.computeIfAbsent(s.getMedicineId(), mid ->
                    medicineRepo.findByTenantIdAndId(tenantId, mid).map(Medicine::getName).orElse("Unknown"));
            String invNum = invNumbers.computeIfAbsent(s.getInvoiceId(), iid ->
                    salesInvoiceRepo.findByTenantIdAndId(tenantId, iid).map(SalesInvoice::getInvoiceNumber).orElse("Unknown"));

            return new ScheduleHRegisterResponse(
                    s.getId(), s.getInvoiceId(), invNum, s.getMedicineId(), medName,
                    "-", s.getPatientName(), s.getPatientAge(), s.getDoctorName(),
                    s.getDoctorRegistrationNo(), s.getPrescriptionDate(), 0, s.getCreatedAt()
            );
        });
    }

    private InvoiceResponse toInvoiceResponse(SalesInvoice invoice, List<InvoiceItem> items, Customer customer) {
        List<InvoiceItemResponse> itemResponses = buildItemResponses(invoice.getTenantId(), items);

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getCustomerId(),
                customer != null ? customer.getName() : null,
                customer != null ? customer.getPhone() : null,
                customer != null ? customer.getGstin() : null,
                customer != null ? customer.getStateCode() : null,
                invoice.getSubtotal(),
                invoice.getCgstAmount(),
                invoice.getSgstAmount(),
                invoice.getIgstAmount(),
                invoice.getTotalAmount(),
                invoice.getPaymentMode(),
                invoice.getStatus(),
                invoice.getCreatedBy(),
                invoice.getCreatedAt(),
                itemResponses
        );
    }

    private List<InvoiceItemResponse> buildItemResponses(String tenantId, List<InvoiceItem> items) {
        Map<String, Medicine> medicineCache = new HashMap<>();
        Map<String, StockBatch> batchCache = new HashMap<>();

        return items.stream().map(it -> {
            Medicine med = medicineCache.computeIfAbsent(it.getMedicineId(), mid ->
                    medicineRepo.findByTenantIdAndId(tenantId, mid).orElse(null));
            StockBatch batch = batchCache.computeIfAbsent(it.getBatchId(), bid ->
                    stockBatchRepo.findByTenantIdAndId(tenantId, bid).orElse(null));

            BigDecimal cgstAmt = it.getTaxableValue().multiply(it.getCgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal sgstAmt = it.getTaxableValue().multiply(it.getSgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal igstAmt = it.getTaxableValue().multiply(it.getIgstRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            return new InvoiceItemResponse(
                    it.getId(),
                    it.getMedicineId(),
                    med != null ? med.getName() : "Unknown",
                    med != null ? med.getHsnCode() : "",
                    it.getBatchId(),
                    batch != null ? batch.getBatchNumber() : "Unknown",
                    batch != null ? batch.getExpiryDate() : null,
                    it.getQuantity(),
                    it.getUnitPrice(),
                    batch != null ? batch.getMrp() : it.getUnitPrice(),
                    it.getTaxableValue(),
                    it.getCgstRate(),
                    cgstAmt,
                    it.getSgstRate(),
                    sgstAmt,
                    it.getIgstRate(),
                    igstAmt,
                    it.getTotalAmount(),
                    med != null && med.isScheduleH()
            );
        }).collect(Collectors.toList());
    }

    private String calculateFinancialYear(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int startYear = (month >= 4) ? year : (year - 1);
        int endYearShort = (startYear + 1) % 100;
        return String.format("%d-%02d", startYear, endYearShort);
    }

    private String hashRequest(CreateInvoiceRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to calculate request hash", e);
        }
    }
}

