package com.mtms.finance.application.dto;

import java.time.LocalDate;

/**
 * P0-FIN-08 / BR-FIN-08
 * Response indicating whether opening balances have been posted and returning the active batch.
 */
public record OpeningBalanceStatusResponse(
        boolean isPosted,
        LocalDate cutoverDate,
        OpeningBalanceBatchDto activeBatch
) {}
