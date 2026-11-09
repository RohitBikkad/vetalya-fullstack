package com.pet.vetalya.medshop.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportDtos {

    public record DashboardResponse(
            BigDecimal todaySalesAmount,
            long todayInvoiceCount,
            BigDecimal monthToDateSalesAmount,
            long lowStockCount,
            long expiringBatches30Days,
            long expiringBatches60Days,
            long expiringBatches90Days,
            BigDecimal expiredStockValue,
            List<TopMedicineReportItem> topMedicinesThisMonth
    ) {}

    public record SalesSummaryReportItem(
            String periodOrKey,
            long invoiceCount,
            BigDecimal totalTaxable,
            BigDecimal totalTax,
            BigDecimal totalAmount
    ) {}

    public record TopMedicineReportItem(
            String medicineId,
            String medicineName,
            long totalQuantitySold,
            BigDecimal totalRevenue
    ) {}

    public record ProfitReportResponse(
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal totalRevenue,
            BigDecimal totalCost,
            BigDecimal grossProfit,
            BigDecimal grossProfitMarginPercent
    ) {}

    public record ExpiringBatchReportItem(
            String batchId,
            String medicineId,
            String medicineName,
            String batchNumber,
            LocalDate expiryDate,
            int quantityAvailable,
            BigDecimal purchasePrice,
            BigDecimal mrp,
            BigDecimal totalPurchaseValue,
            BigDecimal totalMrpValue
    ) {}
}

