package com.pet.vetalya.medshop.billing;

import com.pet.vetalya.medshop.billing.calculation.GstTaxCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GstTaxCalculatorTest {

    @Test
    @DisplayName("Intra-state standard tax calculation with exact paisa balancing")
    void testIntraStateTaxCalculation() {
        BigDecimal unitPrice = new BigDecimal("100.00");
        int quantity = 1;
        BigDecimal gstRate = new BigDecimal("18.00"); // 18% GST -> 9% CGST + 9% SGST

        GstTaxCalculator.RowTaxCalculation row = GstTaxCalculator.calculateRow(
                unitPrice, quantity, gstRate, false, true);

        assertThat(row.totalAmount()).isEqualByComparingTo("100.00");
        // taxable = 100 / 1.18 = 84.75
        assertThat(row.taxableValue()).isEqualByComparingTo("84.75");
        assertThat(row.cgstRate()).isEqualByComparingTo("9.00");
        assertThat(row.sgstRate()).isEqualByComparingTo("9.00");
        assertThat(row.igstRate()).isEqualByComparingTo("0.00");

        // Taxable 84.75 + CGST 7.63 + SGST 7.62 = 100.00
        assertThat(row.cgstAmount().add(row.sgstAmount())).isEqualByComparingTo("15.25");
        assertThat(row.taxableValue().add(row.cgstAmount()).add(row.sgstAmount()).add(row.igstAmount()))
                .isEqualByComparingTo(row.totalAmount());
    }

    @Test
    @DisplayName("Inter-state tax calculation with full IGST and zero CGST/SGST")
    void testInterStateTaxCalculation() {
        BigDecimal unitPrice = new BigDecimal("100.00");
        int quantity = 2; // total gross = 200.00
        BigDecimal gstRate = new BigDecimal("12.00"); // 12% IGST

        GstTaxCalculator.RowTaxCalculation row = GstTaxCalculator.calculateRow(
                unitPrice, quantity, gstRate, false, false);

        assertThat(row.totalAmount()).isEqualByComparingTo("200.00");
        assertThat(row.cgstRate()).isEqualByComparingTo("0.00");
        assertThat(row.sgstRate()).isEqualByComparingTo("0.00");
        assertThat(row.igstRate()).isEqualByComparingTo("12.00");
        assertThat(row.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(row.sgstAmount()).isEqualByComparingTo("0.00");

        // taxable = 200 / 1.12 = 178.57
        assertThat(row.taxableValue()).isEqualByComparingTo("178.57");
        assertThat(row.igstAmount()).isEqualByComparingTo("21.43");
        assertThat(row.taxableValue().add(row.igstAmount())).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Composition dealer charges NO tax: taxable equals gross, tax rates and amounts are zero")
    void testCompositionDealerCalculation() {
        BigDecimal unitPrice = new BigDecimal("250.00");
        int quantity = 3;
        BigDecimal gstRate = new BigDecimal("18.00"); // Even if item has 18% rate, composition dealer charges 0

        GstTaxCalculator.RowTaxCalculation row = GstTaxCalculator.calculateRow(
                unitPrice, quantity, gstRate, true, true);

        assertThat(row.totalAmount()).isEqualByComparingTo("750.00");
        assertThat(row.taxableValue()).isEqualByComparingTo("750.00");
        assertThat(row.cgstRate()).isEqualByComparingTo("0.00");
        assertThat(row.sgstRate()).isEqualByComparingTo("0.00");
        assertThat(row.igstRate()).isEqualByComparingTo("0.00");
        assertThat(row.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(row.sgstAmount()).isEqualByComparingTo("0.00");
        assertThat(row.igstAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Odd paisa rounding preserves exact invariant: sum of components equals total amount")
    void testOddPaisaInvariant() {
        // Test many unusual price points to ensure strict paisa balancing invariant never fails
        BigDecimal[] prices = {
                new BigDecimal("33.33"), new BigDecimal("17.49"), new BigDecimal("99.99"),
                new BigDecimal("123.45"), new BigDecimal("0.75"), new BigDecimal("199.95")
        };
        BigDecimal[] rates = {
                new BigDecimal("5.00"), new BigDecimal("12.00"), new BigDecimal("18.00"), new BigDecimal("28.00")
        };

        for (BigDecimal price : prices) {
            for (BigDecimal rate : rates) {
                GstTaxCalculator.RowTaxCalculation intra = GstTaxCalculator.calculateRow(price, 3, rate, false, true);
                assertThat(intra.taxableValue().add(intra.cgstAmount()).add(intra.sgstAmount()).add(intra.igstAmount()))
                        .as("Intra-state invariant for price %s and rate %s", price, rate)
                        .isEqualByComparingTo(intra.totalAmount());

                GstTaxCalculator.RowTaxCalculation inter = GstTaxCalculator.calculateRow(price, 3, rate, false, false);
                assertThat(inter.taxableValue().add(inter.cgstAmount()).add(inter.sgstAmount()).add(inter.igstAmount()))
                        .as("Inter-state invariant for price %s and rate %s", price, rate)
                        .isEqualByComparingTo(inter.totalAmount());
            }
        }
    }

    @Test
    @DisplayName("Invalid parameters throw IllegalArgumentException")
    void testInvalidParameters() {
        assertThatThrownBy(() -> GstTaxCalculator.calculateRow(new BigDecimal("-1.00"), 1, BigDecimal.ZERO, false, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GstTaxCalculator.calculateRow(BigDecimal.TEN, 0, BigDecimal.ZERO, false, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GstTaxCalculator.calculateRow(BigDecimal.TEN, 1, new BigDecimal("-5.00"), false, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

