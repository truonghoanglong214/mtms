package com.mtms.stockcount.domain.model;

import com.mtms.stockcount.domain.StockCountExceptions.InvalidStockCountStatusException;
import com.mtms.stockcount.domain.StockCountExceptions.StockCountImmutableException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class StockCount {

    private final UUID id;
    private final UUID businessEntityId;
    private final UUID branchId;
    private final String countNumber;
    private final LocalDate countedOn;
    private StockCountStatus status;
    private final StockCountType countType;
    private int totalItems;
    private BigDecimal totalSurplusQty;
    private BigDecimal totalShortageQty;
    private BigDecimal totalSurplusValue;
    private BigDecimal totalShortageValue;
    private BigDecimal netVarianceValue;
    private String notes;
    private final UUID createdBy;
    private UUID submittedBy;
    private Instant submittedAt;
    private UUID approvedBy;
    private Instant postedAt;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<StockCountLine> lines;

    public StockCount(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            String countNumber,
            LocalDate countedOn,
            StockCountStatus status,
            StockCountType countType,
            int totalItems,
            BigDecimal totalSurplusQty,
            BigDecimal totalShortageQty,
            BigDecimal totalSurplusValue,
            BigDecimal totalShortageValue,
            BigDecimal netVarianceValue,
            String notes,
            UUID createdBy,
            UUID submittedBy,
            Instant submittedAt,
            UUID approvedBy,
            Instant postedAt,
            Instant createdAt,
            Instant updatedAt,
            List<StockCountLine> lines
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "businessEntityId must not be null");
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.countNumber = Objects.requireNonNull(countNumber, "countNumber must not be null");
        this.countedOn = Objects.requireNonNull(countedOn, "countedOn must not be null");
        this.status = status != null ? status : StockCountStatus.DRAFT;
        this.countType = countType != null ? countType : StockCountType.PERIODIC;
        this.totalItems = totalItems;
        this.totalSurplusQty = totalSurplusQty != null ? totalSurplusQty : BigDecimal.ZERO;
        this.totalShortageQty = totalShortageQty != null ? totalShortageQty : BigDecimal.ZERO;
        this.totalSurplusValue = totalSurplusValue != null ? totalSurplusValue : BigDecimal.ZERO;
        this.totalShortageValue = totalShortageValue != null ? totalShortageValue : BigDecimal.ZERO;
        this.netVarianceValue = netVarianceValue != null ? netVarianceValue : BigDecimal.ZERO;
        this.notes = notes;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy must not be null");
        this.submittedBy = submittedBy;
        this.submittedAt = submittedAt;
        this.approvedBy = approvedBy;
        this.postedAt = postedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    public static StockCount createDraft(
            UUID businessEntityId,
            UUID branchId,
            String countNumber,
            LocalDate countedOn,
            StockCountType countType,
            String notes,
            UUID createdBy
    ) {
        return new StockCount(
                UUID.randomUUID(),
                businessEntityId,
                branchId,
                countNumber,
                countedOn,
                StockCountStatus.DRAFT,
                countType,
                0,
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                notes,
                createdBy,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                new ArrayList<>()
        );
    }

    public void addLine(StockCountLine line) {
        ensureModifiable();
        this.lines.add(line);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.totalItems = lines.size();
        BigDecimal surplusQty = BigDecimal.ZERO;
        BigDecimal shortageQty = BigDecimal.ZERO;
        BigDecimal surplusVal = BigDecimal.ZERO;
        BigDecimal shortageVal = BigDecimal.ZERO;

        for (StockCountLine line : lines) {
            BigDecimal vQty = line.getVarianceQty();
            BigDecimal vVal = line.getVarianceValue();
            if (vQty.compareTo(BigDecimal.ZERO) > 0) {
                surplusQty = surplusQty.add(vQty);
                surplusVal = surplusVal.add(vVal);
            } else if (vQty.compareTo(BigDecimal.ZERO) < 0) {
                shortageQty = shortageQty.add(vQty.abs());
                shortageVal = shortageVal.add(vVal.abs());
            }
        }

        this.totalSurplusQty = surplusQty.setScale(3, RoundingMode.HALF_UP);
        this.totalShortageQty = shortageQty.setScale(3, RoundingMode.HALF_UP);
        this.totalSurplusValue = surplusVal.setScale(2, RoundingMode.HALF_UP);
        this.totalShortageValue = shortageVal.setScale(2, RoundingMode.HALF_UP);
        this.netVarianceValue = this.totalSurplusValue.subtract(this.totalShortageValue).setScale(2, RoundingMode.HALF_UP);
        this.updatedAt = Instant.now();
    }

    public void submit(UUID submitterId) {
        if (this.status != StockCountStatus.DRAFT) {
            throw new InvalidStockCountStatusException("Chỉ có thể nộp duyệt đợt kiểm kê đang ở trạng thái DRAFT");
        }
        this.status = StockCountStatus.SUBMITTED;
        this.submittedBy = submitterId;
        this.submittedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void approveAndPost(UUID approverId) {
        if (this.status != StockCountStatus.SUBMITTED && this.status != StockCountStatus.DRAFT) {
            throw new InvalidStockCountStatusException("Chỉ có thể phê duyệt đợt kiểm kê ở trạng thái DRAFT hoặc SUBMITTED");
        }
        this.status = StockCountStatus.POSTED;
        this.approvedBy = approverId;
        this.postedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == StockCountStatus.POSTED) {
            throw new StockCountImmutableException("Không thể hủy đợt kiểm kê đã hoàn tất POSTED");
        }
        this.status = StockCountStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void ensureModifiable() {
        if (this.status == StockCountStatus.POSTED || this.status == StockCountStatus.CANCELLED) {
            throw new StockCountImmutableException("Đợt kiểm kê ở trạng thái " + this.status + " không thể chỉnh sửa");
        }
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

    public String getCountNumber() {
        return countNumber;
    }

    public LocalDate getCountedOn() {
        return countedOn;
    }

    public StockCountStatus getStatus() {
        return status;
    }

    public StockCountType getCountType() {
        return countType;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public BigDecimal getTotalSurplusQty() {
        return totalSurplusQty;
    }

    public BigDecimal getTotalShortageQty() {
        return totalShortageQty;
    }

    public BigDecimal getTotalSurplusValue() {
        return totalSurplusValue;
    }

    public BigDecimal getTotalShortageValue() {
        return totalShortageValue;
    }

    public BigDecimal getNetVarianceValue() {
        return netVarianceValue;
    }

    public String getNotes() {
        return notes;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<StockCountLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
