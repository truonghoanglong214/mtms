package com.mtms.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-FIN-05 / BR-FIN-02 / TC-009
 * Domain entity representing an installment payment against an outstanding payable debt.
 */
public class PayablePayment {

    private final UUID id;
    private final UUID payableId;
    private final UUID accountId;
    private final BigDecimal amount;
    private final LocalDate paidOn;
    private final PaymentMethod paymentMethod;
    private final String bankReference;
    private final UUID evidenceFileId;
    private final String status;
    private final UUID postedBy;
    private final Instant createdAt;

    public PayablePayment(
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
        this.payableId = Objects.requireNonNull(payableId, "payableId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        this.paidOn = Objects.requireNonNull(paidOn, "paidOn must not be null");
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.BANK_TRANSFER;
        this.bankReference = bankReference;
        this.evidenceFileId = evidenceFileId;
        this.status = status != null ? status : "POSTED";
        this.postedBy = Objects.requireNonNull(postedBy, "postedBy must not be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPayableId() { return payableId; }
    public UUID getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getPaidOn() { return paidOn; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getBankReference() { return bankReference; }
    public UUID getEvidenceFileId() { return evidenceFileId; }
    public String getStatus() { return status; }
    public UUID getPostedBy() { return postedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
