-- V025: Create monthly closes and close components for P0-CLS-01..04 (BR-CLS-01, BR-CLS-02, TC-016, TC-040, TC-041)

CREATE TABLE mtms.monthly_closes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    month_start DATE NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(28) NOT NULL DEFAULT 'OPEN' CHECK (
        status IN ('OPEN', 'CLOSING', 'CLOSED', 'REOPEN_REQUESTED', 'REOPENED', 'SUPERSEDED')
    ),
    managerial_profit NUMERIC(22, 2),
    is_provisional BOOLEAN NOT NULL DEFAULT TRUE,
    closed_by UUID REFERENCES mtms.users(id),
    closed_at TIMESTAMPTZ,
    snapshot JSONB,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_monthly_closes_branch_month_rev UNIQUE (branch_id, month_start, revision),
    CONSTRAINT ck_monthly_closes_month_start_first_day CHECK (EXTRACT(DAY FROM month_start) = 1)
);

CREATE UNIQUE INDEX uq_monthly_closes_current 
    ON mtms.monthly_closes (branch_id, month_start) 
    WHERE is_current;

CREATE INDEX idx_monthly_closes_branch_status 
    ON mtms.monthly_closes (branch_id, status);

CREATE TABLE mtms.close_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monthly_close_id UUID NOT NULL REFERENCES mtms.monthly_closes(id) ON DELETE CASCADE,
    component_code VARCHAR(50) NOT NULL,
    signed_amount NUMERIC(22, 2) NOT NULL,
    provenance JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_close_components_close_code UNIQUE (monthly_close_id, component_code),
    CONSTRAINT ck_close_components_code CHECK (
        component_code IN (
            'REVENUE_TOTAL',
            'REVENUE_CASH',
            'REVENUE_BANK',
            'REVENUE_APP_NET',
            'COGS_TOTAL',
            'GROSS_PROFIT',
            'PAYROLL_ACCRUAL',
            'OPEX_TOTAL',
            'TAX_OBLIGATIONS',
            'MANAGERIAL_NET_PROFIT',
            'CASH_NET_FLOW',
            'ACCRUAL_CASH_VARIANCE'
        )
    )
);

CREATE INDEX idx_close_components_close_id 
    ON mtms.close_components (monthly_close_id);
