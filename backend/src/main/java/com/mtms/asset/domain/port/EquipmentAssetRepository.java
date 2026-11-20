package com.mtms.asset.domain.port;

import com.mtms.asset.domain.model.AssetCategory;
import com.mtms.asset.domain.model.AssetStatus;
import com.mtms.asset.domain.model.EquipmentAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipmentAssetRepository {

    EquipmentAsset save(EquipmentAsset asset);

    Optional<EquipmentAsset> findById(UUID id);

    Optional<EquipmentAsset> findByAssetCode(String assetCode);

    boolean existsByAssetCode(String assetCode);

    boolean existsByAssetCodeAndIdNot(String assetCode, UUID id);

    Page<EquipmentAsset> findByBranchId(UUID branchId, AssetCategory category, AssetStatus status, String search, Pageable pageable);

    List<EquipmentAsset> findAllByBranchId(UUID branchId);

    long countByBranchIdAndStatus(UUID branchId, AssetStatus status);

    long countByBranchIdAndCategory(UUID branchId, AssetCategory category);

    long countExpiringWarranty(UUID branchId, LocalDate today, LocalDate thresholdDate);

    long countExpiredWarranty(UUID branchId, LocalDate today);

    BigDecimal calculateTotalAcquisitionCost(UUID branchId);

    void deleteById(UUID id);
}
