package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.Payable;
import com.mtms.inventory.domain.PayableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payables", schema = "mtms")
public class PayableJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "counterparty_label", nullable = false, length = 200)
    private String counterpartyLabel;

    @Column(name = "source_type", nullable = false, length = 48)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "incurred_date", nullable = false)
    private LocalDate incurredDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "original_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayableStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PayableJpaEntity() {
    }

    public PayableJpaEntity(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String counterpartyLabel,
            String sourceType,
            UUID sourceId,
            LocalDate incurredDate,
            LocalDate dueDate,
            BigDecimal originalAmount,
            BigDecimal paidAmount,
            PayableStatus status,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.counterpartyLabel = counterpartyLabel;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.incurredDate = incurredDate;
        this.dueDate = dueDate;
        this.originalAmount = originalAmount;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.status = status != null ? status : PayableStatus.OPEN;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PayableJpaEntity fromDomain(Payable payable) {
        return new PayableJpaEntity(
                payable.getId(),
                payable.getBusinessEntityId(),
                payable.getBranchId(),
                payable.getCounterpartyLabel(),
                payable.getSourceType(),
                payable.getSourceId(),
                payable.getIncurredDate(),
                payable.getDueDate(),
                payable.getOriginalAmount(),
                payable.getPaidAmount(),
                payable.getStatus(),
                payable.getCreatedAt()
        );
    }

    public Payable toDomain() {
        return new Payable(
                id,
                businessEntityId,
                branchId,
                counterpartyLabel,
                sourceType,
                sourceId,
                incurredDate,
                dueDate,
                originalAmount,
                paidAmount,
                status,
                createdAt
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

    public String getCounterpartyLabel() {
        return counterpartyLabel;
    }

    public void setCounterpartyLabel(String counterpartyLabel) {
        this.counterpartyLabel = counterpartyLabel;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public LocalDate getIncurredDate() {
        return incurredDate;
    }

    public void setIncurredDate(LocalDate incurredDate) {
        this.incurredDate = incurredDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public PayableStatus getStatus() {
        return status;
    }

    public void setStatus(PayableStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
