package com.mtms.inventory.application.service;

import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.inventory.application.dto.CreateStockIssueCommand;
import com.mtms.inventory.application.dto.CreateStockIssueLineCommand;
import com.mtms.inventory.application.dto.StockDocumentDetailDto;
import com.mtms.inventory.application.dto.StockDocumentDto;
import com.mtms.inventory.application.dto.StockDocumentLineDto;
import com.mtms.inventory.application.dto.StockDocumentListResponse;
import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.domain.CatalogItemRepository;
import com.mtms.inventory.domain.InsufficientStockException;
import com.mtms.inventory.domain.InvalidIssueQuantityException;
import com.mtms.inventory.domain.StockBalance;
import com.mtms.inventory.domain.StockBalanceRepository;
import com.mtms.inventory.domain.StockDocument;
import com.mtms.inventory.domain.StockDocumentAlreadyPostedException;
import com.mtms.inventory.domain.StockDocumentLine;
import com.mtms.inventory.domain.StockDocumentNotFoundException;
import com.mtms.inventory.domain.StockDocumentRepository;
import com.mtms.inventory.domain.StockDocumentStatus;
import com.mtms.inventory.domain.StockDocumentType;
import com.mtms.inventory.domain.StockLot;
import com.mtms.inventory.domain.StockLotRepository;
import com.mtms.inventory.domain.StockMovement;
import com.mtms.inventory.domain.StockMovementRepository;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-INV-04 / P0-INV-05 / BR-INV-02 / BR-INV-03 / TC-020 / TC-021 / TC-022
 * Service for managing manual stock consumption issues, snapshotting Moving WAC,
 * and maintaining stock ledger invariants.
 */
@Service
public class StockIssueService {

    private final BranchJpaRepository branchRepository;
    private final CatalogItemJpaRepository catalogItemRepository;
    private final UserJpaRepository userRepository;
    private final StockDocumentRepository stockDocumentRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLotRepository stockLotRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockIssueService(
            BranchJpaRepository branchRepository,
            CatalogItemJpaRepository catalogItemRepository,
            UserJpaRepository userRepository,
            StockDocumentRepository stockDocumentRepository,
            StockBalanceRepository stockBalanceRepository,
            StockLotRepository stockLotRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this.branchRepository = branchRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.userRepository = userRepository;
        this.stockDocumentRepository = stockDocumentRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockLotRepository = stockLotRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public StockDocumentListResponse getIssueDocuments(
            UUID branchId,
            StockDocumentStatus status,
            StockDocumentType documentType,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        if (!branchRepository.existsById(branchId)) {
            throw new BranchNotFoundException(branchId);
        }

        Page<StockDocument> page = stockDocumentRepository.findByBranchIdWithFilters(
                branchId, status, documentType, startDate, endDate, search, pageable
        );

        List<StockDocumentDto> dtoList = page.getContent().stream()
                .map(this::toStockDocumentDto)
                .toList();

        return new StockDocumentListResponse(dtoList, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public StockDocumentDetailDto getIssueDocumentById(UUID branchId, UUID documentId) {
        if (!branchRepository.existsById(branchId)) {
            throw new BranchNotFoundException(branchId);
        }

        StockDocument document = stockDocumentRepository.findByBranchIdAndId(branchId, documentId)
                .orElseThrow(() -> new StockDocumentNotFoundException(documentId));

        return toStockDocumentDetailDto(document);
    }

    @Transactional
    public StockDocumentDetailDto createIssueDocument(
            UUID branchId,
            CreateStockIssueCommand command,
            UUID actorUserId
    ) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        int year = command.documentDate().getYear();
        int month = command.documentDate().getMonthValue();
        long count = stockDocumentRepository.countByBranchIdAndMonth(branchId, year, month);
        String documentNumber = String.format("PXK-%04d%02d-%04d", year, month, count + 1);

        UUID documentId = UUID.randomUUID();
        List<StockDocumentLine> lines = new ArrayList<>();
        BigDecimal runningTotalQty = BigDecimal.ZERO;
        int lineNo = 1;

        for (CreateStockIssueLineCommand lineCmd : command.lines()) {
            CatalogItemJpaEntity itemEntity = catalogItemRepository.findById(lineCmd.itemId())
                    .orElseThrow(() -> new ItemNotFoundException(lineCmd.itemId()));

            if (!itemEntity.getBusinessEntityId().equals(branch.getBusinessEntityId())) {
                throw new IllegalArgumentException("Item does not belong to branch entity: " + lineCmd.itemId());
            }

            BigDecimal issueQty = lineCmd.issueQuantity();
            if (issueQty == null || issueQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidIssueQuantityException("Issue quantity must be strictly positive (> 0)");
            }

            if (lineCmd.lotId() != null) {
                StockLot lot = stockLotRepository.findById(lineCmd.lotId())
                        .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lineCmd.lotId()));
                if (!lot.getBranchId().equals(branchId) || !lot.getItemId().equals(lineCmd.itemId())) {
                    throw new IllegalArgumentException("Lot does not match branch or item: " + lineCmd.lotId());
                }
            }

            StockDocumentLine line = new StockDocumentLine(
                    UUID.randomUUID(),
                    documentId,
                    lineNo++,
                    lineCmd.itemId(),
                    lineCmd.lotId(),
                    issueQty,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    lineCmd.notes()
            );
            lines.add(line);
            runningTotalQty = runningTotalQty.add(issueQty);
        }

        StockDocument document = new StockDocument(
                documentId,
                branch.getBusinessEntityId(),
                branchId,
                documentNumber,
                command.getEffectiveDocumentType(),
                command.documentDate(),
                StockDocumentStatus.DRAFT,
                command.reason(),
                lines.size(),
                runningTotalQty.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                actorUserId,
                null,
                null,
                null,
                lines
        );

        document = stockDocumentRepository.save(document);

        if (Boolean.TRUE.equals(command.autoPost())) {
            document = postIssueInternal(document, branch, actorUserId);
        }

        return toStockDocumentDetailDto(document);
    }

