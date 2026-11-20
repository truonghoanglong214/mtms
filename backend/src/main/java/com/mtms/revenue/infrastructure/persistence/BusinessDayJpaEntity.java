package com.mtms.revenue.infrastructure.persistence;

import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.revenue.domain.BusinessDay;
import com.mtms.revenue.domain.OperationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "business_days", schema = "mtms")
public class BusinessDayJpaEntity {

    @Id
    private UUID id;

    @Column(name = "business_entity_id", nullable = false)
    private UUID businessEntityId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_status", nullable = false, length = 16)
    private OperationStatus operationStatus;

    @Column(name = "close_reason", columnDefinition = "TEXT")
    private String closeReason;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", insertable = false, updatable = false)
    private UserJpaEntity recordedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BusinessDayJpaEntity() {
    }

    public BusinessDayJpaEntity(
            UUID id,
            UUID businessEntityId,
            UUID branchId,
            LocalDate businessDate,
            OperationStatus operationStatus,
            String closeReason,
            UUID recordedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchId = branchId;
        this.businessDate = businessDate;
        this.operationStatus = operationStatus;
        this.closeReason = closeReason;
        this.recordedBy = recordedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static BusinessDayJpaEntity fromDomain(BusinessDay domain) {
        return new BusinessDayJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getBusinessDate(),
                domain.getOperationStatus(),
                domain.getCloseReason(),
                domain.getRecordedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public BusinessDay toDomain() {
        return new BusinessDay(
                this.id,
                this.businessEntityId,
                this.branchId,
                this.businessDate,
                this.operationStatus,
                this.closeReason,
                this.recordedBy,
                this.createdAt,
                this.updatedAt
        );
    }

    public void updateFromDomain(BusinessDay domain) {
        this.operationStatus = domain.getOperationStatus();
        this.closeReason = domain.getCloseReason();
        this.recordedBy = domain.getRecordedBy();
        this.updatedAt = domain.getUpdatedAt();
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

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(UUID recordedBy) {
        this.recordedBy = recordedBy;
    }

    public UserJpaEntity getRecordedByUser() {
        return recordedByUser;
    }

    public void setRecordedByUser(UserJpaEntity recordedByUser) {
        this.recordedByUser = recordedByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
