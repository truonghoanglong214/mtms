package com.mtms.organization.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing an operating branch / shop.
 * Invariant: Every branch must belong to exactly one BusinessEntity.
 */
public class Branch {

    public static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";

    private final UUID id;
    private final UUID businessEntityId;
    private final String branchCode;
    private final String name;
    private final String timeZone;
    private final BranchStatus status;
    private final Instant createdAt;

    public Branch(UUID id,
                  UUID businessEntityId,
                  String branchCode,
                  String name,
                  String timeZone,
                  BranchStatus status,
                  Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Branch ID must not be null");
        this.businessEntityId = Objects.requireNonNull(businessEntityId, "BusinessEntity ID must not be null");
        this.branchCode = validateCode(branchCode);
        this.name = validateName(name);
        this.timeZone = (timeZone != null && !timeZone.trim().isEmpty()) ? timeZone.trim() : DEFAULT_TIME_ZONE;
        this.status = Objects.requireNonNull(status, "Branch status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
    }

    private static String validateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch code must not be blank");
        }
        if (code.trim().length() > 40) {
            throw new IllegalArgumentException("Branch code must not exceed 40 characters");
        }
        return code.trim().toUpperCase();
    }

    private static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name must not be blank");
        }
        if (name.trim().length() > 160) {
            throw new IllegalArgumentException("Branch name must not exceed 160 characters");
        }
        return name.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessEntityId() {
        return businessEntityId;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public String getName() {
        return name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public BranchStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
