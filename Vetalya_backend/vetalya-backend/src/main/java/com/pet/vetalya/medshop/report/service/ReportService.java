package com.pet.vetalya.medshop.report.service;

import com.pet.vetalya.medshop.billing.repository.SalesInvoiceRepository;
import com.pet.vetalya.medshop.catalog.entity.Medicine;
import com.pet.vetalya.medshop.catalog.repository.MedicineRepository;
import com.pet.vetalya.medshop.common.TenantContext;
import com.pet.vetalya.medshop.report.dto.ReportDtos.*;
import com.pet.vetalya.medshop.stock.entity.StockBatch;
import com.pet.vetalya.medshop.stock.repository.StockBatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final SalesInvoiceRepository salesInvoiceRepo;
    private final StockBatchRepository stockBatchRepo;
    private final MedicineRepository medicineRepo;

    @Value("${app.zone:Asia/Kolkata}")
    private String zoneId;

    public ReportService(SalesInvoiceRepository salesInvoiceRepo,
                         StockBatchRepository stockBatchRepo,
                         MedicineRepository medicineRepo) {
        this.salesInvoiceRepo = salesInvoiceRepo;
        this.stockBatchRepo = stockBatchRepo;
        this.medicineRepo = medicineRepo;
    }

    public DashboardResponse getDashboard() {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now(ZoneId.of(zoneId));
        LocalDate firstOfMonth = today.withDayOfMonth(1);

        BigDecimal todaySales = salesInvoiceRepo.getTodaySalesAmount(tenantId, today);
        long todayCount = salesInvoiceRepo.getTodayInvoiceCount(tenantId, today);
        BigDecimal mtdSales = salesInvoiceRepo.getSalesAmountBetween(tenantId, firstOfMonth, today);

        long lowStockCount = medicineRepo.findLowStockMedicines(tenantId, today).size();

        long expiring30 = stockBatchRepo.findExpiringBatches(tenantId, today, today.plusDays(30)).size();
        long expiring60 = stockBatchRepo.findExpiringBatches(tenantId, today, today.plusDays(60)).size();
        long expiring90 = stockBatchRepo.findExpiringBatches(tenantId, today, today.plusDays(90)).size();

        List<StockBatch> expiredBatches = stockBatchRepo.findExpiredBatchesWithStock(tenantId, today);
        BigDecimal expiredStockValue = expiredBatches.stream()
                .map(b -> b.getPurchasePrice().multiply(BigDecimal.valueOf(b.getQuantityAvailable())))
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        List<SalesInvoiceRepository.TopMedicineProjection> topProjections =
                salesInvoiceRepo.getTopMedicines(tenantId, firstOfMonth, today, 5);

        List<TopMedicineReportItem> top5 = topProjections.stream()
                .map(p -> new TopMedicineReportItem(p.getMedicineId(), p.getMedicineName(), p.getTotalQuantitySold(), p.getTotalRevenue()))
                .collect(Collectors.toList());

        return new DashboardResponse(
                todaySales != null ? todaySales : BigDecimal.ZERO,
                todayCount,
                mtdSales != null ? mtdSales : BigDecimal.ZERO,
                lowStockCount,
                expiring30,
                expiring60,
                expiring90,
                expiredStockValue,
                top5
        );
    }

    public List<SalesSummaryReportItem> getSalesReport(LocalDate from, LocalDate to, String groupBy) {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now(ZoneId.of(zoneId));
        LocalDate effectiveFrom = from != null ? from : today.minusDays(30);
        LocalDate effectiveTo = to != null ? to : today;

        if ("paymentMode".equalsIgnoreCase(groupBy)) {
            return salesInvoiceRepo.getSalesGroupedByPaymentMode(tenantId, effectiveFrom, effectiveTo).stream()
                    .map(p -> new SalesSummaryReportItem(p.getGroupKey(), p.getInvoiceCount(), p.getTotalTaxable(), p.getTotalTax(), p.getTotalAmount()))
                    .collect(Collectors.toList());
        } else {
            return salesInvoiceRepo.getSalesGroupedByDay(tenantId, effectiveFrom, effectiveTo).stream()
                    .map(p -> new SalesSummaryReportItem(p.getGroupKey(), p.getInvoiceCount(), p.getTotalTaxable(), p.getTotalTax(), p.getTotalAmount()))
                    .collect(Collectors.toList());
        }
    }

    public List<TopMedicineReportItem> getTopMedicinesReport(LocalDate from, LocalDate to, int limit) {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now(ZoneId.of(zoneId));
        LocalDate effectiveFrom = from != null ? from : today.minusDays(30);
        LocalDate effectiveTo = to != null ? to : today;
        int effectiveLimit = limit > 0 ? limit : 10;

        return salesInvoiceRepo.getTopMedicines(tenantId, effectiveFrom, effectiveTo, effectiveLimit).stream()
                .map(p -> new TopMedicineReportItem(p.getMedicineId(), p.getMedicineName(), p.getTotalQuantitySold(), p.getTotalRevenue()))
                .collect(Collectors.toList());
    }

    public ProfitReportResponse getProfitReport(LocalDate from, LocalDate to) {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now(ZoneId.of(zoneId));
        LocalDate effectiveFrom = from != null ? from : today.minusDays(30);
        LocalDate effectiveTo = to != null ? to : today;

        SalesInvoiceRepository.ProfitReportProjection p = salesInvoiceRepo.getProfitReport(tenantId, effectiveFrom, effectiveTo);

        BigDecimal revenue = (p != null && p.getTotalRevenue() != null) ? p.getTotalRevenue() : BigDecimal.ZERO;
        BigDecimal cost = (p != null && p.getTotalCost() != null) ? p.getTotalCost() : BigDecimal.ZERO;
        BigDecimal grossProfit = (p != null && p.getGrossProfit() != null) ? p.getGrossProfit() : revenue.subtract(cost);

        BigDecimal margin = BigDecimal.ZERO;
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            margin = grossProfit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }

        return new ProfitReportResponse(effectiveFrom, effectiveTo, revenue, cost, grossProfit, margin);
    }

    public List<ExpiringBatchReportItem> getExpiryReport(int days) {
        String tenantId = TenantContext.getTenantId();
        LocalDate today = LocalDate.now(ZoneId.of(zoneId));
        int effectiveDays = days > 0 ? days : 30;
        LocalDate cutoff = today.plusDays(effectiveDays);

        List<StockBatch> batches = stockBatchRepo.findExpiringBatches(tenantId, today, cutoff);
        Map<String, String> medNames = new HashMap<>();

        return batches.stream().map(b -> {
            String medName = medNames.computeIfAbsent(b.getMedicineId(),
                    mid -> medicineRepo.findByTenantIdAndId(tenantId, mid).map(m -> m.getName()).orElse("Unknown"));

            BigDecimal qty = BigDecimal.valueOf(b.getQuantityAvailable());
            BigDecimal purchaseVal = b.getPurchasePrice().multiply(qty);
            BigDecimal mrpVal = b.getMrp().multiply(qty);

            return new ExpiringBatchReportItem(
                    b.getId(),
                    b.getMedicineId(),
                    medName,
                    b.getBatchNumber(),
                    b.getExpiryDate(),
                    b.getQuantityAvailable(),
                    b.getPurchasePrice(),
                    b.getMrp(),
                    purchaseVal,
                    mrpVal
            );
        }).collect(Collectors.toList());
    }
}

