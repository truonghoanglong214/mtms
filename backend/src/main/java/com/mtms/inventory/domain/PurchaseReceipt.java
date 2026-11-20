package com.mtms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-INV-03 / BR-INV-05
 * Domain entity representing a goods purchase receipt from a supplier.
 */
public class PurchaseReceipt {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final String receiptNumber;
    private final UUID supplierId;
    private final LocalDate receivedOn;
    private final String invoiceNumber;
    private BigDecimal invoiceTotal;
    private PaymentStatus paymentStatus;
    private UUID payableId;
    private ReceiptStatus status;
    private String notes;
    private UUID postedBy;
    private Instant postedAt;
    private final Instant createdAt;
    private final List<PurchaseReceiptLine> lines = new ArrayList<>();

    public PurchaseReceipt(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String receiptNumber,
            UUID supplierId,
            LocalDate receivedOn,
            String invoiceNumber,
            BigDecimal invoiceTotal,
            PaymentStatus paymentStatus,
            UUID payableId,
            ReceiptStatus status,
            String notes,
            UUID postedBy,
            Instant postedAt,
            Instant createdAt,
            List<PurchaseReceiptLine> lines
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.receiptNumber = Objects.requireNonNull(receiptNumber, "receiptNumber must not be null");
        this.supplierId = supplierId;
        this.receivedOn = Objects.requireNonNull(receivedOn, "receivedOn must not be null");
        this.invoiceNumber = invoiceNumber;
        setInvoiceTotal(invoiceTotal != null ? invoiceTotal : BigDecimal.ZERO);
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.UNPAID;
        this.payableId = payableId;
        this.status = status != null ? status : ReceiptStatus.DRAFT;
        this.notes = notes;
        this.postedBy = postedBy;
        this.postedAt = postedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        if (lines != null) {
            this.lines.addAll(lines);
        }
    }

    public void post(UUID actorUserId) {
        if (this.status == ReceiptStatus.POSTED) {
            throw new ReceiptAlreadyPostedException("Receipt has already been posted: " + this.receiptNumber);
        }
        this.status = ReceiptStatus.POSTED;
        this.postedBy = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        this.postedAt = Instant.now();
    }

    public void setPayableId(UUID payableId) {
        this.payableId = payableId;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");
    }

    public void setInvoiceTotal(BigDecimal invoiceTotal) {
        Objects.requireNonNull(invoiceTotal, "invoiceTotal must not be null");
        if (invoiceTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invoice total must be non-negative (>= 0)");
        }
        this.invoiceTotal = invoiceTotal.setScale(2, RoundingMode.HALF_UP);
    }

    public void addLine(PurchaseReceiptLine line) {
        Objects.requireNonNull(line, "line must not be null");
        this.lines.add(line);
    }

    public void recalculateTotalFromLines() {
        BigDecimal sum = BigDecimal.ZERO;
        for (PurchaseReceiptLine line : this.lines) {
            sum = sum.add(line.getLineTotal());
        }
        this.invoiceTotal = sum.setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public BigDecimal getInvoiceTotal() {
        return invoiceTotal;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public UUID getPayableId() {
        return payableId;
    }

    public ReceiptStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public UUID getPostedBy() {
        return postedBy;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PurchaseReceiptLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
