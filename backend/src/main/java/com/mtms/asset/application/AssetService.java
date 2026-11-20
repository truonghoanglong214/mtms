package com.mtms.asset.application;

import com.mtms.asset.application.dto.AssetDtos.AssetSummaryDto;
import com.mtms.asset.application.dto.AssetDtos.CreateEquipmentAssetRequest;
import com.mtms.asset.application.dto.AssetDtos.EquipmentAssetDto;
import com.mtms.asset.application.dto.AssetDtos.UpdateEquipmentAssetRequest;
import com.mtms.asset.domain.AssetExceptions.AssetCodeConflictException;
import com.mtms.asset.domain.AssetExceptions.AssetNotFoundException;
import com.mtms.asset.domain.model.AssetCategory;
import com.mtms.asset.domain.model.AssetStatus;
import com.mtms.asset.domain.model.EquipmentAsset;
import com.mtms.asset.domain.port.EquipmentAssetRepository;
import com.mtms.inventory.infrastructure.persistence.SupplierJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AssetService {

    public static final String DEPRECIATION_DISCLAIMER_P0 =
            "P0 P&L excludes depreciation (TC-051 / ADR-023). Chi phí khấu hao chưa được trích vào P&L tạm tính giai đoạn MVP.";

    private final EquipmentAssetRepository assetRepository;
    private final BranchJpaRepository branchRepository;
    private final SupplierJpaRepository supplierRepository;

    public AssetService(EquipmentAssetRepository assetRepository,
                        BranchJpaRepository branchRepository,
                        SupplierJpaRepository supplierRepository) {
        this.assetRepository = assetRepository;
        this.branchRepository = branchRepository;
        this.supplierRepository = supplierRepository;
    }

    public EquipmentAssetDto createAsset(UUID branchId, CreateEquipmentAssetRequest request) {
        String assetCode = request.assetCode().trim();
        if (assetRepository.existsByAssetCode(assetCode)) {
            throw new AssetCodeConflictException("Mã tài sản '" + assetCode + "' đã tồn tại trong hệ thống");
        }

        EquipmentAsset asset = new EquipmentAsset(
                null,
                branchId,
                assetCode,
                request.name(),
                request.category(),
                request.serialNumber(),
                request.model(),
                request.purchasedOn(),
                request.acquisitionCost() != null ? request.acquisitionCost() : BigDecimal.ZERO,
                request.warrantyUntil(),
                request.status() != null ? request.status() : AssetStatus.IN_USE,
                request.locationInBranch(),
                request.supplierId(),
                request.evidenceFileId(),
                request.maintenanceNote(),
                null,
                null,
                null
        );

        EquipmentAsset saved = assetRepository.save(asset);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<EquipmentAssetDto> getAssets(UUID branchId, AssetCategory category, AssetStatus status,
                                            String search, Pageable pageable) {
        return assetRepository.findByBranchId(branchId, category, status, search, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public EquipmentAssetDto getAssetById(UUID branchId, UUID assetId) {
        EquipmentAsset asset = assetRepository.findById(assetId)
                .filter(a -> a.getBranchId().equals(branchId))
                .orElseThrow(() -> new AssetNotFoundException("Không tìm thấy thiết bị với ID: " + assetId + " tại chi nhánh này"));
        return toDto(asset);
    }

    public EquipmentAssetDto updateAsset(UUID branchId, UUID assetId, UpdateEquipmentAssetRequest request) {
        EquipmentAsset asset = assetRepository.findById(assetId)
                .filter(a -> a.getBranchId().equals(branchId))
                .orElseThrow(() -> new AssetNotFoundException("Không tìm thấy thiết bị với ID: " + assetId + " tại chi nhánh này"));

        asset.update(
                request.name(),
                request.category(),
                request.serialNumber(),
                request.model(),
                request.purchasedOn(),
                request.acquisitionCost() != null ? request.acquisitionCost() : BigDecimal.ZERO,
                request.warrantyUntil(),
                request.status(),
                request.locationInBranch(),
                request.supplierId(),
                request.evidenceFileId(),
                request.maintenanceNote()
        );

        EquipmentAsset updated = assetRepository.save(asset);
        return toDto(updated);
    }

    public void deleteAsset(UUID branchId, UUID assetId) {
        EquipmentAsset asset = assetRepository.findById(assetId)
                .filter(a -> a.getBranchId().equals(branchId))
                .orElseThrow(() -> new AssetNotFoundException("Không tìm thấy thiết bị với ID: " + assetId + " tại chi nhánh này"));
        assetRepository.deleteById(asset.getId());
    }

    @Transactional(readOnly = true)
    public AssetSummaryDto getAssetSummary(UUID branchId) {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        long inUse = assetRepository.countByBranchIdAndStatus(branchId, AssetStatus.IN_USE);
        long inRepair = assetRepository.countByBranchIdAndStatus(branchId, AssetStatus.IN_REPAIR);
        long stored = assetRepository.countByBranchIdAndStatus(branchId, AssetStatus.STORED);
        long disposed = assetRepository.countByBranchIdAndStatus(branchId, AssetStatus.DISPOSED);
        long transferred = assetRepository.countByBranchIdAndStatus(branchId, AssetStatus.TRANSFERRED);

        long totalAssets = inUse + inRepair + stored + disposed + transferred;
        BigDecimal totalCost = assetRepository.calculateTotalAcquisitionCost(branchId);

        long expiring = assetRepository.countExpiringWarranty(branchId, today, threshold);
        long expired = assetRepository.countExpiredWarranty(branchId, today);

        Map<AssetCategory, Long> categoryMap = new EnumMap<>(AssetCategory.class);
        for (AssetCategory cat : AssetCategory.values()) {
            categoryMap.put(cat, assetRepository.countByBranchIdAndCategory(branchId, cat));
        }

        return new AssetSummaryDto(
                branchId,
                totalAssets,
                totalCost,
                inUse,
                inRepair,
                stored,
                disposed,
                expiring,
                expired,
                categoryMap,
                DEPRECIATION_DISCLAIMER_P0
        );
    }

    private EquipmentAssetDto toDto(EquipmentAsset asset) {
        String branchName = branchRepository.findById(asset.getBranchId())
                .map(com.mtms.organization.infrastructure.persistence.BranchJpaEntity::getName)
                .orElse("Chi nhánh");

        String supplierName = null;
        if (asset.getSupplierId() != null) {
            supplierName = supplierRepository.findById(asset.getSupplierId())
                    .map(com.mtms.inventory.infrastructure.persistence.SupplierJpaEntity::getName)
                    .orElse(null);
        }

        return EquipmentAssetDto.fromDomain(asset, branchName, supplierName);
    }
}
