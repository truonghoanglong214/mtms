-- P0-HR-05 & P0-HR-06: Payroll Periods, Lines, and Adjustments
-- Authoritative: docs/api/contracts/P0-HR-05-06.md
-- BR-HR-04, BR-HR-05, BR-HR-10, TC-013, TC-014, TC-015, TC-017

CREATE TABLE mtms.payroll_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    month_start DATE NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'APPROVED', 'SUPERSEDED', 'VOIDED')),
    total_gross_salary NUMERIC(20,2) NOT NULL DEFAULT 0.00 CHECK (total_gross_salary >= 0),
    total_base_salary NUMERIC(20,2) NOT NULL DEFAULT 0.00 CHECK (total_base_salary >= 0),
    total_adjustments NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    employee_count INTEGER NOT NULL DEFAULT 0 CHECK (employee_count >= 0),
    note TEXT,
    approved_by UUID REFERENCES mtms.users(id),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (branch_id, month_start, revision),
    CHECK (EXTRACT(DAY FROM month_start) = 1)
);

CREATE UNIQUE INDEX uq_payroll_period_current ON mtms.payroll_periods(branch_id, month_start) WHERE is_current;

CREATE TABLE mtms.payroll_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_period_id UUID NOT NULL REFERENCES mtms.payroll_periods(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES mtms.employees(id),
    expense_branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    rate_type VARCHAR(16) NOT NULL CHECK (rate_type IN ('HOURLY', 'MONTHLY')),
    approved_minutes INTEGER NOT NULL DEFAULT 0 CHECK (approved_minutes >= 0),
    payable_workdays NUMERIC(7,2) NOT NULL DEFAULT 0.00 CHECK (payable_workdays >= 0),
    base_salary NUMERIC(20,2) NOT NULL DEFAULT 0.00 CHECK (base_salary >= 0),
    adjustments_total NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    gross_salary NUMERIC(20,2) NOT NULL CHECK (gross_salary >= 0),
    paid_amount NUMERIC(20,2) NOT NULL DEFAULT 0.00 CHECK (paid_amount >= 0),
    policy_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    calculation_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (payroll_period_id, employee_id),
    CHECK (paid_amount <= gross_salary)
);

CREATE INDEX idx_payroll_lines_period ON mtms.payroll_lines(payroll_period_id);
CREATE INDEX idx_payroll_lines_employee ON mtms.payroll_lines(employee_id);

CREATE TABLE mtms.payroll_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_line_id UUID NOT NULL REFERENCES mtms.payroll_lines(id) ON DELETE CASCADE,
    adjustment_code VARCHAR(50) NOT NULL,
    signed_amount NUMERIC(20,2) NOT NULL CHECK (signed_amount <> 0),
    explanation TEXT NOT NULL,
    approved_by UUID REFERENCES mtms.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payroll_adjustments_line ON mtms.payroll_adjustments(payroll_line_id);
