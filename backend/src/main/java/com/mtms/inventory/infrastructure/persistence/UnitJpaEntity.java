package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.Unit;
import com.mtms.inventory.domain.UnitDimension;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "units", schema = "mtms")
public class UnitJpaEntity {

    @Id
    private UUID id;

    @Column(name = "unit_code", nullable = false, unique = true, length = 20)
    private String unitCode;

    @Column(name = "unit_name", nullable = false, length = 70)
    private String unitName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", nullable = false, length = 16)
    private UnitDimension dimension;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UnitJpaEntity() {
    }

    public UnitJpaEntity(UUID id, String unitCode, String unitName, UnitDimension dimension, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.dimension = dimension;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static UnitJpaEntity fromDomain(Unit domain) {
        return new UnitJpaEntity(
                domain.getId(),
                domain.getUnitCode(),
                domain.getUnitName(),
                domain.getDimension(),
                domain.getCreatedAt()
        );
    }

    public Unit toDomain() {
        return new Unit(
                this.id,
                this.unitCode,
                this.unitName,
                this.dimension,
                this.createdAt
        );
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
}
