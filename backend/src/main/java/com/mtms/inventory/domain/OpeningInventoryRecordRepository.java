package com.mtms.inventory.domain;

import java.util.Optional;
import java.util.UUID;

public interface OpeningInventoryRecordRepository {

    boolean existsByBranchId(UUID branchId);

    Optional<OpeningInventoryRecord> findByBranchId(UUID branchId);

    OpeningInventoryRecord save(OpeningInventoryRecord record);
}
