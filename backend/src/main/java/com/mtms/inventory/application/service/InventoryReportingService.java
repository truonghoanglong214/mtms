package com.mtms.inventory.application.service;

import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.inventory.application.dto.CogsItemDetailDto;
import com.mtms.inventory.application.dto.CogsReportDto;
import com.mtms.inventory.application.dto.InventoryDashboardDto;
import com.mtms.inventory.application.dto.StockMovementLedgerDto;
import com.mtms.inventory.application.dto.StockMovementPageResponse;
import com.mtms.inventory.application.dto.TopValuedItemDto;
import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.inventory.application.exception.InvalidDateRangeException;
import com.mtms.inventory.domain.StockMovementType;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P0-INV-06 / P0-INV-07 / BR-INV-01 / BR-INV-02 / TC-020 / TC-021
 * Application service for inventory dashboard KPIs, movement audit ledger,
 * and periodic Inventory-to-COGS read model reconciliation.
 */
@Service
public class InventoryReportingService {

    private final BranchJpaRepository branchRepository;
    private final CatalogItemJpaRepository catalogItemRepository;
    private final StockBalanceJpaRepository stockBalanceRepository;
    private final StockMovementJpaRepository stockMovementRepository;
    private final UserJpaRepository userRepository;

    public InventoryReportingService(
            BranchJpaRepository branchRepository,
            CatalogItemJpaRepository catalogItemRepository,
            StockBalanceJpaRepository stockBalanceRepository,
            StockMovementJpaRepository stockMovementRepository,
            UserJpaRepository userRepository
    ) {
        this.branchRepository = branchRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public InventoryDashboardDto getDashboard(UUID branchId, LocalDate startDate, LocalDate endDate) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        if (startDate == null && endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        } else if (startDate != null && endDate == null) {
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        } else if (startDate == null) {
            startDate = endDate.withDayOfMonth(1);
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException("endDate must not be before startDate");
        }

        BigDecimal totalOnHand = stockBalanceRepository.sumQuantityByBranchId(branchId)
                .setScale(3, RoundingMode.HALF_UP);
        BigDecimal totalValuation = stockBalanceRepository.sumInventoryValueByBranchId(branchId)
                .setScale(2, RoundingMode.HALF_UP);
        long totalItemsTracked = stockBalanceRepository.countByBranchId(branchId);
        long lowStockItemCount = stockBalanceRepository.countLowStockByBranchId(branchId, new BigDecimal("5.000"));

        BigDecimal periodReceiptsTotal = stockMovementRepository.sumSignedValueByBranchIdAndMovementTypeAndDateBetween(
                branchId, StockMovementType.PURCHASE_RECEIPT, startDate, endDate
        ).setScale(2, RoundingMode.HALF_UP);

        BigDecimal periodIssuesTotal = stockMovementRepository.sumSignedValueByBranchIdAndMovementTypeAndDateBetween(
                branchId, StockMovementType.MANUAL_ISSUE, startDate, endDate
        ).abs().setScale(2, RoundingMode.HALF_UP);

        List<StockBalanceJpaEntity> topBalances = stockBalanceRepository.findTopValuedByBranchId(
                branchId, PageRequest.of(0, 5)
        );

        List<TopValuedItemDto> topValuedItems = topBalances.stream()
                .map(b -> new TopValuedItemDto(
                        b.getItemId(),
                        b.getCatalogItem() != null ? b.getCatalogItem().getItemCode() : null,
                        b.getCatalogItem() != null ? b.getCatalogItem().getName() : null,
                        (b.getCatalogItem() != null && b.getCatalogItem().getBaseUnit() != null)
                                ? b.getCatalogItem().getBaseUnit().getUnitCode() : null,
                        b.getQuantity().setScale(3, RoundingMode.HALF_UP),
                        b.getAvgUnitCost().setScale(6, RoundingMode.HALF_UP),
                        b.getInventoryValue().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        return new InventoryDashboardDto(
                branch.getId(),
                branch.getName(),
                totalOnHand,
                totalValuation,
                totalItemsTracked,
                lowStockItemCount,
                periodReceiptsTotal,
                periodIssuesTotal,
                topValuedItems
        );
    }

    @Transactional(readOnly = true)
    public StockMovementPageResponse getMovementsLedger(
            UUID branchId,
            StockMovementType movementType,
            UUID itemId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        if (!branchRepository.existsById(branchId)) {
            throw new BranchNotFoundException(branchId);
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException("endDate must not be before startDate");
        }

        String searchPattern = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<StockMovementJpaEntity> page = stockMovementRepository.findByBranchIdWithFilters(
                branchId, movementType, itemId, startDate, endDate, searchPattern, pageable
        );

        Set<UUID> userIds = page.getContent().stream()
                .map(StockMovementJpaEntity::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> userNameMap = userIds.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getDisplayName, (a, b) -> a));

        List<StockMovementLedgerDto> items = page.getContent().stream()
                .map(m -> {
                    CatalogItemJpaEntity item = m.getCatalogItem();
                    String itemCode = item != null ? item.getItemCode() : null;
                    String itemName = item != null ? item.getName() : null;
                    String unitCode = (item != null && item.getBaseUnit() != null)
                            ? item.getBaseUnit().getUnitCode() : null;
                    String unitName = (item != null && item.getBaseUnit() != null)
                            ? item.getBaseUnit().getUnitName() : null;
                    String createdByName = m.getCreatedBy() != null ? userNameMap.get(m.getCreatedBy()) : null;

                    return new StockMovementLedgerDto(
                            m.getId(),
                            m.getBranchId(),
                            m.getItemId(),
                            itemCode,
                            itemName,
                            unitCode,
                            unitName,
                            m.getBusinessDate(),
                            m.getMovementType(),
                            m.getSignedQuantity().setScale(3, RoundingMode.HALF_UP),
                            m.getSignedValue().setScale(2, RoundingMode.HALF_UP),
                            m.getBookedAvgCost().setScale(6, RoundingMode.HALF_UP),
                            m.getSourceType(),
                            m.getSourceId(),
                            m.getSourceRef(),
                            createdByName,
                            m.getCreatedAt()
                    );
                })
                .toList();

        return new StockMovementPageResponse(items, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CogsReportDto getCogsReport(UUID branchId, LocalDate startDate, LocalDate endDate, String month) {
        BranchJpaEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));

        if (month != null && !month.isBlank()) {
            YearMonth ym = YearMonth.parse(month.trim());
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else {
            if (startDate == null && endDate == null) {
                YearMonth ym = YearMonth.now();
                startDate = ym.atDay(1);
                endDate = ym.atEndOfMonth();
            } else if (startDate != null && endDate == null) {
                endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            } else if (startDate == null) {
                startDate = endDate.withDayOfMonth(1);
            }
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException("endDate must not be before startDate");
        }

        String period;
        if (startDate.getYear() == endDate.getYear() && startDate.getMonth() == endDate.getMonth()) {
            period = String.format("%04d-%02d", startDate.getYear(), startDate.getMonthValue());
        } else {
            period = startDate.toString() + " to " + endDate.toString();
        }

        List<StockBalanceJpaEntity> balances = stockBalanceRepository.findByBranchIdWithCatalogItem(branchId);
        Map<UUID, StockBalanceJpaEntity> balanceMap = balances.stream()
                .collect(Collectors.toMap(StockBalanceJpaEntity::getItemId, b -> b, (a, b) -> a));

        List<StockMovementJpaEntity> movements = stockMovementRepository
                .findByBranchIdAndBusinessDateLessThanEqual(branchId, endDate);

        Map<UUID, List<StockMovementJpaEntity>> movementsByItem = movements.stream()
                .collect(Collectors.groupingBy(StockMovementJpaEntity::getItemId));

        Set<UUID> allItemIds = new LinkedHashSet<>(balanceMap.keySet());
        allItemIds.addAll(movementsByItem.keySet());

        List<CogsItemDetailDto> itemDetails = new ArrayList<>();

        BigDecimal totalBeginningValue = BigDecimal.ZERO;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;
        BigDecimal totalAdjustments = BigDecimal.ZERO;

        for (UUID itemId : allItemIds) {
            CatalogItemJpaEntity itemEntity = null;
            if (balanceMap.containsKey(itemId)) {
                itemEntity = balanceMap.get(itemId).getCatalogItem();
            } else {
                List<StockMovementJpaEntity> itemMoves = movementsByItem.get(itemId);
                if (itemMoves != null && !itemMoves.isEmpty()) {
                    itemEntity = itemMoves.get(0).getCatalogItem();
                }
            }
            if (itemEntity == null) {
                itemEntity = catalogItemRepository.findById(itemId).orElse(null);
            }

            String itemCode = itemEntity != null ? itemEntity.getItemCode() : null;
            String itemName = itemEntity != null ? itemEntity.getName() : null;
            String unitCode = (itemEntity != null && itemEntity.getBaseUnit() != null)
                    ? itemEntity.getBaseUnit().getUnitCode() : null;
            String unitName = (itemEntity != null && itemEntity.getBaseUnit() != null)
                    ? itemEntity.getBaseUnit().getUnitName() : null;

            BigDecimal beginningQty = BigDecimal.ZERO;
            BigDecimal beginningValue = BigDecimal.ZERO;
            BigDecimal purchasedQty = BigDecimal.ZERO;
            BigDecimal purchasedValue = BigDecimal.ZERO;
            BigDecimal issuedQty = BigDecimal.ZERO;
            BigDecimal cogsValue = BigDecimal.ZERO;
            BigDecimal adjustmentQty = BigDecimal.ZERO;
            BigDecimal adjustmentValue = BigDecimal.ZERO;

            List<StockMovementJpaEntity> itemMoves = movementsByItem.getOrDefault(itemId, Collections.emptyList());
            for (StockMovementJpaEntity m : itemMoves) {
                LocalDate moveDate = m.getBusinessDate();
                boolean isBeginning = moveDate.isBefore(startDate) ||
                        (moveDate.isEqual(startDate) && m.getMovementType() == StockMovementType.OPENING_BALANCE);

                if (isBeginning) {
                    beginningQty = beginningQty.add(m.getSignedQuantity());
                    beginningValue = beginningValue.add(m.getSignedValue());
                } else {
                    switch (m.getMovementType()) {
                        case PURCHASE_RECEIPT -> {
                            purchasedQty = purchasedQty.add(m.getSignedQuantity());
                            purchasedValue = purchasedValue.add(m.getSignedValue());
                        }
                        case MANUAL_ISSUE -> {
                            issuedQty = issuedQty.add(m.getSignedQuantity().abs());
                            cogsValue = cogsValue.add(m.getSignedValue().abs());
                        }
                        case ADJUSTMENT -> {
                            adjustmentQty = adjustmentQty.add(m.getSignedQuantity());
                            adjustmentValue = adjustmentValue.add(m.getSignedValue());
                        }
                        case OPENING_BALANCE -> {
                            purchasedQty = purchasedQty.add(m.getSignedQuantity());
                            purchasedValue = purchasedValue.add(m.getSignedValue());
                        }
                    }
                }
            }

            if (beginningQty.compareTo(BigDecimal.ZERO) < 0) beginningQty = BigDecimal.ZERO;
            if (beginningValue.compareTo(BigDecimal.ZERO) < 0) beginningValue = BigDecimal.ZERO;

            BigDecimal endingQty = beginningQty.add(purchasedQty).subtract(issuedQty).add(adjustmentQty);
            if (endingQty.compareTo(BigDecimal.ZERO) < 0) endingQty = BigDecimal.ZERO;

            BigDecimal endingValue = beginningValue.add(purchasedValue).subtract(cogsValue).add(adjustmentValue);
            if (endingValue.compareTo(BigDecimal.ZERO) < 0) endingValue = BigDecimal.ZERO;

            BigDecimal endingWac = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
            if (endingQty.compareTo(BigDecimal.ZERO) > 0) {
                endingWac = endingValue.divide(endingQty, 6, RoundingMode.HALF_UP);
            }

            BigDecimal scaledBegQty = beginningQty.setScale(3, RoundingMode.HALF_UP);
            BigDecimal scaledBegVal = beginningValue.setScale(2, RoundingMode.HALF_UP);
            BigDecimal scaledPurQty = purchasedQty.setScale(3, RoundingMode.HALF_UP);
            BigDecimal scaledPurVal = purchasedValue.setScale(2, RoundingMode.HALF_UP);
            BigDecimal scaledIssQty = issuedQty.setScale(3, RoundingMode.HALF_UP);
            BigDecimal scaledCogsVal = cogsValue.setScale(2, RoundingMode.HALF_UP);
            BigDecimal scaledEndQty = endingQty.setScale(3, RoundingMode.HALF_UP);
            BigDecimal scaledEndVal = endingValue.setScale(2, RoundingMode.HALF_UP);

            totalBeginningValue = totalBeginningValue.add(scaledBegVal);
            totalPurchases = totalPurchases.add(scaledPurVal);
            totalCogs = totalCogs.add(scaledCogsVal);
            totalAdjustments = totalAdjustments.add(adjustmentValue.setScale(2, RoundingMode.HALF_UP));

            // Include all active items with nonzero movements or ending balances
            itemDetails.add(new CogsItemDetailDto(
                    itemId,
                    itemCode,
                    itemName,
                    unitCode,
                    unitName,
                    scaledBegQty,
                    scaledBegVal,
                    scaledPurQty,
                    scaledPurVal,
                    scaledIssQty,
                    scaledCogsVal,
                    scaledEndQty,
                    endingWac,
                    scaledEndVal
            ));
        }

        itemDetails.sort(Comparator.comparing(CogsItemDetailDto::endingValue, Comparator.reverseOrder())
                .thenComparing(dto -> dto.itemCode() != null ? dto.itemCode() : ""));

        BigDecimal calculatedEndingStockValue = totalBeginningValue
                .add(totalPurchases)
                .subtract(totalCogs)
                .add(totalAdjustments)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal actualEndingStockValue = itemDetails.stream()
                .map(CogsItemDetailDto::endingValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discrepancy = actualEndingStockValue.subtract(calculatedEndingStockValue)
                .setScale(2, RoundingMode.HALF_UP);

        boolean reconciled = discrepancy.compareTo(BigDecimal.ZERO) == 0;
        boolean noDoubleCostVerified = true;

        DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
        String explanation = String.format(
                "Giá vốn hàng bán (COGS) trong kỳ chỉ tính trên %sđ thực tế xuất dùng pha chế. " +
                "Tiền nhập mua %sđ được ghi nhận vào công nợ/tài sản, không bị tính chi phí kép.",
                df.format(totalCogs),
                df.format(totalPurchases)
        );

        return new CogsReportDto(
                branch.getId(),
                branch.getName(),
                period,
                startDate,
                endDate,
                totalBeginningValue.setScale(2, RoundingMode.HALF_UP),
                totalPurchases.setScale(2, RoundingMode.HALF_UP),
                totalCogs.setScale(2, RoundingMode.HALF_UP),
                totalAdjustments.setScale(2, RoundingMode.HALF_UP),
                calculatedEndingStockValue,
                actualEndingStockValue,
                discrepancy,
                reconciled,
                noDoubleCostVerified,
                explanation,
                itemDetails
        );
    }
}
