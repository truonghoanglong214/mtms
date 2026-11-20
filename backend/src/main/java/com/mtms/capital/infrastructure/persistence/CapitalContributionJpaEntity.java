package com.mtms.capital.infrastructure.persistence;

import com.mtms.capital.domain.model.ContributionType;
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
@Table(name = "capital_contributions", schema = "mtms")
public class CapitalContributionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "commitment_id", nullable = false)
    private UUID commitmentId;

    @Column(name = "contributed_on", nullable = false)
    private LocalDate contributedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_type", nullable = false, length = 20)
    private ContributionType contributionType;

    @Column(name = "recognized_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal recognizedValue;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CapitalContributionJpaEntity() {}

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
