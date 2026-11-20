package com.mtms.finance.domain;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012
 * Lifecycle disposition for cash/bank reconciliation records.
 */
public enum ReconciliationDisposition {
    OPEN,
    EXPLAINED,
    ADJUSTED,
    CLOSED
}
