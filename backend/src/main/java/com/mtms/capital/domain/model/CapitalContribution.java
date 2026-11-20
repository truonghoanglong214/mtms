package com.mtms.capital.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class CapitalContribution {
    private UUID id;
    private UUID commitmentId;
    private LocalDate contributedOn;
    private ContributionType contributionType;
    private BigDecimal recognizedValue;
    private UUID accountId;
    private UUID assetId;
    private UUID ledgerEntryId;
    private UUID evidenceFileId;
    private String notes;
    private UUID createdBy;
    private Instant createdAt;

    public CapitalContribution() {}

    public CapitalContribution(UUID id, UUID commitmentId, LocalDate contributedOn,
                               ContributionType contributionType, BigDecimal recognizedValue,
                               UUID accountId, UUID assetId, UUID ledgerEntryId,
                               UUID evidenceFileId, String notes, UUID createdBy,
                               Instant createdAt) {
        this.id = id;
        this.commitmentId = commitmentId;
        this.contributedOn = contributedOn;
        this.contributionType = contributionType;
        this.recognizedValue = recognizedValue;
        this.accountId = accountId;
        this.assetId = assetId;
        this.ledgerEntryId = ledgerEntryId;
        this.evidenceFileId = evidenceFileId;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static CapitalContribution create(UUID commitmentId, LocalDate contributedOn,
                                             ContributionType contributionType, BigDecimal recognizedValue,
                                             UUID accountId, UUID assetId, UUID ledgerEntryId,
                                             UUID evidenceFileId, String notes, UUID createdBy) {
        return new CapitalContribution(UUID.randomUUID(), commitmentId, contributedOn,
                contributionType, recognizedValue, accountId, assetId, ledgerEntryId,
                evidenceFileId, notes, createdBy, Instant.now());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCommitmentId() { return commitmentId; }
    public void setCommitmentId(UUID commitmentId) { this.commitmentId = commitmentId; }

    public LocalDate getContributedOn() { return contributedOn; }
    public void setContributedOn(LocalDate contributedOn) { this.contributedOn = contributedOn; }

    public ContributionType getContributionType() { return contributionType; }
    public void setContributionType(ContributionType contributionType) { this.contributionType = contributionType; }

    public BigDecimal getRecognizedValue() { return recognizedValue; }
    public void setRecognizedValue(BigDecimal recognizedValue) { this.recognizedValue = recognizedValue; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }

    public UUID getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(UUID ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }

    public UUID getEvidenceFileId() { return evidenceFileId; }
    public void setEvidenceFileId(UUID evidenceFileId) { this.evidenceFileId = evidenceFileId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
