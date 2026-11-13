package com.mtms.common.decimal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Technical utility for exact financial and quantity arithmetic.
 * Invariants: Currency is 2 decimal places (or 0 for VND integer bookkeeping),
 * quantity is 3 decimal places, unit costs unrounded until booked.
 */
public final class MoneyUtils {

    public static final int MONEY_SCALE = 2;
    public static final int QUANTITY_SCALE = 3;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE, DEFAULT_ROUNDING);

    private MoneyUtils() {
    }

    public static BigDecimal roundMoney(BigDecimal amount) {
        if (amount == null) {
            return ZERO_MONEY;
        }
        return amount.setScale(MONEY_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal roundQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO.setScale(QUANTITY_SCALE, DEFAULT_ROUNDING);
        }
        return quantity.setScale(QUANTITY_SCALE, DEFAULT_ROUNDING);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNonNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }
}
