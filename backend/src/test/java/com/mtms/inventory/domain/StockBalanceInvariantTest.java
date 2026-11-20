package com.mtms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockBalanceInvariantTest {

    @Test
    @DisplayName("Initial WAC math: inventory value = quantity * unit cost, WAC = unit cost")
    void testInitialWacCalculation() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        BigDecimal qty = new BigDecimal("50.000");
        BigDecimal unitCost = new BigDecimal("24000.000000");

        StockBalance balance = StockBalance.createInitial(entityId, branchId, itemId, qty, unitCost);

        assertThat(balance.getQuantity()).isEqualByComparingTo(new BigDecimal("50.000"));
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo(new BigDecimal("24000.000000"));
        assertThat(balance.getInventoryValue()).isEqualByComparingTo(new BigDecimal("1200000.00"));
    }

    @Test
    @DisplayName("Quantity <= 0 must be rejected on initial creation")
    void testInitialZeroQuantityRejected() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        assertThatThrownBy(() -> StockBalance.createInitial(
                entityId, branchId, itemId, BigDecimal.ZERO, new BigDecimal("10000")
        )).isInstanceOf(InvalidStockQuantityException.class);

        assertThatThrownBy(() -> StockBalance.createInitial(
                entityId, branchId, itemId, new BigDecimal("-1.5"), new BigDecimal("10000")
        )).isInstanceOf(InvalidStockQuantityException.class);
    }

    @Test
    @DisplayName("Negative unit cost must be rejected")
    void testNegativeUnitCostRejected() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        assertThatThrownBy(() -> StockBalance.createInitial(
                entityId, branchId, itemId, new BigDecimal("10.000"), new BigDecimal("-500")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Lot requires non-blank lot code")
    void testLotRequiresLotCode() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        assertThatThrownBy(() -> StockLot.createInitial(
                entityId, branchId, itemId, "", null, new BigDecimal("10")
        )).isInstanceOf(MissingLotCodeException.class);

        assertThatThrownBy(() -> StockLot.createInitial(
                entityId, branchId, itemId, "   ", null, new BigDecimal("10")
        )).isInstanceOf(MissingLotCodeException.class);
    }

    @Test
    @DisplayName("Opening movement signed quantity cannot be zero")
    void testStockMovementNonZeroSignedQuantity() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        assertThatThrownBy(() -> new StockMovement(
                UUID.randomUUID(), entityId, branchId, itemId, null,
                java.time.LocalDate.now(), StockMovementType.OPENING_BALANCE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "MANUAL", UUID.randomUUID(), null, UUID.randomUUID(), java.time.Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
