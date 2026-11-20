package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockLot;
import com.mtms.inventory.domain.StockLotRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StockLotRepositoryImpl implements StockLotRepository {

    private final StockLotJpaRepository jpaRepository;

    public StockLotRepositoryImpl(StockLotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<StockLot> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(StockLotJpaEntity::toDomain);
    }

    @Override
    public Optional<StockLot> findByBranchIdAndItemIdAndLotCode(UUID branchId, UUID itemId, String lotCode) {
        return jpaRepository.findByBranchIdAndItemIdAndLotCode(branchId, itemId, lotCode)
                .map(StockLotJpaEntity::toDomain);
    }

    @Override
    public List<StockLot> findByBranchId(UUID branchId) {
        return jpaRepository.findByBranchId(branchId).stream()
                .map(StockLotJpaEntity::toDomain)
                .toList();
    }

    @Override
    public StockLot save(StockLot lot) {
        StockLotJpaEntity entity = StockLotJpaEntity.fromDomain(lot);
        StockLotJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
