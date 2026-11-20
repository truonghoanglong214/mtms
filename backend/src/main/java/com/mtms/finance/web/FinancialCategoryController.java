package com.mtms.finance.web;

import com.mtms.finance.application.FinancialCategoryService;
import com.mtms.finance.application.dto.CreateFinancialCategoryCommand;
import com.mtms.finance.application.dto.FinancialCategoryDto;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.FlowDirection;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/categories")
public class FinancialCategoryController {

    private final FinancialCategoryService categoryService;

    public FinancialCategoryController(FinancialCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<FinancialCategoryDto>> getCategories(
            @RequestParam(required = false) CategoryGroup group,
            @RequestParam(required = false) FlowDirection flowDirection,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<FinancialCategoryDto> categories = categoryService.getCategories(group, flowDirection, activeOnly);
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    @PreAuthorize("@branchAuth.isSystemAdmin() or hasAnyRole('BRANCH_OWNER', 'OWNER', 'ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<FinancialCategoryDto> createCategory(
            @Valid @RequestBody CreateFinancialCategoryCommand command
    ) {
        FinancialCategoryDto created = categoryService.createCustomExpenseCategory(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
