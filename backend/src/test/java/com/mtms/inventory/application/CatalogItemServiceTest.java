package com.mtms.inventory.application;

import com.mtms.inventory.application.dto.CatalogItemDto;
import com.mtms.inventory.application.dto.CreateCatalogItemCommand;
import com.mtms.inventory.application.dto.UpdateCatalogItemCommand;
import com.mtms.inventory.application.exception.DuplicateItemCodeException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.application.exception.UnitNotFoundException;
import com.mtms.inventory.application.service.CatalogItemService;
import com.mtms.inventory.domain.CatalogItem;
import com.mtms.inventory.domain.CatalogItemRepository;
import com.mtms.inventory.domain.InvalidTrackingInvariantException;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.SupplierPackConversionRepository;
import com.mtms.inventory.domain.Unit;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.domain.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogItemServiceTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private SupplierPackConversionRepository conversionRepository;

    private CatalogItemService service;

    private final UUID entityId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();
    private final Unit unitCan = new Unit(unitId, "CAN", "Lon", UnitDimension.COUNT, Instant.now());

    @BeforeEach
    void setUp() {
        service = new CatalogItemService(catalogItemRepository, unitRepository, conversionRepository);
    }

    @Test
    @DisplayName("Create valid item: returns CatalogItemDto with unitCode and packConversionCount = 0")
    void shouldCreateItemSuccessfully() {
        CreateCatalogItemCommand command = new CreateCatalogItemCommand(
                "ING-MILK-OT",
                "Sữa đặc Ông Thọ",
                unitId,
                ItemKind.INGREDIENT,
                true,
                true
        );

        when(catalogItemRepository.existsByEntityIdAndItemCode(entityId, "ING-MILK-OT")).thenReturn(false);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unitCan));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogItemDto result = service.createItem(entityId, command);

        assertThat(result).isNotNull();
        assertThat(result.itemCode()).isEqualTo("ING-MILK-OT");
        assertThat(result.name()).isEqualTo("Sữa đặc Ông Thọ");
        assertThat(result.baseUnitCode()).isEqualTo("CAN");
        assertThat(result.baseUnitName()).isEqualTo("Lon");
        assertThat(result.packConversionCount()).isEqualTo(0);
        assertThat(result.lotTrackingRequired()).isTrue();
        assertThat(result.expirationTrackingRequired()).isTrue();
    }

    @Test
    @DisplayName("Create item: reject expiration tracking when lot tracking is disabled")
    void shouldRejectInvalidTrackingInvariantOnCreate() {
        CreateCatalogItemCommand command = new CreateCatalogItemCommand(
                "ING-MILK-OT",
                "Sữa đặc Ông Thọ",
                unitId,
                ItemKind.INGREDIENT,
                false, // lot tracking false
                true   // expiration tracking true
        );

        assertThatThrownBy(() -> service.createItem(entityId, command))
                .isInstanceOf(InvalidTrackingInvariantException.class);
    }

    @Test
    @DisplayName("Create item: reject duplicate item code within same entity")
    void shouldRejectDuplicateItemCode() {
        CreateCatalogItemCommand command = new CreateCatalogItemCommand(
                "ING-MILK-OT",
                "Sữa đặc Ông Thọ",
                unitId,
                ItemKind.INGREDIENT,
                true,
                true
        );

        when(catalogItemRepository.existsByEntityIdAndItemCode(entityId, "ING-MILK-OT")).thenReturn(true);

        assertThatThrownBy(() -> service.createItem(entityId, command))
                .isInstanceOf(DuplicateItemCodeException.class)
                .hasMessageContaining("ING-MILK-OT");
    }

    @Test
    @DisplayName("Create item: reject non-existent base unit")
    void shouldRejectNonExistentBaseUnit() {
        CreateCatalogItemCommand command = new CreateCatalogItemCommand(
                "ING-MILK-OT",
                "Sữa đặc Ông Thọ",
                unitId,
                ItemKind.INGREDIENT,
                true,
                true
        );

        when(catalogItemRepository.existsByEntityIdAndItemCode(entityId, "ING-MILK-OT")).thenReturn(false);
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createItem(entityId, command))
                .isInstanceOf(UnitNotFoundException.class);
    }

    @Test
    @DisplayName("Update item: update attributes and recalculate timestamp")
    void shouldUpdateItemSuccessfully() {
        UUID itemId = UUID.randomUUID();
        CatalogItem existing = new CatalogItem(
                itemId,
                entityId,
                "ING-MILK-OT",
                "Sữa đặc Ông Thọ",
                unitId,
                ItemKind.INGREDIENT,
                true,
                true,
                ItemStatus.ACTIVE,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(3600)
        );

        UpdateCatalogItemCommand command = new UpdateCatalogItemCommand(
                "Sữa đặc Ông Thọ (Mẫu mới)",
                ItemKind.INGREDIENT,
                true,
                true,
                ItemStatus.ACTIVE
        );

        when(catalogItemRepository.findByEntityIdAndId(entityId, itemId)).thenReturn(Optional.of(existing));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unitCan));
        when(conversionRepository.countByEntityIdAndItemId(entityId, itemId)).thenReturn(2L);

        CatalogItemDto result = service.updateItem(entityId, itemId, command);

        assertThat(result.name()).isEqualTo("Sữa đặc Ông Thọ (Mẫu mới)");
        assertThat(result.packConversionCount()).isEqualTo(2);
        verify(catalogItemRepository).save(existing);
    }

    @Test
    @DisplayName("Get item by ID: throws ItemNotFoundException when missing")
    void shouldThrowWhenItemNotFound() {
        UUID itemId = UUID.randomUUID();
        when(catalogItemRepository.findByEntityIdAndId(entityId, itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getItemById(entityId, itemId))
                .isInstanceOf(ItemNotFoundException.class);
    }
}
