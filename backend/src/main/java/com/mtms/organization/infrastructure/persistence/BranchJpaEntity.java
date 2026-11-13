package com.mtms.organization.infrastructure.persistence;

import com.mtms.organization.domain.BranchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "branches", schema = "mtms")
public class BranchJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "business_entity_id", nullable = false, updatable = false)
    private UUID businessEntityId;

    @Column(name = "branch_code", nullable = false, length = 40)
    private String branchCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BranchStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BranchJpaEntity() {
    }

    public BranchJpaEntity(UUID id,
                           UUID businessEntityId,
                           String branchCode,
                           String name,
                           String timeZone,
                           BranchStatus status,
                           Instant createdAt) {
        this.id = id;
        this.businessEntityId = businessEntityId;
        this.branchCode = branchCode;
        this.name = name;
        this.timeZone = timeZone;
        this.status = status;
        this.createdAt = createdAt;
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

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public BranchStatus getStatus() {
        return status;
    }

    public void setStatus(BranchStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