    @Transactional
    public StockDocumentDetailDto postIssueDocument(UUID branchId, UUID documentId, UUID actorUserId) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        StockDocument document = stockDocumentRepository.findByBranchIdAndId(branchId, documentId)
                .orElseThrow(() -> new StockDocumentNotFoundException(documentId));

        if (document.getStatus() == StockDocumentStatus.POSTED) {
            throw new StockDocumentAlreadyPostedException("Stock document has already been posted: " + document.getDocumentNumber());
        }

        document = postIssueInternal(document, branch, actorUserId);
        return toStockDocumentDetailDto(document);
    }

    private StockDocument postIssueInternal(StockDocument document, BranchJpaEntity branch, UUID actorUserId) {
        BigDecimal runningTotalBookedCost = BigDecimal.ZERO;
        BigDecimal runningTotalQuantity = BigDecimal.ZERO;

        for (StockDocumentLine line : document.getLines()) {
            UUID itemId = line.getItemId();
            BigDecimal issueQty = line.getIssueQuantity();

            // A. Lock Stock Balance row (TC-022 Pessimistic Lock)
            StockBalance balance = stockBalanceRepository.findWithLockByBranchIdAndItemId(branch.getId(), itemId)
                    .orElseThrow(() -> {
                        CatalogItemJpaEntity itemEntity = catalogItemRepository.findById(itemId).orElse(null);
                        String itemName = itemEntity != null ? itemEntity.getName() : itemId.toString();
                        return new InsufficientStockException(
                                "Insufficient stock balance for item: " + itemName + ". Available: 0, Requested: " + issueQty
                        );
                    });

            // B. Enforce no negative stock (TC-021) and snapshot Moving WAC (TC-020)
            BigDecimal bookedUnitCost = balance.getAvgUnitCost();
            BigDecimal bookedTotal = balance.issueStock(issueQty); // Enforces quantity >= issueQty, deducts balance & inventory value
            stockBalanceRepository.save(balance);

            // C. Lot deduction if lotId specified
            if (line.getLotId() != null) {
                StockLot lot = stockLotRepository.findById(line.getLotId())
                        .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + line.getLotId()));
                if (!lot.getBranchId().equals(branch.getId()) || !lot.getItemId().equals(itemId)) {
                    throw new IllegalArgumentException("Lot does not match branch or item: " + line.getLotId());
                }
                if (lot.getQuantity().compareTo(issueQty) < 0) {
                    throw new InsufficientStockException(
                            "Insufficient lot quantity for lot " + lot.getLotCode() + ". Available: " + lot.getQuantity() + ", Requested: " + issueQty
                    );
                }
                lot.setQuantity(lot.getQuantity().subtract(issueQty));
                stockLotRepository.save(lot);
            }

            // D. Snapshot booked cost on document line
            line.setBookedCost(bookedUnitCost, bookedTotal);
            runningTotalBookedCost = runningTotalBookedCost.add(bookedTotal);
            runningTotalQuantity = runningTotalQuantity.add(issueQty);

            // E. Stock Movement Ledger (Immutable audit record)
            StockMovement movement = StockMovement.createManualIssueMovement(
                    branch.getBusinessEntityId(),
                    branch.getId(),
                    itemId,
                    line.getLotId(),
                    document.getDocumentDate(),
                    issueQty,
                    bookedTotal,
                    bookedUnitCost,
                    document.getId(),
                    document.getDocumentNumber(),
                    actorUserId
            );
            stockMovementRepository.save(movement);
        }

        document.post(actorUserId, runningTotalBookedCost, runningTotalQuantity, document.getLines().size());
        return stockDocumentRepository.save(document);
    }

    private StockDocumentDto toStockDocumentDto(StockDocument doc) {
        String createdByName = resolveUserName(doc.getCreatedBy());
        String approvedByName = resolveUserName(doc.getApprovedBy());

        return new StockDocumentDto(
                doc.getId(),
                doc.getBranchId(),
                doc.getDocumentNumber(),
                doc.getDocumentType(),
                doc.getDocumentDate(),
                doc.getStatus(),
                doc.getReason(),
                doc.getTotalLines(),
                doc.getTotalQuantity(),
                doc.getTotalBookedCost(),
                doc.getCreatedBy(),
                createdByName,
                doc.getApprovedBy(),
                approvedByName,
                doc.getPostedAt(),
                doc.getCreatedAt()
        );
    }

    private StockDocumentDetailDto toStockDocumentDetailDto(StockDocument doc) {
        String createdByName = resolveUserName(doc.getCreatedBy());
        String approvedByName = resolveUserName(doc.getApprovedBy());

        List<StockDocumentLineDto> lineDtos = new ArrayList<>();
        for (StockDocumentLine line : doc.getLines()) {
            CatalogItemJpaEntity item = catalogItemRepository.findById(line.getItemId()).orElse(null);
            String itemCode = item != null ? item.getItemCode() : null;
            String itemName = item != null ? item.getName() : null;
            String unitName = (item != null && item.getBaseUnit() != null) ? item.getBaseUnit().getUnitName() : null;

            String lotCode = null;
            if (line.getLotId() != null) {
                lotCode = stockLotRepository.findById(line.getLotId())
                        .map(StockLot::getLotCode)
                        .orElse(null);
            }

            lineDtos.add(new StockDocumentLineDto(
                    line.getId(),
                    line.getDocumentId(),
                    line.getLineNo(),
                    line.getItemId(),
                    itemCode,
                    itemName,
                    unitName,
                    line.getLotId(),
                    lotCode,
                    line.getIssueQuantity(),
                    line.getBookedUnitCost(),
                    line.getBookedTotal(),
                    line.getNotes()
            ));
        }

        return new StockDocumentDetailDto(
                doc.getId(),
                doc.getBranchId(),
                doc.getDocumentNumber(),
                doc.getDocumentType(),
                doc.getDocumentDate(),
                doc.getStatus(),
                doc.getReason(),
                doc.getTotalLines(),
                doc.getTotalQuantity(),
                doc.getTotalBookedCost(),
                doc.getCreatedBy(),
                createdByName,
                doc.getApprovedBy(),
                approvedByName,
                doc.getPostedAt(),
                doc.getCreatedAt(),
                lineDtos
        );
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(UserJpaEntity::getDisplayName)
                .orElse(null);
    }
}
