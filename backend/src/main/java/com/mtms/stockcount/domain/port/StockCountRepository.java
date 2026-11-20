package com.mtms.stockcount.domain.port;

import com.mtms.stockcount.domain.model.StockCount;

import java.util.Optional;
import java.util.UUID;

public interface StockCountRepository {
    StockCount save(StockCount stockCount);
    Optional<StockCount> findById(UUID id);
    Optional<StockCount> findByIdWithLines(UUID id);
    long countByBranchIdAndYearMonth(UUID branchId, int year, int month);
}
