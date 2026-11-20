package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.Unit;
import com.mtms.inventory.domain.UnitDimension;

import java.util.UUID;

public record UnitDto(
        UUID id,
        String unitCode,
        String unitName,
        UnitDimension dimension
) {
    public static UnitDto fromDomain(Unit unit) {
        return new UnitDto(
                unit.getId(),
                unit.getUnitCode(),
                unit.getUnitName(),
                unit.getDimension()
        );
    }
}
