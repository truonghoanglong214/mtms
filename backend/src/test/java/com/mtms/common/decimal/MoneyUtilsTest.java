package com.mtms.common.decimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilsTest {

    @Test
    @DisplayName("Should round money amounts to 2 decimal places with HALF_UP")
    void testRoundMoney() {
        BigDecimal unrounded = new BigDecimal("123.456");
        BigDecimal rounded = MoneyUtils.roundMoney(unrounded);

        assertThat(rounded).isEqualByComparingTo("123.46");
        assertThat(rounded.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Null money amount returns ZERO with scale 2")
    void testNullMoney() {
        BigDecimal rounded = MoneyUtils.roundMoney(null);
        assertThat(rounded).isEqualByComparingTo("0.00");
        assertThat(rounded.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should test positivity and non-negativity")
    void testPositiveAndNonNegative() {
        assertThat(MoneyUtils.isPositive(new BigDecimal("0.01"))).isTrue();
        assertThat(MoneyUtils.isPositive(BigDecimal.ZERO)).isFalse();
        assertThat(MoneyUtils.isNonNegative(BigDecimal.ZERO)).isTrue();
        assertThat(MoneyUtils.isNonNegative(new BigDecimal("-0.01"))).isFalse();
    }
}
