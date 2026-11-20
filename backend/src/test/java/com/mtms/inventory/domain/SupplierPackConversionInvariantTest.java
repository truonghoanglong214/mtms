package com.mtms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierPackConversionInvariantTest {

    private final UUID entityId = UUID.randomUUID();
    private final UUID supplierId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Test
    @DisplayName("Reject non-positive baseUnitsPerPack <= 0")
    void shouldRejectNonPositiveBaseUnits() {
        assertThatThrownBy(() -> new SupplierPackConversion(
                UUID.randomUUID(),
                entityId,
                supplierId,
                itemId,
                "Thùng 24 lon",
                BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1),
                null,
                Instant.now()
        )).isInstanceOf(InvalidConversionRateException.class)
                .hasMessageContaining("Base units per pack must be greater than zero");
    }

    @Test
    @DisplayName("Reject effectiveTo before effectiveFrom")
    void shouldRejectEffectiveToBeforeEffectiveFrom() {
        assertThatThrownBy(() -> new SupplierPackConversion(
                UUID.randomUUID(),
                entityId,
                supplierId,
                itemId,
                "Thùng 24 lon",
                new BigDecimal("24.000"),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 31),
                Instant.now()
        )).isInstanceOf(InvalidConversionRateException.class)
                .hasMessageContaining("cannot be before effectiveFrom");
    }

    @Test
    @DisplayName("Check active status on various business dates")
    void shouldEvaluateActiveStatusCorrectly() {
        SupplierPackConversion conversion = new SupplierPackConversion(
                UUID.randomUUID(),
                entityId,
                supplierId,
                itemId,
                "Thùng 24 lon",
                new BigDecimal("24.000"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Instant.now()
        );

        assertThat(conversion.isActiveOn(LocalDate.of(2025, 12, 31))).isFalse();
        assertThat(conversion.isActiveOn(LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(conversion.isActiveOn(LocalDate.of(2026, 6, 15))).isTrue();
        assertThat(conversion.isActiveOn(LocalDate.of(2026, 12, 31))).isTrue();
        assertThat(conversion.isActiveOn(LocalDate.of(2027, 1, 1))).isFalse();
    }
}
