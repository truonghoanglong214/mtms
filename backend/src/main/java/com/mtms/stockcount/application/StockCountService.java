package com.mtms.stockcount.application;

import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.inventory.domain.StockMovementType;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.stockcount.application.dto.StockCountDtos.CreateStockCountRequest;
import com.mtms.stockcount.application.dto.StockCountDtos.StockCountDetailDto;
import com.mtms.stockcount.application.dto.StockCountDtos.StockCountLineDto;
import com.mtms.stockcount.application.dto.StockCountDtos.StockCountListResponse;
import com.mtms.stockcount.application.dto.StockCountDtos.StockCountSummaryDto;
import com.mtms.stockcount.application.dto.StockCountDtos.UpdateCountLineRequest;
import com.mtms.stockcount.application.dto.StockCountDtos.UpdateStockCountRequest;
import com.mtms.stockcount.domain.StockCountExceptions.MissingDiscrepancyReasonException;
import com.mtms.stockcount.domain.StockCountExceptions.StockCountImmutableException;
import com.mtms.stockcount.domain.StockCountExceptions.StockCountInsufficientStockException;
import com.mtms.stockcount.domain.StockCountExceptions.StockCountNotFoundException;
import com.mtms.stockcount.domain.model.StockCount;
import com.mtms.stockcount.domain.model.StockCountLine;
import com.mtms.stockcount.domain.model.StockCountStatus;
import com.mtms.stockcount.domain.model.StockCountType;
import com.mtms.stockcount.infrastructure.persistence.SpringDataStockCountRepository;
import com.mtms.stockcount.infrastructure.persistence.StockCountJpaEntity;
import com.mtms.stockcount.infrastructure.persistence.StockCountLineJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockCountService {

    private final SpringDataStockCountRepository stockCountRepository;
    private final StockBalanceJpaRepository stockBalanceRepository;
    private final StockMovementJpaRepository stockMovementRepository;
    private final CatalogItemJpaRepository catalogItemRepository;
    private final BranchJpaRepository branchRepository;
    private final UserJpaRepository userRepository;

    public StockCountService(
            SpringDataStockCountRepository stockCountRepository,
            StockBalanceJpaRepository stockBalanceRepository,
            StockMovementJpaRepository stockMovementRepository,
            CatalogItemJpaRepository catalogItemRepository,
            BranchJpaRepository branchRepository,
            UserJpaRepository userRepository
    ) {
        this.stockCountRepository = stockCountRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StockCountDetailDto createStockCount(UUID branchId, CreateStockCountRequest request, UUID actorUserId) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        LocalDate countedOn = request.countedOn();
        long countIndex = stockCountRepository.countByBranchIdAndYearMonth(branchId, countedOn.getYear(), countedOn.getMonthValue()) + 1;
        String countNumber = String.format("KK-%d%02d-%04d", countedOn.getYear(), countedOn.getMonthValue(), countIndex);

        StockCount stockCount = StockCount.createDraft(
                branch.getBusinessEntityId(),
                branchId,
                countNumber,
                countedOn,
                request.countType(),
                request.notes(),
                actorUserId
        );

        List<StockBalanceJpaEntity> balances;
        if (request.itemIds() != null && !request.itemIds().isEmpty()) {
            balances = new ArrayList<>();
            for (UUID itemId : request.itemIds()) {
                stockBalanceRepository.findByBranchIdAndItemId(branchId, itemId)
                        .ifPresent(balances::add);
            }
        } else {
            balances = stockBalanceRepository.findByBranchId(branchId);
        }

        int lineNo = 1;
        for (StockBalanceJpaEntity bal : balances) {
            BigDecimal expectedQty = bal.getQuantity() != null ? bal.getQuantity() : BigDecimal.ZERO;
            BigDecimal unitCost = bal.getAvgUnitCost() != null ? bal.getAvgUnitCost() : BigDecimal.ZERO;

            StockCountLine line = StockCountLine.createInitial(
                    stockCount.getId(),
                    lineNo++,
                    bal.getItemId(),
                    null,
                    expectedQty,
                    unitCost
            );
            stockCount.addLine(line);
        }

        StockCountJpaEntity entity = StockCountJpaEntity.fromDomain(stockCount);
        StockCountJpaEntity saved = stockCountRepository.save(entity);

        return mapToDetailDto(saved);
    }

    @Transactional
    public StockCountDetailDto updateCounts(UUID branchId, UUID countId, UpdateStockCountRequest request, UUID actorUserId) {
        StockCountJpaEntity entity = stockCountRepository.findByIdAndBranchWithLines(countId, branchId)
                .orElseThrow(() -> new StockCountNotFoundException(countId));

        StockCount domain = entity.toDomain();
        domain.ensureModifiable();

        Map<UUID, UpdateCountLineRequest> reqMap = request.lines().stream()
                .collect(Collectors.toMap(UpdateCountLineRequest::lineId, r -> r));

        for (StockCountLine line : domain.getLines()) {
            UpdateCountLineRequest lineReq = reqMap.get(line.getId());
            if (lineReq != null) {
                BigDecimal countedQty = lineReq.countedQty();
                if (countedQty.compareTo(line.getExpectedQty()) != 0) {
                    if (lineReq.discrepancyReason() == null || lineReq.discrepancyReason().trim().isEmpty()) {
                        String itemName = catalogItemRepository.findById(line.getItemId())
                                .map(CatalogItemJpaEntity::getName).orElse(line.getItemId().toString());
                        throw new MissingDiscrepancyReasonException(itemName, countedQty.subtract(line.getExpectedQty()));
                    }
                }
                line.updateCountedQty(countedQty, lineReq.discrepancyReason(), lineReq.notes());
            }
        }

        domain.recalculateTotals();
        StockCountJpaEntity updated = StockCountJpaEntity.fromDomain(domain);
        StockCountJpaEntity saved = stockCountRepository.save(updated);

        return mapToDetailDto(saved);
    }

    @Transactional
    public StockCountDetailDto submitStockCount(UUID branchId, UUID countId, UUID actorUserId) {
        StockCountJpaEntity entity = stockCountRepository.findByIdAndBranchWithLines(countId, branchId)
                .orElseThrow(() -> new StockCountNotFoundException(countId));

        StockCount domain = entity.toDomain();
        domain.ensureModifiable();

        for (StockCountLine line : domain.getLines()) {
            if (line.getVarianceQty().compareTo(BigDecimal.ZERO) != 0) {
                if (line.getDiscrepancyReason() == null || line.getDiscrepancyReason().trim().isEmpty()) {
                    String itemName = catalogItemRepository.findById(line.getItemId())
                            .map(CatalogItemJpaEntity::getName).orElse(line.getItemId().toString());
                    throw new MissingDiscrepancyReasonException(itemName, line.getVarianceQty());
                }
            }
        }

        domain.submit(actorUserId);
        StockCountJpaEntity updated = StockCountJpaEntity.fromDomain(domain);
        StockCountJpaEntity saved = stockCountRepository.save(updated);

        return mapToDetailDto(saved);
    }

    @Transactional
    public StockCountDetailDto approveAndPostStockCount(UUID branchId, UUID countId, UUID actorUserId) {
        StockCountJpaEntity entity = stockCountRepository.findByIdAndBranchWithLines(countId, branchId)
                .orElseThrow(() -> new StockCountNotFoundException(countId));

        StockCount domain = entity.toDomain();
        domain.approveAndPost(actorUserId);

        // Core TC-026: Atomic Stock Balance Adjustments & Movement Postings
        for (StockCountLine line : domain.getLines()) {
            BigDecimal varianceQty = line.getVarianceQty();
            if (varianceQty.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Pessimistic Write Lock on StockBalance
            StockBalanceJpaEntity balance = stockBalanceRepository.findWithLockByBranchIdAndItemId(branchId, line.getItemId())
                    .orElseGet(() -> {
                        StockBalanceJpaEntity newBal = new StockBalanceJpaEntity(
                                UUID.randomUUID(),
                                domain.getBusinessEntityId(),
                                branchId,
                                line.getItemId(),
                                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                line.getSnapshotUnitCost(),
                                0L,
                                Instant.now(),
                                Instant.now()
                        );
                        return stockBalanceRepository.save(newBal);
                    });

            String itemName = catalogItemRepository.findById(line.getItemId())
                    .map(CatalogItemJpaEntity::getName).orElse("Sản phẩm");

            if (varianceQty.compareTo(BigDecimal.ZERO) < 0) {
                // Shortage (Thiếu kho)
                BigDecimal shortageQty = varianceQty.abs();
                if (balance.getQuantity().compareTo(shortageQty) < 0) {
                    throw new StockCountInsufficientStockException(itemName, balance.getQuantity(), shortageQty);
                }

                BigDecimal newQty = balance.getQuantity().subtract(shortageQty).setScale(3, RoundingMode.HALF_UP);
                BigDecimal newInvVal = balance.getInventoryValue().subtract(line.getVarianceValue().abs()).setScale(2, RoundingMode.HALF_UP);
                balance.setQuantity(newQty);
                balance.setInventoryValue(newInvVal);
                balance.setUpdatedAt(Instant.now());
                stockBalanceRepository.save(balance);

                StockMovementJpaEntity movement = new StockMovementJpaEntity(
                        UUID.randomUUID(),
                        domain.getBusinessEntityId(),
                        branchId,
                        line.getItemId(),
                        line.getLotId(),
                        domain.getCountedOn(),
                        StockMovementType.ADJUSTMENT,
                        varianceQty, // negative signed qty
                        line.getVarianceValue(), // negative signed val
                        line.getSnapshotUnitCost(),
                        "STOCK_COUNT",
                        domain.getId(),
                        domain.getCountNumber(),
                        actorUserId,
                        Instant.now()
                );
                stockMovementRepository.save(movement);
            } else {
                // Surplus (Thừa kho)
                BigDecimal surplusQty = varianceQty;
                BigDecimal newQty = balance.getQuantity().add(surplusQty).setScale(3, RoundingMode.HALF_UP);
                BigDecimal newInvVal = balance.getInventoryValue().add(line.getVarianceValue()).setScale(2, RoundingMode.HALF_UP);

                BigDecimal newAvgCost;
                if (newQty.compareTo(BigDecimal.ZERO) > 0) {
                    newAvgCost = newInvVal.divide(newQty, 6, RoundingMode.HALF_UP);
                } else {
                    newAvgCost = line.getSnapshotUnitCost();
                }

                balance.setQuantity(newQty);
                balance.setInventoryValue(newInvVal);
                balance.setAvgUnitCost(newAvgCost);
                balance.setUpdatedAt(Instant.now());
                stockBalanceRepository.save(balance);

                StockMovementJpaEntity movement = new StockMovementJpaEntity(
                        UUID.randomUUID(),
                        domain.getBusinessEntityId(),
                        branchId,
                        line.getItemId(),
                        line.getLotId(),
                        domain.getCountedOn(),
                        StockMovementType.ADJUSTMENT,
                        varianceQty, // positive signed qty
                        line.getVarianceValue(), // positive signed val
                        line.getSnapshotUnitCost(),
                        "STOCK_COUNT",
                        domain.getId(),
                        domain.getCountNumber(),
                        actorUserId,
                        Instant.now()
                );
                stockMovementRepository.save(movement);
            }
        }

        StockCountJpaEntity updated = StockCountJpaEntity.fromDomain(domain);
        StockCountJpaEntity saved = stockCountRepository.save(updated);

        return mapToDetailDto(saved);
    }

    @Transactional
    public void cancelStockCount(UUID branchId, UUID countId, UUID actorUserId) {
        StockCountJpaEntity entity = stockCountRepository.findByIdAndBranchWithLines(countId, branchId)
                .orElseThrow(() -> new StockCountNotFoundException(countId));

        StockCount domain = entity.toDomain();
        domain.cancel();

        StockCountJpaEntity updated = StockCountJpaEntity.fromDomain(domain);
        stockCountRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public StockCountDetailDto getStockCountDetail(UUID branchId, UUID countId) {
        StockCountJpaEntity entity = stockCountRepository.findByIdAndBranchWithLines(countId, branchId)
                .orElseThrow(() -> new StockCountNotFoundException(countId));
        return mapToDetailDto(entity);
    }

    @Transactional(readOnly = true)
    public StockCountListResponse getStockCounts(
            UUID branchId,
            StockCountStatus status,
            StockCountType countType,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        Page<StockCountJpaEntity> page = stockCountRepository.findWithFilters(
                branchId, status, countType, startDate, endDate, search, pageable
        );

        List<StockCountDetailDto> dtos = page.getContent().stream()
                .map(this::mapToDetailDto)
                .collect(Collectors.toList());

        return new StockCountListResponse(dtos, page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }

    @Transactional(readOnly = true)
    public StockCountSummaryDto getStockCountSummary(UUID branchId) {
        List<StockCountJpaEntity> counts = stockCountRepository.findAll().stream()
                .filter(c -> c.getBranchId().equals(branchId))
                .toList();

        long totalCounts = counts.size();
        long pendingCount = counts.stream()
                .filter(c -> c.getStatus() == StockCountStatus.DRAFT || c.getStatus() == StockCountStatus.SUBMITTED)
                .count();

        BigDecimal surplusVal = counts.stream()
                .filter(c -> c.getStatus() == StockCountStatus.POSTED)
                .map(StockCountJpaEntity::getTotalSurplusValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shortageVal = counts.stream()
                .filter(c -> c.getStatus() == StockCountStatus.POSTED)
                .map(StockCountJpaEntity::getTotalShortageValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netVal = surplusVal.subtract(shortageVal).setScale(2, RoundingMode.HALF_UP);

        return new StockCountSummaryDto(totalCounts, pendingCount, surplusVal, shortageVal, netVal);
    }

    private StockCountDetailDto mapToDetailDto(StockCountJpaEntity entity) {
        String branchName = entity.getBranch() != null ? entity.getBranch().getName() :
                branchRepository.findById(entity.getBranchId()).map(BranchJpaEntity::getName).orElse("Chi nhánh");

        String createdByName = resolveUserName(entity.getCreatedBy());
        String submittedByName = resolveUserName(entity.getSubmittedBy());
        String approvedByName = resolveUserName(entity.getApprovedBy());

        List<StockCountLineDto> lineDtos = entity.getLines().stream()
                .map(this::mapToLineDto)
                .collect(Collectors.toList());

        return new StockCountDetailDto(
                entity.getId(),
                entity.getCountNumber(),
                entity.getBusinessEntityId(),
                entity.getBranchId(),
                branchName,
                entity.getCountedOn(),
                entity.getStatus(),
                entity.getCountType(),
                entity.getTotalItems(),
                entity.getTotalSurplusQty(),
                entity.getTotalShortageQty(),
                entity.getTotalSurplusValue(),
                entity.getTotalShortageValue(),
                entity.getNetVarianceValue(),
                entity.getNotes(),
                entity.getCreatedBy(),
                createdByName,
                entity.getSubmittedBy(),
                submittedByName,
                entity.getSubmittedAt(),
                entity.getApprovedBy(),
                approvedByName,
                entity.getPostedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                lineDtos
        );
    }

    private StockCountLineDto mapToLineDto(StockCountLineJpaEntity line) {
        CatalogItemJpaEntity item = line.getCatalogItem() != null ? line.getCatalogItem() :
                catalogItemRepository.findById(line.getItemId()).orElse(null);

        String itemCode = item != null ? item.getItemCode() : "";
        String itemName = item != null ? item.getName() : "";
        String baseUnit = (item != null && item.getBaseUnit() != null) ? item.getBaseUnit().getUnitCode() : "";

        return new StockCountLineDto(
                line.getId(),
                line.getLineNumber(),
                line.getItemId(),
                itemCode,
                itemName,
                baseUnit,
                line.getLotId(),
                line.getExpectedQty(),
                line.getCountedQty(),
                line.getVarianceQty(),
                line.getSnapshotUnitCost(),
                line.getVarianceValue(),
                line.getDiscrepancyReason(),
                line.getNotes()
        );
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(UserJpaEntity::getDisplayName)
                .orElse("User " + userId);
    }
}
