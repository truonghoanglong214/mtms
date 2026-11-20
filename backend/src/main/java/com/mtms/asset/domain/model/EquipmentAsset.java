package com.mtms.asset.domain.model;

import com.mtms.asset.domain.AssetExceptions.InvalidAssetCostException;
import com.mtms.asset.domain.AssetExceptions.InvalidAssetDataException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class EquipmentAsset {

    private final UUID id;
    private final UUID branchId;
    private String assetCode;
    private String name;
    private AssetCategory category;
    private String serialNumber;
    private String model;
    private LocalDate purchasedOn;
    private BigDecimal acquisitionCost;
    private LocalDate warrantyUntil;
    private AssetStatus status;
    private String locationInBranch;
    private UUID supplierId;
    private UUID evidenceFileId;
    private String maintenanceNote;
    private String depreciationPolicy;
    private final Instant createdAt;
    private Instant updatedAt;

    public EquipmentAsset(UUID id, UUID branchId, String assetCode, String name, AssetCategory category,
                          String serialNumber, String model, LocalDate purchasedOn, BigDecimal acquisitionCost,
                          LocalDate warrantyUntil, AssetStatus status, String locationInBranch, UUID supplierId,
                          UUID evidenceFileId, String maintenanceNote, String depreciationPolicy,
                          Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.branchId = Objects.requireNonNull(branchId, "Branch ID cannot be null");
        this.assetCode = validateAssetCode(assetCode);
        this.name = validateName(name);
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.serialNumber = serialNumber;
        this.model = model;
        this.purchasedOn = purchasedOn;
        this.acquisitionCost = validateCost(acquisitionCost);
        this.warrantyUntil = warrantyUntil;
        this.status = status != null ? status : AssetStatus.IN_USE;
        this.locationInBranch = locationInBranch;
        this.supplierId = supplierId;
        this.evidenceFileId = evidenceFileId;
        this.maintenanceNote = maintenanceNote;
        this.depreciationPolicy = depreciationPolicy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static String validateAssetCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidAssetDataException("Asset code cannot be blank");
        }
        return code.trim();
    }

    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidAssetDataException("Asset name cannot be blank");
        }
        return name.trim();
    }

    public static BigDecimal validateCost(BigDecimal cost) {
        if (cost == null) {
            return BigDecimal.ZERO;
        }
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAssetCostException("Acquisition cost cannot be negative: " + cost);
        }
        return cost;
    }

    public void update(String name, AssetCategory category, String serialNumber, String model,
                       LocalDate purchasedOn, BigDecimal acquisitionCost, LocalDate warrantyUntil,
                       AssetStatus status, String locationInBranch, UUID supplierId,
                       UUID evidenceFileId, String maintenanceNote) {
        this.name = validateName(name);
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.serialNumber = serialNumber;
        this.model = model;
        this.purchasedOn = purchasedOn;
        this.acquisitionCost = validateCost(acquisitionCost);
        this.warrantyUntil = warrantyUntil;
        if (status != null) {
            this.status = status;
        }
        this.locationInBranch = locationInBranch;
        this.supplierId = supplierId;
        this.evidenceFileId = evidenceFileId;
        this.maintenanceNote = maintenanceNote;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public String getAssetCode() { return assetCode; }
    public String getName() { return name; }
    public AssetCategory getCategory() { return category; }
    public String getSerialNumber() { return serialNumber; }
    public String getModel() { return model; }
    public LocalDate getPurchasedOn() { return purchasedOn; }
    public BigDecimal getAcquisitionCost() { return acquisitionCost; }
    public LocalDate getWarrantyUntil() { return warrantyUntil; }
    public AssetStatus getStatus() { return status; }
    public String getLocationInBranch() { return locationInBranch; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getEvidenceFileId() { return evidenceFileId; }
    public String getMaintenanceNote() { return maintenanceNote; }
    public String getDepreciationPolicy() { return depreciationPolicy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
