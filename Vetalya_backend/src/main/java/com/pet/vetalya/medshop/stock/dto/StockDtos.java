package com.pet.vetalya.medshop.stock.dto;

import com.pet.vetalya.medshop.stock.entity.StockTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public final class StockDtos {

    private StockDtos() {}

    public record BatchDto(
            String id,
            String tenantId,
            String medicineId,
            String medicineName,
            String batchNumber,
            LocalDate manufacturingDate,
            LocalDate expiryDate,
            int quantityReceived,
            int quantityAvailable,
            BigDecimal purchasePrice,
            BigDecimal mrp,
            Instant receivedAt
    ) {}

    public record StockReceiveRequest(
            @NotBlank(message = "Medicine ID is required")
            String medicineId,
            @NotBlank(message = "Batch number is required")
            String batchNumber,
            LocalDate manufacturingDate,
            @NotNull(message = "Expiry date is required")
            LocalDate expiryDate,
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity received must be at least 1")
            Integer quantity,
            @NotNull(message = "Purchase price is required")
            @DecimalMin(value = "0.01", message = "Purchase price must be positive")
            BigDecimal purchasePrice,
            @NotNull(message = "MRP is required")
            @DecimalMin(value = "0.01", message = "MRP must be positive")
            BigDecimal mrp,
            String supplierId
    ) {}

    public record UpdateBatchRequest(
            @NotBlank(message = "Batch number is required")
            String batchNumber,
            LocalDate manufacturingDate,
            @NotNull(message = "Expiry date is required")
            LocalDate expiryDate,
            @NotNull(message = "Purchase price is required")
            @DecimalMin(value = "0.01", message = "Purchase price must be positive")
            BigDecimal purchasePrice,
            @NotNull(message = "MRP is required")
            @DecimalMin(value = "0.01", message = "MRP must be positive")
            BigDecimal mrp
    ) {}

    public record StockAdjustRequest(
            @NotBlank(message = "Batch ID is required")
            String batchId,
            @NotNull(message = "Quantity delta is required")
            Integer quantityDelta,
            @NotNull(message = "Adjustment type is required (ADJUSTMENT, DAMAGED, EXPIRY_WRITE_OFF, PURCHASE_RETURN)")
            StockTransaction.Type type,
            @NotBlank(message = "Reason is required")
            String reason
    ) {}

    public record StockTransactionDto(
            String id,
            String tenantId,
            String medicineId,
            String batchId,
            StockTransaction.Type transactionType,
            int quantity,
            String reason,
            String performedBy,
            Instant createdAt
    ) {}

    public record CategoryValuationDto(
            String categoryName,
            BigDecimal totalPurchaseValue,
            BigDecimal totalMrpValue
    ) {}

    public record StockValuationResponse(
            BigDecimal overallPurchaseValue,
            BigDecimal overallMrpValue,
            List<CategoryValuationDto> categories
    ) {}

    public record LedgerMismatchDto(
            String batchId,
            String medicineId,
            String batchNumber,
            int quantityReceived,
            int quantityAvailable,
            int ledgerSum,
            int expectedAvailable
    ) {}
}

