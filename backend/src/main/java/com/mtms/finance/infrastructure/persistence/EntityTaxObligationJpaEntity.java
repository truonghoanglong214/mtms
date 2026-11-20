package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.TaxObligationStatus;
import com.mtms.finance.domain.TaxObligationType;
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

/**
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * JPA entity representing a manual tax obligation.
 */
@Entity
@Table(name = "entity_tax_obligations", schema = "mtms")
public class EntityTaxObligationJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "obligation_code", nullable = false, length = 60)
    private String obligationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_type", nullable = false, length = 40)
    private TaxObligationType obligationType;

    @Column(name = "tax_authority_name", length = 200)
    private String taxAuthorityName;

    @Column(name = "tax_period_start", nullable = false)
    private LocalDate taxPeriodStart;

    @Column(name = "tax_period_end", nullable = false)
    private LocalDate taxPeriodEnd;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "amount_due", nullable = false, precision = 20, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "amount_paid", nullable = false, precision = 20, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "amount_allocated", nullable = false, precision = 20, scale = 2)
    private BigDecimal amountAllocated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private TaxObligationStatus status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "entered_by", nullable = false)
    private UUID enteredBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public EntityTaxObligationJpaEntity() {}

    public EntityTaxObligationJpaEntity(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String obligationCode,
            TaxObligationType obligationType,
            String taxAuthorityName,
            LocalDate taxPeriodStart,
            LocalDate taxPeriodEnd,
            LocalDate dueDate,
            BigDecimal amountDue,
            BigDecimal amountPaid,
            BigDecimal amountAllocated,
            TaxObligationStatus status,
            String note,
            UUID evidenceFileId,
            UUID enteredBy,
            Instant createdAt
    ) {
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.obligationCode = obligationCode;
        this.obligationType = obligationType;
        this.taxAuthorityName = taxAuthorityName;
        this.taxPeriodStart = taxPeriodStart;
        this.taxPeriodEnd = taxPeriodEnd;
        this.dueDate = dueDate;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        this.amountAllocated = amountAllocated != null ? amountAllocated : BigDecimal.ZERO;
        this.status = status != null ? status : TaxObligationStatus.OPEN;
        this.note = note;
        this.evidenceFileId = evidenceFileId;
        this.enteredBy = enteredBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public void setBusinessEntityId(UUID businessEntityId) {
        this.businessEntityId = businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public String getObligationCode() {
        return obligationCode;
    }

    public void setObligationCode(String obligationCode) {
        this.obligationCode = obligationCode;
    }

    public TaxObligationType getObligationType() {
        return obligationType;
    }

    public void setObligationType(TaxObligationType obligationType) {
        this.obligationType = obligationType;
    }

    public String getTaxAuthorityName() {
        return taxAuthorityName;
    }

    public void setTaxAuthorityName(String taxAuthorityName) {
        this.taxAuthorityName = taxAuthorityName;
    }

    public LocalDate getTaxPeriodStart() {
        return taxPeriodStart;
    }

    public void setTaxPeriodStart(LocalDate taxPeriodStart) {
        this.taxPeriodStart = taxPeriodStart;
    }

    public LocalDate getTaxPeriodEnd() {
        return taxPeriodEnd;
    }

    public void setTaxPeriodEnd(LocalDate taxPeriodEnd) {
        this.taxPeriodEnd = taxPeriodEnd;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public void setAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getAmountAllocated() {
        return amountAllocated;
    }

    public void setAmountAllocated(BigDecimal amountAllocated) {
        this.amountAllocated = amountAllocated;
    }

    public TaxObligationStatus getStatus() {
        return status;
    }

    public void setStatus(TaxObligationStatus status) {
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

    public UUID getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(UUID enteredBy) {
        this.enteredBy = enteredBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
