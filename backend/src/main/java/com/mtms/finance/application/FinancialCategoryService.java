package com.mtms.finance.application;

import com.mtms.finance.application.dto.CreateFinancialCategoryCommand;
import com.mtms.finance.application.dto.FinancialCategoryDto;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.FinancialCategory;
import com.mtms.finance.domain.FinancialCategoryNotFoundException;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.InvalidCategoryGroupException;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinancialCategoryService {

    private final FinancialCategoryJpaRepository categoryRepository;

    public FinancialCategoryService(FinancialCategoryJpaRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<FinancialCategoryDto> getCategories(CategoryGroup group, FlowDirection flowDirection, boolean activeOnly) {
        List<FinancialCategoryJpaEntity> entities = categoryRepository.findCategories(group, flowDirection, activeOnly);
        return entities.stream().map(this::toDto).toList();
    }

    public FinancialCategoryDto getCategoryById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .map(this::toDto)
                .orElseThrow(() -> new FinancialCategoryNotFoundException(categoryId));
    }

    public FinancialCategoryJpaEntity getCategoryEntity(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new FinancialCategoryNotFoundException(categoryId));
    }

    @Transactional
    public FinancialCategoryDto createCustomExpenseCategory(CreateFinancialCategoryCommand command) {
        String code = command.categoryCode().trim().toUpperCase();
        if (categoryRepository.existsByCategoryCode(code)) {
            throw new IllegalArgumentException("Mã danh mục '" + code + "' đã tồn tại trong hệ thống");
        }

        FinancialCategory category = FinancialCategory.createCustomExpenseCategory(
                UUID.randomUUID(),
                code,
                command.categoryName(),
                command.description()
        );

        FinancialCategoryJpaEntity saved = categoryRepository.save(FinancialCategoryJpaEntity.fromDomain(category));
        return toDto(saved);
    }

    public FinancialCategoryDto toDto(FinancialCategoryJpaEntity entity) {
        return new FinancialCategoryDto(
                entity.getId(),
                entity.getCategoryCode(),
                entity.getCategoryName(),
                entity.getCategoryGroup(),
                entity.getFlowDirection(),
                entity.isSystem(),
                entity.isActive(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
