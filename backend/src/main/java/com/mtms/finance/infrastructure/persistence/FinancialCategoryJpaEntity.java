package com.mtms.finance.infrastructure.persistence;

import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.FinancialCategory;
import com.mtms.finance.domain.FlowDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "financial_categories", schema = "mtms")
public class FinancialCategoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "category_code", nullable = false, unique = true, length = 60)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 160)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_group", nullable = false, length = 40)
    private CategoryGroup categoryGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow_direction", nullable = false, length = 20)
    private FlowDirection flowDirection;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FinancialCategoryJpaEntity() {
    }

    public FinancialCategoryJpaEntity(
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
        this.id = id;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.categoryGroup = categoryGroup;
        this.flowDirection = flowDirection;
        this.isSystem = isSystem;
        this.isActive = isActive;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static FinancialCategoryJpaEntity fromDomain(FinancialCategory domain) {
        return new FinancialCategoryJpaEntity(
                domain.getId(),
                domain.getCategoryCode(),
                domain.getCategoryName(),
                domain.getCategoryGroup(),
                domain.getFlowDirection(),
                domain.isSystem(),
                domain.isActive(),
                domain.getDescription(),
                domain.getCreatedAt()
        );
    }

    public FinancialCategory toDomain() {
        return new FinancialCategory(
                this.id,
                this.categoryCode,
                this.categoryName,
                this.categoryGroup,
                this.flowDirection,
                this.isSystem,
                this.isActive,
                this.description,
                this.createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public CategoryGroup getCategoryGroup() {
        return categoryGroup;
    }

    public void setCategoryGroup(CategoryGroup categoryGroup) {
        this.categoryGroup = categoryGroup;
    }

    public FlowDirection getFlowDirection() {
        return flowDirection;
    }

    public void setFlowDirection(FlowDirection flowDirection) {
        this.flowDirection = flowDirection;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
