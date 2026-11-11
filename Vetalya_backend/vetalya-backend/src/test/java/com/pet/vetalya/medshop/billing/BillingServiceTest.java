package com.pet.vetalya.medshop.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.medshop.audit.service.StoreAuditService;
import com.pet.vetalya.medshop.billing.dto.BillingDtos.*;
import com.pet.vetalya.medshop.billing.entity.*;
import com.pet.vetalya.medshop.billing.pdf.InvoicePdfGenerator;
import com.pet.vetalya.medshop.billing.repository.*;
import com.pet.vetalya.medshop.billing.service.BillingService;
import com.pet.vetalya.medshop.catalog.entity.Medicine;
import com.pet.vetalya.medshop.catalog.repository.MedicineRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.customer.repository.CustomerRepository;
import com.pet.vetalya.medshop.gst.entity.GstReturn;
import com.pet.vetalya.medshop.gst.repository.GstReturnRepository;
import com.pet.vetalya.medshop.stock.entity.StockBatch;
import com.pet.vetalya.medshop.stock.entity.StockTransaction;
import com.pet.vetalya.medshop.stock.repository.StockBatchRepository;
import com.pet.vetalya.medshop.stock.repository.StockTransactionRepository;
import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock private SalesInvoiceRepository salesInvoiceRepo;
    @Mock private InvoiceItemRepository invoiceItemRepo;
    @Mock private InvoiceSequenceRepository invoiceSequenceRepo;
    @Mock private IdempotencyKeyRepository idempotencyKeyRepo;
    @Mock private ScheduleHRegisterRepository scheduleHRegisterRepo;
    @Mock private GstReturnRepository gstReturnRepo;
    @Mock private StockBatchRepository stockBatchRepo;
    @Mock private StockTransactionRepository stockTransactionRepo;
    @Mock private MedicineRepository medicineRepo;
    @Mock private CustomerRepository customerRepo;
    @Mock private TenantRepository tenantRepo;
    @Mock private StoreAuditService auditService;
    @Mock private InvoicePdfGenerator pdfGenerator;

    private BillingService billingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tenantId = "tenant-test-123";
    private final String userId = "user-cashier-456";

    @BeforeEach
    void setUp() {
        billingService = new BillingService(
                salesInvoiceRepo, invoiceItemRepo, invoiceSequenceRepo, idempotencyKeyRepo,
                scheduleHRegisterRepo, gstReturnRepo, stockBatchRepo, stockTransactionRepo,
                medicineRepo, customerRepo, tenantRepo, auditService, pdfGenerator, objectMapper
        );
        ReflectionTestUtils.setField(billingService, "zoneId", "Asia/Kolkata");
        ReflectionTestUtils.setField(billingService, "minShelfLifeDays", 30);

        TenantContext.setTenantId(tenantId);
        TenantContext.setUserId(userId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("FEFO allocates across multiple batches ordered by nearest expiry")
    void testFefoMultiBatchAllocation() {
        Tenant tenant = new Tenant("tenant-test-123", "MediStore", "27AAAAA0000A1Z5", "27", "Pune", "9876543210", "shop@med.com", "BASIC", false);
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));

        Medicine med = new Medicine();
        med.setId("med-1");
        med.setName("Paracetamol 500mg");
        med.setTenantId(tenantId);
        med.setGstRate(new BigDecimal("12.00"));
        med.setScheduleH(false);
        med.setActive(true);
        when(medicineRepo.findByTenantIdAndId(tenantId, "med-1")).thenReturn(Optional.of(med));

        // Two batches: batch1 expires in 40 days (qty 5), batch2 expires in 90 days (qty 10)
        LocalDate today = LocalDate.now();
        StockBatch batch1 = new StockBatch(tenantId, "med-1", "B001", today.minusMonths(2), today.plusDays(40), 10, 5, new BigDecimal("10.00"), new BigDecimal("20.00"));
        StockBatch batch2 = new StockBatch(tenantId, "med-1", "B002", today.minusMonths(1), today.plusDays(90), 15, 10, new BigDecimal("10.00"), new BigDecimal("20.00"));
        when(stockBatchRepo.findEligibleBatchesForSale(eq(tenantId), eq("med-1"), any(LocalDate.class)))
                .thenReturn(List.of(batch1, batch2));

        InvoiceSequence seq = new InvoiceSequence(tenantId, "2026-27", 0L);
        when(invoiceSequenceRepo.findWithLock(eq(tenantId), anyString())).thenReturn(Optional.of(seq));

        // Request 8 units (5 from batch1 + 3 from batch2)
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                null, "Walkin Customer", "9999999999", null, null, null, null,
                SalesInvoice.PaymentMode.CASH,
                List.of(new CreateInvoiceItemRequest("med-1", null, 8, null, null))
        );

        InvoiceResponse response = billingService.createInvoice(request, null);

        assertThat(response).isNotNull();
        assertThat(response.invoiceNumber()).isEqualTo("INV/26-27/000001");
        assertThat(batch1.getQuantityAvailable()).isEqualTo(0);
        assertThat(batch2.getQuantityAvailable()).isEqualTo(7);

        // Verify stock transactions recorded
        verify(stockTransactionRepo, times(2)).save(any(StockTransaction.class));
        verify(salesInvoiceRepo).save(any(SalesInvoice.class));
    }

    @Test
    @DisplayName("Unit price exceeding MRP throws 400 Bad Request")
    void testPriceExceedingMrpRejected() {
        Tenant tenant = new Tenant(tenantId, "MediStore", "27AAAAA0000A1Z5", "27", "Pune", "9876543210", "shop@med.com", "BASIC", false);
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));

        Medicine med = new Medicine();
        med.setId("med-1");
        med.setName("Amoxicillin");
        med.setTenantId(tenantId);
        med.setActive(true);
        when(medicineRepo.findByTenantIdAndId(tenantId, "med-1")).thenReturn(Optional.of(med));

        StockBatch batch = new StockBatch(tenantId, "med-1", "B001", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6), 20, 20, new BigDecimal("30.00"), new BigDecimal("50.00"));
        when(stockBatchRepo.findForUpdateByTenantIdAndId(tenantId, "batch-1")).thenReturn(Optional.of(batch));

        InvoiceSequence seq = new InvoiceSequence(tenantId, "2026-27", 0L);
        when(invoiceSequenceRepo.findWithLock(eq(tenantId), anyString())).thenReturn(Optional.of(seq));

        // Request price of 55.00 when MRP is 50.00
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                null, "Customer", "9999999999", null, null, null, null,
                SalesInvoice.PaymentMode.UPI,
                List.of(new CreateInvoiceItemRequest("med-1", "batch-1", 1, new BigDecimal("55.00"), null))
        );

        assertThatThrownBy(() -> billingService.createInvoice(request, null))
                .isInstanceOf(MedShopException.BadRequestException.class)
                .hasMessageContaining("cannot exceed MRP");
    }

    @Test
    @DisplayName("Dispensing Schedule H medicine without doctor name and reg number throws 400 Bad Request")
    void testScheduleHMedicineValidation() {
        Tenant tenant = new Tenant(tenantId, "MediStore", "27AAAAA0000A1Z5", "27", "Pune", "9876543210", "shop@med.com", "BASIC", false);
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));

        Medicine med = new Medicine();
        med.setId("med-sch-h");
        med.setName("Alprazolam 0.5mg");
        med.setTenantId(tenantId);
        med.setScheduleH(true);
        med.setActive(true);
        when(medicineRepo.findByTenantIdAndId(tenantId, "med-sch-h")).thenReturn(Optional.of(med));

        InvoiceSequence seq = new InvoiceSequence(tenantId, "2026-27", 0L);
        when(invoiceSequenceRepo.findWithLock(eq(tenantId), anyString())).thenReturn(Optional.of(seq));

        // Missing doctorName and doctorRegistrationNo
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                null, "Patient X", "9999999999", null, null, null, null,
                SalesInvoice.PaymentMode.CASH,
                List.of(new CreateInvoiceItemRequest("med-sch-h", null, 1, null, null))
        );

        assertThatThrownBy(() -> billingService.createInvoice(request, null))
                .isInstanceOf(MedShopException.BadRequestException.class)
                .hasMessageContaining("Schedule H");
    }

    @Test
    @DisplayName("Replaying same request with same Idempotency-Key returns existing invoice")
    void testIdempotencyKeyMatching() {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                null, "Customer", "9876543210", null, null, null, null,
                SalesInvoice.PaymentMode.CASH,
                List.of(new CreateInvoiceItemRequest("med-1", null, 1, null, null))
        );

        // Pre-existing idempotency key matching hash
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String requestHash;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(requestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) {
                String hex = Integer.toHexString(0xff & x);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            requestHash = sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        IdempotencyKey existingKey = new IdempotencyKey(tenantId, "key-xyz", requestHash, "inv-existing-1");
        when(idempotencyKeyRepo.findByTenantIdAndIdempotencyKey(tenantId, "key-xyz"))
                .thenReturn(Optional.of(existingKey));

        SalesInvoice existingInvoice = new SalesInvoice("inv-existing-1");
        existingInvoice.setTenantId(tenantId);
        existingInvoice.setInvoiceNumber("INV/26-27/000001");
        existingInvoice.setInvoiceDate(LocalDate.now());
        existingInvoice.setSubtotal(new BigDecimal("100.00"));
        existingInvoice.setTotalAmount(new BigDecimal("112.00"));
        existingInvoice.setPaymentMode(SalesInvoice.PaymentMode.CASH);
        existingInvoice.setStatus(SalesInvoice.Status.FINALIZED);
        existingInvoice.setCreatedBy(userId);

        when(salesInvoiceRepo.findByTenantIdAndId(tenantId, "inv-existing-1")).thenReturn(Optional.of(existingInvoice));
        when(invoiceItemRepo.findByTenantIdAndInvoiceId(tenantId, "inv-existing-1")).thenReturn(List.of());

        InvoiceResponse response = billingService.createInvoice(request, "key-xyz");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("inv-existing-1");
        assertThat(response.invoiceNumber()).isEqualTo("INV/26-27/000001");

        // Verify sequence and new invoice are NOT created
        verify(invoiceSequenceRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Reusing Idempotency-Key with different payload throws 409 Conflict")
    void testIdempotencyKeyMismatchConflict() {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                null, "Different Customer", "1111111111", null, null, null, null,
                SalesInvoice.PaymentMode.CARD,
                List.of(new CreateInvoiceItemRequest("med-1", null, 5, null, null))
        );

        IdempotencyKey existingKey = new IdempotencyKey(tenantId, "key-xyz", "different-hash-value", "inv-existing-1");
        when(idempotencyKeyRepo.findByTenantIdAndIdempotencyKey(tenantId, "key-xyz"))
                .thenReturn(Optional.of(existingKey));

        assertThatThrownBy(() -> billingService.createInvoice(request, "key-xyz"))
                .isInstanceOf(MedShopException.ConflictException.class)
                .hasMessageContaining("different request payload");
    }

    @Test
    @DisplayName("Invoice cancellation restores batch stock and records SALE_RETURN transaction")
    void testCancelInvoiceRestoresStock() {
        SalesInvoice invoice = new SalesInvoice("inv-cancel-1");
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNumber("INV/26-27/000001");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setStatus(SalesInvoice.Status.FINALIZED);
        invoice.setSubtotal(new BigDecimal("50.00"));
        invoice.setTotalAmount(new BigDecimal("56.00"));
        invoice.setPaymentMode(SalesInvoice.PaymentMode.CASH);
        invoice.setCreatedBy(userId);
        when(salesInvoiceRepo.findByTenantIdAndId(tenantId, "inv-cancel-1")).thenReturn(Optional.of(invoice));

        // GST return for this month is NOT filed
        when(gstReturnRepo.existsByTenantIdAndReturnYearAndReturnMonthAndStatus(
                eq(tenantId), anyInt(), anyInt(), eq(GstReturn.Status.FILED))).thenReturn(false);

        InvoiceItem item = new InvoiceItem();
        item.setTenantId(tenantId);
        item.setInvoiceId("inv-cancel-1");
        item.setMedicineId("med-1");
        item.setBatchId("batch-1");
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setTaxableValue(new BigDecimal("50.00"));
        item.setTotalAmount(new BigDecimal("56.00"));
        when(invoiceItemRepo.findByTenantIdAndInvoiceId(tenantId, "inv-cancel-1")).thenReturn(List.of(item));

        StockBatch batch = new StockBatch(tenantId, "med-1", "B001", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6), 10, 7, new BigDecimal("10.00"), new BigDecimal("20.00"));
        when(stockBatchRepo.findForUpdateByTenantIdAndId(tenantId, "batch-1")).thenReturn(Optional.of(batch));

        InvoiceResponse response = billingService.cancelInvoice("inv-cancel-1", new CancelInvoiceRequest("Customer returned goods"));

        assertThat(response.status()).isEqualTo(SalesInvoice.Status.CANCELLED);
        // Quantity available was 7, restored 3 -> now 10
        assertThat(batch.getQuantityAvailable()).isEqualTo(10);
        verify(stockTransactionRepo).save(argThat(txn ->
                txn.getTransactionType() == StockTransaction.Type.SALE_RETURN && txn.getQuantity() == 3
        ));
    }

    @Test
    @DisplayName("Invoice cancellation blocked when GST return is FILED")
    void testCancelBlockedWhenGstReturnFiled() {
        SalesInvoice invoice = new SalesInvoice("inv-locked-1");
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNumber("INV/26-27/000001");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setStatus(SalesInvoice.Status.FINALIZED);
        when(salesInvoiceRepo.findByTenantIdAndId(tenantId, "inv-locked-1")).thenReturn(Optional.of(invoice));

        // Return IS FILED
        when(gstReturnRepo.existsByTenantIdAndReturnYearAndReturnMonthAndStatus(
                eq(tenantId), anyInt(), anyInt(), eq(GstReturn.Status.FILED))).thenReturn(true);

        assertThatThrownBy(() -> billingService.cancelInvoice("inv-locked-1", new CancelInvoiceRequest("Mistake in billing")))
                .isInstanceOf(MedShopException.ConflictException.class)
                .hasMessageContaining("GST return has already been filed");
    }
}

