package com.mtms.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PayableRepository {

    Optional<Payable> findById(UUID id);

    Optional<Payable> findByBranchIdAndId(UUID branchId, UUID id);

    Page<Payable> findByBranchIdWithFilters(
            UUID branchId,
            PayableStatus status,
            String search,
            Pageable pageable
    );

    BigDecimal calculateTotalOutstandingDebt(UUID branchId);

    Payable save(Payable payable);
}
