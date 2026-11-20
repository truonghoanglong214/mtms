package com.mtms.capital.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class InvestorAgreement {
    private UUID id;
    private UUID investorId;
    private UUID branchId;
    private String agreementRef;
    private ContributionType contributionType;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private AgreementStatus status;
    private String eligibilityTerms;
    private UUID evidenceFileId;
    private Instant createdAt;
    private Instant updatedAt;

    public InvestorAgreement() {}

    public InvestorAgreement(UUID id, UUID investorId, UUID branchId, String agreementRef,
                             ContributionType contributionType, LocalDate effectiveFrom,
                             LocalDate effectiveTo, AgreementStatus status,
                             String eligibilityTerms, UUID evidenceFileId,
                             Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.investorId = investorId;
        this.branchId = branchId;
        this.agreementRef = agreementRef;
        this.contributionType = contributionType;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = status;
        this.eligibilityTerms = eligibilityTerms;
        this.evidenceFileId = evidenceFileId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InvestorAgreement create(UUID investorId, UUID branchId, String agreementRef,
                                           ContributionType contributionType, LocalDate effectiveFrom,
                                           LocalDate effectiveTo, String eligibilityTerms, UUID evidenceFileId) {
        Instant now = Instant.now();
        return new InvestorAgreement(UUID.randomUUID(), investorId, branchId, agreementRef,
                contributionType, effectiveFrom, effectiveTo, AgreementStatus.ACTIVE,
                eligibilityTerms, evidenceFileId, now, now);
    }

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
