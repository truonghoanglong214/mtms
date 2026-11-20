package com.mtms.finance.domain;

/**
 * P0-FIN-02 / BR-FIN-09
 * Business source generating the cash ledger movement.
 */
public enum SourceType {
    DAILY_REVENUE,
    SUPPLIER_PAYMENT,
    INTERNAL_TRANSFER,
    MANUAL_EXPENSE,
    CAPITAL_FLOW,
    PAYROLL_PAYMENT,
    MANUAL_ADJUSTMENT,
    RECEIVABLE_RECEIPT,
    TAX_PAYMENT
}
