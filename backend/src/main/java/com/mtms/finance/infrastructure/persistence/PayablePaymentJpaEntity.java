package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.PayablePayment;
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

@Entity
@Table(name = "payable_payments", schema = "mtms")
public class PayablePaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "payable_id", nullable = false)
    private UUID payableId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    private PaymentMethod paymentMethod;

    @Column(name = "bank_reference", length = 160)
    private String bankReference;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "posted_by", nullable = false)
    private UUID postedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PayablePaymentJpaEntity() {}

    public PayablePaymentJpaEntity(
            UUID id,
            UUID payableId,
            UUID accountId,
            BigDecimal amount,
            LocalDate paidOn,
            PaymentMethod paymentMethod,
            String bankReference,
            UUID evidenceFileId,
            String status,
            UUID postedBy,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.payableId = payableId;
        this.accountId = accountId;
        this.amount = amount;
        this.paidOn = paidOn;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.BANK_TRANSFER;
        this.bankReference = bankReference;
        this.evidenceFileId = evidenceFileId;
        this.status = status != null ? status : "POSTED";
        this.postedBy = postedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PayablePaymentJpaEntity fromDomain(PayablePayment domain) {
        return new PayablePaymentJpaEntity(
                domain.getId(),
                domain.getPayableId(),
                domain.getAccountId(),
                domain.getAmount(),
                domain.getPaidOn(),
                domain.getPaymentMethod(),
                domain.getBankReference(),
                domain.getEvidenceFileId(),
                domain.getStatus(),
                domain.getPostedBy(),
                domain.getCreatedAt()
        );
    }

    public PayablePayment toDomain() {
        return new PayablePayment(
                id,
                payableId,
                accountId,
                amount,
                paidOn,
                paymentMethod,
                bankReference,
                evidenceFileId,
                status,
                postedBy,
                createdAt
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPayableId() { return payableId; }
    public void setPayableId(UUID payableId) { this.payableId = payableId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }
    public UUID getEvidenceFileId() { return evidenceFileId; }
    public void setEvidenceFileId(UUID evidenceFileId) { this.evidenceFileId = evidenceFileId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getPostedBy() { return postedBy; }
    public void setPostedBy(UUID postedBy) { this.postedBy = postedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
