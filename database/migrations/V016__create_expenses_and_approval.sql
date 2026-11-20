-- P0-FIN-03 / P0-FIN-04 / BR-FIN-07 / TC-011
-- Forward-only migration for operating expenses, branch alert thresholds, and expense approval lifecycle.

-- 1. Branch Expense Alert Threshold Configurations
CREATE TABLE mtms.branch_expense_configs (
    branch_id UUID PRIMARY KEY REFERENCES mtms.branches(id),
    alert_threshold_amount NUMERIC(20, 2) NOT NULL DEFAULT 2000000.00,
    evidence_required_threshold NUMERIC(20, 2) NOT NULL DEFAULT 500000.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Operating Expenses (OPEX Ledger)
CREATE TABLE mtms.expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    expense_code VARCHAR(60) NOT NULL,
    expense_date DATE NOT NULL,
    category_id UUID NOT NULL REFERENCES mtms.financial_categories(id),
    amount NUMERIC(20, 2) NOT NULL CHECK (amount > 0),
    is_paid BOOLEAN NOT NULL DEFAULT TRUE,
    payment_account_id UUID REFERENCES mtms.financial_accounts(id),
    payable_id UUID REFERENCES mtms.payables(id),
    evidence_file_id UUID REFERENCES mtms.evidence_files(id),
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'CONFIRMED', 'VOIDED')),
    review_flag BOOLEAN NOT NULL DEFAULT FALSE,
    reason TEXT NOT NULL,
    created_by UUID NOT NULL REFERENCES mtms.users(id),
    confirmed_by UUID REFERENCES mtms.users(id),
    confirmed_at TIMESTAMPTZ,
    voided_by UUID REFERENCES mtms.users(id),
    voided_at TIMESTAMPTZ,
    void_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_expenses_branch_code UNIQUE (branch_id, expense_code),
    CONSTRAINT ck_expenses_payment_account CHECK (NOT is_paid OR payment_account_id IS NOT NULL)
);

CREATE INDEX idx_expenses_branch_date ON mtms.expenses (branch_id, expense_date);
CREATE INDEX idx_expenses_branch_status ON mtms.expenses (branch_id, status);
CREATE INDEX idx_expenses_category ON mtms.expenses (category_id);
CREATE INDEX idx_expenses_review_flag ON mtms.expenses (branch_id, review_flag) WHERE review_flag = TRUE;
