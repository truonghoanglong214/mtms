package com.mtms.inventory.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierPackConversionRepository {

    Optional<SupplierPackConversion> findById(UUID id);

    List<SupplierPackConversion> findByEntityIdAndItemId(UUID entityId, UUID itemId);

    Optional<SupplierPackConversion> findActiveConversion(UUID entityId, UUID itemId, UUID supplierId, LocalDate date);

    boolean existsBySupplierIdAndItemIdAndPackNameAndEffectiveFrom(
            UUID supplierId,
            UUID itemId,
            String packName,
            LocalDate effectiveFrom
    );

    SupplierPackConversion save(SupplierPackConversion conversion);

    long countByEntityIdAndItemId(UUID entityId, UUID itemId);
}
