package com.mtms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayableInvariantTest {

    @Test
    @DisplayName("Payable invariant: paidAmount cannot exceed originalAmount")
    void testPaidAmountExceedsOriginal() {
        UUID branchId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        assertThatThrownBy(() -> new Payable(
                UUID.randomUUID(), entityId, branchId, "Vinamilk",
                "PURCHASE_RECEIPT", UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30),
                new BigDecimal("5000000.00"), new BigDecimal("6000000.00"),
                PayableStatus.OPEN, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Paid amount cannot exceed original amount");
    }

    @Test
    @DisplayName("Payable invariant: originalAmount cannot be negative")
    void testOriginalAmountNegative() {
        UUID branchId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        assertThatThrownBy(() -> new Payable(
                UUID.randomUUID(), entityId, branchId, "Vinamilk",
                "PURCHASE_RECEIPT", UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30),
                new BigDecimal("-1000.00"), BigDecimal.ZERO,
                PayableStatus.OPEN, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Original amount cannot be negative");
    }

    @Test
    @DisplayName("Payable calculation: remainingAmount = originalAmount - paidAmount")
    void testRemainingAmountCalculation() {
        UUID branchId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        Payable payable = Payable.createPurchaseReceiptDebt(
                entityId, branchId, "Vinamilk",
                UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30),
                new BigDecimal("5000000.00")
        );

        assertThat(payable.getOriginalAmount()).isEqualByComparingTo("5000000.00");
        assertThat(payable.getPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(payable.getRemainingAmount()).isEqualByComparingTo("5000000.00");

        payable.setPaidAmount(new BigDecimal("2000000.00"));
        assertThat(payable.getRemainingAmount()).isEqualByComparingTo("3000000.00");
    }
}
