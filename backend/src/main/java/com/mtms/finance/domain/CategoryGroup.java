package com.mtms.finance.domain;

/**
 * P0-FIN-01 / BR-FIN-01
 * Financial category grouping defining the nature and accounting treatment of transactions.
 */
public enum CategoryGroup {
    OPERATING_EXPENSE,
    CAPITAL_FLOW,
    INTERNAL_TRANSFER,
    DEBT_SETTLEMENT,
    REVENUE,
    PAYROLL,
    OTHER
}
