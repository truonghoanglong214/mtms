package com.mtms.closing.domain.model;

/**
 * P0-CLS-03 / BR-CLS-02 / TC-041
 * Standard accounting and managerial components captured during monthly close.
 */
public enum CloseComponentCode {
    REVENUE_TOTAL,
    REVENUE_CASH,
    REVENUE_BANK,
    REVENUE_APP_NET,
    COGS_TOTAL,
    GROSS_PROFIT,
    PAYROLL_ACCRUAL,
    OPEX_TOTAL,
    TAX_OBLIGATIONS,
    MANAGERIAL_NET_PROFIT,
    CASH_NET_FLOW,
    ACCRUAL_CASH_VARIANCE
}
