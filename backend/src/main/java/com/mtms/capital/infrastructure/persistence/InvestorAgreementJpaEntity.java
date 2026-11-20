package com.mtms.capital.infrastructure.persistence;

import com.mtms.capital.domain.model.AgreementStatus;
import com.mtms.capital.domain.model.ContributionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "investor_agreements", schema = "mtms")
public class InvestorAgreementJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "agreement_ref", nullable = false, length = 120)
    private String agreementRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_type", nullable = false, length = 20)
    private ContributionType contributionType;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgreementStatus status = AgreementStatus.ACTIVE;

    @Column(name = "eligibility_terms", columnDefinition = "TEXT")
    private String eligibilityTerms;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InvestorAgreementJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInvestorId() { return investorId; }
    public void setInvestorId(UUID investorId) { this.investorId = investorId; }

    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }

    public String getAgreementRef() { return agreementRef; }
    public void setAgreementRef(String agreementRef) { this.agreementRef = agreementRef; }

    public ContributionType getContributionType() { return contributionType; }
    public void setContributionType(ContributionType contributionType) { this.contributionType = contributionType; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public AgreementStatus getStatus() { return status; }
    public void setStatus(AgreementStatus status) { this.status = status; }

    public String getEligibilityTerms() { return eligibilityTerms; }
    public void setEligibilityTerms(String eligibilityTerms) { this.eligibilityTerms = eligibilityTerms; }

    public UUID getEvidenceFileId() { return evidenceFileId; }
    public void setEvidenceFileId(UUID evidenceFileId) { this.evidenceFileId = evidenceFileId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
