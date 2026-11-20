package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.StockDocument;
import com.mtms.inventory.domain.StockDocumentRepository;
import com.mtms.inventory.domain.StockDocumentStatus;
import com.mtms.inventory.domain.StockDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StockDocumentRepositoryImpl implements StockDocumentRepository {

    private final StockDocumentJpaRepository jpaRepository;

    public StockDocumentRepositoryImpl(StockDocumentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<StockDocument> findById(UUID id) {
        return jpaRepository.findByIdWithLines(id).map(StockDocumentJpaEntity::toDomain);
    }

    @Override
    public Optional<StockDocument> findByBranchIdAndId(UUID branchId, UUID id) {
        return jpaRepository.findByBranchIdAndIdWithLines(branchId, id).map(StockDocumentJpaEntity::toDomain);
    }

    @Override
    public Page<StockDocument> findByBranchIdWithFilters(
            UUID branchId,
            StockDocumentStatus status,
            StockDocumentType documentType,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        return jpaRepository.findByBranchIdWithFilters(branchId, status, documentType, startDate, endDate, search, pageable)
                .map(StockDocumentJpaEntity::toDomain);
    }

    @Override
    public long countByBranchIdAndMonth(UUID branchId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return jpaRepository.countByBranchIdAndDocumentDateBetween(branchId, ym.atDay(1), ym.atEndOfMonth());
    }

    @Override
    public StockDocument save(StockDocument document) {
        StockDocumentJpaEntity entity = StockDocumentJpaEntity.fromDomain(document);
        StockDocumentJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
