package com.mtms.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository {

    Optional<Supplier> findById(UUID id);

    Optional<Supplier> findByEntityIdAndId(UUID entityId, UUID id);

    Optional<Supplier> findByEntityIdAndSupplierCode(UUID entityId, String supplierCode);

    List<Supplier> findByEntityIdAndStatus(UUID entityId, SupplierStatus status);

    List<Supplier> findByEntityId(UUID entityId);

    Supplier save(Supplier supplier);

    boolean existsByEntityIdAndSupplierCode(UUID entityId, String supplierCode);
}
