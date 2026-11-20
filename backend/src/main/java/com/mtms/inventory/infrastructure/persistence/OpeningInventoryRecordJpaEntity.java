package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.OpeningInventoryRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "opening_inventory_records", schema = "mtms")
public class OpeningInventoryRecordJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false, unique = true)
    private UUID branchId;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "total_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalQuantity;

    @Column(name = "total_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "approved_by", nullable = false)
    private UUID approvedBy;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    @Column(name = "note")
    private String note;

    public OpeningInventoryRecordJpaEntity() {
    }

    public OpeningInventoryRecordJpaEntity(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            UUID importBatchId,
            LocalDate openingDate,
            int totalItems,
            BigDecimal totalQuantity,
            BigDecimal totalValue,
            String status,
            UUID approvedBy,
            Instant approvedAt,
            String note
    ) {
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.importBatchId = importBatchId;
        this.openingDate = openingDate;
        this.totalItems = totalItems;
        this.totalQuantity = totalQuantity;
        this.totalValue = totalValue;
        this.status = status != null ? status : "COMMITTED";
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt != null ? approvedAt : Instant.now();
        this.note = note;
    }

    public static OpeningInventoryRecordJpaEntity fromDomain(OpeningInventoryRecord domain) {
        return new OpeningInventoryRecordJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getImportBatchId(),
                domain.getOpeningDate(),
                domain.getTotalItems(),
                domain.getTotalQuantity(),
                domain.getTotalValue(),
                domain.getStatus(),
                domain.getApprovedBy(),
                domain.getApprovedAt(),
                domain.getNote()
        );
    }

    public OpeningInventoryRecord toDomain() {
        return new OpeningInventoryRecord(
                this.id,
                this.businessEntityId,
                this.branchId,
                this.importBatchId,
                this.openingDate,
                this.totalItems,
                this.totalQuantity,
                this.totalValue,
                this.status,
                this.approvedBy,
                this.approvedAt,
                this.note
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public void setBusinessEntityId(UUID businessEntityId) {
        this.businessEntityId = businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public void setImportBatchId(UUID importBatchId) {
        this.importBatchId = importBatchId;
    }

    public LocalDate getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(LocalDate openingDate) {
        this.openingDate = openingDate;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
