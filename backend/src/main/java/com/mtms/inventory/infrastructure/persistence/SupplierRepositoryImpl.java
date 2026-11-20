package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.Supplier;
import com.mtms.inventory.domain.SupplierRepository;
import com.mtms.inventory.domain.SupplierStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SupplierRepositoryImpl implements SupplierRepository {

    private final SupplierJpaRepository jpaRepository;

    public SupplierRepositoryImpl(SupplierJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return jpaRepository.findById(id).map(SupplierJpaEntity::toDomain);
    }

    @Override
    public Optional<Supplier> findByEntityIdAndId(UUID entityId, UUID id) {
        return jpaRepository.findByBusinessEntityIdAndId(entityId, id).map(SupplierJpaEntity::toDomain);
    }

    @Override
    public Optional<Supplier> findByEntityIdAndSupplierCode(UUID entityId, String supplierCode) {
        return jpaRepository.findByBusinessEntityIdAndSupplierCode(entityId, supplierCode).map(SupplierJpaEntity::toDomain);
    }

    @Override
    public List<Supplier> findByEntityIdAndStatus(UUID entityId, SupplierStatus status) {
        return jpaRepository.findByBusinessEntityIdAndStatusOrderBySupplierCodeAsc(entityId, status)
                .stream()
                .map(SupplierJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Supplier> findByEntityId(UUID entityId) {
        return jpaRepository.findByBusinessEntityIdOrderBySupplierCodeAsc(entityId)
                .stream()
                .map(SupplierJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Supplier save(Supplier supplier) {
        SupplierJpaEntity entity = SupplierJpaEntity.fromDomain(supplier);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public boolean existsByEntityIdAndSupplierCode(UUID entityId, String supplierCode) {
        return jpaRepository.existsByBusinessEntityIdAndSupplierCode(entityId, supplierCode);
    }
}
