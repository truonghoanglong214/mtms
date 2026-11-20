package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockMovement;
import com.mtms.inventory.domain.StockMovementRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class StockMovementRepositoryImpl implements StockMovementRepository {

    private final StockMovementJpaRepository jpaRepository;

    public StockMovementRepositoryImpl(StockMovementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        StockMovementJpaEntity entity = StockMovementJpaEntity.fromDomain(movement);
        StockMovementJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<StockMovement> findByBranchId(UUID branchId) {
        return jpaRepository.findByBranchId(branchId).stream()
                .map(StockMovementJpaEntity::toDomain)
                .toList();
    }
}
