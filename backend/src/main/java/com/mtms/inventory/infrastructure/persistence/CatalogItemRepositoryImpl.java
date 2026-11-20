package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.CatalogItem;
import com.mtms.inventory.domain.CatalogItemRepository;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatalogItemRepositoryImpl implements CatalogItemRepository {

    private final CatalogItemJpaRepository jpaRepository;

    public CatalogItemRepositoryImpl(CatalogItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<CatalogItem> findById(UUID id) {
        return jpaRepository.findById(id).map(CatalogItemJpaEntity::toDomain);
    }

    @Override
    public Optional<CatalogItem> findByEntityIdAndId(UUID entityId, UUID id) {
        return jpaRepository.findByBusinessEntityIdAndId(entityId, id).map(CatalogItemJpaEntity::toDomain);
    }

    @Override
    public Optional<CatalogItem> findByEntityIdAndItemCode(UUID entityId, String itemCode) {
        return jpaRepository.findByBusinessEntityIdAndItemCode(entityId, itemCode).map(CatalogItemJpaEntity::toDomain);
    }

    @Override
    public List<CatalogItem> findByEntityId(UUID entityId, ItemKind itemKind, ItemStatus status, String search) {
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return jpaRepository.searchItems(entityId, itemKind, status, trimmedSearch)
                .stream()
                .map(CatalogItemJpaEntity::toDomain)
                .toList();
    }

    @Override
    public CatalogItem save(CatalogItem item) {
        CatalogItemJpaEntity entity = CatalogItemJpaEntity.fromDomain(item);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public boolean existsByEntityIdAndItemCode(UUID entityId, String itemCode) {
        return jpaRepository.existsByBusinessEntityIdAndItemCode(entityId, itemCode);
    }
}
