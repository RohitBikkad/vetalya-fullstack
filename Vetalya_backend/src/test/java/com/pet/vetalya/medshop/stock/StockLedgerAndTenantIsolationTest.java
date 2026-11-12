package com.pet.vetalya.medshop.stock;

import com.pet.vetalya.medshop.audit.service.StoreAuditService;
import com.pet.vetalya.medshop.catalog.entity.Medicine;
import com.pet.vetalya.medshop.catalog.repository.MedicineRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.stock.dto.StockDtos.*;
import com.pet.vetalya.medshop.stock.entity.StockBatch;
import com.pet.vetalya.medshop.stock.entity.StockTransaction;
import com.pet.vetalya.medshop.stock.repository.StockBatchRepository;
import com.pet.vetalya.medshop.stock.repository.StockTransactionRepository;
import com.pet.vetalya.medshop.stock.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLedgerAndTenantIsolationTest {

    @Mock private StockBatchRepository batchRepo;
    @Mock private StockTransactionRepository txnRepo;
    @Mock private MedicineRepository medicineRepo;
    @Mock private StoreAuditService auditService;

    private StockService stockService;

    private final String tenantA = "tenant-shop-A";
    private final String userId = "user-1";

    @BeforeEach
    void setUp() {
        stockService = new StockService(batchRepo, txnRepo, medicineRepo, auditService, "Asia/Kolkata");
        TenantContext.setTenantId(tenantA);
        TenantContext.setUserId(userId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Tenant A cannot access Tenant B batch -> 404 Not Found")
    void testTenantIsolationCrossTenantAccessBlocked() {
        // Batch belongs to tenant B
        when(batchRepo.findByTenantIdAndId(tenantA, "batch-belonging-to-tenant-B"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.getBatchById(tenantA, "batch-belonging-to-tenant-B"))
                .isInstanceOf(MedShopException.NotFoundException.class)
                .hasMessageContaining("Batch not found");
    }

    @Test
    @DisplayName("Stock ledger reconciliation detects discrepancy between batch balance and transaction history")
    void testLedgerMismatchDetection() {
        StockBatchRepository.LedgerMismatchProjection mismatch = new StockBatchRepository.LedgerMismatchProjection() {
            @Override public String getBatchId() { return "b-1"; }
            @Override public String getMedicineId() { return "m-1"; }
            @Override public String getBatchNumber() { return "BATCH-101"; }
            @Override public int getQuantityReceived() { return 100; }
            @Override public int getQuantityAvailable() { return 45; } // Corrupted: should be 70
            @Override public int getLedgerSum() { return -30; } // Sold 30
            @Override public int getExpectedAvailable() { return 70; }
        };

        when(batchRepo.findLedgerMismatches(tenantA)).thenReturn(List.of(mismatch));

        List<LedgerMismatchDto> report = stockService.verifyLedger(tenantA);

        assertThat(report).hasSize(1);
        LedgerMismatchDto item = report.get(0);
        assertThat(item.batchNumber()).isEqualTo("BATCH-101");
        assertThat(item.quantityAvailable()).isEqualTo(45);
        assertThat(item.expectedAvailable()).isEqualTo(70);
    }

    @Test
    @DisplayName("Manual stock adjustment updates batch quantity and logs ADJUSTMENT transaction")
    void testStockAdjustment() {
        StockBatch batch = new StockBatch(tenantA, "m-1", "B-101", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6), 50, 40, new BigDecimal("10.00"), new BigDecimal("15.00"));
        when(batchRepo.findByTenantIdAndId(tenantA, "b-1")).thenReturn(Optional.of(batch));
        when(batchRepo.save(any(StockBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Medicine med = new Medicine();
        med.setName("Amoxicillin");
        when(medicineRepo.findByTenantIdAndId(tenantA, "m-1")).thenReturn(Optional.of(med));

        StockAdjustRequest request = new StockAdjustRequest(
                "b-1", 10, StockTransaction.Type.ADJUSTMENT, "Physical stock audit found 10 more units"
        );

        BatchDto response = stockService.adjustStock(tenantA, userId, request, "127.0.0.1");

        assertThat(response.quantityAvailable()).isEqualTo(50);
        // Adjustment delta was +10
        verify(txnRepo).save(argThat(txn ->
                txn.getTransactionType() == StockTransaction.Type.ADJUSTMENT &&
                txn.getQuantity() == 10 &&
                txn.getReason().contains("Physical stock audit")
        ));
    }
}
