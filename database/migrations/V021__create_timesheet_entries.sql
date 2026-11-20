-- P0-HR-03 & P0-HR-04: Timesheet Entries and Paid-time Review
-- Authoritative: docs/api/contracts/P0-HR-03-04.md
-- BR-HR-02, TC-013, TC-017

CREATE TABLE mtms.timesheet_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES mtms.employees(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    work_date DATE NOT NULL,
    raw_minutes INTEGER CHECK (raw_minutes IS NULL OR raw_minutes >= 0),
    approved_minutes INTEGER NOT NULL DEFAULT 0 CHECK (approved_minutes >= 0),
    payable_workdays NUMERIC(6,2) NOT NULL DEFAULT 0.00 CHECK (payable_workdays >= 0),
    source_type VARCHAR(24) NOT NULL DEFAULT 'MANUAL' CHECK (source_type IN ('MANUAL', 'IMPORT', 'AUTO')),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    note TEXT,
    approved_by UUID REFERENCES mtms.users(id),
    approved_at TIMESTAMPTZ,
    approval_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (employee_id, branch_id, work_date)
);

CREATE INDEX idx_timesheet_branch_date ON mtms.timesheet_entries(branch_id, work_date);
CREATE INDEX idx_timesheet_emp_date ON mtms.timesheet_entries(employee_id, work_date);
CREATE INDEX idx_timesheet_branch_status ON mtms.timesheet_entries(branch_id, status);
