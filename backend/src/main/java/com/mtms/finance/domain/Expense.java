package com.mtms.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an operating expense (OPEX).
 */
public class Expense {

    private final UUID id;
    private final UUID branchId;
    private final String expenseCode;
    private final LocalDate expenseDate;
    private final UUID categoryId;
    private final BigDecimal amount;
    private final boolean isPaid;
    private final UUID paymentAccountId;
    private UUID payableId;
    private final UUID evidenceFileId;
    private ExpenseStatus status;
    private final boolean reviewFlag;
    private final String reason;
    private final UUID createdBy;
    private UUID confirmedBy;
    private Instant confirmedAt;
    private UUID voidedBy;
    private Instant voidedAt;
    private String voidReason;
    private final Instant createdAt;

    public Expense(
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
        this.branchId = Objects.requireNonNull(branchId, "Branch ID must not be null");
        this.expenseCode = Objects.requireNonNull(expenseCode, "Expense code must not be null");
        this.expenseDate = Objects.requireNonNull(expenseDate, "Expense date must not be null");
        this.categoryId = Objects.requireNonNull(categoryId, "Category ID must not be null");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền chi phí phải lớn hơn 0");
        }
        this.amount = amount;
        this.isPaid = isPaid;
        if (isPaid && paymentAccountId == null) {
            throw new IllegalArgumentException("Tài khoản thanh toán là bắt buộc khi chi phí đã thanh toán ngay");
        }
        this.paymentAccountId = paymentAccountId;
        this.payableId = payableId;
        this.evidenceFileId = evidenceFileId;
        this.status = status != null ? status : ExpenseStatus.DRAFT;
        this.reviewFlag = reviewFlag;
        this.reason = Objects.requireNonNull(reason, "Lý do chi phí không được để trống");
        this.createdBy = Objects.requireNonNull(createdBy, "Người tạo không được để trống");
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
        this.voidedBy = voidedBy;
        this.voidedAt = voidedAt;
        this.voidReason = voidReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void confirm(UUID actorUserId, Instant now) {
        if (this.status == ExpenseStatus.CONFIRMED) {
            throw new ExpenseAlreadyConfirmedException("Phiếu chi đã được xác nhận trước đó: " + this.expenseCode);
        }
        if (this.status == ExpenseStatus.VOIDED) {
            throw new ExpenseAlreadyVoidedException("Không thể xác nhận phiếu chi đã bị hủy: " + this.expenseCode);
        }
        this.status = ExpenseStatus.CONFIRMED;
        this.confirmedBy = Objects.requireNonNull(actorUserId, "Actor user ID must not be null");
        this.confirmedAt = now != null ? now : Instant.now();
    }

    public void voidExpense(UUID actorUserId, String reason, Instant now) {
        if (this.status == ExpenseStatus.VOIDED) {
            throw new ExpenseAlreadyVoidedException("Phiếu chi đã bị hủy trước đó: " + this.expenseCode);
        }
        this.status = ExpenseStatus.VOIDED;
        this.voidedBy = Objects.requireNonNull(actorUserId, "Actor user ID must not be null");
        this.voidReason = reason != null ? reason.trim() : "Hủy phiếu chi";
        this.voidedAt = now != null ? now : Instant.now();
    }

    public void linkPayable(UUID payableId) {
        this.payableId = payableId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getExpenseCode() {
        return expenseCode;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public UUID getPaymentAccountId() {
        return paymentAccountId;
    }

    public UUID getPayableId() {
        return payableId;
    }

    public UUID getEvidenceFileId() {
        return evidenceFileId;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public boolean isReviewFlag() {
        return reviewFlag;
    }

    public String getReason() {
        return reason;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public UUID getVoidedBy() {
        return voidedBy;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
