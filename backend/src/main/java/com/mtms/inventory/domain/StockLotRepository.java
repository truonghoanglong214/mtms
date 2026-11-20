package com.mtms.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockLotRepository {

    Optional<StockLot> findById(UUID id);

    Optional<StockLot> findByBranchIdAndItemIdAndLotCode(UUID branchId, UUID itemId, String lotCode);

    List<StockLot> findByBranchId(UUID branchId);

    StockLot save(StockLot lot);
}
