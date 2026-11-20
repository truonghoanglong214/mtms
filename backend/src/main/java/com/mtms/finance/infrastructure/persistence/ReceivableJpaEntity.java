package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.Receivable;
import com.mtms.finance.domain.ReceivableStatus;
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
@Table(name = "receivables", schema = "mtms")
public class ReceivableJpaEntity {

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

    @Column(name = "received_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal receivedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceivableStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReceivableJpaEntity() {}

    public ReceivableJpaEntity(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String counterpartyLabel,
            String sourceType,
            UUID sourceId,
            LocalDate incurredDate,
            LocalDate dueDate,
            BigDecimal originalAmount,
            BigDecimal receivedAmount,
            ReceivableStatus status,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.counterpartyLabel = counterpartyLabel;
        this.sourceType = sourceType != null ? sourceType : "MANUAL_RECEIVABLE";
        this.sourceId = sourceId;
        this.incurredDate = incurredDate;
        this.dueDate = dueDate;
        this.originalAmount = originalAmount;
        this.receivedAmount = receivedAmount != null ? receivedAmount : BigDecimal.ZERO;
        this.status = status != null ? status : ReceivableStatus.OPEN;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ReceivableJpaEntity fromDomain(Receivable domain) {
        return new ReceivableJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getCounterpartyLabel(),
                domain.getSourceType(),
                domain.getSourceId(),
                domain.getIncurredDate(),
                domain.getDueDate(),
                domain.getOriginalAmount(),
                domain.getReceivedAmount(),
                domain.getStatus(),
                domain.getNotes(),
                domain.getCreatedBy(),
                domain.getCreatedAt()
        );
    }

    public Receivable toDomain() {
        return new Receivable(
                id,
                businessEntityId,
                branchId,
                counterpartyLabel,
                sourceType,
                sourceId,
                incurredDate,
                dueDate,
                originalAmount,
                receivedAmount,
                status,
                notes,
                createdBy,
                createdAt
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public void setBusinessEntityId(UUID businessEntityId) { this.businessEntityId = businessEntityId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public String getCounterpartyLabel() { return counterpartyLabel; }
    public void setCounterpartyLabel(String counterpartyLabel) { this.counterpartyLabel = counterpartyLabel; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    public LocalDate getIncurredDate() { return incurredDate; }
    public void setIncurredDate(LocalDate incurredDate) { this.incurredDate = incurredDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public ReceivableStatus getStatus() { return status; }
    public void setStatus(ReceivableStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
