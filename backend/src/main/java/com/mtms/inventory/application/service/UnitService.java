package com.mtms.inventory.application.service;

import com.mtms.inventory.application.dto.CreateUnitCommand;
import com.mtms.inventory.application.dto.UnitDto;
import com.mtms.inventory.application.exception.DuplicateUnitCodeException;
import com.mtms.inventory.application.exception.UnitNotFoundException;
import com.mtms.inventory.domain.Unit;
import com.mtms.inventory.domain.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UnitService {

    private final UnitRepository unitRepository;

    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    public List<UnitDto> getAllUnits() {
        return unitRepository.findAll().stream()
                .map(UnitDto::fromDomain)
                .toList();
    }

    public UnitDto getUnitById(UUID id) {
        return unitRepository.findById(id)
                .map(UnitDto::fromDomain)
                .orElseThrow(() -> new UnitNotFoundException(id));
    }

    @Transactional
    public UnitDto createUnit(CreateUnitCommand command) {
        String normalizedCode = command.unitCode().trim().toUpperCase();
        if (unitRepository.existsByUnitCode(normalizedCode)) {
            throw new DuplicateUnitCodeException(normalizedCode);
        }

        Unit unit = new Unit(
                UUID.randomUUID(),
                normalizedCode,
                command.unitName().trim(),
                command.dimension(),
                Instant.now()
        );

        Unit saved = unitRepository.save(unit);
        return UnitDto.fromDomain(saved);
    }
}
