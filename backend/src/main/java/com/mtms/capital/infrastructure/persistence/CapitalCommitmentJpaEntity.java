package com.mtms.capital.infrastructure.persistence;

import com.mtms.capital.domain.model.CommitmentStatus;
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
@Table(name = "capital_commitments", schema = "mtms")
public class CapitalCommitmentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "agreement_id", nullable = false)
    private UUID agreementId;

    @Column(name = "committed_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal committedValue = BigDecimal.ZERO;

    @Column(name = "recognized_non_cash_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal recognizedNonCashValue = BigDecimal.ZERO;

    @Column(name = "commitment_due_date")
    private LocalDate commitmentDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private CommitmentStatus status = CommitmentStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CapitalCommitmentJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAgreementId() { return agreementId; }
    public void setAgreementId(UUID agreementId) { this.agreementId = agreementId; }

    public BigDecimal getCommittedValue() { return committedValue; }
    public void setCommittedValue(BigDecimal committedValue) { this.committedValue = committedValue; }

    public BigDecimal getRecognizedNonCashValue() { return recognizedNonCashValue; }
    public void setRecognizedNonCashValue(BigDecimal recognizedNonCashValue) { this.recognizedNonCashValue = recognizedNonCashValue; }

    public LocalDate getCommitmentDueDate() { return commitmentDueDate; }
    public void setCommitmentDueDate(LocalDate commitmentDueDate) { this.commitmentDueDate = commitmentDueDate; }

    public CommitmentStatus getStatus() { return status; }
    public void setStatus(CommitmentStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
