package com.mtms.inventory.application.service;

import com.mtms.inventory.application.dto.CatalogItemDto;
import com.mtms.inventory.application.dto.CreateCatalogItemCommand;
import com.mtms.inventory.application.dto.UpdateCatalogItemCommand;
import com.mtms.inventory.application.exception.DuplicateItemCodeException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.application.exception.UnitNotFoundException;
import com.mtms.inventory.domain.CatalogItem;
import com.mtms.inventory.domain.CatalogItemRepository;
import com.mtms.inventory.domain.InvalidTrackingInvariantException;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.SupplierPackConversionRepository;
import com.mtms.inventory.domain.Unit;
import com.mtms.inventory.domain.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CatalogItemService {

    private final CatalogItemRepository catalogItemRepository;
    private final UnitRepository unitRepository;
    private final SupplierPackConversionRepository conversionRepository;

    public CatalogItemService(
            CatalogItemRepository catalogItemRepository,
            UnitRepository unitRepository,
            SupplierPackConversionRepository conversionRepository
    ) {
        this.catalogItemRepository = catalogItemRepository;
        this.unitRepository = unitRepository;
        this.conversionRepository = conversionRepository;
    }

    public List<CatalogItemDto> getItems(UUID entityId, ItemKind itemKind, ItemStatus status, String search) {
        List<CatalogItem> items = catalogItemRepository.findByEntityId(entityId, itemKind, status, search);
        if (items.isEmpty()) {
            return List.of();
        }

        Map<UUID, Unit> unitMap = unitRepository.findAll().stream()
                .collect(Collectors.toMap(Unit::getId, Function.identity(), (a, b) -> a));

        return items.stream().map(item -> {
            Unit unit = unitMap.get(item.getBaseUnitId());
            String unitCode = unit != null ? unit.getUnitCode() : "";
            String unitName = unit != null ? unit.getUnitName() : "";
            long conversionCount = conversionRepository.countByEntityIdAndItemId(entityId, item.getId());
            return CatalogItemDto.from(item, unitCode, unitName, (int) conversionCount);
        }).toList();
    }

    public CatalogItemDto getItemById(UUID entityId, UUID itemId) {
        CatalogItem item = catalogItemRepository.findByEntityIdAndId(entityId, itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        Unit unit = unitRepository.findById(item.getBaseUnitId())
                .orElseThrow(() -> new UnitNotFoundException(item.getBaseUnitId()));

        long conversionCount = conversionRepository.countByEntityIdAndItemId(entityId, item.getId());
        return CatalogItemDto.from(item, unit.getUnitCode(), unit.getUnitName(), (int) conversionCount);
    }

    @Transactional
    public CatalogItemDto createItem(UUID entityId, CreateCatalogItemCommand command) {
        if (command.expirationTrackingRequired() && !command.lotTrackingRequired()) {
            throw new InvalidTrackingInvariantException(
                    "Expiration tracking requires lot tracking to be enabled (itemCode: " + command.itemCode() + ")"
            );
        }

        String normalizedCode = command.itemCode().trim().toUpperCase();
        if (catalogItemRepository.existsByEntityIdAndItemCode(entityId, normalizedCode)) {
            throw new DuplicateItemCodeException(normalizedCode);
        }

        Unit unit = unitRepository.findById(command.baseUnitId())
                .orElseThrow(() -> new UnitNotFoundException(command.baseUnitId()));

        CatalogItem item = new CatalogItem(
                UUID.randomUUID(),
                entityId,
                normalizedCode,
                command.name().trim(),
                unit.getId(),
                command.itemKind(),
                command.lotTrackingRequired(),
                command.expirationTrackingRequired(),
                ItemStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        CatalogItem saved = catalogItemRepository.save(item);
        return CatalogItemDto.from(saved, unit.getUnitCode(), unit.getUnitName(), 0);
    }

    @Transactional
    public CatalogItemDto updateItem(UUID entityId, UUID itemId, UpdateCatalogItemCommand command) {
        CatalogItem item = catalogItemRepository.findByEntityIdAndId(entityId, itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (command.expirationTrackingRequired() && !command.lotTrackingRequired()) {
            throw new InvalidTrackingInvariantException(
                    "Expiration tracking requires lot tracking to be enabled (itemId: " + itemId + ")"
            );
        }

        item.update(
                command.name().trim(),
                command.itemKind(),
                command.lotTrackingRequired(),
                command.expirationTrackingRequired(),
                command.status()
        );

        CatalogItem saved = catalogItemRepository.save(item);
        Unit unit = unitRepository.findById(saved.getBaseUnitId())
                .orElseThrow(() -> new UnitNotFoundException(saved.getBaseUnitId()));
        long conversionCount = conversionRepository.countByEntityIdAndItemId(entityId, saved.getId());

        return CatalogItemDto.from(saved, unit.getUnitCode(), unit.getUnitName(), (int) conversionCount);
    }
}
