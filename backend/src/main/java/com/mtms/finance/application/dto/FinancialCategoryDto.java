package com.mtms.finance.application.dto;

import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.FlowDirection;

import java.time.Instant;
import java.util.UUID;

public record FinancialCategoryDto(
        UUID id,
        String categoryCode,
        String categoryName,
        CategoryGroup categoryGroup,
        FlowDirection flowDirection,
        boolean isSystem,
        boolean isActive,
        String description,
        Instant createdAt
) {}
