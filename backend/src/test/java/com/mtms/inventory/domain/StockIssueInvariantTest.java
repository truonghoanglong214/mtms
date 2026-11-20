package com.mtms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-02 / BR-INV-03 / TC-020 / TC-021
 * Unit tests for stock issue Moving WAC valuation, negative balance rejection, and document lifecycle.
 */
class StockIssueInvariantTest {

    @Test
    @DisplayName("TC-020: 10kg@100k + 10kg@120k -> WAC=110k, issue 5kg -> issue value=550k, remaining WAC=110k")
    void testTc020MovingWacValuationOnIssue() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        // 1. Initial Opening Stock: 10kg @ 100,000đ = 1,000,000đ
        StockBalance balance = StockBalance.createInitial(
                entityId, branchId, itemId,
                new BigDecimal("10.000"),
                new BigDecimal("100000.000000")
        );

        assertThat(balance.getQuantity()).isEqualByComparingTo("10.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("1000000.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("100000.000000");

        // 2. Goods Receipt: 10kg @ 120,000đ = 1,200,000đ
        balance.receiveGoods(new BigDecimal("10.000"), new BigDecimal("1200000.00"));

        // Combined: 20kg, 2,200,000đ, WAC = 110,000đ/kg
        assertThat(balance.getQuantity()).isEqualByComparingTo("20.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("2200000.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("110000.000000");

        // 3. Issue 5kg at current Moving WAC (110,000đ)
        BigDecimal bookedTotal = balance.issueStock(new BigDecimal("5.000"));

        // Issue value: 5.000 * 110,000 = 550,000.00đ
        assertThat(bookedTotal).isEqualByComparingTo("550000.00");

        // Remaining balance: 15.000kg, Value: 1,650,000.00đ, WAC: 110,000đ/kg (WAC invariant preserved!)
        assertThat(balance.getQuantity()).isEqualByComparingTo("15.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("1650000.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("110000.000000");

        // 4. Sequential issue: Issue another 5kg
        BigDecimal secondBookedTotal = balance.issueStock(new BigDecimal("5.000"));
        assertThat(secondBookedTotal).isEqualByComparingTo("550000.00");
        assertThat(balance.getQuantity()).isEqualByComparingTo("10.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("1100000.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("110000.000000");

        // 5. Issue remaining 10kg to zero
        BigDecimal finalBookedTotal = balance.issueStock(new BigDecimal("10.000"));
        assertThat(finalBookedTotal).isEqualByComparingTo("1100000.00");
        assertThat(balance.getQuantity()).isEqualByComparingTo("0.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("0.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("110000.000000");
    }

    @Test
    @DisplayName("TC-021: Request issue quantity > on-hand balance -> Rejected with InsufficientStockException, balance untouched")
    void testTc021RejectOverdraftIssue() {
        StockBalance balance = StockBalance.createInitial(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("4.000"),
                new BigDecimal("50000.000000")
        );

        // Current balance is 4kg, requesting 5kg -> must throw InsufficientStockException
        assertThatThrownBy(() -> balance.issueStock(new BigDecimal("5.000")))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock balance. Requested: 5.000, Available: 4.000");

        // Verify balance state is completely unchanged
        assertThat(balance.getQuantity()).isEqualByComparingTo("4.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("200000.00");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("50000.000000");
    }

    @Test
    @DisplayName("Issue stock: Zero or negative issue quantity is rejected")
    void testZeroOrNegativeIssueQuantity() {
        StockBalance balance = StockBalance.createInitial(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.000"),
                new BigDecimal("50000.000000")
        );

        assertThatThrownBy(() -> balance.issueStock(BigDecimal.ZERO))
                .isInstanceOf(InvalidIssueQuantityException.class);

        assertThatThrownBy(() -> balance.issueStock(new BigDecimal("-1.500")))
                .isInstanceOf(InvalidIssueQuantityException.class);
    }

    @Test
    @DisplayName("StockDocument Lifecycle: Cannot post or cancel already finalized document")
    void testStockDocumentLifecycleInvariants() {
        UUID docId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        StockDocument doc = new StockDocument(
                docId, entityId, branchId, "PXK-202611-0001",
                StockDocumentType.CONSUMPTION_ISSUE,
                LocalDate.now(),
                StockDocumentStatus.DRAFT,
                "Xuất dùng",
                1,
                new BigDecimal("5.000"),
                BigDecimal.ZERO,
                userId, null, null, null,
                new ArrayList<>()
        );

        assertThat(doc.getStatus()).isEqualTo(StockDocumentStatus.DRAFT);

        // Post the document
        doc.post(userId, new BigDecimal("550000.00"), new BigDecimal("5.000"), 1);
        assertThat(doc.getStatus()).isEqualTo(StockDocumentStatus.POSTED);
        assertThat(doc.getApprovedBy()).isEqualTo(userId);
        assertThat(doc.getPostedAt()).isNotNull();

        // Cannot post again
        assertThatThrownBy(() -> doc.post(userId, new BigDecimal("550000.00"), new BigDecimal("5.000"), 1))
                .isInstanceOf(StockDocumentAlreadyPostedException.class);

        // Cannot cancel
        assertThatThrownBy(doc::cancel)
                .isInstanceOf(StockDocumentAlreadyPostedException.class);
    }
}
