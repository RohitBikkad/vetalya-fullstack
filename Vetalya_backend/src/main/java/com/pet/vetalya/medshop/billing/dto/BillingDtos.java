package com.pet.vetalya.medshop.billing.dto;

import com.pet.vetalya.medshop.billing.entity.SalesInvoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public class BillingDtos {

    public record CreateInvoiceItemRequest(
            @NotBlank(message = "Medicine ID is required")
            String medicineId,

            String batchId, // Optional; if omitted, FEFO is applied

            @Positive(message = "Quantity must be greater than zero")
            int quantity,

            @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
            BigDecimal unitPrice, // Optional; defaults to batch MRP if omitted

            @DecimalMin(value = "0.00", message = "Discount percent cannot be negative")
            @DecimalMax(value = "100.00", message = "Discount percent cannot exceed 100")
            BigDecimal discountPercent
    ) {}

    public record CreateInvoiceRequest(
            String customerId, // Optional; null for walk-in customer

            String customerName,
            String customerPhone,

            String doctorName,
            String doctorRegistrationNo,
            Integer patientAge,
            LocalDate prescriptionDate,

            @NotNull(message = "Payment mode is required")
            SalesInvoice.PaymentMode paymentMode,

            @NotEmpty(message = "Invoice must contain at least one item")
            @Valid
            List<CreateInvoiceItemRequest> items
    ) {}

    public record CancelInvoiceRequest(
            @NotBlank(message = "Cancellation reason is required")
            @Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
            String reason
    ) {}

    public record InvoiceItemResponse(
            String id,
            String medicineId,
            String medicineName,
            String hsnCode,
            String batchId,
            String batchNumber,
            LocalDate expiryDate,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal mrp,
            BigDecimal taxableValue,
            BigDecimal cgstRate,
            BigDecimal cgstAmount,
            BigDecimal sgstRate,
            BigDecimal sgstAmount,
            BigDecimal igstRate,
            BigDecimal igstAmount,
            BigDecimal totalAmount,
            boolean isScheduleH
    ) {}

    public record InvoiceResponse(
            String id,
            String invoiceNumber,
            LocalDate invoiceDate,
            String customerId,
            String customerName,
            String customerPhone,
            String customerGstin,
            String customerStateCode,
            BigDecimal subtotal,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalAmount,
            SalesInvoice.PaymentMode paymentMode,
            SalesInvoice.Status status,
            String createdBy,
            Instant createdAt,
            List<InvoiceItemResponse> items
    ) {}

    public record TenantPdfInfo(
            String shopName,
            String address,
            String phone,
            String email,
            String gstin,
            String stateCode,
            boolean compositionDealer
    ) {}

    public record CustomerPdfInfo(
            String name,
            String phone,
            String gstin,
            String stateCode
    ) {}

    public record InvoiceDetailsResponse(
            TenantPdfInfo tenant,
            CustomerPdfInfo customer,
            String invoiceNumber,
            LocalDate invoiceDate,
            SalesInvoice.PaymentMode paymentMode,
            SalesInvoice.Status status,
            List<InvoiceItemResponse> items,
            BigDecimal subtotal,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalAmount
    ) {}

    public record ScheduleHRegisterResponse(
            String id,
            String invoiceId,
            String invoiceNumber,
            String medicineId,
            String medicineName,
            String batchNumber,
            String patientName,
            Integer patientAge,
            String doctorName,
            String doctorRegistrationNo,
            LocalDate prescriptionDate,
            int quantity,
            Instant createdAt
    ) {}
}
