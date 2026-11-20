package com.mtms.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockBalanceRepository {

    Optional<StockBalance> findByBranchIdAndItemId(UUID branchId, UUID itemId);

    Optional<StockBalance> findWithLockByBranchIdAndItemId(UUID branchId, UUID itemId);

    List<StockBalance> findByBranchId(UUID branchId);

    StockBalance save(StockBalance balance);
}
