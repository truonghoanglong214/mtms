package com.mtms.finance.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-FIN-01 / BR-FIN-01 / TC-010
 * Financial Category domain aggregate.
 * Taxonomical separation: System categories are seeded and immutable regarding group/flow direction.
 * Custom categories created by users must belong to OPERATING_EXPENSE with OUTFLOW direction.
 */
public class FinancialCategory {

    private final UUID id;
    private final String categoryCode;
    private String categoryName;
    private final CategoryGroup categoryGroup;
    private final FlowDirection flowDirection;
    private final boolean isSystem;
    private boolean isActive;
    private String description;
    private final Instant createdAt;

    public FinancialCategory(
            UUID id,
            String categoryCode,
            String categoryName,
            CategoryGroup categoryGroup,
            FlowDirection flowDirection,
            boolean isSystem,
            boolean isActive,
            String description,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Category ID must not be null");
        this.categoryCode = Objects.requireNonNull(categoryCode, "Category code must not be null").trim().toUpperCase();
        this.categoryName = Objects.requireNonNull(categoryName, "Category name must not be null").trim();
        this.categoryGroup = Objects.requireNonNull(categoryGroup, "Category group must not be null");
        this.flowDirection = Objects.requireNonNull(flowDirection, "Flow direction must not be null");
        this.isSystem = isSystem;
        this.isActive = isActive;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static FinancialCategory createCustomExpenseCategory(
            UUID id,
            String categoryCode,
            String categoryName,
            String description
    ) {
        return new FinancialCategory(
                id,
                categoryCode,
                categoryName,
                CategoryGroup.OPERATING_EXPENSE,
                FlowDirection.OUTFLOW,
                false,
                true,
                description,
                Instant.now()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = Objects.requireNonNull(categoryName, "Category name must not be null").trim();
    }

    public CategoryGroup getCategoryGroup() {
        return categoryGroup;
    }

    public FlowDirection getFlowDirection() {
        return flowDirection;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialCategory that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
