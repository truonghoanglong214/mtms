package com.mtms.asset.infrastructure.persistence;

import com.mtms.asset.domain.model.AssetCategory;
import com.mtms.asset.domain.model.AssetStatus;
import com.mtms.asset.domain.model.EquipmentAsset;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "equipment_assets", schema = "mtms")
public class EquipmentAssetJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "asset_code", nullable = false, unique = true, length = 60)
    private String assetCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private AssetCategory category;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "purchased_on")
    private LocalDate purchasedOn;

    @Column(name = "acquisition_cost", nullable = false, precision = 20, scale = 2)
    private BigDecimal acquisitionCost;

    @Column(name = "warranty_until")
    private LocalDate warrantyUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private AssetStatus status;

    @Column(name = "location_in_branch", length = 120)
    private String locationInBranch;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "maintenance_note", columnDefinition = "TEXT")
    private String maintenanceNote;

    @Column(name = "depreciation_policy", columnDefinition = "TEXT")
    private String depreciationPolicy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EquipmentAssetJpaEntity() {}

    public static EquipmentAssetJpaEntity fromDomain(EquipmentAsset domain) {
        EquipmentAssetJpaEntity entity = new EquipmentAssetJpaEntity();
        entity.id = domain.getId();
        entity.branchId = domain.getBranchId();
        entity.assetCode = domain.getAssetCode();
        entity.name = domain.getName();
        entity.category = domain.getCategory();
        entity.serialNumber = domain.getSerialNumber();
        entity.model = domain.getModel();
        entity.purchasedOn = domain.getPurchasedOn();
        entity.acquisitionCost = domain.getAcquisitionCost();
        entity.warrantyUntil = domain.getWarrantyUntil();
        entity.status = domain.getStatus();
        entity.locationInBranch = domain.getLocationInBranch();
        entity.supplierId = domain.getSupplierId();
        entity.evidenceFileId = domain.getEvidenceFileId();
        entity.maintenanceNote = domain.getMaintenanceNote();
        entity.depreciationPolicy = domain.getDepreciationPolicy();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }

    public EquipmentAsset toDomain() {
        return new EquipmentAsset(
                this.id,
                this.branchId,
                this.assetCode,
                this.name,
                this.category,
                this.serialNumber,
                this.model,
                this.purchasedOn,
                this.acquisitionCost,
                this.warrantyUntil,
                this.status,
                this.locationInBranch,
                this.supplierId,
                this.evidenceFileId,
                this.maintenanceNote,
                this.depreciationPolicy,
                this.createdAt,
                this.updatedAt
        );
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AssetCategory getCategory() { return category; }
    public void setCategory(AssetCategory category) { this.category = category; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public LocalDate getPurchasedOn() { return purchasedOn; }
    public void setPurchasedOn(LocalDate purchasedOn) { this.purchasedOn = purchasedOn; }
    public BigDecimal getAcquisitionCost() { return acquisitionCost; }
    public void setAcquisitionCost(BigDecimal acquisitionCost) { this.acquisitionCost = acquisitionCost; }
    public LocalDate getWarrantyUntil() { return warrantyUntil; }
    public void setWarrantyUntil(LocalDate warrantyUntil) { this.warrantyUntil = warrantyUntil; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public String getLocationInBranch() { return locationInBranch; }
    public void setLocationInBranch(String locationInBranch) { this.locationInBranch = locationInBranch; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public UUID getEvidenceFileId() { return evidenceFileId; }
    public void setEvidenceFileId(UUID evidenceFileId) { this.evidenceFileId = evidenceFileId; }
    public String getMaintenanceNote() { return maintenanceNote; }
    public void setMaintenanceNote(String maintenanceNote) { this.maintenanceNote = maintenanceNote; }
    public String getDepreciationPolicy() { return depreciationPolicy; }
    public void setDepreciationPolicy(String depreciationPolicy) { this.depreciationPolicy = depreciationPolicy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
