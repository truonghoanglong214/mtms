package com.mtms.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogItemRepository {

    Optional<CatalogItem> findById(UUID id);

    Optional<CatalogItem> findByEntityIdAndId(UUID entityId, UUID id);

    Optional<CatalogItem> findByEntityIdAndItemCode(UUID entityId, String itemCode);

    List<CatalogItem> findByEntityId(UUID entityId, ItemKind itemKind, ItemStatus status, String search);

    CatalogItem save(CatalogItem item);

    boolean existsByEntityIdAndItemCode(UUID entityId, String itemCode);
}
