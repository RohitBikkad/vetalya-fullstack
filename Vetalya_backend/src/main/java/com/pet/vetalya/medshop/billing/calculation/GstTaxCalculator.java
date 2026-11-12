package com.pet.vetalya.medshop.billing.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure, unit-testable class for Indian GST tax calculations, rounding, and composition dealer rules.
 * All math uses BigDecimal with scale 2 and RoundingMode.HALF_UP.
 */
public final class GstTaxCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private GstTaxCalculator() {}

    public record RowTaxCalculation(
            BigDecimal gross,
            BigDecimal taxableValue,
            BigDecimal cgstRate,
            BigDecimal sgstRate,
            BigDecimal igstRate,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalAmount
    ) {
        public RowTaxCalculation {
            // Strict invariant: sum of taxable value and taxes must exactly equal gross / totalAmount to the paisa
            BigDecimal sum = taxableValue.add(cgstAmount).add(sgstAmount).add(igstAmount);
            if (sum.compareTo(totalAmount) != 0) {
                throw new IllegalStateException("Tax components sum (" + sum + ") does not equal total amount (" + totalAmount + ")");
            }
        }
    }

    public static RowTaxCalculation calculateRow(
            BigDecimal unitPrice,
            int quantity,
            BigDecimal gstRate,
            boolean isCompositionDealer,
            boolean isIntraState) {

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price must be non-negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (gstRate == null || gstRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("GST rate must be non-negative");
        }

        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        // Composition dealers charge NO GST (all rates and tax amounts are zero)
        if (isCompositionDealer || gstRate.compareTo(BigDecimal.ZERO) == 0) {
            return new RowTaxCalculation(
                    gross,
                    gross,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    gross
            );
        }

        // MRP in India is tax-inclusive: taxable_value = gross / (1 + gst_rate / 100)
        BigDecimal divisor = BigDecimal.ONE.add(gstRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        BigDecimal taxableValue = gross.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal totalTax = gross.subtract(taxableValue);

        if (isIntraState) {
            BigDecimal halfRate = gstRate.divide(TWO, 2, RoundingMode.HALF_UP);
            BigDecimal cgst = totalTax.divide(TWO, 2, RoundingMode.HALF_UP);
            BigDecimal sgst = totalTax.subtract(cgst); // Remainder goes to SGST so parts sum exactly to the paisa

            return new RowTaxCalculation(
                    gross,
                    taxableValue,
                    halfRate,
                    halfRate,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    cgst,
                    sgst,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    gross
            );
        } else {
            // Inter-state supply
            return new RowTaxCalculation(
                    gross,
                    taxableValue,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    gstRate.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    totalTax,
                    gross
            );
        }
    }
}

