package com.mtms.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpeningInventoryRecordJpaRepository extends JpaRepository<OpeningInventoryRecordJpaEntity, UUID> {

    boolean existsByBranchId(UUID branchId);

    Optional<OpeningInventoryRecordJpaEntity> findByBranchId(UUID branchId);
}
