package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.PurchaseReceipt;
import com.mtms.inventory.domain.PurchaseReceiptRepository;
import com.mtms.inventory.domain.ReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PurchaseReceiptRepositoryImpl implements PurchaseReceiptRepository {

    private final PurchaseReceiptJpaRepository jpaRepository;

    public PurchaseReceiptRepositoryImpl(PurchaseReceiptJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PurchaseReceipt> findById(UUID id) {
        return jpaRepository.findByIdWithLines(id).map(PurchaseReceiptJpaEntity::toDomain);
    }

    @Override
    public Optional<PurchaseReceipt> findByBranchIdAndId(UUID branchId, UUID id) {
        return jpaRepository.findByBranchIdAndIdWithLines(branchId, id).map(PurchaseReceiptJpaEntity::toDomain);
    }

    @Override
    public Page<PurchaseReceipt> findByBranchIdWithFilters(
            UUID branchId,
            ReceiptStatus status,
            UUID supplierId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        return jpaRepository.findByBranchIdWithFilters(branchId, status, supplierId, startDate, endDate, search, pageable)
                .map(PurchaseReceiptJpaEntity::toDomain);
    }

    @Override
    public long countByBranchIdAndMonth(UUID branchId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return jpaRepository.countByBranchIdAndReceivedOnBetween(branchId, start, end);
    }

    @Override
    public PurchaseReceipt save(PurchaseReceipt receipt) {
        PurchaseReceiptJpaEntity entity = PurchaseReceiptJpaEntity.fromDomain(receipt);
        PurchaseReceiptJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
