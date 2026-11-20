package com.mtms.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Unit {

    private final UUID id;
    private final String unitCode;
    private final String unitName;
    private final UnitDimension dimension;
    private final Instant createdAt;

    public Unit(UUID id, String unitCode, String unitName, UnitDimension dimension, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Unit ID must not be null");
        this.unitCode = Objects.requireNonNull(unitCode, "Unit code must not be null").trim().toUpperCase();
        this.unitName = Objects.requireNonNull(unitName, "Unit name must not be null").trim();
        this.dimension = Objects.requireNonNull(dimension, "Dimension must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public UnitDimension getDimension() {
        return dimension;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Unit unit)) return false;
        return Objects.equals(id, unit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
