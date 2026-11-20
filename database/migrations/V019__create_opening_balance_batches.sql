-- P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009
-- Forward-only migration for opening balance batches and cutover reconciliations.

-- 1. Seed System Category for Opening Balance
INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system, description) VALUES
('OPENING_BALANCE', 'Số dư tiền đầu kỳ', 'CAPITAL_FLOW', 'INFLOW', TRUE, 'Số dư tiền mặt và ngân hàng đầu kỳ khi đưa chi nhánh vào vận hành hệ thống (BR-FIN-08)')
ON CONFLICT (category_code) DO NOTHING;

-- 2. Create table opening_balance_batches
CREATE TABLE mtms.opening_balance_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL,
    cutover_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    total_cash_amount NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    total_bank_amount NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    total_inventory_valuation NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    total_payable_amount NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    total_receivable_amount NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    total_tax_liability_amount NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    net_working_capital NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    summary_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    note TEXT,
    evidence_file_id UUID,
    created_by UUID NOT NULL,
    posted_by UUID,
    posted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_opening_batches_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_opening_batches_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT fk_opening_batches_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT fk_opening_batches_posted_by FOREIGN KEY (posted_by) REFERENCES mtms.users(id),
    CONSTRAINT uq_opening_batches_branch UNIQUE (branch_id),
    CONSTRAINT ck_opening_batches_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'POSTED')),
    CONSTRAINT ck_opening_batches_cash CHECK (total_cash_amount >= 0),
    CONSTRAINT ck_opening_batches_bank CHECK (total_bank_amount >= 0),
    CONSTRAINT ck_opening_batches_inv CHECK (total_inventory_valuation >= 0),
    CONSTRAINT ck_opening_batches_payable CHECK (total_payable_amount >= 0),
    CONSTRAINT ck_opening_batches_receivable CHECK (total_receivable_amount >= 0),
    CONSTRAINT ck_opening_batches_tax CHECK (total_tax_liability_amount >= 0)
);

CREATE INDEX idx_opening_batches_branch ON mtms.opening_balance_batches (branch_id);

-- 3. Create table opening_account_balances
CREATE TABLE mtms.opening_account_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL,
    account_id UUID NOT NULL,
    opening_balance NUMERIC(20,2) NOT NULL,
    cash_ledger_entry_id UUID,
    CONSTRAINT fk_opening_accounts_batch FOREIGN KEY (batch_id) REFERENCES mtms.opening_balance_batches(id),
    CONSTRAINT fk_opening_accounts_account FOREIGN KEY (account_id) REFERENCES mtms.financial_accounts(id),
    CONSTRAINT fk_opening_accounts_entry FOREIGN KEY (cash_ledger_entry_id) REFERENCES mtms.cash_ledger_entries(id),
    CONSTRAINT uq_opening_account_batch UNIQUE (batch_id, account_id),
    CONSTRAINT ck_opening_account_balance CHECK (opening_balance >= 0)
);

CREATE INDEX idx_opening_accounts_batch ON mtms.opening_account_balances (batch_id);
