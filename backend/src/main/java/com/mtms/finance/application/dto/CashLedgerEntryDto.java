package com.mtms.finance.application.dto;

import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.LedgerStatus;
import com.mtms.finance.domain.SourceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CashLedgerEntryDto(
        UUID id,
        UUID accountId,
        String accountName,
        UUID branchId,
        LocalDate businessDate,
        EntryType entryType,
        BigDecimal amount,
        BigDecimal signedAmount,
        BigDecimal runningBalance,
        UUID categoryId,
        String categoryCode,
        String categoryName,
        CategoryGroup categoryGroup,
        SourceType sourceType,
        UUID sourceId,
        String sourceLeg,
        LedgerStatus status,
        UUID reversalOfId,
        String memo,
        UUID evidenceFileId,
        String evidenceToken,
        UUID createdBy,
        Instant postedAt
) {}
