package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.Payable;
import com.mtms.inventory.domain.PayableRepository;
import com.mtms.inventory.domain.PayableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PayableRepositoryImpl implements PayableRepository {

    private final PayableJpaRepository jpaRepository;

    public PayableRepositoryImpl(PayableJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Payable> findById(UUID id) {
        return jpaRepository.findById(id).map(PayableJpaEntity::toDomain);
    }

    @Override
    public Optional<Payable> findByBranchIdAndId(UUID branchId, UUID id) {
        return jpaRepository.findByBranchIdAndId(branchId, id).map(PayableJpaEntity::toDomain);
    }

    @Override
    public Page<Payable> findByBranchIdWithFilters(
            UUID branchId,
            PayableStatus status,
            String search,
            Pageable pageable
    ) {
        return jpaRepository.findByBranchIdWithFilters(branchId, status, search, pageable)
                .map(PayableJpaEntity::toDomain);
    }

    @Override
    public BigDecimal calculateTotalOutstandingDebt(UUID branchId) {
        BigDecimal sum = jpaRepository.sumOutstandingDebtByBranchId(branchId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public Payable save(Payable payable) {
        PayableJpaEntity entity = PayableJpaEntity.fromDomain(payable);
        PayableJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
