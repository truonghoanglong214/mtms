package com.mtms.movement.infrastructure.persistence;

import com.mtms.movement.domain.model.TransferStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_transfers", schema = "mtms")
public class StockTransferJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transfer_number", nullable = false, length = 64)
    private String transferNumber;

    @Column(name = "business_entity_id", nullable = false, updatable = false)
    private UUID businessEntityId;

    @Column(name = "from_branch_id", nullable = false, updatable = false)
    private UUID fromBranchId;

    @Column(name = "to_branch_id", nullable = false, updatable = false)
    private UUID toBranchId;

    @Column(name = "requested_on", nullable = false)
    private LocalDate requestedOn;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private TransferStatus status;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "dispatched_by")
    private UUID dispatchedBy;

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(name = "total_lines", nullable = false)
    private int totalLines;

    @Column(name = "total_dispatched_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalDispatchedQuantity;

    @Column(name = "total_received_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalReceivedQuantity;

    @Column(name = "total_transit_variance_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalTransitVarianceQuantity;

    @Column(name = "total_dispatched_cost", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalDispatchedCost;

    @Column(name = "total_received_cost", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalReceivedCost;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<StockTransferLineJpaEntity> lines = new ArrayList<>();

    public StockTransferJpaEntity() {}

    public StockTransferJpaEntity(
            UUID id,
            String transferNumber,
            UUID businessEntityId,
            UUID fromBranchId,
            UUID toBranchId,
            LocalDate requestedOn,
            Instant dispatchedAt,
            Instant completedAt,
            TransferStatus status,
            UUID requestedBy,
            UUID dispatchedBy,
            UUID receivedBy,
            int totalLines,
            BigDecimal totalDispatchedQuantity,
            BigDecimal totalReceivedQuantity,
            BigDecimal totalTransitVarianceQuantity,
            BigDecimal totalDispatchedCost,
            BigDecimal totalReceivedCost,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.transferNumber = transferNumber;
        this.businessEntityId = businessEntityId;
        this.fromBranchId = fromBranchId;
        this.toBranchId = toBranchId;
        this.requestedOn = requestedOn;
        this.dispatchedAt = dispatchedAt;
        this.completedAt = completedAt;
        this.status = status;
        this.requestedBy = requestedBy;
        this.dispatchedBy = dispatchedBy;
        this.receivedBy = receivedBy;
        this.totalLines = totalLines;
        this.totalDispatchedQuantity = totalDispatchedQuantity;
        this.totalReceivedQuantity = totalReceivedQuantity;
        this.totalTransitVarianceQuantity = totalTransitVarianceQuantity;
        this.totalDispatchedCost = totalDispatchedCost;
        this.totalReceivedCost = totalReceivedCost;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTransferNumber() {
        return transferNumber;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getFromBranchId() {
        return fromBranchId;
    }

    public UUID getToBranchId() {
        return toBranchId;
    }

    public LocalDate getRequestedOn() {
        return requestedOn;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public UUID getDispatchedBy() {
        return dispatchedBy;
    }

    public void setDispatchedBy(UUID dispatchedBy) {
        this.dispatchedBy = dispatchedBy;
    }

    public UUID getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(UUID receivedBy) {
        this.receivedBy = receivedBy;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public BigDecimal getTotalDispatchedQuantity() {
        return totalDispatchedQuantity;
    }

    public void setTotalDispatchedQuantity(BigDecimal totalDispatchedQuantity) {
        this.totalDispatchedQuantity = totalDispatchedQuantity;
    }

    public BigDecimal getTotalReceivedQuantity() {
        return totalReceivedQuantity;
    }

    public void setTotalReceivedQuantity(BigDecimal totalReceivedQuantity) {
        this.totalReceivedQuantity = totalReceivedQuantity;
    }

    public BigDecimal getTotalTransitVarianceQuantity() {
        return totalTransitVarianceQuantity;
    }

    public void setTotalTransitVarianceQuantity(BigDecimal totalTransitVarianceQuantity) {
        this.totalTransitVarianceQuantity = totalTransitVarianceQuantity;
    }

    public BigDecimal getTotalDispatchedCost() {
        return totalDispatchedCost;
    }

    public void setTotalDispatchedCost(BigDecimal totalDispatchedCost) {
        this.totalDispatchedCost = totalDispatchedCost;
    }

    public BigDecimal getTotalReceivedCost() {
        return totalReceivedCost;
    }

    public void setTotalReceivedCost(BigDecimal totalReceivedCost) {
        this.totalReceivedCost = totalReceivedCost;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<StockTransferLineJpaEntity> getLines() {
        return lines;
    }

    public void setLines(List<StockTransferLineJpaEntity> lines) {
        this.lines = lines;
    }
}
