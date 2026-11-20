package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.Unit;
import com.mtms.inventory.domain.UnitRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UnitRepositoryImpl implements UnitRepository {

    private final UnitJpaRepository jpaRepository;

    public UnitRepositoryImpl(UnitJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Unit> findById(UUID id) {
        return jpaRepository.findById(id).map(UnitJpaEntity::toDomain);
    }

    @Override
    public Optional<Unit> findByUnitCode(String unitCode) {
        return jpaRepository.findByUnitCode(unitCode).map(UnitJpaEntity::toDomain);
    }

    @Override
    public List<Unit> findAll() {
        return jpaRepository.findAll().stream().map(UnitJpaEntity::toDomain).toList();
    }

    @Override
    public Unit save(Unit unit) {
        UnitJpaEntity entity = UnitJpaEntity.fromDomain(unit);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public boolean existsByUnitCode(String unitCode) {
        return jpaRepository.existsByUnitCode(unitCode);
    }
}
