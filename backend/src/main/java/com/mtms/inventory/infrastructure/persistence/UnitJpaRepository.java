package com.mtms.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitJpaRepository extends JpaRepository<UnitJpaEntity, UUID> {

    Optional<UnitJpaEntity> findByUnitCode(String unitCode);

    boolean existsByUnitCode(String unitCode);
}
