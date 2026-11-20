package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.PaymentStatus;
import com.mtms.inventory.domain.PurchaseReceipt;
import com.mtms.inventory.domain.PurchaseReceiptLine;
import com.mtms.inventory.domain.ReceiptStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "purchase_receipts", schema = "mtms")
public class PurchaseReceiptJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "receipt_number", nullable = false, length = 60)
    private String receiptNumber;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "received_on", nullable = false)
    private LocalDate receivedOn;

    @Column(name = "invoice_number", length = 120)
    private String invoiceNumber;

    @Column(name = "invoice_total", nullable = false, precision = 20, scale = 2)
    private BigDecimal invoiceTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 24)
    private PaymentStatus paymentStatus;

    @Column(name = "payable_id")
    private UUID payableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceiptStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "posted_by")
    private UUID postedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<PurchaseReceiptLineJpaEntity> lines = new ArrayList<>();

    public PurchaseReceiptJpaEntity() {
    }

    public PurchaseReceiptJpaEntity(
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
            Instant createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.receiptNumber = receiptNumber;
        this.supplierId = supplierId;
        this.receivedOn = receivedOn;
        this.invoiceNumber = invoiceNumber;
        this.invoiceTotal = invoiceTotal != null ? invoiceTotal : BigDecimal.ZERO;
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.UNPAID;
        this.payableId = payableId;
        this.status = status != null ? status : ReceiptStatus.DRAFT;
        this.notes = notes;
        this.postedBy = postedBy;
        this.postedAt = postedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static PurchaseReceiptJpaEntity fromDomain(PurchaseReceipt receipt) {
        PurchaseReceiptJpaEntity entity = new PurchaseReceiptJpaEntity(
                receipt.getId(),
                receipt.getBusinessEntityId(),
                receipt.getBranchId(),
                receipt.getReceiptNumber(),
                receipt.getSupplierId(),
                receipt.getReceivedOn(),
                receipt.getInvoiceNumber(),
                receipt.getInvoiceTotal(),
                receipt.getPaymentStatus(),
                receipt.getPayableId(),
                receipt.getStatus(),
                receipt.getNotes(),
                receipt.getPostedBy(),
                receipt.getPostedAt(),
                receipt.getCreatedAt()
        );
        for (PurchaseReceiptLine line : receipt.getLines()) {
            PurchaseReceiptLineJpaEntity lineEntity = PurchaseReceiptLineJpaEntity.fromDomain(line);
            lineEntity.setReceipt(entity);
            entity.addLine(lineEntity);
        }
        return entity;
    }

    public PurchaseReceipt toDomain() {
        List<PurchaseReceiptLine> domainLines = lines.stream()
                .map(PurchaseReceiptLineJpaEntity::toDomain)
                .collect(Collectors.toList());
        return new PurchaseReceipt(
                id,
                businessEntityId,
                branchId,
                receiptNumber,
                supplierId,
                receivedOn,
                invoiceNumber,
                invoiceTotal,
                paymentStatus,
                payableId,
                status,
                notes,
                postedBy,
                postedAt,
                createdAt,
                domainLines
        );
    }

    public void addLine(PurchaseReceiptLineJpaEntity line) {
        line.setReceipt(this);
        this.lines.add(line);
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

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public void setReceivedOn(LocalDate receivedOn) {
        this.receivedOn = receivedOn;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getInvoiceTotal() {
        return invoiceTotal;
    }

    public void setInvoiceTotal(BigDecimal invoiceTotal) {
        this.invoiceTotal = invoiceTotal;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public UUID getPayableId() {
        return payableId;
    }

    public void setPayableId(UUID payableId) {
        this.payableId = payableId;
    }

    public ReceiptStatus getStatus() {
        return status;
    }

    public void setStatus(ReceiptStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(UUID postedBy) {
        this.postedBy = postedBy;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<PurchaseReceiptLineJpaEntity> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseReceiptLineJpaEntity> lines) {
        this.lines = lines;
    }
}
