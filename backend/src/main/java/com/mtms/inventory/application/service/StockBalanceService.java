package com.mtms.inventory.application.service;

import com.mtms.inventory.application.dto.StockBalanceItemDto;
import com.mtms.inventory.application.dto.StockBalanceResponse;
import com.mtms.inventory.application.exception.BranchNotFoundException;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class StockBalanceService {

    private final StockBalanceJpaRepository stockBalanceRepository;
    private final BranchJpaRepository branchJpaRepository;

    public StockBalanceService(
            StockBalanceJpaRepository stockBalanceRepository,
            BranchJpaRepository branchJpaRepository
    ) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.branchJpaRepository = branchJpaRepository;
    }

    @Transactional(readOnly = true)
    public StockBalanceResponse getBranchBalances(UUID branchId, String search, Pageable pageable) {
        if (!branchJpaRepository.existsById(branchId)) {
            throw new BranchNotFoundException(branchId);
        }

        Page<StockBalanceJpaEntity> pageResult;
        if (search != null && !search.isBlank()) {
            pageResult = stockBalanceRepository.searchByBranchId(branchId, search.trim(), pageable);
        } else {
            pageResult = stockBalanceRepository.findByBranchId(branchId, pageable);
        }

        List<StockBalanceItemDto> items = pageResult.getContent().stream()
                .map(entity -> {
                    CatalogItemJpaEntity item = entity.getCatalogItem();
                    String itemCode = item != null ? item.getItemCode() : null;
                    String itemName = item != null ? item.getName() : null;
                    String unitCode = (item != null && item.getBaseUnit() != null) ? item.getBaseUnit().getUnitCode() : null;
                    String unitName = (item != null && item.getBaseUnit() != null) ? item.getBaseUnit().getUnitName() : null;

                    return new StockBalanceItemDto(
                            entity.getId(),
                            entity.getBranchId(),
                            entity.getItemId(),
                            itemCode,
                            itemName,
                            unitCode,
                            unitName,
                            entity.getQuantity().setScale(3, RoundingMode.HALF_UP),
                            entity.getAvgUnitCost().setScale(6, RoundingMode.HALF_UP),
                            entity.getInventoryValue().setScale(2, RoundingMode.HALF_UP),
                            entity.getUpdatedAt()
                    );
                })
                .toList();

        BigDecimal totalBranchValue = stockBalanceRepository.sumInventoryValueByBranchId(branchId)
                .setScale(2, RoundingMode.HALF_UP);

        return new StockBalanceResponse(items, pageResult.getTotalElements(), totalBranchValue);
    }
}
