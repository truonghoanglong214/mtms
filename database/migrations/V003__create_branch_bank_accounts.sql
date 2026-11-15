-- P0-ORG-07 / BR-ORG-03 / TC-003
-- Forward-only migration for branch bank accounts and operational readiness gate.
-- IDs are supplied by the application; no database UUID extension is required.

CREATE TABLE mtms.branch_bank_accounts (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL,
    bank_name VARCHAR(160) NOT NULL,
    account_holder_name VARCHAR(160) NOT NULL,
    account_number_ciphertext TEXT NOT NULL,
    masked_account_number VARCHAR(40) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_branch_bank_accounts_branch
        FOREIGN KEY (branch_id)
        REFERENCES mtms.branches (id),
    CONSTRAINT ck_branch_bank_accounts_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_branch_bank_accounts_branch_id
    ON mtms.branch_bank_accounts (branch_id);

CREATE UNIQUE INDEX uq_branch_primary_bank
    ON mtms.branch_bank_accounts (branch_id)
    WHERE is_primary = TRUE AND status = 'ACTIVE';
