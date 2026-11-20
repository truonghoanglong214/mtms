package com.mtms.inventory.application.service;

import com.mtms.inventory.application.dto.CreateSupplierPackConversionCommand;
import com.mtms.inventory.application.dto.PackConversionCalculationRequest;
import com.mtms.inventory.application.dto.PackConversionCalculationResponse;
import com.mtms.inventory.application.dto.SupplierPackConversionDto;
import com.mtms.inventory.application.exception.ConversionNotFoundException;
import com.mtms.inventory.application.exception.DuplicateConversionException;
import com.mtms.inventory.application.exception.ItemNotFoundException;
import com.mtms.inventory.application.exception.SupplierNotFoundException;
import com.mtms.inventory.application.exception.UnitNotFoundException;
import com.mtms.inventory.domain.CatalogItem;
import com.mtms.inventory.domain.CatalogItemRepository;
import com.mtms.inventory.domain.InvalidConversionRateException;
import com.mtms.inventory.domain.PackConversionCalculationResult;
import com.mtms.inventory.domain.PackConversionCalculator;
import com.mtms.inventory.domain.Supplier;
import com.mtms.inventory.domain.SupplierPackConversion;
import com.mtms.inventory.domain.SupplierPackConversionRepository;
import com.mtms.inventory.domain.SupplierRepository;
import com.mtms.inventory.domain.Unit;
import com.mtms.inventory.domain.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SupplierPackConversionService {

    private final SupplierPackConversionRepository conversionRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final SupplierRepository supplierRepository;
    private final UnitRepository unitRepository;

    public SupplierPackConversionService(
            SupplierPackConversionRepository conversionRepository,
            CatalogItemRepository catalogItemRepository,
            SupplierRepository supplierRepository,
            UnitRepository unitRepository
    ) {
        this.conversionRepository = conversionRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.supplierRepository = supplierRepository;
        this.unitRepository = unitRepository;
    }

    public List<SupplierPackConversionDto> getPackConversionsForItem(UUID entityId, UUID itemId) {
        CatalogItem item = catalogItemRepository.findByEntityIdAndId(entityId, itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        List<SupplierPackConversion> conversions = conversionRepository.findByEntityIdAndItemId(entityId, itemId);
        if (conversions.isEmpty()) {
            return List.of();
        }

        Map<UUID, Supplier> supplierMap = supplierRepository.findByEntityId(entityId).stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity(), (a, b) -> a));

        return conversions.stream().map(c -> {
            Supplier supplier = supplierMap.get(c.getSupplierId());
            String supplierName = supplier != null ? supplier.getName() : "Unknown Supplier";
            return new SupplierPackConversionDto(
                    c.getId(),
                    c.getBusinessEntityId(),
                    c.getSupplierId(),
                    supplierName,
                    c.getItemId(),
                    item.getItemCode(),
                    c.getPackName(),
                    c.getBaseUnitsPerPack(),
                    c.getEffectiveFrom(),
                    c.getEffectiveTo(),
                    c.getCreatedAt()
            );
        }).toList();
    }

    @Transactional
    public SupplierPackConversionDto createPackConversion(
            UUID entityId,
            UUID itemId,
            CreateSupplierPackConversionCommand command
    ) {
        CatalogItem item = catalogItemRepository.findByEntityIdAndId(entityId, itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        Supplier supplier = supplierRepository.findByEntityIdAndId(entityId, command.supplierId())
                .orElseThrow(() -> new SupplierNotFoundException(command.supplierId()));

        if (command.baseUnitsPerPack().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidConversionRateException("Base units per pack must be greater than zero: " + command.baseUnitsPerPack());
        }

        if (command.effectiveTo() != null && command.effectiveTo().isBefore(command.effectiveFrom())) {
            throw new InvalidConversionRateException("effectiveTo cannot be before effectiveFrom");
        }

        String normalizedPackName = command.packName().trim();
        if (conversionRepository.existsBySupplierIdAndItemIdAndPackNameAndEffectiveFrom(
                command.supplierId(),
                itemId,
                normalizedPackName,
                command.effectiveFrom()
        )) {
            throw new DuplicateConversionException(normalizedPackName, command.effectiveFrom());
        }

        SupplierPackConversion conversion = new SupplierPackConversion(
                UUID.randomUUID(),
                entityId,
                command.supplierId(),
                itemId,
                normalizedPackName,
                command.baseUnitsPerPack(),
                command.effectiveFrom(),
                command.effectiveTo(),
                Instant.now()
        );

        SupplierPackConversion saved = conversionRepository.save(conversion);

        return new SupplierPackConversionDto(
                saved.getId(),
                saved.getBusinessEntityId(),
                saved.getSupplierId(),
                supplier.getName(),
                saved.getItemId(),
                item.getItemCode(),
                saved.getPackName(),
                saved.getBaseUnitsPerPack(),
                saved.getEffectiveFrom(),
                saved.getEffectiveTo(),
                saved.getCreatedAt()
        );
    }

    public PackConversionCalculationResponse calculatePackConversion(
            UUID entityId,
            PackConversionCalculationRequest request
    ) {
        CatalogItem item = catalogItemRepository.findByEntityIdAndId(entityId, request.itemId())
                .orElseThrow(() -> new ItemNotFoundException(request.itemId()));

        Unit unit = unitRepository.findById(item.getBaseUnitId())
                .orElseThrow(() -> new UnitNotFoundException(item.getBaseUnitId()));

        SupplierPackConversion conversion = conversionRepository
                .findActiveConversion(entityId, request.itemId(), request.supplierId(), request.businessDate())
                .orElseThrow(() -> new ConversionNotFoundException(request.itemId(), request.supplierId(), request.businessDate()));

        Supplier supplier = supplierRepository.findByEntityIdAndId(entityId, conversion.getSupplierId())
                .orElseThrow(() -> new SupplierNotFoundException(conversion.getSupplierId()));

        PackConversionCalculationResult calculation = PackConversionCalculator.calculate(
                request.packQuantity(),
                conversion.getBaseUnitsPerPack(),
                conversion.getPackName(),
                unit.getUnitName()
        );

        return new PackConversionCalculationResponse(
                item.getId(),
                item.getItemCode(),
                item.getName(),
                supplier.getId(),
                supplier.getName(),
                conversion.getPackName(),
                request.packQuantity(),
                conversion.getBaseUnitsPerPack(),
                unit.getUnitCode(),
                unit.getUnitName(),
                calculation.calculatedBaseQuantity(),
                calculation.conversionSnapshot(),
                calculation.formulaExplanation()
        );
    }
}
