package com.mtms.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierPackConversionJpaRepository extends JpaRepository<SupplierPackConversionJpaEntity, UUID> {

    @Query("""
        SELECT c FROM SupplierPackConversionJpaEntity c
        JOIN FETCH c.supplier
        JOIN FETCH c.catalogItem
        WHERE c.businessEntityId = :entityId AND c.itemId = :itemId
        ORDER BY c.effectiveFrom DESC, c.packName ASC
    """)
    List<SupplierPackConversionJpaEntity> findByEntityIdAndItemIdWithDetails(
            @Param("entityId") UUID entityId,
            @Param("itemId") UUID itemId
    );

    @Query("""
        SELECT c FROM SupplierPackConversionJpaEntity c
        JOIN FETCH c.supplier
        JOIN FETCH c.catalogItem item
        JOIN FETCH item.baseUnit
        WHERE c.businessEntityId = :entityId
          AND c.itemId = :itemId
          AND (:supplierId IS NULL OR c.supplierId = :supplierId)
          AND c.effectiveFrom <= :date
          AND (c.effectiveTo IS NULL OR c.effectiveTo >= :date)
        ORDER BY c.effectiveFrom DESC
    """)
    List<SupplierPackConversionJpaEntity> findActiveConversions(
            @Param("entityId") UUID entityId,
            @Param("itemId") UUID itemId,
            @Param("supplierId") UUID supplierId,
            @Param("date") LocalDate date
    );

    boolean existsBySupplierIdAndItemIdAndPackNameAndEffectiveFrom(
            UUID supplierId,
            UUID itemId,
            String packName,
            LocalDate effectiveFrom
    );

    long countByBusinessEntityIdAndItemId(UUID businessEntityId, UUID itemId);
}
