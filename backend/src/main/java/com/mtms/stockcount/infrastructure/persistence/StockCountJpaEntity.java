package com.mtms.stockcount.infrastructure.persistence;

import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.stockcount.domain.model.StockCount;
import com.mtms.stockcount.domain.model.StockCountStatus;
import com.mtms.stockcount.domain.model.StockCountType;
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
@Table(name = "stock_counts", schema = "mtms")
public class StockCountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private BranchJpaEntity branch;

    @Column(name = "count_number", nullable = false, length = 60)
    private String countNumber;

    @Column(name = "counted_on", nullable = false)
    private LocalDate countedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockCountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_type", nullable = false, length = 20)
    private StockCountType countType;

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "total_surplus_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalSurplusQty;

    @Column(name = "total_shortage_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalShortageQty;

    @Column(name = "total_surplus_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal totalSurplusValue;

    @Column(name = "total_shortage_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal totalShortageValue;

    @Column(name = "net_variance_value", nullable = false, precision = 22, scale = 2)
    private BigDecimal netVarianceValue;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<StockCountLineJpaEntity> lines = new ArrayList<>();

    public StockCountJpaEntity() {}

    public StockCount toDomain() {
        return new StockCount(
                id,
                businessEntityId,
                branchId,
                countNumber,
                countedOn,
                status,
                countType,
                totalItems,
                totalSurplusQty,
                totalShortageQty,
                totalSurplusValue,
                totalShortageValue,
                netVarianceValue,
                notes,
                createdBy,
                submittedBy,
                submittedAt,
                approvedBy,
                postedAt,
                createdAt,
                updatedAt,
                lines.stream().map(StockCountLineJpaEntity::toDomain).collect(Collectors.toList())
        );
    }

    public static StockCountJpaEntity fromDomain(StockCount domain) {
        StockCountJpaEntity entity = new StockCountJpaEntity();
        entity.id = domain.getId();
        entity.businessEntityId = domain.getBusinessEntityId();
        entity.branchId = domain.getBranchId();
        entity.countNumber = domain.getCountNumber();
        entity.countedOn = domain.getCountedOn();
        entity.status = domain.getStatus();
        entity.countType = domain.getCountType();
        entity.totalItems = domain.getTotalItems();
        entity.totalSurplusQty = domain.getTotalSurplusQty();
        entity.totalShortageQty = domain.getTotalShortageQty();
        entity.totalSurplusValue = domain.getTotalSurplusValue();
        entity.totalShortageValue = domain.getTotalShortageValue();
        entity.netVarianceValue = domain.getNetVarianceValue();
        entity.notes = domain.getNotes();
        entity.createdBy = domain.getCreatedBy();
        entity.submittedBy = domain.getSubmittedBy();
        entity.submittedAt = domain.getSubmittedAt();
        entity.approvedBy = domain.getApprovedBy();
        entity.postedAt = domain.getPostedAt();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();

        if (domain.getLines() != null) {
            entity.lines = domain.getLines().stream()
                    .map(line -> StockCountLineJpaEntity.fromDomain(line, entity))
                    .collect(Collectors.toList());
        }
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getBusinessEntityId() { return businessEntityId; }
    public UUID getBranchId() { return branchId; }
    public BranchJpaEntity getBranch() { return branch; }
    public String getCountNumber() { return countNumber; }
    public LocalDate getCountedOn() { return countedOn; }
    public StockCountStatus getStatus() { return status; }
    public StockCountType getCountType() { return countType; }
    public int getTotalItems() { return totalItems; }
    public BigDecimal getTotalSurplusQty() { return totalSurplusQty; }
    public BigDecimal getTotalShortageQty() { return totalShortageQty; }
    public BigDecimal getTotalSurplusValue() { return totalSurplusValue; }
    public BigDecimal getTotalShortageValue() { return totalShortageValue; }
    public BigDecimal getNetVarianceValue() { return netVarianceValue; }
    public String getNotes() { return notes; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getSubmittedBy() { return submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getApprovedBy() { return approvedBy; }
    public Instant getPostedAt() { return postedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<StockCountLineJpaEntity> getLines() { return lines; }
}
