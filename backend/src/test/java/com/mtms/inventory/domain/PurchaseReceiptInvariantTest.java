package com.mtms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseReceiptInvariantTest {

    @Test
    @DisplayName("Line invariant: lineNo must be strictly positive")
    void testLineNoInvariant() {
        UUID receiptId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        assertThatThrownBy(() -> new PurchaseReceiptLine(
                UUID.randomUUID(), receiptId, 0, itemId, "Thùng",
                new BigDecimal("1"), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("1000"), new BigDecimal("10000"), null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lineNo must be strictly positive");
    }

    @Test
    @DisplayName("Line invariant: receivedBaseQuantity must be strictly positive")
    void testReceivedBaseQuantityInvariant() {
        UUID receiptId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        assertThatThrownBy(() -> new PurchaseReceiptLine(
                UUID.randomUUID(), receiptId, 1, itemId, "Thùng",
                null, BigDecimal.ONE, BigDecimal.ZERO,
                new BigDecimal("1000"), BigDecimal.ZERO, null, null
        )).isInstanceOf(InvalidReceiptQuantityException.class)
                .hasMessageContaining("Received base quantity must be strictly positive");
    }

    @Test
    @DisplayName("Line invariant: baseUnitCost cannot be negative")
    void testBaseUnitCostNegative() {
        UUID receiptId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        assertThatThrownBy(() -> new PurchaseReceiptLine(
                UUID.randomUUID(), receiptId, 1, itemId, null,
                null, BigDecimal.ONE, new BigDecimal("10"),
                new BigDecimal("-500"), BigDecimal.ZERO, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUnitCost must be non-negative");
    }

    @Test
    @DisplayName("Receipt invariant: Posting an already posted receipt throws ReceiptAlreadyPostedException")
    void testAlreadyPostedInvariant() {
        UUID branchId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        PurchaseReceipt receipt = new PurchaseReceipt(
                UUID.randomUUID(), entityId, branchId, "PNK-202611-0001",
                UUID.randomUUID(), LocalDate.now(), "INV-01",
                new BigDecimal("5000000.00"), PaymentStatus.UNPAID, null,
                ReceiptStatus.DRAFT, "Notes", null, null, null, new ArrayList<>()
        );

        receipt.post(actorId);
        assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.POSTED);
        assertThat(receipt.getPostedBy()).isEqualTo(actorId);
        assertThat(receipt.getPostedAt()).isNotNull();

        assertThatThrownBy(() -> receipt.post(actorId))
                .isInstanceOf(ReceiptAlreadyPostedException.class)
                .hasMessageContaining("already been posted");
    }

    @Test
    @DisplayName("Receipt recalculation: sums line totals correctly")
    void testReceiptTotalCalculation() {
        UUID branchId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        UUID receiptId = UUID.randomUUID();

        PurchaseReceiptLine line1 = new PurchaseReceiptLine(
                UUID.randomUUID(), receiptId, 1, UUID.randomUUID(), "Thùng 24 lon",
                new BigDecimal("5"), new BigDecimal("24"), new BigDecimal("120"),
                new BigDecimal("25000"), new BigDecimal("3000000.00"), "LOT-01", LocalDate.now().plusYears(1)
        );

        PurchaseReceiptLine line2 = new PurchaseReceiptLine(
                UUID.randomUUID(), receiptId, 2, UUID.randomUUID(), null,
                null, BigDecimal.ONE, new BigDecimal("10"),
                new BigDecimal("200000"), new BigDecimal("2000000.00"), null, null
        );

        PurchaseReceipt receipt = new PurchaseReceipt(
                receiptId, entityId, branchId, "PNK-202611-0001",
                UUID.randomUUID(), LocalDate.now(), "INV-01",
                BigDecimal.ZERO, PaymentStatus.UNPAID, null,
                ReceiptStatus.DRAFT, null, null, null, null, List.of(line1, line2)
        );

        receipt.recalculateTotalFromLines();
        assertThat(receipt.getInvoiceTotal()).isEqualByComparingTo("5000000.00");
    }
}
