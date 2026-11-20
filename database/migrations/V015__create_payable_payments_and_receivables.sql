-- P0-FIN-05 / BR-FIN-02 / TC-009
-- Forward-only migration for payable payments (debt settlement) and receivables ledger.

-- 1. Payable Payments (Installments against mtms.payables)
CREATE TABLE mtms.payable_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payable_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(20, 2) NOT NULL,
    paid_on DATE NOT NULL,
    payment_method VARCHAR(24) NOT NULL DEFAULT 'BANK_TRANSFER',
    bank_reference VARCHAR(160),
    evidence_file_id UUID,
    status VARCHAR(16) NOT NULL DEFAULT 'POSTED',
    posted_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_payable_payments_payable FOREIGN KEY (payable_id) REFERENCES mtms.payables(id),
    CONSTRAINT fk_payable_payments_account FOREIGN KEY (account_id) REFERENCES mtms.financial_accounts(id),
    CONSTRAINT fk_payable_payments_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT fk_payable_payments_posted_by FOREIGN KEY (posted_by) REFERENCES mtms.users(id),
    CONSTRAINT ck_payable_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payable_payments_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER')),
    CONSTRAINT ck_payable_payments_status CHECK (status IN ('POSTED', 'REVERSED'))
);

CREATE INDEX idx_payable_payments_payable ON mtms.payable_payments (payable_id);
CREATE INDEX idx_payable_payments_account_date ON mtms.payable_payments (account_id, paid_on);

-- 2. Manual Receivables (Phải thu khác: bồi thường, thanh lý phế liệu, hoàn tiền đối tác...)
CREATE TABLE mtms.receivables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    counterparty_label VARCHAR(200) NOT NULL,
    source_type VARCHAR(48) NOT NULL DEFAULT 'MANUAL_RECEIVABLE',
    source_id UUID,
    incurred_date DATE NOT NULL,
    due_date DATE,
    original_amount NUMERIC(20, 2) NOT NULL,
    received_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_receivables_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_receivables_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_receivables_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT ck_receivables_original_amount CHECK (original_amount >= 0),
    CONSTRAINT ck_receivables_received_amount CHECK (received_amount >= 0),
    CONSTRAINT ck_receivables_received_le_original CHECK (received_amount <= original_amount),
    CONSTRAINT ck_receivables_status CHECK (status IN ('OPEN', 'SETTLED', 'CANCELLED'))
);

CREATE INDEX idx_receivables_branch_status ON mtms.receivables (branch_id, status);
CREATE INDEX idx_receivables_business_entity ON mtms.receivables (business_entity_id);

-- 3. Receivable Receipts (Thu tiền công nợ phải thu)
CREATE TABLE mtms.receivable_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receivable_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(20, 2) NOT NULL,
    received_on DATE NOT NULL,
    payment_method VARCHAR(24) NOT NULL DEFAULT 'BANK_TRANSFER',
    bank_reference VARCHAR(160),
    evidence_file_id UUID,
    status VARCHAR(16) NOT NULL DEFAULT 'POSTED',
    posted_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_receivable_receipts_receivable FOREIGN KEY (receivable_id) REFERENCES mtms.receivables(id),
    CONSTRAINT fk_receivable_receipts_account FOREIGN KEY (account_id) REFERENCES mtms.financial_accounts(id),
    CONSTRAINT fk_receivable_receipts_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT fk_receivable_receipts_posted_by FOREIGN KEY (posted_by) REFERENCES mtms.users(id),
    CONSTRAINT ck_receivable_receipts_amount CHECK (amount > 0),
    CONSTRAINT ck_receivable_receipts_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER')),
    CONSTRAINT ck_receivable_receipts_status CHECK (status IN ('POSTED', 'REVERSED'))
);

CREATE INDEX idx_receivable_receipts_receivable ON mtms.receivable_receipts (receivable_id);
CREATE INDEX idx_receivable_receipts_account_date ON mtms.receivable_receipts (account_id, received_on);

-- 4. Cash Ledger Support for Receivable Receipts
ALTER TABLE mtms.cash_ledger_entries DROP CONSTRAINT ck_cash_ledger_source_type;
ALTER TABLE mtms.cash_ledger_entries ADD CONSTRAINT ck_cash_ledger_source_type CHECK (source_type IN (
    'DAILY_REVENUE', 'SUPPLIER_PAYMENT', 'INTERNAL_TRANSFER',
    'MANUAL_EXPENSE', 'CAPITAL_FLOW', 'PAYROLL_PAYMENT', 'MANUAL_ADJUSTMENT', 'RECEIVABLE_RECEIPT'
));

-- 5. Seed System Category for Other Receivables
INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system, description) VALUES
('REC_OTHER', 'Thu hồi công nợ phải thu khác', 'OTHER', 'INFLOW', TRUE, 'Thu hồi các khoản phải thu khác (bồi thường, hoàn tiền, thanh lý...)')
ON CONFLICT (category_code) DO NOTHING;
