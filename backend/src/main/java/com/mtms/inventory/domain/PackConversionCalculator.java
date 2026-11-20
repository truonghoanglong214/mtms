package com.mtms.inventory.domain;

import com.mtms.common.decimal.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

/**
 * Technical domain calculator for supplier packaging conversions (TC-023).
 * Ensures exact decimal arithmetic without floating-point inaccuracies.
 */
public final class PackConversionCalculator {

    private PackConversionCalculator() {
    }

    public static PackConversionCalculationResult calculate(
            BigDecimal packQuantity,
            BigDecimal baseUnitsPerPack,
            String packName,
            String baseUnitName
    ) {
        if (packQuantity == null || packQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidConversionRateException("Pack quantity must be strictly positive: " + packQuantity);
        }
        if (baseUnitsPerPack == null || baseUnitsPerPack.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidConversionRateException("Base units per pack must be strictly positive: " + baseUnitsPerPack);
        }
        Objects.requireNonNull(packName, "Pack name must not be null");
        Objects.requireNonNull(baseUnitName, "Base unit name must not be null");

        BigDecimal calculatedBaseQuantity = packQuantity.multiply(baseUnitsPerPack)
                .setScale(MoneyUtils.QUANTITY_SCALE, RoundingMode.HALF_UP);
        BigDecimal conversionSnapshot = baseUnitsPerPack
                .setScale(MoneyUtils.QUANTITY_SCALE, RoundingMode.HALF_UP);
        BigDecimal formattedPackQuantity = packQuantity
                .setScale(MoneyUtils.QUANTITY_SCALE, RoundingMode.HALF_UP);

        String formulaExplanation = String.format(
                Locale.ROOT,
                "%s %s × %s = %s %s",
                formattedPackQuantity.toPlainString(),
                packName.trim(),
                conversionSnapshot.toPlainString(),
                calculatedBaseQuantity.toPlainString(),
                baseUnitName.trim()
        );

        return new PackConversionCalculationResult(
                calculatedBaseQuantity,
                conversionSnapshot,
                formulaExplanation
        );
    }
}
