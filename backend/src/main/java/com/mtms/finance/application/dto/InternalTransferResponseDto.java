package com.mtms.finance.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InternalTransferResponseDto(
        UUID id,
        UUID branchId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        LocalDate businessDate,
        String status,
        String note,
        UUID evidenceFileId,
        UUID sourceEntryId,
        UUID destinationEntryId,
        UUID createdBy,
        Instant createdAt
) {}
