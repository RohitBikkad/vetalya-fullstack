package com.pet.vetalya.medshop.gst.dto;

import com.pet.vetalya.medshop.gst.entity.GstReturn;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class GstDtos {

    public record GenerateGstReturnRequest(
            @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12")
            int returnMonth,

            @Min(value = 2020, message = "Year must be 2020 or later")
            int returnYear,

            @NotNull(message = "Return type is required")
            GstReturn.Type returnType
    ) {}

    public record MarkFiledRequest(
            @NotBlank(message = "Filing ARN / reference number is required")
            String filedRefNumber
    ) {}

    public record GstReturnResponse(
            String id,
            String tenantId,
            int returnMonth,
            int returnYear,
            GstReturn.Type returnType,
            BigDecimal totalTaxableValue,
            BigDecimal totalCgst,
            BigDecimal totalSgst,
            BigDecimal totalIgst,
            GstReturn.Status status,
            String filedRefNumber,
            Instant filedAt
    ) {}

    public record Gstr1B2bInvoice(
            String invoiceNumber,
            LocalDate invoiceDate,
            String customerName,
            String customerGstin,
            String customerStateCode,
            BigDecimal taxableValue,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalAmount
    ) {}

    public record Gstr1B2csSummary(
            String placeOfSupply,
            BigDecimal gstRate,
            BigDecimal taxableValue,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalAmount
    ) {}

    public record Gstr1HsnSummary(
            String hsnCode,
            String medicineName,
            int totalQuantity,
            BigDecimal totalValue,
            BigDecimal taxableValue,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount
    ) {}

    public record Gstr1ExportResponse(
            GstReturnResponse returnDetails,
            List<Gstr1B2bInvoice> b2bInvoices,
            List<Gstr1B2csSummary> b2csSummary,
            List<Gstr1HsnSummary> hsnSummary
    ) {}
}

