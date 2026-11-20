package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.SupplierPackConversion;
import com.mtms.inventory.domain.SupplierPackConversionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SupplierPackConversionRepositoryImpl implements SupplierPackConversionRepository {

    private final SupplierPackConversionJpaRepository jpaRepository;

    public SupplierPackConversionRepositoryImpl(SupplierPackConversionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<SupplierPackConversion> findById(UUID id) {
        return jpaRepository.findById(id).map(SupplierPackConversionJpaEntity::toDomain);
    }

    @Override
    public List<SupplierPackConversion> findByEntityIdAndItemId(UUID entityId, UUID itemId) {
        return jpaRepository.findByEntityIdAndItemIdWithDetails(entityId, itemId)
                .stream()
                .map(SupplierPackConversionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<SupplierPackConversion> findActiveConversion(UUID entityId, UUID itemId, UUID supplierId, LocalDate date) {
        List<SupplierPackConversionJpaEntity> conversions = jpaRepository.findActiveConversions(entityId, itemId, supplierId, date);
        if (conversions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(conversions.get(0).toDomain());
    }

    @Override
    public boolean existsBySupplierIdAndItemIdAndPackNameAndEffectiveFrom(
            UUID supplierId,
            UUID itemId,
            String packName,
            LocalDate effectiveFrom
    ) {
        return jpaRepository.existsBySupplierIdAndItemIdAndPackNameAndEffectiveFrom(
                supplierId,
                itemId,
                packName,
                effectiveFrom
        );
    }

    @Override
    public SupplierPackConversion save(SupplierPackConversion conversion) {
        SupplierPackConversionJpaEntity entity = SupplierPackConversionJpaEntity.fromDomain(conversion);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public long countByEntityIdAndItemId(UUID entityId, UUID itemId) {
        return jpaRepository.countByBusinessEntityIdAndItemId(entityId, itemId);
    }
}
