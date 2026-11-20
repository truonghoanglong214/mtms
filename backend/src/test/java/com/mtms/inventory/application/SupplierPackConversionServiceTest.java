package com.mtms.inventory.application;

import com.mtms.inventory.application.dto.CreateSupplierPackConversionCommand;
import com.mtms.inventory.application.dto.PackConversionCalculationRequest;
import com.mtms.inventory.application.dto.PackConversionCalculationResponse;
import com.mtms.inventory.application.dto.SupplierPackConversionDto;
import com.mtms.inventory.application.exception.ConversionNotFoundException;
import com.mtms.inventory.application.exception.DuplicateConversionException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.application.exception.SupplierNotFoundException;
import com.mtms.inventory.application.service.SupplierPackConversionService;
import com.mtms.inventory.domain.CatalogItem;
import com.mtms.inventory.domain.CatalogItemRepository;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.Supplier;
import com.mtms.inventory.domain.SupplierPackConversion;
import com.mtms.inventory.domain.SupplierPackConversionRepository;
import com.mtms.inventory.domain.SupplierRepository;
import com.mtms.inventory.domain.SupplierStatus;
import com.mtms.inventory.domain.Unit;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.domain.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierPackConversionServiceTest {

    @Mock
    private SupplierPackConversionRepository conversionRepository;

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private UnitRepository unitRepository;

    private SupplierPackConversionService service;

    private final UUID entityId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID supplierId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();

    private final Unit unitCan = new Unit(unitId, "CAN", "Lon", UnitDimension.COUNT, Instant.now());
    private final Supplier supplierVinamilk = new Supplier(
            supplierId, entityId, "SUP-VINAMILK", "Công ty Vinamilk", "Hotline 1900", SupplierStatus.ACTIVE, Instant.now()
    );
    private final CatalogItem itemMilk = new CatalogItem(
            itemId, entityId, "ING-MILK-OT", "Sữa đặc Ông Thọ", unitId, ItemKind.INGREDIENT, true, true, ItemStatus.ACTIVE, Instant.now(), Instant.now()
    );

    @BeforeEach
    void setUp() {
        service = new SupplierPackConversionService(
                conversionRepository,
                catalogItemRepository,
                supplierRepository,
                unitRepository
        );
    }

    @Test
    @DisplayName("Create valid pack conversion: returns DTO with supplier name and item code")
    void shouldCreatePackConversionSuccessfully() {
        CreateSupplierPackConversionCommand command = new CreateSupplierPackConversionCommand(
                supplierId,
                "Thùng 24 lon",
                new BigDecimal("24.000"),
                LocalDate.of(2026, 1, 1),
                null
        );

        when(catalogItemRepository.findByEntityIdAndId(entityId, itemId)).thenReturn(Optional.of(itemMilk));
        when(supplierRepository.findByEntityIdAndId(entityId, supplierId)).thenReturn(Optional.of(supplierVinamilk));
        when(conversionRepository.existsBySupplierIdAndItemIdAndPackNameAndEffectiveFrom(
                supplierId, itemId, "Thùng 24 lon", LocalDate.of(2026, 1, 1)
        )).thenReturn(false);
        when(conversionRepository.save(any(SupplierPackConversion.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierPackConversionDto dto = service.createPackConversion(entityId, itemId, command);

        assertThat(dto).isNotNull();
        assertThat(dto.packName()).isEqualTo("Thùng 24 lon");
        assertThat(dto.baseUnitsPerPack()).isEqualByComparingTo("24.000");
        assertThat(dto.supplierName()).isEqualTo("Công ty Vinamilk");
        assertThat(dto.itemCode()).isEqualTo("ING-MILK-OT");
    }

    @Test
    @DisplayName("Create pack conversion: reject duplicate (supplierId, itemId, packName, effectiveFrom)")
    void shouldRejectDuplicateConversion() {
        CreateSupplierPackConversionCommand command = new CreateSupplierPackConversionCommand(
                supplierId,
                "Thùng 24 lon",
                new BigDecimal("24.000"),
                LocalDate.of(2026, 1, 1),
                null
        );

        when(catalogItemRepository.findByEntityIdAndId(entityId, itemId)).thenReturn(Optional.of(itemMilk));
        when(supplierRepository.findByEntityIdAndId(entityId, supplierId)).thenReturn(Optional.of(supplierVinamilk));
        when(conversionRepository.existsBySupplierIdAndItemIdAndPackNameAndEffectiveFrom(
                supplierId, itemId, "Thùng 24 lon", LocalDate.of(2026, 1, 1)
        )).thenReturn(true);

        assertThatThrownBy(() -> service.createPackConversion(entityId, itemId, command))
                .isInstanceOf(DuplicateConversionException.class)
                .hasMessageContaining("Thùng 24 lon");
    }

    @Test
    @DisplayName("TC-023 calculation: 2 boxes of 24 cans -> 48 cans snapshot and formula")
    void shouldCalculateTC023SnapshotCorrectly() {
        PackConversionCalculationRequest request = new PackConversionCalculationRequest(
                itemId,
                supplierId,
                new BigDecimal("2.000"),
                LocalDate.of(2026, 11, 16)
        );

        SupplierPackConversion conversion = new SupplierPackConversion(
                UUID.randomUUID(),
                entityId,
                supplierId,
                itemId,
                "Thùng 24 lon",
                new BigDecimal("24.000"),
                LocalDate.of(2026, 1, 1),
                null,
                Instant.now()
        );

        when(catalogItemRepository.findByEntityIdAndId(entityId, itemId)).thenReturn(Optional.of(itemMilk));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unitCan));
        when(conversionRepository.findActiveConversion(entityId, itemId, supplierId, LocalDate.of(2026, 11, 16)))
                .thenReturn(Optional.of(conversion));
        when(supplierRepository.findByEntityIdAndId(entityId, supplierId)).thenReturn(Optional.of(supplierVinamilk));

        PackConversionCalculationResponse response = service.calculatePackConversion(entityId, request);

        assertThat(response).isNotNull();
        assertThat(response.itemCode()).isEqualTo("ING-MILK-OT");
        assertThat(response.supplierName()).isEqualTo("Công ty Vinamilk");
        assertThat(response.packName()).isEqualTo("Thùng 24 lon");
        assertThat(response.packQuantity()).isEqualByComparingTo("2.000");
        assertThat(response.baseUnitsPerPack()).isEqualByComparingTo("24.000");
        assertThat(response.baseUnitCode()).isEqualTo("CAN");
        assertThat(response.baseUnitName()).isEqualTo("Lon");
        assertThat(response.calculatedBaseQuantity()).isEqualByComparingTo("48.000");
        assertThat(response.conversionSnapshot()).isEqualByComparingTo("24.000");
        assertThat(response.formulaExplanation()).isEqualTo("2.000 Thùng 24 lon × 24.000 = 48.000 Lon");
    }

    @Test
    @DisplayName("Calculate pack conversion: throws ConversionNotFoundException when no active rate exists")
    void shouldThrowWhenNoActiveConversionFound() {
        PackConversionCalculationRequest request = new PackConversionCalculationRequest(
                itemId,
                supplierId,
                new BigDecimal("2.000"),
                LocalDate.of(2026, 11, 16)
        );

        when(catalogItemRepository.findByEntityIdAndId(entityId, itemId)).thenReturn(Optional.of(itemMilk));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unitCan));
        when(conversionRepository.findActiveConversion(entityId, itemId, supplierId, LocalDate.of(2026, 11, 16)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculatePackConversion(entityId, request))
                .isInstanceOf(ConversionNotFoundException.class);
    }
}
