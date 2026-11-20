package com.mtms.asset.infrastructure.persistence;

import com.mtms.asset.domain.model.AssetCategory;
import com.mtms.asset.domain.model.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataEquipmentAssetRepository extends JpaRepository<EquipmentAssetJpaEntity, UUID> {

    Optional<EquipmentAssetJpaEntity> findByAssetCode(String assetCode);

    boolean existsByAssetCode(String assetCode);

    boolean existsByAssetCodeAndIdNot(String assetCode, UUID id);

    List<EquipmentAssetJpaEntity> findAllByBranchIdOrderByCreatedAtDesc(UUID branchId);

    @Query("SELECT e FROM EquipmentAssetJpaEntity e WHERE e.branchId = :branchId " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:search IS NULL OR LOWER(e.assetCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.serialNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.model) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY e.createdAt DESC")
    Page<EquipmentAssetJpaEntity> findByFilter(@Param("branchId") UUID branchId,
                                               @Param("category") AssetCategory category,
                                               @Param("status") AssetStatus status,
                                               @Param("search") String search,
                                               Pageable pageable);

    long countByBranchIdAndStatus(UUID branchId, AssetStatus status);

    long countByBranchIdAndCategory(UUID branchId, AssetCategory category);

    @Query("SELECT COUNT(e) FROM EquipmentAssetJpaEntity e WHERE e.branchId = :branchId " +
           "AND e.warrantyUntil IS NOT NULL " +
           "AND e.warrantyUntil >= :today AND e.warrantyUntil <= :thresholdDate")
    long countExpiringWarranty(@Param("branchId") UUID branchId,
                               @Param("today") LocalDate today,
                               @Param("thresholdDate") LocalDate thresholdDate);

    @Query("SELECT COUNT(e) FROM EquipmentAssetJpaEntity e WHERE e.branchId = :branchId " +
           "AND e.warrantyUntil IS NOT NULL AND e.warrantyUntil < :today")
    long countExpiredWarranty(@Param("branchId") UUID branchId,
                              @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(e.acquisitionCost), 0) FROM EquipmentAssetJpaEntity e WHERE e.branchId = :branchId AND e.status != 'DISPOSED'")
    BigDecimal calculateTotalAcquisitionCost(@Param("branchId") UUID branchId);
}
