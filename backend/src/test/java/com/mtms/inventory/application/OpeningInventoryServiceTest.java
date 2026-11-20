package com.mtms.inventory.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.importer.infrastructure.persistence.ImportBatchJpaRepository;
import com.mtms.importer.infrastructure.persistence.ImportRowJpaRepository;
import com.mtms.inventory.application.dto.CommitOpeningCommand;
import com.mtms.inventory.application.dto.OpeningInventoryRecordDto;
import com.mtms.inventory.application.dto.OpeningInventoryStatusResponse;
import com.mtms.inventory.application.dto.OpeningItemRow;
import com.mtms.inventory.application.dto.OpeningPreviewResponse;
import com.mtms.inventory.application.dto.PreviewOpeningCommand;
import com.mtms.inventory.application.service.OpeningInventoryService;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.MissingLotCodeException;
import com.mtms.inventory.domain.OpeningInventoryAlreadyPostedException;
import com.mtms.inventory.domain.OpeningInventoryRecord;
import com.mtms.inventory.domain.OpeningInventoryRecordRepository;
import com.mtms.inventory.domain.StockBalanceRepository;
import com.mtms.inventory.domain.StockLotRepository;
import com.mtms.inventory.domain.StockMovementRepository;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.UnitJpaEntity;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpeningInventoryServiceTest {

    @Mock
    private BranchJpaRepository branchJpaRepository;

    @Mock
    private CatalogItemJpaRepository catalogItemRepository;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private ImportBatchJpaRepository importBatchRepository;

    @Mock
    private ImportRowJpaRepository importRowRepository;

    @Mock
    private OpeningInventoryRecordRepository openingInventoryRecordRepository;

    @Mock
    private StockBalanceRepository stockBalanceRepository;

    @Mock
    private StockLotRepository stockLotRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    private OpeningInventoryService service;

    private UUID entityId;
    private UUID branchId;
    private UUID actorUserId;
    private BranchJpaEntity branch;
    private CatalogItemJpaEntity milkItem;
    private CatalogItemJpaEntity coffeeItem;

    @BeforeEach
    void setUp() {
        service = new OpeningInventoryService(
                branchJpaRepository,
                catalogItemRepository,
                userRepository,
                importBatchRepository,
                importRowRepository,
                openingInventoryRecordRepository,
                stockBalanceRepository,
                stockLotRepository,
                stockMovementRepository,
                new ObjectMapper()
        );

        entityId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();

        branch = new BranchJpaEntity(
                branchId, entityId, "BR-01", "Branch 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        );

        UnitJpaEntity canUnit = new UnitJpaEntity(UUID.randomUUID(), "CAN", "Lon", UnitDimension.COUNT, Instant.now());
        UnitJpaEntity kgUnit = new UnitJpaEntity(UUID.randomUUID(), "KG", "Kilogram", UnitDimension.MASS, Instant.now());

        milkItem = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityId, "ING-MILK-OT", "Sua Ong Tho",
                canUnit.getId(), ItemKind.INGREDIENT, true, true,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        milkItem.setBaseUnit(canUnit);

        coffeeItem = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityId, "ING-COFFEE-01", "Ca Phe Robusta",
                kgUnit.getId(), ItemKind.INGREDIENT, false, false,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        coffeeItem.setBaseUnit(kgUnit);
    }

    @Test
    @DisplayName("getOpeningStatus returns isOpeningCommitted=false when branch has no opening record")
    void shouldReturnUncommittedStatus() {
        when(branchJpaRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(openingInventoryRecordRepository.findByBranchId(branchId)).thenReturn(Optional.empty());

        OpeningInventoryStatusResponse response = service.getOpeningStatus(branchId);

        assertThat(response.isOpeningCommitted()).isFalse();
        assertThat(response.openingRecord()).isNull();
        assertThat(response.branchName()).isEqualTo("Branch 1");
    }

    @Test
    @DisplayName("previewOpening calculates correct initial WAC and totals")
    void shouldPreviewOpeningSuccessfully() {
        when(branchJpaRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(catalogItemRepository.findByBusinessEntityIdAndItemCode(entityId, "ING-MILK-OT"))
                .thenReturn(Optional.of(milkItem));
        when(catalogItemRepository.findByBusinessEntityIdAndItemCode(entityId, "ING-COFFEE-01"))
                .thenReturn(Optional.of(coffeeItem));

        List<OpeningItemRow> rows = List.of(
                new OpeningItemRow("ING-MILK-OT", new BigDecimal("50.000"), new BigDecimal("24000.000000"), "LOT-01", LocalDate.now().plusYears(1)),
                new OpeningItemRow("ING-COFFEE-01", new BigDecimal("20.000"), new BigDecimal("150000.000000"), null, null)
        );

        PreviewOpeningCommand command = new PreviewOpeningCommand(null, LocalDate.now(), rows);
        OpeningPreviewResponse preview = service.previewOpening(branchId, command);

        assertThat(preview.hasErrors()).isFalse();
        assertThat(preview.totalItems()).isEqualTo(2);
        assertThat(preview.totalQuantity()).isEqualByComparingTo(new BigDecimal("70.000"));
        assertThat(preview.totalValuation()).isEqualByComparingTo(new BigDecimal("4200000.00"));
        assertThat(preview.previewRows()).hasSize(2);
        assertThat(preview.previewRows().get(0).calculatedWac()).isEqualByComparingTo(new BigDecimal("24000.000000"));
        assertThat(preview.previewRows().get(0).lineTotal()).isEqualByComparingTo(new BigDecimal("1200000.00"));
    }

    @Test
    @DisplayName("commitOpening throws OpeningInventoryAlreadyPostedException if opening was already committed (TC-024)")
    void shouldRejectSecondCommitOnSameBranch() {
        when(openingInventoryRecordRepository.existsByBranchId(branchId)).thenReturn(true);

        CommitOpeningCommand command = new CommitOpeningCommand(null, LocalDate.now(), List.of(), "Note");

        assertThatThrownBy(() -> service.commitOpening(branchId, command, actorUserId))
                .isInstanceOf(OpeningInventoryAlreadyPostedException.class)
                .hasMessageContaining("already been committed and locked");
    }

    @Test
    @DisplayName("commitOpening throws MissingLotCodeException when item requires lot tracking but lotCode is null")
    void shouldRejectMissingLotCodeWhenTrackingRequired() {
        when(openingInventoryRecordRepository.existsByBranchId(branchId)).thenReturn(false);
        when(branchJpaRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(catalogItemRepository.findByBusinessEntityIdAndItemCode(entityId, "ING-MILK-OT"))
                .thenReturn(Optional.of(milkItem));

        List<OpeningItemRow> rows = List.of(
                new OpeningItemRow("ING-MILK-OT", new BigDecimal("50.000"), new BigDecimal("24000.000000"), null, null)
        );
        CommitOpeningCommand command = new CommitOpeningCommand(null, LocalDate.now(), rows, "Note");

        assertThatThrownBy(() -> service.commitOpening(branchId, command, actorUserId))
                .isInstanceOf(MissingLotCodeException.class)
                .hasMessageContaining("requires lot tracking");
    }

    @Test
    @DisplayName("commitOpening successfully persists opening record, balances, lots, and movements")
    void shouldCommitOpeningSuccessfully() {
        when(openingInventoryRecordRepository.existsByBranchId(branchId)).thenReturn(false);
        when(branchJpaRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(catalogItemRepository.findByBusinessEntityIdAndItemCode(entityId, "ING-MILK-OT"))
                .thenReturn(Optional.of(milkItem));
        when(catalogItemRepository.findByBusinessEntityIdAndItemCode(entityId, "ING-COFFEE-01"))
                .thenReturn(Optional.of(coffeeItem));

        when(openingInventoryRecordRepository.save(any(OpeningInventoryRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserJpaEntity user = new UserJpaEntity();
        user.setId(actorUserId);
        user.setDisplayName("Manager Nguyễn");
        when(userRepository.findById(actorUserId)).thenReturn(Optional.of(user));

        List<OpeningItemRow> rows = List.of(
                new OpeningItemRow("ING-MILK-OT", new BigDecimal("50.000"), new BigDecimal("24000.000000"), "LOT-01", LocalDate.now().plusYears(1)),
                new OpeningItemRow("ING-COFFEE-01", new BigDecimal("20.000"), new BigDecimal("150000.000000"), null, null)
        );

        CommitOpeningCommand command = new CommitOpeningCommand(null, LocalDate.now(), rows, "Khai trương đầu kỳ");
        OpeningInventoryRecordDto result = service.commitOpening(branchId, command, actorUserId);

        assertThat(result.status()).isEqualTo("COMMITTED");
        assertThat(result.totalItems()).isEqualTo(2);
        assertThat(result.totalQuantity()).isEqualByComparingTo(new BigDecimal("70.000"));
        assertThat(result.totalValue()).isEqualByComparingTo(new BigDecimal("4200000.00"));
        assertThat(result.approvedByName()).isEqualTo("Manager Nguyễn");

        verify(openingInventoryRecordRepository).save(any(OpeningInventoryRecord.class));
        verify(stockBalanceRepository, org.mockito.Mockito.times(2)).save(any());
        verify(stockLotRepository, org.mockito.Mockito.times(1)).save(any());
        verify(stockMovementRepository, org.mockito.Mockito.times(2)).save(any());
    }
}
