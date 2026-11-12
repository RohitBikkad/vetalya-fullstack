package com.pet.vetalya.medshop.billing.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.pet.vetalya.medshop.billing.dto.BillingDtos.InvoiceDetailsResponse;
import com.pet.vetalya.medshop.billing.dto.BillingDtos.InvoiceItemResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Component
public class InvoicePdfGenerator {

    public byte[] generateInvoicePdf(InvoiceDetailsResponse inv) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            boolean isComposition = inv.tenant() != null && inv.tenant().compositionDealer();
            String title = isComposition ? "BILL OF SUPPLY" : "TAX INVOICE";

            // Title & Shop Header
            Paragraph header = new Paragraph(title)
                    .setFontSize(18)
                    .setFont(boldFont)
                    .setTextAlignment(TextAlignment.CENTER);
            doc.add(header);

            if (inv.tenant() != null) {
                Paragraph shopInfo = new Paragraph(String.format(
                        "%s\n%s\nPhone: %s | Email: %s\nGSTIN: %s | State Code: %s",
                        inv.tenant().shopName(),
                        inv.tenant().address(),
                        inv.tenant().phone(),
                        inv.tenant().email(),
                        inv.tenant().gstin() != null ? inv.tenant().gstin() : "N/A",
                        inv.tenant().stateCode()
                )).setTextAlignment(TextAlignment.CENTER).setFontSize(10);
                doc.add(shopInfo);
            }

            doc.add(new Paragraph("\n"));

            // Invoice & Customer Info Table
            Table metaTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            metaTable.addCell(new Cell().setBorder(null).add(new Paragraph(String.format(
                    "Invoice No: %s\nDate: %s\nPayment Mode: %s\nStatus: %s",
                    inv.invoiceNumber(),
                    inv.invoiceDate(),
                    inv.paymentMode(),
                    inv.status()
            )).setFontSize(10)));

            String customerText = inv.customer() != null
                    ? String.format("Billed To:\nName: %s\nPhone: %s\nGSTIN: %s\nState Code: %s",
                        inv.customer().name(), inv.customer().phone(),
                        inv.customer().gstin() != null ? inv.customer().gstin() : "N/A",
                        inv.customer().stateCode() != null ? inv.customer().stateCode() : "N/A")
                    : "Billed To: Walk-in Customer";

            metaTable.addCell(new Cell().setBorder(null).add(new Paragraph(customerText).setFontSize(10)));
            doc.add(metaTable);

            doc.add(new Paragraph("\n"));

            // Items Table
            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{5, 25, 10, 10, 10, 8, 11, 10, 11})).useAllAvailableWidth();
            itemTable.addHeaderCell(headerCell("#", boldFont));
            itemTable.addHeaderCell(headerCell("Medicine", boldFont));
            itemTable.addHeaderCell(headerCell("HSN", boldFont));
            itemTable.addHeaderCell(headerCell("Batch", boldFont));
            itemTable.addHeaderCell(headerCell("Expiry", boldFont));
            itemTable.addHeaderCell(headerCell("Qty", boldFont));
            itemTable.addHeaderCell(headerCell("Unit MRP", boldFont));
            itemTable.addHeaderCell(headerCell("Tax %", boldFont));
            itemTable.addHeaderCell(headerCell("Total", boldFont));

            int idx = 1;
            for (InvoiceItemResponse item : inv.items()) {
                itemTable.addCell(bodyCell(String.valueOf(idx++)));
                itemTable.addCell(bodyCell(item.medicineName()));
                itemTable.addCell(bodyCell(item.hsnCode()));
                itemTable.addCell(bodyCell(item.batchNumber()));
                itemTable.addCell(bodyCell(item.expiryDate() != null ? item.expiryDate().toString() : "-"));
                itemTable.addCell(bodyCell(String.valueOf(item.quantity())));
                itemTable.addCell(bodyCell("₹" + item.unitPrice().toPlainString()));

                BigDecimal effectiveRate = item.igstRate().compareTo(BigDecimal.ZERO) > 0
                        ? item.igstRate()
                        : item.cgstRate().add(item.sgstRate());
                itemTable.addCell(bodyCell(effectiveRate.toPlainString() + "%"));
                itemTable.addCell(bodyCell("₹" + item.totalAmount().toPlainString()));
            }

            doc.add(itemTable);
            doc.add(new Paragraph("\n"));

            // Summary Table
            Table sumTable = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
            sumTable.addCell(new Cell().setBorder(null).add(new Paragraph(
                    "Amount in words:\n" + convertToIndianWords(inv.totalAmount())
            ).setFontSize(10).setFont(italicFont)));

            Paragraph totalsPara = new Paragraph(String.format(
                    "Taxable Subtotal: ₹%s\nCGST: ₹%s\nSGST: ₹%s\nIGST: ₹%s\nGrand Total: ₹%s",
                    inv.subtotal().toPlainString(),
                    inv.cgstAmount().toPlainString(),
                    inv.sgstAmount().toPlainString(),
                    inv.igstAmount().toPlainString(),
                    inv.totalAmount().toPlainString()
            )).setTextAlignment(TextAlignment.RIGHT).setFont(boldFont).setFontSize(10);

            sumTable.addCell(new Cell().setBorder(null).add(totalsPara));
            doc.add(sumTable);

            // Footer note
            doc.add(new Paragraph("\nThank you for choosing MedShop! This is a computer generated invoice.")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate invoice PDF", e);
        }
    }

    private Cell headerCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(9))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
    }

    private Cell bodyCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(8));
    }

    public static String convertToIndianWords(BigDecimal amount) {
        if (amount == null) return "Zero Rupees Only";
        long rupees = amount.longValue();
        int paise = amount.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();

        String words = "Rupees " + convertNumberToWords(rupees);
        if (paise > 0) {
            words += " and " + convertNumberToWords(paise) + " Paise";
        }
        return words + " Only";
    }

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static String convertNumberToWords(long n) {
        if (n == 0) return "Zero";
        if (n < 0) return "Minus " + convertNumberToWords(-n);

        if (n < 20) return UNITS[(int) n];
        if (n < 100) return TENS[(int) (n / 10)] + ((n % 10 != 0) ? " " + UNITS[(int) (n % 10)] : "");
        if (n < 1000) return UNITS[(int) (n / 100)] + " Hundred" + ((n % 100 != 0) ? " " + convertNumberToWords(n % 100) : "");
        if (n < 100000) return convertNumberToWords(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " + convertNumberToWords(n % 1000) : "");
        if (n < 10000000) return convertNumberToWords(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " + convertNumberToWords(n % 100000) : "");
        return convertNumberToWords(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " + convertNumberToWords(n % 10000000) : "");
    }
}
