package com.pet.vetalya.medshop.stock.service;

import com.pet.vetalya.medshop.audit.service.StoreAuditService;
import com.pet.vetalya.medshop.catalog.entity.Medicine;
import com.pet.vetalya.medshop.catalog.repository.MedicineRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.stock.dto.StockDtos.*;
import com.pet.vetalya.medshop.stock.entity.StockBatch;
import com.pet.vetalya.medshop.stock.entity.StockTransaction;
import com.pet.vetalya.medshop.stock.repository.StockBatchRepository;
import com.pet.vetalya.medshop.stock.repository.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    private final StockBatchRepository batchRepo;
    private final StockTransactionRepository transactionRepo;
    private final MedicineRepository medicineRepo;
    private final StoreAuditService auditService;
    private final ZoneId zoneId;

    public StockService(
            StockBatchRepository batchRepo,
            StockTransactionRepository transactionRepo,
            MedicineRepository medicineRepo,
            StoreAuditService auditService,
            @Value("${app.zone:Asia/Kolkata}") String zone) {
        this.batchRepo = batchRepo;
        this.transactionRepo = transactionRepo;
        this.medicineRepo = medicineRepo;
        this.auditService = auditService;
        this.zoneId = ZoneId.of(zone);
    }

    @Transactional
    public BatchDto receiveStock(String tenantId, String userId, StockReceiveRequest req, String ipAddress) {
        Medicine med = medicineRepo.findByTenantIdAndId(tenantId, req.medicineId())
                .orElseThrow(() -> new MedShopException.NotFoundException("Medicine not found"));

        LocalDate today = LocalDate.now(zoneId);
        if (req.expiryDate().isBefore(today)) {
            throw new MedShopException.BadRequestException("Cannot receive stock with an expiry date in the past");
        }

        String cleanBatchNumber = req.batchNumber().trim().toUpperCase();
        if (batchRepo.findByTenantIdAndMedicineIdAndBatchNumber(tenantId, req.medicineId(), cleanBatchNumber).isPresent()) {
            throw new MedShopException.ConflictException(
                    "Batch number '" + cleanBatchNumber + "' already exists for this medicine in your store");
        }

        // 1. Create Stock Batch
        StockBatch batch = new StockBatch();
        batch.setTenantId(tenantId);
        batch.setMedicineId(med.getId());
        batch.setBatchNumber(cleanBatchNumber);
        batch.setManufacturingDate(req.manufacturingDate());
        batch.setExpiryDate(req.expiryDate());
        batch.setQuantityReceived(req.quantity());
        batch.setQuantityAvailable(req.quantity());
        batch.setPurchasePrice(req.purchasePrice());
        batch.setMrp(req.mrp());
        batch = batchRepo.save(batch);

        // 2. Write PURCHASE transaction ledger row
        StockTransaction tx = new StockTransaction(
                tenantId, med.getId(), batch.getId(), StockTransaction.Type.PURCHASE,
                req.quantity(), "Initial stock receive", userId);
        transactionRepo.save(tx);

        auditService.record(tenantId, userId, "STOCK_BATCH", batch.getId(), "RECEIVE", null, batch, ipAddress);
        return mapBatch(batch, med.getName());
    }

    @Transactional(readOnly = true)
    public Page<BatchDto> getBatches(
            String tenantId, String medicineId, Integer expiringWithinDays,
            Boolean expired, Boolean hasStock, Pageable pageable) {

        LocalDate today = LocalDate.now(zoneId);
        LocalDate minExpiry = null;
        LocalDate maxExpiry = null;

        if (Boolean.TRUE.equals(expired)) {
            maxExpiry = today.minusDays(1);
        } else if (expiringWithinDays != null && expiringWithinDays > 0) {
            minExpiry = today;
            maxExpiry = today.plusDays(expiringWithinDays);
        }

        return batchRepo.findBatchesFiltered(tenantId, medicineId, minExpiry, maxExpiry, hasStock, pageable)
                .map(b -> {
                    String medName = medicineRepo.findByTenantIdAndId(tenantId, b.getMedicineId())
                            .map(m -> m.getName()).orElse("Unknown");
                    return mapBatch(b, medName);
                });
    }

    @Transactional(readOnly = true)
    public BatchDto getBatchById(String tenantId, String id) {
        StockBatch b = batchRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Batch not found"));
        String medName = medicineRepo.findByTenantIdAndId(tenantId, b.getMedicineId())
                .map(m -> m.getName()).orElse("Unknown");
        return mapBatch(b, medName);
    }

    @Transactional
    public BatchDto updateBatch(String tenantId, String userId, String id, UpdateBatchRequest req, String ipAddress) {
        StockBatch b = batchRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Batch not found"));

        long nonPurchaseTxCount = transactionRepo.countByTenantIdAndBatchIdAndTransactionTypeNot(
                tenantId, id, StockTransaction.Type.PURCHASE);

        if (nonPurchaseTxCount > 0) {
            throw new MedShopException.ConflictException(
                    "Cannot modify batch details: sales or adjustments have already occurred for this batch.");
        }

        b.setBatchNumber(req.batchNumber().trim().toUpperCase());
        b.setManufacturingDate(req.manufacturingDate());
        b.setExpiryDate(req.expiryDate());
        b.setPurchasePrice(req.purchasePrice());
        b.setMrp(req.mrp());

        b = batchRepo.save(b);
        auditService.record(tenantId, userId, "STOCK_BATCH", id, "UPDATE_CORRECTION", null, b, ipAddress);

        String medName = medicineRepo.findByTenantIdAndId(tenantId, b.getMedicineId())
                .map(m -> m.getName()).orElse("Unknown");
        return mapBatch(b, medName);
    }

    @Transactional
    public BatchDto adjustStock(String tenantId, String userId, StockAdjustRequest req, String ipAddress) {
        StockBatch b = batchRepo.findByTenantIdAndId(tenantId, req.batchId())
                .orElseThrow(() -> new MedShopException.NotFoundException("Batch not found"));

        int newQuantity = b.getQuantityAvailable() + req.quantityDelta();
        if (newQuantity < 0) {
            throw new MedShopException.BadRequestException(
                    "Adjustment would result in negative available quantity (available: " + b.getQuantityAvailable() + ")");
        }
        if (newQuantity > b.getQuantityReceived()) {
            throw new MedShopException.BadRequestException(
                    "Adjustment cannot exceed initial quantity received (" + b.getQuantityReceived() + ")");
        }

        b.setQuantityAvailable(newQuantity);
        b = batchRepo.save(b);

        StockTransaction tx = new StockTransaction(
                tenantId, b.getMedicineId(), b.getId(), req.type(),
                req.quantityDelta(), req.reason(), userId);
        transactionRepo.save(tx);

        auditService.record(tenantId, userId, "STOCK_BATCH", b.getId(), "ADJUST",
                "delta=" + req.quantityDelta() + ";reason=" + req.reason(), b, ipAddress);

        String medName = medicineRepo.findByTenantIdAndId(tenantId, b.getMedicineId())
                .map(m -> m.getName()).orElse("Unknown");
        return mapBatch(b, medName);
    }

    @Transactional
    public int writeOffExpired(String tenantId, String userId, String ipAddress) {
        LocalDate today = LocalDate.now(zoneId);
        List<StockBatch> expiredBatches = batchRepo.findExpiredBatchesWithStock(tenantId, today);

        int writtenOffCount = 0;
        for (StockBatch b : expiredBatches) {
            int qty = b.getQuantityAvailable();
            if (qty > 0) {
                b.setQuantityAvailable(0);
                batchRepo.save(b);

                StockTransaction tx = new StockTransaction(
                        tenantId, b.getMedicineId(), b.getId(), StockTransaction.Type.EXPIRY_WRITE_OFF,
                        -qty, "Batch write-off for expired stock (expired: " + b.getExpiryDate() + ")", userId);
                transactionRepo.save(tx);
                writtenOffCount++;
            }
        }

        if (writtenOffCount > 0) {
            auditService.record(tenantId, userId, "STOCK", tenantId, "WRITE_OFF_EXPIRED",
                    null, "Written off " + writtenOffCount + " batches", ipAddress);
        }

        return writtenOffCount;
    }

    @Transactional(readOnly = true)
    public Page<StockTransactionDto> getTransactions(
            String tenantId, String medicineId, String batchId,
            StockTransaction.Type type, Instant from, Instant to, Pageable pageable) {

        return transactionRepo.findFiltered(tenantId, medicineId, batchId, type, from, to, pageable)
                .map(t -> new StockTransactionDto(
                        t.getId(), t.getTenantId(), t.getMedicineId(), t.getBatchId(),
                        t.getTransactionType(), t.getQuantity(), t.getReason(),
                        t.getPerformedBy(), t.getCreatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public List<BatchDto> getExpiringBatches(String tenantId, int days) {
        LocalDate today = LocalDate.now(zoneId);
        LocalDate cutoff = today.plusDays(days);
        return batchRepo.findExpiringBatches(tenantId, today, cutoff).stream()
                .map(b -> {
                    String medName = medicineRepo.findByTenantIdAndId(tenantId, b.getMedicineId())
                            .map(m -> m.getName()).orElse("Unknown");
                    return mapBatch(b, medName);
                }).toList();
    }

    @Transactional(readOnly = true)
    public StockValuationResponse getValuation(String tenantId) {
        List<StockBatchRepository.CategoryValuationProjection> rows = batchRepo.getValuationByCategory(tenantId);
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalMrp = BigDecimal.ZERO;

        List<CategoryValuationDto> list = new ArrayList<>();
        for (StockBatchRepository.CategoryValuationProjection r : rows) {
            BigDecimal pVal = r.getTotalPurchaseValue() != null ? r.getTotalPurchaseValue() : BigDecimal.ZERO;
            BigDecimal mVal = r.getTotalMrpValue() != null ? r.getTotalMrpValue() : BigDecimal.ZERO;
            totalPurchase = totalPurchase.add(pVal);
            totalMrp = totalMrp.add(mVal);
            list.add(new CategoryValuationDto(r.getCategoryName(), pVal, mVal));
        }

        return new StockValuationResponse(totalPurchase, totalMrp, list);
    }

    @Transactional(readOnly = true)
    public List<LedgerMismatchDto> verifyLedger(String tenantId) {
        List<StockBatchRepository.LedgerMismatchProjection> mismatches = batchRepo.findLedgerMismatches(tenantId);
        return mismatches.stream().map(m -> new LedgerMismatchDto(
                m.getBatchId(), m.getMedicineId(), m.getBatchNumber(),
                m.getQuantityReceived(), m.getQuantityAvailable(),
                m.getLedgerSum(), m.getExpectedAvailable()
        )).toList();
    }

    private BatchDto mapBatch(StockBatch b, String medicineName) {
        return new BatchDto(
                b.getId(), b.getTenantId(), b.getMedicineId(), medicineName,
                b.getBatchNumber(), b.getManufacturingDate(), b.getExpiryDate(),
                b.getQuantityReceived(), b.getQuantityAvailable(),
                b.getPurchasePrice(), b.getMrp(), b.getReceivedAt()
        );
    }
}

