package com.mtms.finance.application.dto;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * Command to execute an authorized adjusting ledger entry for variance.
 */
public record AdjustReconciliationCommand(
        String reason
) {}
