package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.InternalTransfer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "internal_transfers", schema = "mtms")
public class InternalTransferJpaEntity {

    @Id
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public InternalTransferJpaEntity() {
    }

    public InternalTransferJpaEntity(
            UUID id,
            UUID branchId,
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            LocalDate businessDate,
            String status,
            String note,
            UUID evidenceFileId,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = id;
        this.branchId = branchId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.businessDate = businessDate;
        this.status = status != null ? status : "COMPLETED";
        this.note = note;
        this.evidenceFileId = evidenceFileId;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static InternalTransferJpaEntity fromDomain(InternalTransfer domain) {
        return new InternalTransferJpaEntity(
                domain.getId(),
                domain.getBranchId(),
                domain.getFromAccountId(),
                domain.getToAccountId(),
                domain.getAmount(),
                domain.getBusinessDate(),
                domain.getStatus(),
                domain.getNote(),
                domain.getEvidenceFileId(),
                domain.getCreatedBy(),
                domain.getCreatedAt()
        );
    }

    public InternalTransfer toDomain() {
        return new InternalTransfer(
                this.id,
                this.branchId,
                this.fromAccountId,
                this.toAccountId,
                this.amount,
                this.businessDate,
                this.status,
                this.note,
                this.evidenceFileId,
                this.createdBy,
                this.createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(UUID fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(UUID toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public void setEvidenceFileId(UUID evidenceFileId) {
        this.evidenceFileId = evidenceFileId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
