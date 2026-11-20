package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.OpeningInventoryRecord;
import com.mtms.inventory.domain.OpeningInventoryRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class OpeningInventoryRecordRepositoryImpl implements OpeningInventoryRecordRepository {

    private final OpeningInventoryRecordJpaRepository jpaRepository;

    public OpeningInventoryRecordRepositoryImpl(OpeningInventoryRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByBranchId(UUID branchId) {
        return jpaRepository.existsByBranchId(branchId);
    }

    @Override
    public Optional<OpeningInventoryRecord> findByBranchId(UUID branchId) {
        return jpaRepository.findByBranchId(branchId)
                .map(OpeningInventoryRecordJpaEntity::toDomain);
    }

    @Override
    public OpeningInventoryRecord save(OpeningInventoryRecord record) {
        OpeningInventoryRecordJpaEntity entity = OpeningInventoryRecordJpaEntity.fromDomain(record);
        OpeningInventoryRecordJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
