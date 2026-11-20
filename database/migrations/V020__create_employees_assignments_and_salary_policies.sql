-- P0-HR-01 & P0-HR-02: Employees, Assignments, and Salary Policies
-- Authoritative: docs/api/contracts/P0-HR-01-02.md
-- BR-HR-01, BR-HR-03, TC-013, TC-015

CREATE TABLE mtms.employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE REFERENCES mtms.users(id),
    staff_code VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    position_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30),
    email VARCHAR(160),
    hired_on DATE NOT NULL,
    left_on DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'TERMINATED', 'SUSPENDED')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (left_on IS NULL OR left_on >= hired_on)
);

CREATE INDEX idx_employees_staff_code ON mtms.employees(staff_code);
CREATE INDEX idx_employees_status ON mtms.employees(status);

CREATE TABLE mtms.employee_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES mtms.employees(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_emp_assignments_lookup ON mtms.employee_assignments(employee_id, branch_id, effective_from, effective_to);
CREATE INDEX idx_emp_assignments_branch ON mtms.employee_assignments(branch_id);

CREATE TABLE mtms.salary_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES mtms.employees(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    rate_type VARCHAR(16) NOT NULL CHECK (rate_type IN ('HOURLY', 'MONTHLY')),
    rate_amount NUMERIC(20,2) NOT NULL CHECK (rate_amount >= 0),
    standard_workdays NUMERIC(7,2) DEFAULT 26.00 CHECK (standard_workdays IS NULL OR standard_workdays > 0),
    paid_minutes_rounding_block SMALLINT DEFAULT 15 CHECK (paid_minutes_rounding_block IS NULL OR paid_minutes_rounding_block IN (1, 5, 10, 15, 30, 60)),
    effective_from DATE NOT NULL,
    effective_to DATE,
    notes TEXT,
    approved_by UUID REFERENCES mtms.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_salary_policies_lookup ON mtms.salary_policies(employee_id, branch_id, effective_from, effective_to);
CREATE INDEX idx_salary_policies_branch ON mtms.salary_policies(branch_id);
