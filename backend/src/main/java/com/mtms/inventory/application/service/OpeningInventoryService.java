package com.mtms.inventory.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.importer.domain.ImportBatchStatus;
import com.mtms.importer.domain.ImportRowStatus;
import com.mtms.importer.infrastructure.persistence.ImportBatchJpaEntity;
import com.mtms.importer.infrastructure.persistence.ImportBatchJpaRepository;
import com.mtms.importer.infrastructure.persistence.ImportRowJpaEntity;
import com.mtms.importer.infrastructure.persistence.ImportRowJpaRepository;
import com.mtms.inventory.application.dto.CommitOpeningCommand;
import com.mtms.inventory.application.dto.OpeningInventoryRecordDto;
import com.mtms.inventory.application.dto.OpeningInventoryStatusResponse;
import com.mtms.inventory.application.dto.OpeningItemRow;
import com.mtms.inventory.application.dto.OpeningPreviewResponse;
import com.mtms.inventory.application.dto.OpeningPreviewRowDto;
import com.mtms.inventory.application.dto.PreviewOpeningCommand;
import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.inventory.application.exception.ImportBatchNotFoundException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.domain.InvalidStockQuantityException;
import com.mtms.inventory.domain.MissingLotCodeException;
import com.mtms.inventory.domain.OpeningInventoryAlreadyPostedException;
import com.mtms.inventory.domain.OpeningInventoryRecord;
import com.mtms.inventory.domain.OpeningInventoryRecordRepository;
import com.mtms.inventory.domain.StockBalance;
import com.mtms.inventory.domain.StockBalanceRepository;
import com.mtms.inventory.domain.StockLot;
import com.mtms.inventory.domain.StockLotRepository;
import com.mtms.inventory.domain.StockMovement;
import com.mtms.inventory.domain.StockMovementRepository;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-INV-02 / BR-INV-08 / TC-024
 * Service handling opening inventory staging preview, status check, and single atomic commit with lock.
 */
@Service
public class OpeningInventoryService {

    private final BranchJpaRepository branchJpaRepository;
    private final CatalogItemJpaRepository catalogItemRepository;
    private final UserJpaRepository userRepository;
    private final ImportBatchJpaRepository importBatchRepository;
    private final ImportRowJpaRepository importRowRepository;
    private final OpeningInventoryRecordRepository openingInventoryRecordRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLotRepository stockLotRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ObjectMapper objectMapper;

