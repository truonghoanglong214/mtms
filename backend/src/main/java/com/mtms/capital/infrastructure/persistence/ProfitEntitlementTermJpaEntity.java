package com.mtms.capital.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "profit_entitlement_terms", schema = "mtms")
public class ProfitEntitlementTermJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "agreement_id", nullable = false)
    private UUID agreementId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "profit_share_ratio", nullable = false, precision = 9, scale = 6)
    private BigDecimal profitShareRatio;

    @Column(name = "eligibility_conditions", columnDefinition = "TEXT")
    private String eligibilityConditions;

    @Column(name = "approved_by", nullable = false)
    private UUID approvedBy;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ProfitEntitlementTermJpaEntity() {}

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
