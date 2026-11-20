package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.Expense;
import com.mtms.finance.domain.ExpenseStatus;
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
@Table(name = "expenses", schema = "mtms")
public class ExpenseJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "expense_code", nullable = false, length = 60)
    private String expenseCode;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_paid", nullable = false)
    private boolean isPaid;

    @Column(name = "payment_account_id")
    private UUID paymentAccountId;

    @Column(name = "payable_id")
    private UUID payableId;

    @Column(name = "evidence_file_id")
    private UUID evidenceFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ExpenseStatus status;

    @Column(name = "review_flag", nullable = false)
    private boolean reviewFlag;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "voided_by")
    private UUID voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ExpenseJpaEntity() {
    }

    public ExpenseJpaEntity(
            UUID id,
            UUID branchId,
            String expenseCode,
            LocalDate expenseDate,
            UUID categoryId,
            BigDecimal amount,
            boolean isPaid,
            UUID paymentAccountId,
            UUID payableId,
            UUID evidenceFileId,
            ExpenseStatus status,
            boolean reviewFlag,
            String reason,
            UUID createdBy,
            UUID confirmedBy,
            Instant confirmedAt,
            UUID voidedBy,
            Instant voidedAt,
            String voidReason,
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.branchId = branchId;
        this.expenseCode = expenseCode;
        this.expenseDate = expenseDate;
        this.categoryId = categoryId;
        this.amount = amount;
        this.isPaid = isPaid;
        this.paymentAccountId = paymentAccountId;
        this.payableId = payableId;
        this.evidenceFileId = evidenceFileId;
        this.status = status != null ? status : ExpenseStatus.DRAFT;
        this.reviewFlag = reviewFlag;
        this.reason = reason;
        this.createdBy = createdBy;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
        this.voidedBy = voidedBy;
        this.voidedAt = voidedAt;
        this.voidReason = voidReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ExpenseJpaEntity fromDomain(Expense expense) {
        return new ExpenseJpaEntity(
                expense.getId(),
                expense.getBranchId(),
                expense.getExpenseCode(),
                expense.getExpenseDate(),
                expense.getCategoryId(),
                expense.getAmount(),
                expense.isPaid(),
                expense.getPaymentAccountId(),
                expense.getPayableId(),
                expense.getEvidenceFileId(),
                expense.getStatus(),
                expense.isReviewFlag(),
                expense.getReason(),
                expense.getCreatedBy(),
                expense.getConfirmedBy(),
                expense.getConfirmedAt(),
                expense.getVoidedBy(),
                expense.getVoidedAt(),
                expense.getVoidReason(),
                expense.getCreatedAt()
        );
    }

    public Expense toDomain() {
        return new Expense(
                id,
                branchId,
                expenseCode,
                expenseDate,
                categoryId,
                amount,
                isPaid,
                paymentAccountId,
                payableId,
                evidenceFileId,
                status,
                reviewFlag,
                reason,
                createdBy,
                confirmedBy,
                confirmedAt,
                voidedBy,
                voidedAt,
                voidReason,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public String getExpenseCode() {
        return expenseCode;
    }

    public void setExpenseCode(String expenseCode) {
        this.expenseCode = expenseCode;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public UUID getPaymentAccountId() {
        return paymentAccountId;
    }

    public void setPaymentAccountId(UUID paymentAccountId) {
        this.paymentAccountId = paymentAccountId;
    }

    public UUID getPayableId() {
        return payableId;
    }

    public void setPayableId(UUID payableId) {
        this.payableId = payableId;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public void setEvidenceFileId(UUID evidenceFileId) {
        this.evidenceFileId = evidenceFileId;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatus status) {
        this.status = status;
    }

    public boolean isReviewFlag() {
        return reviewFlag;
    }

    public void setReviewFlag(boolean reviewFlag) {
        this.reviewFlag = reviewFlag;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(UUID confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public UUID getVoidedBy() {
        return voidedBy;
    }

    public void setVoidedBy(UUID voidedBy) {
        this.voidedBy = voidedBy;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public void setVoidedAt(Instant voidedAt) {
        this.voidedAt = voidedAt;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
