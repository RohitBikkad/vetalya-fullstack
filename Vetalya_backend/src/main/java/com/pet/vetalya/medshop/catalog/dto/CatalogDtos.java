package com.pet.vetalya.medshop.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CatalogDtos {

    private CatalogDtos() {}

    // ── Category DTOs ──
    public record CategoryDto(
            String id,
            String name,
            String description,
            String parentId,
            List<CategoryDto> children
    ) {}

    public record CreateCategoryRequest(
            @NotBlank(message = "Category name is required")
            String name,
            String description,
            String parentId
    ) {}

    public record UpdateCategoryRequest(
            @NotBlank(message = "Category name is required")
            String name,
            String description,
            String parentId
    ) {}

    // ── Supplier DTOs ──
    public record SupplierDto(
            String id,
            String name,
            String contactPerson,
            String phone,
            String gstin,
            String address,
            boolean active
    ) {}

    public record CreateSupplierRequest(
            @NotBlank(message = "Supplier name is required")
            String name,
            String contactPerson,
            @NotBlank(message = "Phone number is required")
            String phone,
            String gstin,
            String address
    ) {}

    public record UpdateSupplierRequest(
            @NotBlank(message = "Supplier name is required")
            String name,
            String contactPerson,
            @NotBlank(message = "Phone number is required")
            String phone,
            String gstin,
            String address,
            Boolean active
    ) {}

    // ── Medicine DTOs ──
    public record MedicineFullDto(
            String id,
            String name,
            String genericName,
            String hsnCode,
            String categoryId,
            String categoryName,
            String supplierId,
            String supplierName,
            String manufacturer,
            String form,
            String strength,
            BigDecimal mrp,
            BigDecimal purchasePrice,
            BigDecimal gstRate,
            boolean scheduleH,
            boolean active,
            int reorderLevel,
            int availableStock,
            LocalDate nearestExpiry
    ) {}

    // Cashier view: purchase_price and supplier fields are strictly omitted
    public record MedicineCashierDto(
            String id,
            String name,
            String genericName,
            String hsnCode,
            String categoryId,
            String categoryName,
            String manufacturer,
            String form,
            String strength,
            BigDecimal mrp,
            BigDecimal gstRate,
            boolean scheduleH,
            boolean active,
            int availableStock,
            LocalDate nearestExpiry
    ) {}

    public record CreateMedicineRequest(
            @NotBlank(message = "Medicine name is required")
            String name,
            String genericName,
            @NotBlank(message = "HSN code is required")
            @Pattern(regexp = "^(\\d{4}|\\d{6}|\\d{8})$", message = "HSN code must be 4, 6, or 8 digits")
            String hsnCode,
            String categoryId,
            String supplierId,
            String manufacturer,
            String form,
            String strength,
            @NotNull(message = "MRP is required")
            @DecimalMin(value = "0.01", message = "MRP must be positive")
            BigDecimal mrp,
            @NotNull(message = "Purchase price is required")
            @DecimalMin(value = "0.01", message = "Purchase price must be positive")
            BigDecimal purchasePrice,
            @NotNull(message = "GST rate is required")
            BigDecimal gstRate,
            Boolean scheduleH,
            Integer reorderLevel
    ) {}

    public record UpdateMedicineRequest(
            @NotBlank(message = "Medicine name is required")
            String name,
            String genericName,
            @NotBlank(message = "HSN code is required")
            @Pattern(regexp = "^(\\d{4}|\\d{6}|\\d{8})$", message = "HSN code must be 4, 6, or 8 digits")
            String hsnCode,
            String categoryId,
            String supplierId,
            String manufacturer,
            String form,
            String strength,
            @NotNull(message = "MRP is required")
            @DecimalMin(value = "0.01", message = "MRP must be positive")
            BigDecimal mrp,
            @NotNull(message = "Purchase price is required")
            @DecimalMin(value = "0.01", message = "Purchase price must be positive")
            BigDecimal purchasePrice,
            @NotNull(message = "GST rate is required")
            BigDecimal gstRate,
            Boolean scheduleH,
            Boolean active,
            Integer reorderLevel
    ) {}
}

