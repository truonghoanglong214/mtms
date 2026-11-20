package com.mtms.finance.application.dto;

import java.util.List;

public record CashLedgerPageResponse(
        List<CashLedgerEntryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
