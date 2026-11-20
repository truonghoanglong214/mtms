package com.mtms.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchExpenseConfigJpaRepository extends JpaRepository<BranchExpenseConfigJpaEntity, UUID> {

    Optional<BranchExpenseConfigJpaEntity> findByBranchId(UUID branchId);
}
