package com.mtms.capital.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class CapitalCommitment {
    private UUID id;
    private UUID agreementId;
    private BigDecimal committedValue;
    private BigDecimal recognizedNonCashValue;
    private LocalDate commitmentDueDate;
    private CommitmentStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public CapitalCommitment() {}

    public CapitalCommitment(UUID id, UUID agreementId, BigDecimal committedValue,
                             BigDecimal recognizedNonCashValue, LocalDate commitmentDueDate,
                             CommitmentStatus status, String notes,
                             Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.agreementId = agreementId;
        this.committedValue = committedValue != null ? committedValue : BigDecimal.ZERO;
        this.recognizedNonCashValue = recognizedNonCashValue != null ? recognizedNonCashValue : BigDecimal.ZERO;
        this.commitmentDueDate = commitmentDueDate;
        this.status = status != null ? status : CommitmentStatus.PENDING;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CapitalCommitment create(UUID agreementId, BigDecimal committedValue,
                                           BigDecimal recognizedNonCashValue, LocalDate commitmentDueDate,
                                           String notes) {
        Instant now = Instant.now();
        return new CapitalCommitment(UUID.randomUUID(), agreementId, committedValue,
                recognizedNonCashValue, commitmentDueDate, CommitmentStatus.PENDING,
                notes, now, now);
    }

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
