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
 * P0-INV-04 / P0-INV-05 / BR-INV-02 / BR-INV-03
 * Domain entity representing a stock issue document (e.g. manual consumption, scrap disposal, adjustment).
 */
public class StockDocument {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final String documentNumber;
    private final StockDocumentType documentType;
    private final LocalDate documentDate;
    private StockDocumentStatus status;
    private final String reason;
    private int totalLines;
    private BigDecimal totalQuantity;
    private BigDecimal totalBookedCost;
    private final UUID createdBy;
    private UUID approvedBy;
    private Instant postedAt;
    private final Instant createdAt;
    private final List<StockDocumentLine> lines;

    public StockDocument(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String documentNumber,
            StockDocumentType documentType,
            LocalDate documentDate,
            StockDocumentStatus status,
            String reason,
            int totalLines,
            BigDecimal totalQuantity,
            BigDecimal totalBookedCost,
            UUID createdBy,
            UUID approvedBy,
            Instant postedAt,
            Instant createdAt,
            List<StockDocumentLine> lines
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.documentNumber = Objects.requireNonNull(documentNumber, "documentNumber must not be null");
        this.documentType = documentType != null ? documentType : StockDocumentType.CONSUMPTION_ISSUE;
        this.documentDate = Objects.requireNonNull(documentDate, "documentDate must not be null");
        this.status = status != null ? status : StockDocumentStatus.DRAFT;
        this.reason = reason;
        this.totalLines = Math.max(0, totalLines);
        this.totalQuantity = totalQuantity != null ? totalQuantity.setScale(3, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        this.totalBookedCost = totalBookedCost != null ? totalBookedCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
        this.approvedBy = approvedBy;
        this.postedAt = postedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    public void post(UUID approverId, BigDecimal totalBookedCost, BigDecimal totalQuantity, int totalLines) {
        if (this.status == StockDocumentStatus.POSTED) {
            throw new StockDocumentAlreadyPostedException("Stock document has already been posted: " + this.documentNumber);
        }
        this.status = StockDocumentStatus.POSTED;
        this.approvedBy = Objects.requireNonNull(approverId, "approverId must not be null");
        this.postedAt = Instant.now();
        this.totalBookedCost = Objects.requireNonNull(totalBookedCost, "totalBookedCost must not be null")
                .setScale(2, RoundingMode.HALF_UP);
        this.totalQuantity = Objects.requireNonNull(totalQuantity, "totalQuantity must not be null")
                .setScale(3, RoundingMode.HALF_UP);
        this.totalLines = totalLines;
    }

    public void cancel() {
        if (this.status == StockDocumentStatus.POSTED) {
            throw new StockDocumentAlreadyPostedException("Cannot cancel an already posted document: " + this.documentNumber);
        }
        this.status = StockDocumentStatus.CANCELLED;
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

    public String getDocumentNumber() {
        return documentNumber;
    }

    public StockDocumentType getDocumentType() {
        return documentType;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public StockDocumentStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalBookedCost() {
        return totalBookedCost;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<StockDocumentLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
