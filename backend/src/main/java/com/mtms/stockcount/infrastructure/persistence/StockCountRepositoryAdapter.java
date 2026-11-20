package com.mtms.stockcount.infrastructure.persistence;

import com.mtms.stockcount.domain.model.StockCount;
import com.mtms.stockcount.domain.port.StockCountRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class StockCountRepositoryAdapter implements StockCountRepository {

    private final SpringDataStockCountRepository springDataRepository;

    public StockCountRepositoryAdapter(SpringDataStockCountRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public StockCount save(StockCount stockCount) {
        StockCountJpaEntity entity = StockCountJpaEntity.fromDomain(stockCount);
        StockCountJpaEntity saved = springDataRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<StockCount> findById(UUID id) {
        return springDataRepository.findById(id).map(StockCountJpaEntity::toDomain);
    }

    @Override
    public Optional<StockCount> findByIdWithLines(UUID id) {
        return springDataRepository.findByIdWithLines(id).map(StockCountJpaEntity::toDomain);
    }

    @Override
    public long countByBranchIdAndYearMonth(UUID branchId, int year, int month) {
        return springDataRepository.countByBranchIdAndYearMonth(branchId, year, month);
    }
}
