package com.mtms.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogItemInvariantTest {

    private final UUID entityId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();

    @Test
    @DisplayName("Invariant: expirationTrackingRequired=true without lotTrackingRequired=true must throw InvalidTrackingInvariantException")
    void shouldRejectExpirationTrackingWithoutLotTracking() {
        assertThatThrownBy(() -> new CatalogItem(
                UUID.randomUUID(),
                entityId,
                "ING-MILK-OT",
                "Sữa đặc",
                unitId,
                ItemKind.INGREDIENT,
                false, // lotTrackingRequired = false
                true,  // expirationTrackingRequired = true
                ItemStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        )).isInstanceOf(InvalidTrackingInvariantException.class)
                .hasMessageContaining("Expiration tracking requires lot tracking to be enabled");
    }

    @Test
    @DisplayName("Valid item: both lot and expiration tracking enabled succeeds")
    void shouldAllowBothLotAndExpirationTracking() {
        CatalogItem item = new CatalogItem(
                UUID.randomUUID(),
                entityId,
                "ING-MILK-OT",
                "Sữa đặc Ông Thọ",
                unitId,
                ItemKind.INGREDIENT,
                true,
                true,
                ItemStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        assertThat(item.isLotTrackingRequired()).isTrue();
        assertThat(item.isExpirationTrackingRequired()).isTrue();
    }

    @Test
    @DisplayName("Valid item: lot tracking enabled without expiration tracking succeeds")
    void shouldAllowLotTrackingWithoutExpirationTracking() {
        CatalogItem item = new CatalogItem(
                UUID.randomUUID(),
                entityId,
                "ING-COFFEE-01",
                "Cà phê hạt Robusta",
                unitId,
                ItemKind.INGREDIENT,
                true,
                false,
                ItemStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        assertThat(item.isLotTrackingRequired()).isTrue();
        assertThat(item.isExpirationTrackingRequired()).isFalse();
    }

    @Test
    @DisplayName("Update item: turning on expiration without lot tracking is rejected")
    void shouldRejectUpdateWithInvalidInvariant() {
        CatalogItem item = new CatalogItem(
                UUID.randomUUID(),
                entityId,
                "ING-CUP-PAPER",
                "Ly giấy 500ml",
                unitId,
                ItemKind.CONSUMABLE,
                false,
                false,
                ItemStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        assertThatThrownBy(() -> item.update("Ly giấy 500ml (New)", ItemKind.CONSUMABLE, false, true, ItemStatus.ACTIVE))
                .isInstanceOf(InvalidTrackingInvariantException.class);
    }
}
