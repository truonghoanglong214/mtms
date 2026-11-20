package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.PaymentMethod;
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
 * JPA entity representing a tax payment settlement installment.
 */
@Entity
@Table(name = "tax_payments", schema = "mtms")
public class TaxPaymentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "obligation_id", nullable = false)
    private UUID obligationId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    private PaymentMethod paymentMethod;

    @Column(name = "financial_account_id")
    private UUID financialAccountId;

    @Column(name = "cash_ledger_entry_id")
    private UUID cashLedgerEntryId;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TaxPaymentJpaEntity() {}

    public TaxPaymentJpaEntity(
            UUID id,
            UUID obligationId,
            UUID branchId,
            LocalDate paymentDate,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            UUID financialAccountId,
            UUID cashLedgerEntryId,
            String receiptNumber,
            String note,
            UUID evidenceFileId,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = id;
        this.obligationId = obligationId;
        this.branchId = branchId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.BANK_TRANSFER;
        this.financialAccountId = financialAccountId;
        this.cashLedgerEntryId = cashLedgerEntryId;
        this.receiptNumber = receiptNumber;
        this.note = note;
        this.evidenceFileId = evidenceFileId;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getObligationId() {
        return obligationId;
    }

    public void setObligationId(UUID obligationId) {
        this.obligationId = obligationId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public UUID getFinancialAccountId() {
        return financialAccountId;
    }

    public void setFinancialAccountId(UUID financialAccountId) {
        this.financialAccountId = financialAccountId;
    }

    public UUID getCashLedgerEntryId() {
        return cashLedgerEntryId;
    }

    public void setCashLedgerEntryId(UUID cashLedgerEntryId) {
        this.cashLedgerEntryId = cashLedgerEntryId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
