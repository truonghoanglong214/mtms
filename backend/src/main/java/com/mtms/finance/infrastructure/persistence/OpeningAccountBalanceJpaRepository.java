package com.mtms.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009
 * Repository for opening account balances.
 */
@Repository
public interface OpeningAccountBalanceJpaRepository extends JpaRepository<OpeningAccountBalanceJpaEntity, UUID> {

    List<OpeningAccountBalanceJpaEntity> findByBatchId(UUID batchId);

    void deleteByBatchId(UUID batchId);
}
