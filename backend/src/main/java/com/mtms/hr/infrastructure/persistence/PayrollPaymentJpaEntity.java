package com.mtms.hr.infrastructure.persistence;

import com.mtms.hr.domain.PaymentMethod;
import com.mtms.hr.domain.PayrollPaymentStatus;
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
 * P0-HR-07 / BR-HR-06 / TC-016
 * JPA entity mapping mtms.payroll_payments.
 */
@Entity
@Table(name = "payroll_payments", schema = "mtms")
public class PayrollPaymentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "payroll_line_id", nullable = false)
    private UUID payrollLineId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollPaymentStatus status;

    @Column(name = "external_reference", length = 160)
    private String externalReference;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PayrollPaymentJpaEntity() {
    }

    public PayrollPaymentJpaEntity(
            UUID id,
            UUID payrollLineId,
            UUID accountId,
            BigDecimal amount,
            LocalDate paidOn,
            PaymentMethod paymentMethod,
            PayrollPaymentStatus status,
            String externalReference,
            UUID evidenceFileId,
            UUID ledgerEntryId,
            String note,
            UUID confirmedBy,
            Instant createdAt
    ) {
        this.id = id;
        this.payrollLineId = payrollLineId;
        this.accountId = accountId;
        this.amount = amount;
        this.paidOn = paidOn;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.BANK_TRANSFER;
        this.status = status != null ? status : PayrollPaymentStatus.POSTED;
        this.externalReference = externalReference;
        this.evidenceFileId = evidenceFileId;
        this.ledgerEntryId = ledgerEntryId;
        this.note = note;
        this.confirmedBy = confirmedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPayrollLineId() {
        return payrollLineId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PayrollPaymentStatus getStatus() {
        return status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public UUID getLedgerEntryId() {
        return ledgerEntryId;
    }

    public String getNote() {
        return note;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setStatus(PayrollPaymentStatus status) {
        this.status = status;
    }

    public void setLedgerEntryId(UUID ledgerEntryId) {
        this.ledgerEntryId = ledgerEntryId;
    }
}
