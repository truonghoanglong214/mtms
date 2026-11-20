package com.mtms.finance.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * P0-FIN-08 / BR-FIN-08 / ADR-009
 * DTO representing an account's opening balance within a cutover batch.
 */
public record OpeningAccountBalanceDto(
        UUID id,
        UUID accountId,
        String accountName,
        String accountKind,
        BigDecimal openingBalance,
        UUID cashLedgerEntryId
) {}
