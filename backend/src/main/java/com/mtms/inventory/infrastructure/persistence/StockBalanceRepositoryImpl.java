package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockBalance;
import com.mtms.inventory.domain.StockBalanceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StockBalanceRepositoryImpl implements StockBalanceRepository {

    private final StockBalanceJpaRepository jpaRepository;

    public StockBalanceRepositoryImpl(StockBalanceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<StockBalance> findByBranchIdAndItemId(UUID branchId, UUID itemId) {
        return jpaRepository.findByBranchIdAndItemId(branchId, itemId)
                .map(StockBalanceJpaEntity::toDomain);
    }

    @Override
    public Optional<StockBalance> findWithLockByBranchIdAndItemId(UUID branchId, UUID itemId) {
        return jpaRepository.findWithLockByBranchIdAndItemId(branchId, itemId)
                .map(StockBalanceJpaEntity::toDomain);
    }

    @Override
    public List<StockBalance> findByBranchId(UUID branchId) {
        return jpaRepository.findByBranchId(branchId).stream()
                .map(StockBalanceJpaEntity::toDomain)
                .toList();
    }

    @Override
    public StockBalance save(StockBalance balance) {
        StockBalanceJpaEntity entity = StockBalanceJpaEntity.fromDomain(balance);
        StockBalanceJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
