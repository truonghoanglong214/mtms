package com.mtms.closing.infrastructure.persistence;

import com.mtms.closing.domain.model.CloseStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * P0-CLS-01..03 / BR-CLS-01, BR-CLS-02 / TC-040, TC-041
 * JPA entity mapping mtms.monthly_closes.
 */
@Entity
@Table(name = "monthly_closes", schema = "mtms")
public class MonthlyCloseJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "month_start", nullable = false)
    private LocalDate monthStart;

    @Column(name = "revision", nullable = false)
    private Integer revision;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 28)
    private CloseStatus status;

    @Column(name = "managerial_profit", precision = 22, scale = 2)
    private BigDecimal managerialProfit;

    @Column(name = "is_provisional", nullable = false)
    private Boolean isProvisional;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot")
    private String snapshot;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "monthlyClose", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CloseComponentJpaEntity> components = new ArrayList<>();

    public MonthlyCloseJpaEntity() {}

    public MonthlyCloseJpaEntity(
            UUID id,
            UUID branchId,
            LocalDate monthStart,
            Integer revision,
            Boolean isCurrent,
            CloseStatus status,
            BigDecimal managerialProfit,
            Boolean isProvisional,
            UUID closedBy,
            Instant closedAt,
            String snapshot,
            String note,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.branchId = branchId;
        this.monthStart = monthStart;
        this.revision = revision;
        this.isCurrent = isCurrent;
        this.status = status;
        this.managerialProfit = managerialProfit;
        this.isProvisional = isProvisional;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
        this.snapshot = snapshot;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public LocalDate getMonthStart() {
        return monthStart;
    }

    public void setMonthStart(LocalDate monthStart) {
        this.monthStart = monthStart;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean current) {
        isCurrent = current;
    }

    public CloseStatus getStatus() {
        return status;
    }

    public void setStatus(CloseStatus status) {
        this.status = status;
    }

    public BigDecimal getManagerialProfit() {
        return managerialProfit;
    }

    public void setManagerialProfit(BigDecimal managerialProfit) {
        this.managerialProfit = managerialProfit;
    }

    public Boolean getIsProvisional() {
        return isProvisional;
    }

    public void setIsProvisional(Boolean provisional) {
        isProvisional = provisional;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public List<CloseComponentJpaEntity> getComponents() {
        return components;
    }

    public void setComponents(List<CloseComponentJpaEntity> components) {
        this.components = components;
    }

    public void addComponent(CloseComponentJpaEntity component) {
        this.components.add(component);
        component.setMonthlyClose(this);
    }
}
