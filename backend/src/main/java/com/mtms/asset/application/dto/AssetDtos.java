package com.mtms.asset.application.dto;

import com.mtms.asset.domain.model.AssetCategory;
import com.mtms.asset.domain.model.AssetStatus;
import com.mtms.asset.domain.model.EquipmentAsset;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public final class AssetDtos {

    private AssetDtos() {}

    public record EquipmentAssetDto(
            UUID id,
            UUID branchId,
            String branchName,
            String assetCode,
            String name,
            AssetCategory category,
            String serialNumber,
            String model,
            LocalDate purchasedOn,
            BigDecimal acquisitionCost,
            LocalDate warrantyUntil,
            AssetStatus status,
            String locationInBranch,
            UUID supplierId,
            String supplierName,
            UUID evidenceFileId,
            String maintenanceNote,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static EquipmentAssetDto fromDomain(EquipmentAsset domain, String branchName, String supplierName) {
            return new EquipmentAssetDto(
                    domain.getId(),
                    domain.getBranchId(),
                    branchName,
                    domain.getAssetCode(),
                    domain.getName(),
                    domain.getCategory(),
                    domain.getSerialNumber(),
                    domain.getModel(),
                    domain.getPurchasedOn(),
                    domain.getAcquisitionCost(),
                    domain.getWarrantyUntil(),
                    domain.getStatus(),
                    domain.getLocationInBranch(),
                    domain.getSupplierId(),
                    supplierName,
                    domain.getEvidenceFileId(),
                    domain.getMaintenanceNote(),
                    domain.getCreatedAt(),
                    domain.getUpdatedAt()
            );
        }
    }

    public record CreateEquipmentAssetRequest(
            @NotBlank(message = "Mã tài sản không được để trống")
            @Size(max = 60, message = "Mã tài sản tối đa 60 ký tự")
            String assetCode,

            @NotBlank(message = "Tên thiết bị không được để trống")
            @Size(max = 160, message = "Tên thiết bị tối đa 160 ký tự")
            String name,

            @NotNull(message = "Nhóm tài sản không được để trống")
            AssetCategory category,

            @Size(max = 100, message = "Số serial tối đa 100 ký tự")
            String serialNumber,

            @Size(max = 100, message = "Model tối đa 100 ký tự")
            String model,

            LocalDate purchasedOn,

            @DecimalMin(value = "0.0", message = "Nguyên giá ban đầu không được âm")
            BigDecimal acquisitionCost,

            LocalDate warrantyUntil,

            AssetStatus status,

            @Size(max = 120, message = "Vị trí tại quán tối đa 120 ký tự")
            String locationInBranch,

            UUID supplierId,

            UUID evidenceFileId,

            String maintenanceNote
    ) {}

    public record UpdateEquipmentAssetRequest(
            @NotBlank(message = "Tên thiết bị không được để trống")
            @Size(max = 160, message = "Tên thiết bị tối đa 160 ký tự")
            String name,

            @NotNull(message = "Nhóm tài sản không được để trống")
            AssetCategory category,

            @Size(max = 100, message = "Số serial tối đa 100 ký tự")
            String serialNumber,

            @Size(max = 100, message = "Model tối đa 100 ký tự")
            String model,

            LocalDate purchasedOn,

            @DecimalMin(value = "0.0", message = "Nguyên giá ban đầu không được âm")
            BigDecimal acquisitionCost,

            LocalDate warrantyUntil,

            AssetStatus status,

            @Size(max = 120, message = "Vị trí tại quán tối đa 120 ký tự")
            String locationInBranch,

            UUID supplierId,

            UUID evidenceFileId,

            String maintenanceNote
    ) {}

    public record AssetSummaryDto(
            UUID branchId,
            long totalAssets,
            BigDecimal totalAcquisitionCost,
            long inUseCount,
            long inRepairCount,
            long storedCount,
            long disposedCount,
            long warrantyExpiringCount,
            long warrantyExpiredCount,
            Map<AssetCategory, Long> categoryBreakdown,
            String depreciationDisclaimer
    ) {}
}
