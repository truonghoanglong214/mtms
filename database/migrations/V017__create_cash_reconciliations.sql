-- P0-FIN-06 / BR-FIN-06 / TC-012 / FE-P0-14
-- Forward-only migration for cash & bank reconciliation records and adjustment categories.

CREATE TABLE mtms.cash_reconciliations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL,
    account_id UUID NOT NULL,
    reconciliation_date DATE NOT NULL,
    recorded_balance NUMERIC(20,2) NOT NULL,
    observed_balance NUMERIC(20,2) NOT NULL,
    variance_amount NUMERIC(20,2) NOT NULL,
    disposition VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    note TEXT,
    evidence_file_id UUID,
    adjustment_ledger_entry_id UUID,
    created_by UUID NOT NULL,
    confirmed_by UUID,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_cash_reconciliations_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_cash_reconciliations_account FOREIGN KEY (account_id) REFERENCES mtms.financial_accounts(id),
    CONSTRAINT fk_cash_reconciliations_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT fk_cash_reconciliations_adj_entry FOREIGN KEY (adjustment_ledger_entry_id) REFERENCES mtms.cash_ledger_entries(id),
    CONSTRAINT fk_cash_reconciliations_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT fk_cash_reconciliations_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES mtms.users(id),
    CONSTRAINT uq_cash_reconciliations_account_date UNIQUE (account_id, reconciliation_date),
    CONSTRAINT ck_cash_reconciliations_disposition CHECK (disposition IN ('OPEN', 'EXPLAINED', 'ADJUSTED', 'CLOSED')),
    CONSTRAINT ck_cash_reconciliations_variance CHECK (variance_amount = observed_balance - recorded_balance)
);

CREATE INDEX idx_cash_reconciliations_branch ON mtms.cash_reconciliations (branch_id, reconciliation_date);
CREATE INDEX idx_cash_reconciliations_account ON mtms.cash_reconciliations (account_id, reconciliation_date);
CREATE INDEX idx_cash_reconciliations_disposition ON mtms.cash_reconciliations (disposition);

-- Seed System Categories for Reconciliation Adjustments
INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system, description) VALUES
('ADJ_SURPLUS', 'Điều chỉnh thừa quỹ kiểm kê/đối soát', 'OTHER', 'INFLOW', TRUE, 'Hạch toán chênh lệch thừa tiền mặt/ngân hàng sau đối soát (TC-012)'),
('ADJ_SHORTAGE', 'Điều chỉnh thiếu quỹ kiểm kê/đối soát', 'OTHER', 'OUTFLOW', TRUE, 'Hạch toán chênh lệch thiếu tiền mặt/ngân hàng sau đối soát (TC-012)');
