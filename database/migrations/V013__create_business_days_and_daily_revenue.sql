-- P0-REV-01 / P0-REV-02 / P0-REV-03 / BR-REV-01 / BR-REV-02 / BR-REV-03 / TC-004 / TC-005 / TC-006
-- Forward-only migration for business calendar and daily aggregated revenue reports.

-- 1. mtms.business_days (Branch Business Calendar & Day Status)
CREATE TABLE mtms.business_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    business_date DATE NOT NULL,
    operation_status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    close_reason TEXT,
    recorded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_days_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_business_days_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_business_days_user FOREIGN KEY (recorded_by) REFERENCES mtms.users(id),
    CONSTRAINT uq_business_days_branch_date UNIQUE (branch_id, business_date),
    CONSTRAINT ck_business_days_status CHECK (operation_status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_business_days_close_reason CHECK (operation_status <> 'CLOSED' OR (close_reason IS NOT NULL AND length(trim(close_reason)) > 0))
);

CREATE INDEX idx_business_days_branch_month ON mtms.business_days(branch_id, business_date);

-- 2. mtms.daily_revenue_reports (Aggregated Daily Revenue)
CREATE TABLE mtms.daily_revenue_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    business_date DATE NOT NULL,
    revision INTEGER NOT NULL DEFAULT 1,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    cash_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    bank_transfer_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    delivery_app_net_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    other_adjustment_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    total_revenue NUMERIC(20, 2) NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    submitted_by UUID NOT NULL,
    submitted_at TIMESTAMPTZ,
    confirmed_by UUID,
    confirmed_at TIMESTAMPTZ,
    adjustment_reason TEXT,
    evidence_file_id UUID,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_daily_revenue_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_daily_revenue_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_daily_revenue_submitted_by FOREIGN KEY (submitted_by) REFERENCES mtms.users(id),
    CONSTRAINT fk_daily_revenue_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES mtms.users(id),
    CONSTRAINT fk_daily_revenue_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT uq_daily_revenue_branch_date_revision UNIQUE (branch_id, business_date, revision),
    CONSTRAINT ck_daily_revenue_revision CHECK (revision > 0),
    CONSTRAINT ck_daily_revenue_cash CHECK (cash_amount >= 0),
    CONSTRAINT ck_daily_revenue_bank CHECK (bank_transfer_amount >= 0),
    CONSTRAINT ck_daily_revenue_delivery CHECK (delivery_app_net_amount >= 0),
    CONSTRAINT ck_daily_revenue_total_pos CHECK (total_revenue >= 0),
    CONSTRAINT ck_daily_revenue_total_math CHECK (total_revenue = cash_amount + bank_transfer_amount + delivery_app_net_amount + other_adjustment_amount),
    CONSTRAINT ck_daily_revenue_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'CONFIRMED', 'SUPERSEDED', 'CANCELLED'))
);

CREATE UNIQUE INDEX uq_daily_revenue_current ON mtms.daily_revenue_reports(branch_id, business_date) WHERE is_current;
CREATE INDEX idx_daily_revenue_branch_month ON mtms.daily_revenue_reports(branch_id, business_date, status);
