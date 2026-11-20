package com.mtms.capital.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ProfitEntitlementTerm {
    private UUID id;
    private UUID agreementId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal profitShareRatio;
    private String eligibilityConditions;
    private UUID approvedBy;
    private UUID evidenceFileId;
    private Instant createdAt;

    public ProfitEntitlementTerm() {}

    public ProfitEntitlementTerm(UUID id, UUID agreementId, LocalDate effectiveFrom,
                                 LocalDate effectiveTo, BigDecimal profitShareRatio,
                                 String eligibilityConditions, UUID approvedBy,
                                 UUID evidenceFileId, Instant createdAt) {
        this.id = id;
        this.agreementId = agreementId;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.profitShareRatio = profitShareRatio;
        this.eligibilityConditions = eligibilityConditions;
        this.approvedBy = approvedBy;
        this.evidenceFileId = evidenceFileId;
        this.createdAt = createdAt;
    }

    public static ProfitEntitlementTerm create(UUID agreementId, LocalDate effectiveFrom,
                                               LocalDate effectiveTo, BigDecimal profitShareRatio,
                                               String eligibilityConditions, UUID approvedBy,
                                               UUID evidenceFileId) {
        return new ProfitEntitlementTerm(UUID.randomUUID(), agreementId, effectiveFrom,
                effectiveTo, profitShareRatio, eligibilityConditions, approvedBy,
                evidenceFileId, Instant.now());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAgreementId() { return agreementId; }
    public void setAgreementId(UUID agreementId) { this.agreementId = agreementId; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public BigDecimal getProfitShareRatio() { return profitShareRatio; }
    public void setProfitShareRatio(BigDecimal profitShareRatio) { this.profitShareRatio = profitShareRatio; }

    public String getEligibilityConditions() { return eligibilityConditions; }
    public void setEligibilityConditions(String eligibilityConditions) { this.eligibilityConditions = eligibilityConditions; }

    public UUID getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }

    public UUID getEvidenceFileId() { return evidenceFileId; }
    public void setEvidenceFileId(UUID evidenceFileId) { this.evidenceFileId = evidenceFileId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
