-- V026: Create investors, agreements, commitments, contributions and profit terms (BR-CAP-01, TC-032, CAP-INV-01)

CREATE TABLE mtms.investors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_id UUID NOT NULL REFERENCES mtms.business_entities(id),
    display_name VARCHAR(160) NOT NULL,
    contact_phone VARCHAR(30),
    contact_email VARCHAR(120),
    identity_number VARCHAR(50),
    contact_note VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_investors_entity_display_name UNIQUE (entity_id, display_name)
);

CREATE TABLE mtms.investor_agreements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id UUID NOT NULL REFERENCES mtms.investors(id) ON DELETE RESTRICT,
    branch_id UUID NOT NULL REFERENCES mtms.branches(id) ON DELETE RESTRICT,
    agreement_ref VARCHAR(120) NOT NULL,
    contribution_type VARCHAR(20) NOT NULL CHECK (
        contribution_type IN ('CASH', 'ASSET', 'WORK', 'MIXED')
    ),
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (
        status IN ('ACTIVE', 'TERMINATED', 'EXPIRED')
    ),
    eligibility_terms TEXT,
    evidence_file_id UUID REFERENCES mtms.evidence_files(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_agreement_date_range CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT uq_agreement_branch_ref UNIQUE (branch_id, agreement_ref)
);

CREATE TABLE mtms.capital_commitments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id UUID NOT NULL REFERENCES mtms.investor_agreements(id) ON DELETE RESTRICT,
    committed_value NUMERIC(22, 2) NOT NULL DEFAULT 0 CHECK (committed_value >= 0),
    recognized_non_cash_value NUMERIC(22, 2) NOT NULL DEFAULT 0 CHECK (recognized_non_cash_value >= 0),
    commitment_due_date DATE,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING' CHECK (
        status IN ('PENDING', 'PARTIALLY_FULFILLED', 'FULFILLED', 'CANCELLED')
    ),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mtms.capital_contributions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    commitment_id UUID NOT NULL REFERENCES mtms.capital_commitments(id) ON DELETE RESTRICT,
    contributed_on DATE NOT NULL,
    contribution_type VARCHAR(20) NOT NULL CHECK (
        contribution_type IN ('CASH', 'ASSET', 'WORK')
    ),
    recognized_value NUMERIC(22, 2) NOT NULL DEFAULT 0 CHECK (recognized_value > 0),
    account_id UUID REFERENCES mtms.financial_accounts(id),
    asset_id UUID REFERENCES mtms.equipment_assets(id),
    ledger_entry_id UUID REFERENCES mtms.cash_ledger_entries(id),
    evidence_file_id UUID REFERENCES mtms.evidence_files(id),
    notes VARCHAR(255),
    created_by UUID NOT NULL REFERENCES mtms.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_contribution_cash_account CHECK (contribution_type != 'CASH' OR account_id IS NOT NULL)
);

CREATE TABLE mtms.profit_entitlement_terms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id UUID NOT NULL REFERENCES mtms.investor_agreements(id) ON DELETE RESTRICT,
    effective_from DATE NOT NULL,
    effective_to DATE,
    profit_share_ratio NUMERIC(9, 6) NOT NULL CHECK (
        profit_share_ratio >= 0 AND profit_share_ratio <= 1
    ),
    eligibility_conditions TEXT,
    approved_by UUID NOT NULL REFERENCES mtms.users(id),
    evidence_file_id UUID REFERENCES mtms.evidence_files(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_profit_terms_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_investor_agreements_branch ON mtms.investor_agreements(branch_id, status);
CREATE INDEX idx_capital_commitments_agreement ON mtms.capital_commitments(agreement_id, status);
CREATE INDEX idx_capital_contributions_commitment ON mtms.capital_contributions(commitment_id, contributed_on);
CREATE INDEX idx_profit_terms_effective ON mtms.profit_entitlement_terms(agreement_id, effective_from, effective_to);