    public OpeningInventoryService(
            BranchJpaRepository branchJpaRepository,
            CatalogItemJpaRepository catalogItemRepository,
            UserJpaRepository userRepository,
            ImportBatchJpaRepository importBatchRepository,
            ImportRowJpaRepository importRowRepository,
            OpeningInventoryRecordRepository openingInventoryRecordRepository,
            StockBalanceRepository stockBalanceRepository,
            StockLotRepository stockLotRepository,
            StockMovementRepository stockMovementRepository,
            ObjectMapper objectMapper
    ) {
        this.branchJpaRepository = branchJpaRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.userRepository = userRepository;
        this.importBatchRepository = importBatchRepository;
        this.importRowRepository = importRowRepository;
        this.openingInventoryRecordRepository = openingInventoryRecordRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockLotRepository = stockLotRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public OpeningInventoryStatusResponse getOpeningStatus(UUID branchId) {
        BranchJpaEntity branch = branchJpaRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        Optional<OpeningInventoryRecord> recordOpt = openingInventoryRecordRepository.findByBranchId(branchId);
        if (recordOpt.isEmpty()) {
            return new OpeningInventoryStatusResponse(branchId, branch.getName(), false, null);
        }

        OpeningInventoryRecord record = recordOpt.get();
        String approvedByName = userRepository.findById(record.getApprovedBy())
                .map(UserJpaEntity::getDisplayName)
                .orElse(null);

        OpeningInventoryRecordDto dto = new OpeningInventoryRecordDto(
                record.getId(),
                record.getBranchId(),
                record.getOpeningDate(),
                record.getTotalItems(),
                record.getTotalQuantity(),
                record.getTotalValue(),
                record.getStatus(),
                record.getApprovedBy(),
                approvedByName,
                record.getApprovedAt(),
                record.getNote()
        );

        return new OpeningInventoryStatusResponse(branchId, branch.getName(), true, dto);
    }

    @Transactional(readOnly = true)
    public OpeningPreviewResponse previewOpening(UUID branchId, PreviewOpeningCommand command) {
        BranchJpaEntity branch = branchJpaRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        List<OpeningItemRow> rows = resolveRows(branchId, command.importBatchId(), command.rows());

        List<OpeningPreviewRowDto> previewRows = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        BigDecimal totalValuation = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean hasErrors = false;

        int rowNumber = 1;
        for (OpeningItemRow row : rows) {
            List<String> errors = new ArrayList<>();
            Optional<CatalogItemJpaEntity> itemOpt = catalogItemRepository
                    .findByBusinessEntityIdAndItemCode(branch.getBusinessEntityId(), row.itemCode());

            UUID itemId = null;
            String itemName = null;
            String unitCode = null;
            String unitName = null;
            boolean lotTrackingRequired = false;
            BigDecimal lineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal calculatedWac = row.unitCost() != null ? row.unitCost().setScale(6, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

            if (itemOpt.isEmpty()) {
                errors.add("Item code '" + row.itemCode() + "' not found in catalog");
            } else {
                CatalogItemJpaEntity item = itemOpt.get();
                itemId = item.getId();
                itemName = item.getName();
                lotTrackingRequired = item.isLotTrackingRequired();

                if (item.getBaseUnit() != null) {
                    unitCode = item.getBaseUnit().getUnitCode();
                    unitName = item.getBaseUnit().getUnitName();
                }

                if (row.quantity() == null || row.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Quantity must be greater than zero");
                }

                if (row.unitCost() == null || row.unitCost().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Unit cost must be non-negative");
                }

                if (lotTrackingRequired && (row.lotCode() == null || row.lotCode().isBlank())) {
                    errors.add("Lot code is required for item requiring lot tracking");
                }

                if (item.isExpirationTrackingRequired() && row.expiresOn() == null) {
                    errors.add("Expiration date is required for item requiring expiration tracking");
                }

                if (errors.isEmpty() && row.quantity() != null && row.unitCost() != null) {
                    BigDecimal qty = row.quantity().setScale(3, RoundingMode.HALF_UP);
                    BigDecimal cost = row.unitCost().setScale(6, RoundingMode.HALF_UP);
                    lineTotal = qty.multiply(cost).setScale(2, RoundingMode.HALF_UP);
                    calculatedWac = cost;

                    totalQuantity = totalQuantity.add(qty);
                    totalValuation = totalValuation.add(lineTotal);
                }
            }

            if (!errors.isEmpty()) {
                hasErrors = true;
            }

            String rowStatus = errors.isEmpty() ? "VALID" : "INVALID";
            previewRows.add(new OpeningPreviewRowDto(
                    rowNumber++,
                    itemId,
                    row.itemCode(),
                    itemName,
                    unitCode,
                    unitName,
                    row.quantity() != null ? row.quantity().setScale(3, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                    row.unitCost() != null ? row.unitCost().setScale(6, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    lineTotal,
                    calculatedWac,
                    row.lotCode(),
                    row.expiresOn(),
                    lotTrackingRequired,
                    rowStatus,
                    errors
            ));
        }

        int totalItems = (int) previewRows.stream()
                .filter(r -> "VALID".equals(r.status()))
                .map(OpeningPreviewRowDto::itemCode)
                .distinct()
                .count();

        return new OpeningPreviewResponse(
                branchId,
                command.openingDate(),
                totalItems,
                totalQuantity,
                totalValuation,
                hasErrors,
                previewRows
        );
    }

    @Transactional
    public OpeningInventoryRecordDto commitOpening(UUID branchId, CommitOpeningCommand command, UUID actorUserId) {
        Objects.requireNonNull(branchId, "branchId must not be null");
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");

        // 1. Post Once Enforcement (TC-024)
        if (openingInventoryRecordRepository.existsByBranchId(branchId)) {
            throw new OpeningInventoryAlreadyPostedException(
                    "Opening inventory for branch " + branchId + " has already been committed and locked."
            );
        }

        BranchJpaEntity branch = branchJpaRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        ImportBatchJpaEntity batch = null;
        if (command.importBatchId() != null) {
            batch = importBatchRepository.findByBranchIdAndId(branchId, command.importBatchId())
                    .orElseThrow(() -> new ImportBatchNotFoundException(command.importBatchId()));
        }

        List<OpeningItemRow> rows = resolveRows(branchId, command.importBatchId(), command.rows());
        if (rows.isEmpty()) {
            throw new InvalidStockQuantityException("At least one opening inventory item is required to commit");
        }

        // 2. Validate all rows upfront
        List<ValidatedOpeningRow> validatedRows = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        BigDecimal totalValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (OpeningItemRow row : rows) {
            CatalogItemJpaEntity item = catalogItemRepository
                    .findByBusinessEntityIdAndItemCode(branch.getBusinessEntityId(), row.itemCode())
                    .orElseThrow(() -> new ItemNotFoundException("Item with code '" + row.itemCode() + "' not found"));

            if (row.quantity() == null || row.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidStockQuantityException("Quantity for item '" + row.itemCode() + "' must be strictly positive");
            }

            if (row.unitCost() == null || row.unitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit cost for item '" + row.itemCode() + "' must be non-negative");
            }

            if (item.isLotTrackingRequired() && (row.lotCode() == null || row.lotCode().isBlank())) {
                throw new MissingLotCodeException("Item '" + row.itemCode() + "' requires lot tracking but no lot code was provided");
            }

            BigDecimal qty = row.quantity().setScale(3, RoundingMode.HALF_UP);
            BigDecimal cost = row.unitCost().setScale(6, RoundingMode.HALF_UP);
            BigDecimal lineTotal = qty.multiply(cost).setScale(2, RoundingMode.HALF_UP);

            totalQuantity = totalQuantity.add(qty);
            totalValue = totalValue.add(lineTotal);

            validatedRows.add(new ValidatedOpeningRow(item, qty, cost, lineTotal, row.lotCode(), row.expiresOn()));
        }

        // 3. Atomically persist OpeningInventoryRecord
        int totalItems = (int) validatedRows.stream()
                .map(r -> r.item.getId())
                .distinct()
                .count();

        OpeningInventoryRecord openingRecord = OpeningInventoryRecord.createCommitted(
                branch.getBusinessEntityId(),
                branchId,
                command.importBatchId(),
                command.openingDate(),
                totalItems,
                totalQuantity,
                totalValue,
                actorUserId,
                command.note()
        );
        OpeningInventoryRecord savedRecord = openingInventoryRecordRepository.save(openingRecord);

        // 4. Atomically persist StockBalances, StockLots, and StockMovements
        String sourceRef = batch != null ? batch.getSourceFilename() : "MANUAL_OPENING";

        for (ValidatedOpeningRow vRow : validatedRows) {
            StockBalance balance = StockBalance.createInitial(
                    branch.getBusinessEntityId(),
                    branchId,
                    vRow.item.getId(),
                    vRow.quantity,
                    vRow.unitCost
            );
            stockBalanceRepository.save(balance);

            StockLot lot = null;
            if (vRow.item.isLotTrackingRequired()) {
                lot = StockLot.createInitial(
                        branch.getBusinessEntityId(),
                        branchId,
                        vRow.item.getId(),
                        vRow.lotCode,
                        vRow.expiresOn,
                        vRow.quantity
                );
                lot = stockLotRepository.save(lot);
            }

            StockMovement movement = StockMovement.createOpeningBalanceMovement(
                    branch.getBusinessEntityId(),
                    branchId,
                    vRow.item.getId(),
                    lot != null ? lot.getId() : null,
                    command.openingDate(),
                    vRow.quantity,
                    vRow.unitCost,
                    savedRecord.getId(),
                    sourceRef,
                    actorUserId
            );
            stockMovementRepository.save(movement);
        }

        // 5. If linked to an import batch, transition status to COMMITTED
        if (batch != null) {
            batch.setStatus(ImportBatchStatus.COMMITTED);
            batch.setCommittedBy(actorUserId);
            batch.setCommittedAt(Instant.now());
            importBatchRepository.save(batch);
        }

        String approvedByName = userRepository.findById(actorUserId)
                .map(UserJpaEntity::getDisplayName)
                .orElse(null);

        return new OpeningInventoryRecordDto(
                savedRecord.getId(),
                savedRecord.getBranchId(),
                savedRecord.getOpeningDate(),
                savedRecord.getTotalItems(),
                savedRecord.getTotalQuantity(),
                savedRecord.getTotalValue(),
                savedRecord.getStatus(),
                savedRecord.getApprovedBy(),
                approvedByName,
                savedRecord.getApprovedAt(),
                savedRecord.getNote()
        );
    }

    private List<OpeningItemRow> resolveRows(UUID branchId, UUID importBatchId, List<OpeningItemRow> directRows) {
        if (importBatchId != null) {
            ImportBatchJpaEntity batch = importBatchRepository.findByBranchIdAndId(branchId, importBatchId)
                    .orElseThrow(() -> new ImportBatchNotFoundException(importBatchId));

            List<ImportRowJpaEntity> rowEntities = importRowRepository
                    .findByImportBatchIdAndStatusOrderByRowNumberAsc(batch.getId(), ImportRowStatus.VALID);

            List<OpeningItemRow> parsedRows = new ArrayList<>();
            for (ImportRowJpaEntity rowEntity : rowEntities) {
                try {
                    String jsonSource = rowEntity.getNormalizedData() != null ? rowEntity.getNormalizedData() : rowEntity.getRawData();
                    JsonNode node = objectMapper.readTree(jsonSource);

                    String itemCode = node.path("item_code").asText(null);
                    BigDecimal quantity = node.hasNonNull("quantity") ? new BigDecimal(node.path("quantity").asText()) : null;
                    BigDecimal unitCost = node.hasNonNull("unit_cost") ? new BigDecimal(node.path("unit_cost").asText()) : null;
                    String lotCode = node.hasNonNull("lot_code") && !node.path("lot_code").asText().isBlank()
                            ? node.path("lot_code").asText().trim()
                            : null;
                    LocalDate expiresOn = node.hasNonNull("expires_on") && !node.path("expires_on").asText().isBlank()
                            ? LocalDate.parse(node.path("expires_on").asText().trim())
                            : null;

                    parsedRows.add(new OpeningItemRow(itemCode, quantity, unitCost, lotCode, expiresOn));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to parse row " + rowEntity.getRowNumber() + " from staged import: " + e.getMessage(), e);
                }
            }
            return parsedRows;
        }

        return directRows != null ? directRows : Collections.emptyList();
    }

    private record ValidatedOpeningRow(
            CatalogItemJpaEntity item,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal lineTotal,
            String lotCode,
            LocalDate expiresOn
    ) {
    }
}
