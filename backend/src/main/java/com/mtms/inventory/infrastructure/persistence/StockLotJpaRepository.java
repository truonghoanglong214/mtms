package com.mtms.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockLotJpaRepository extends JpaRepository<StockLotJpaEntity, UUID> {

    Optional<StockLotJpaEntity> findByBranchIdAndItemIdAndLotCode(UUID branchId, UUID itemId, String lotCode);

    List<StockLotJpaEntity> findByBranchId(UUID branchId);
}
