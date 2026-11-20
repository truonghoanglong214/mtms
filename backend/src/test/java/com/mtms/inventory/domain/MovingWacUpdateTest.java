package com.mtms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovingWacUpdateTest {

    @Test
    @DisplayName("Moving WAC: Initial receipt calculates average unit cost accurately")
    void testInitialStockWac() {
        StockBalance balance = StockBalance.createInitial(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("120.000"), new BigDecimal("25000.000000")
        );

        assertThat(balance.getQuantity()).isEqualByComparingTo("120.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("3000000.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("25000.000000");
    }

    @Test
    @DisplayName("Moving WAC: Secondary receipt recalculates WAC accurately: (old_val + line_total) / (old_qty + received_qty)")
    void testSecondaryReceiptWacRecalculation() {
        // Initial stock: 100 units @ 20,000đ = 2,000,000đ
        StockBalance balance = StockBalance.createInitial(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.000"), new BigDecimal("20000.000000")
        );

        // Receive 100 units @ 30,000đ = 3,000,000đ
        balance.receiveGoods(new BigDecimal("100.000"), new BigDecimal("3000000.00"));

        // New qty = 200, new val = 5,000,000đ, new WAC = 5,000,000 / 200 = 25,000đ
        assertThat(balance.getQuantity()).isEqualByComparingTo("200.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("5000000.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("25000.000000");

        // Receive another 50 units @ 40,000đ = 2,000,000đ
        balance.receiveGoods(new BigDecimal("50.000"), new BigDecimal("2000000.00"));

        // New qty = 250, new val = 7,000,000đ, new WAC = 7,000,000 / 250 = 28,000đ
        assertThat(balance.getQuantity()).isEqualByComparingTo("250.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("7000000.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("28000.000000");
    }

    @Test
    @DisplayName("Moving WAC: Negative received quantity is rejected")
    void testNegativeReceivedQuantity() {
        StockBalance balance = StockBalance.createInitial(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.000"), new BigDecimal("20000.000000")
        );

        assertThatThrownBy(() -> balance.receiveGoods(new BigDecimal("-10.000"), new BigDecimal("200000.00")))
                .isInstanceOf(InvalidStockQuantityException.class)
                .hasMessageContaining("Received quantity must be strictly positive");
    }
}
