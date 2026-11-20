package com.mtms.finance.application.dto;

import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinancialAccountDto(
        UUID id,
        UUID branchId,
        UUID bankAccountId,
        String accountName,
        AccountKind accountKind,
        String currency,
        BigDecimal currentBalance,
        AccountStatus status,
        Instant createdAt
) {}
