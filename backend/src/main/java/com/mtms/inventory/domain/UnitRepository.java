package com.mtms.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository {

    Optional<Unit> findById(UUID id);

    Optional<Unit> findByUnitCode(String unitCode);

    List<Unit> findAll();

    Unit save(Unit unit);

    boolean existsByUnitCode(String unitCode);
}
