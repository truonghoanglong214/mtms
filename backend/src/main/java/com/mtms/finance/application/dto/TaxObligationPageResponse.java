package com.mtms.finance.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * P0-FIN-07 / BR-FIN-04 / TC-012
 * Paginated response wrapper for tax obligations.
 */
public record TaxObligationPageResponse(
        List<TaxObligationDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public static TaxObligationPageResponse of(Page<TaxObligationDto> page) {
        return new TaxObligationPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
