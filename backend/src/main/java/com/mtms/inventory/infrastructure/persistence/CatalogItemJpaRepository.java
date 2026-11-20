package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CatalogItemJpaRepository extends JpaRepository<CatalogItemJpaEntity, UUID> {

    Optional<CatalogItemJpaEntity> findByBusinessEntityIdAndId(UUID businessEntityId, UUID id);

    @Query("SELECT i FROM CatalogItemJpaEntity i JOIN FETCH i.baseUnit WHERE i.businessEntityId = :businessEntityId AND i.id = :id")
    Optional<CatalogItemJpaEntity> findByBusinessEntityIdAndIdWithBaseUnit(
            @Param("businessEntityId") UUID businessEntityId,
            @Param("id") UUID id
    );

    Optional<CatalogItemJpaEntity> findByBusinessEntityIdAndItemCode(UUID businessEntityId, String itemCode);

    boolean existsByBusinessEntityIdAndItemCode(UUID businessEntityId, String itemCode);

    @Query("""
        SELECT i FROM CatalogItemJpaEntity i
        JOIN FETCH i.baseUnit
        WHERE i.businessEntityId = :businessEntityId
          AND (:itemKind IS NULL OR i.itemKind = :itemKind)
          AND (:status IS NULL OR i.status = :status)
          AND (:search IS NULL OR LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY i.itemCode ASC
    """)
    List<CatalogItemJpaEntity> searchItems(
            @Param("businessEntityId") UUID businessEntityId,
            @Param("itemKind") ItemKind itemKind,
            @Param("status") ItemStatus status,
            @Param("search") String search
    );
}
