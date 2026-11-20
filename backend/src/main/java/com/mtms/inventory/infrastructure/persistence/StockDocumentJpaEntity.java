package com.mtms.inventory.infrastructure.persistence;

import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.inventory.domain.StockDocument;
import com.mtms.inventory.domain.StockDocumentLine;
import com.mtms.inventory.domain.StockDocumentStatus;
import com.mtms.inventory.domain.StockDocumentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "stock_documents", schema = "mtms")
public class StockDocumentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "document_number", nullable = false, length = 60)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 28)
    private StockDocumentType documentType;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockDocumentStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "total_lines", nullable = false)
    private int totalLines;

    @Column(name = "total_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalQuantity;

    @Column(name = "total_booked_cost", nullable = false, precision = 22, scale = 2)
    private BigDecimal totalBookedCost;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private UserJpaEntity creator;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", insertable = false, updatable = false)
    private UserJpaEntity approver;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<StockDocumentLineJpaEntity> lines = new ArrayList<>();

    public StockDocumentJpaEntity() {
    }

    public StockDocumentJpaEntity(
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
            Instant createdAt
    ) {
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.documentNumber = documentNumber;
        this.documentType = documentType != null ? documentType : StockDocumentType.CONSUMPTION_ISSUE;
        this.documentDate = documentDate;
        this.status = status != null ? status : StockDocumentStatus.DRAFT;
        this.reason = reason;
        this.totalLines = totalLines;
        this.totalQuantity = totalQuantity;
        this.totalBookedCost = totalBookedCost;
        this.createdBy = createdBy;
        this.approvedBy = approvedBy;
        this.postedAt = postedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockDocumentJpaEntity fromDomain(StockDocument domain) {
        StockDocumentJpaEntity entity = new StockDocumentJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getDocumentNumber(),
                domain.getDocumentType(),
                domain.getDocumentDate(),
                domain.getStatus(),
                domain.getReason(),
                domain.getTotalLines(),
                domain.getTotalQuantity(),
                domain.getTotalBookedCost(),
                domain.getCreatedBy(),
                domain.getApprovedBy(),
                domain.getPostedAt(),
                domain.getCreatedAt()
        );
        for (StockDocumentLine line : domain.getLines()) {
            StockDocumentLineJpaEntity lineEntity = StockDocumentLineJpaEntity.fromDomain(line);
            lineEntity.setDocument(entity);
            entity.addLine(lineEntity);
        }
        return entity;
    }

    public StockDocument toDomain() {
        List<StockDocumentLine> domainLines = lines.stream()
                .map(StockDocumentLineJpaEntity::toDomain)
                .collect(Collectors.toList());
        return new StockDocument(
                id,
                businessEntityId,
                branchId,
                documentNumber,
                documentType,
                documentDate,
                status,
                reason,
                totalLines,
                totalQuantity,
                totalBookedCost,
                createdBy,
                approvedBy,
                postedAt,
                createdAt,
                domainLines
        );
    }

    public void addLine(StockDocumentLineJpaEntity line) {
        lines.add(line);
        line.setDocument(this);
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

    public UserJpaEntity getCreator() {
        return creator;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public UserJpaEntity getApprover() {
        return approver;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<StockDocumentLineJpaEntity> getLines() {
        return lines;
    }
}
