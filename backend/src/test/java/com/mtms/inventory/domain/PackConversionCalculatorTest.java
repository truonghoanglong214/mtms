package com.mtms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackConversionCalculatorTest {

    @Test
    @DisplayName("TC-023: 1 box = 24 units, receipt 2 boxes -> 48 base units snapshot")
    void tc023_shouldCalculate48BaseUnitsFrom2Boxes() {
        BigDecimal packQuantity = new BigDecimal("2.000");
        BigDecimal baseUnitsPerPack = new BigDecimal("24.000");
        String packName = "Thùng 24 lon";
        String baseUnitName = "Lon";

        PackConversionCalculationResult result = PackConversionCalculator.calculate(
                packQuantity,
                baseUnitsPerPack,
                packName,
                baseUnitName
        );

        assertThat(result.calculatedBaseQuantity()).isEqualByComparingTo("48.000");
        assertThat(result.conversionSnapshot()).isEqualByComparingTo("24.000");
        assertThat(result.formulaExplanation()).isEqualTo("2.000 Thùng 24 lon × 24.000 = 48.000 Lon");
    }

    @Test
    @DisplayName("Fractional quantity and rate conversion math with 3 decimal scale")
    void shouldHandleFractionalPackQuantityCorrectly() {
        BigDecimal packQuantity = new BigDecimal("1.500");
        BigDecimal baseUnitsPerPack = new BigDecimal("10.000");
        String packName = "Gói 10 cái";
        String baseUnitName = "Cái";

        PackConversionCalculationResult result = PackConversionCalculator.calculate(
                packQuantity,
                baseUnitsPerPack,
                packName,
                baseUnitName
        );

        assertThat(result.calculatedBaseQuantity()).isEqualByComparingTo("15.000");
        assertThat(result.conversionSnapshot()).isEqualByComparingTo("10.000");
        assertThat(result.formulaExplanation()).isEqualTo("1.500 Gói 10 cái × 10.000 = 15.000 Cái");
    }

    @Test
    @DisplayName("Reject non-positive pack quantity")
    void shouldRejectNonPositivePackQuantity() {
        assertThatThrownBy(() -> PackConversionCalculator.calculate(
                BigDecimal.ZERO,
                new BigDecimal("24.000"),
                "Thùng",
                "Lon"
        )).isInstanceOf(InvalidConversionRateException.class)
                .hasMessageContaining("Pack quantity must be strictly positive");

        assertThatThrownBy(() -> PackConversionCalculator.calculate(
                new BigDecimal("-2.000"),
                new BigDecimal("24.000"),
                "Thùng",
                "Lon"
        )).isInstanceOf(InvalidConversionRateException.class);
    }

    @Test
    @DisplayName("Reject non-positive base units per pack")
    void shouldRejectNonPositiveBaseUnitsPerPack() {
        assertThatThrownBy(() -> PackConversionCalculator.calculate(
                new BigDecimal("2.000"),
                BigDecimal.ZERO,
                "Thùng",
                "Lon"
        )).isInstanceOf(InvalidConversionRateException.class)
                .hasMessageContaining("Base units per pack must be strictly positive");

        assertThatThrownBy(() -> PackConversionCalculator.calculate(
                new BigDecimal("2.000"),
                new BigDecimal("-24.000"),
                "Thùng",
                "Lon"
        )).isInstanceOf(InvalidConversionRateException.class);
    }
}
