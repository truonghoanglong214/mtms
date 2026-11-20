package com.mtms.inventory.infrastructure.persistence;

import com.mtms.inventory.domain.PurchaseReceiptLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_receipt_lines", schema = "mtms")
public class PurchaseReceiptLineJpaEntity {

    @Id
    private UUID id;

    @Column(name = "receipt_id", nullable = false)
    private UUID receiptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", insertable = false, updatable = false)
    private PurchaseReceiptJpaEntity receipt;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "pack_name", length = 100)
    private String packName;

    @Column(name = "declared_pack_quantity", precision = 18, scale = 3)
    private BigDecimal declaredPackQuantity;

    @Column(name = "conversion_snapshot", nullable = false, precision = 18, scale = 6)
    private BigDecimal conversionSnapshot;

    @Column(name = "received_base_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal receivedBaseQuantity;

    @Column(name = "base_unit_cost", nullable = false, precision = 20, scale = 6)
    private BigDecimal baseUnitCost;

    @Column(name = "line_total", nullable = false, precision = 20, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "lot_code", length = 80)
    private String lotCode;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    public PurchaseReceiptLineJpaEntity() {
    }

    public PurchaseReceiptLineJpaEntity(
            UUID id,
            UUID receiptId,
            int lineNo,
            UUID itemId,
            String packName,
            BigDecimal declaredPackQuantity,
            BigDecimal conversionSnapshot,
            BigDecimal receivedBaseQuantity,
            BigDecimal baseUnitCost,
            BigDecimal lineTotal,
            String lotCode,
            LocalDate expiresOn
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.receiptId = receiptId;
        this.lineNo = lineNo;
        this.itemId = itemId;
        this.packName = packName;
        this.declaredPackQuantity = declaredPackQuantity;
        this.conversionSnapshot = conversionSnapshot != null ? conversionSnapshot : BigDecimal.ONE;
        this.receivedBaseQuantity = receivedBaseQuantity;
        this.baseUnitCost = baseUnitCost;
        this.lineTotal = lineTotal;
        this.lotCode = lotCode;
        this.expiresOn = expiresOn;
    }

    public static PurchaseReceiptLineJpaEntity fromDomain(PurchaseReceiptLine line) {
        return new PurchaseReceiptLineJpaEntity(
                line.getId(),
                line.getReceiptId(),
                line.getLineNo(),
                line.getItemId(),
                line.getPackName(),
                line.getDeclaredPackQuantity(),
                line.getConversionSnapshot(),
                line.getReceivedBaseQuantity(),
                line.getBaseUnitCost(),
                line.getLineTotal(),
                line.getLotCode(),
                line.getExpiresOn()
        );
    }

    public PurchaseReceiptLine toDomain() {
        return new PurchaseReceiptLine(
                id,
                receiptId,
                lineNo,
                itemId,
                packName,
                declaredPackQuantity,
                conversionSnapshot,
                receivedBaseQuantity,
                baseUnitCost,
                lineTotal,
                lotCode,
                expiresOn
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(UUID receiptId) {
        this.receiptId = receiptId;
    }

    public PurchaseReceiptJpaEntity getReceipt() {
        return receipt;
    }

    public void setReceipt(PurchaseReceiptJpaEntity receipt) {
        this.receipt = receipt;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getPackName() {
        return packName;
    }

    public void setPackName(String packName) {
        this.packName = packName;
    }

    public BigDecimal getDeclaredPackQuantity() {
        return declaredPackQuantity;
    }

    public void setDeclaredPackQuantity(BigDecimal declaredPackQuantity) {
        this.declaredPackQuantity = declaredPackQuantity;
    }

    public BigDecimal getConversionSnapshot() {
        return conversionSnapshot;
    }

    public void setConversionSnapshot(BigDecimal conversionSnapshot) {
        this.conversionSnapshot = conversionSnapshot;
    }

    public BigDecimal getReceivedBaseQuantity() {
        return receivedBaseQuantity;
    }

    public void setReceivedBaseQuantity(BigDecimal receivedBaseQuantity) {
        this.receivedBaseQuantity = receivedBaseQuantity;
    }

    public BigDecimal getBaseUnitCost() {
        return baseUnitCost;
    }

    public void setBaseUnitCost(BigDecimal baseUnitCost) {
        this.baseUnitCost = baseUnitCost;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getLotCode() {
        return lotCode;
    }

    public void setLotCode(String lotCode) {
        this.lotCode = lotCode;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }
}
