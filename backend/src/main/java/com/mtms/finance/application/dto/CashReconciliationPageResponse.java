package com.mtms.finance.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * Paginated response wrapper for cash reconciliations.
 */
public record CashReconciliationPageResponse(
        List<CashReconciliationDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public static CashReconciliationPageResponse of(Page<CashReconciliationDto> page) {
        return new CashReconciliationPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
