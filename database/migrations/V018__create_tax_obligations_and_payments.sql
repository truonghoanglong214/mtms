-- P0-FIN-07 / BR-FIN-04 / TC-012
-- Forward-only migration for tax manual obligations and tax payments.

-- 1. Alter cash_ledger_entries constraint to allow TAX_PAYMENT source_type
ALTER TABLE mtms.cash_ledger_entries DROP CONSTRAINT ck_cash_ledger_source_type;
ALTER TABLE mtms.cash_ledger_entries ADD CONSTRAINT ck_cash_ledger_source_type CHECK (source_type IN (
    'DAILY_REVENUE', 'SUPPLIER_PAYMENT', 'INTERNAL_TRANSFER',
    'MANUAL_EXPENSE', 'CAPITAL_FLOW', 'PAYROLL_PAYMENT', 'MANUAL_ADJUSTMENT',
    'RECEIVABLE_RECEIPT', 'TAX_PAYMENT'
));

-- 2. Seed System Category for Tax Payment
INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system, description) VALUES
('TAX_PAYMENT', 'Chi nộp thuế và nghĩa vụ nhà nước', 'OTHER', 'OUTFLOW', TRUE, 'Chi nộp thuế môn bài, thuế GTGT, thuế TNDN, thuế TNCN và các nghĩa vụ ngân sách nhà nước (BR-FIN-04)')
ON CONFLICT (category_code) DO NOTHING;

-- 3. Create table entity_tax_obligations
CREATE TABLE mtms.entity_tax_obligations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID,
    obligation_code VARCHAR(60) NOT NULL,
    obligation_type VARCHAR(40) NOT NULL,
    tax_authority_name VARCHAR(200),
    tax_period_start DATE NOT NULL,
    tax_period_end DATE NOT NULL,
    due_date DATE,
    amount_due NUMERIC(20,2) NOT NULL,
    amount_paid NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    amount_allocated NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    note TEXT,
    evidence_file_id UUID,
    entered_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_tax_obligations_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_tax_obligations_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_tax_obligations_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT fk_tax_obligations_entered_by FOREIGN KEY (entered_by) REFERENCES mtms.users(id),
    CONSTRAINT uq_tax_obligations_code UNIQUE (business_entity_id, obligation_code),
    CONSTRAINT ck_tax_obligations_type CHECK (obligation_type IN ('MON_BAI', 'VAT', 'CIT', 'PIT', 'OTHER')),
    CONSTRAINT ck_tax_obligations_status CHECK (status IN ('OPEN', 'PARTIALLY_PAID', 'PAID', 'CANCELLED')),
    CONSTRAINT ck_tax_obligations_period CHECK (tax_period_end >= tax_period_start),
    CONSTRAINT ck_tax_obligations_amount_due CHECK (amount_due >= 0),
    CONSTRAINT ck_tax_obligations_amount_paid CHECK (amount_paid >= 0 AND amount_paid <= amount_due),
    CONSTRAINT ck_tax_obligations_amount_allocated CHECK (amount_allocated >= 0 AND amount_allocated <= amount_due)
);

CREATE INDEX idx_tax_obligations_entity ON mtms.entity_tax_obligations (business_entity_id, status);
CREATE INDEX idx_tax_obligations_branch ON mtms.entity_tax_obligations (branch_id, status);
CREATE INDEX idx_tax_obligations_period ON mtms.entity_tax_obligations (tax_period_start, tax_period_end);

-- 4. Create table tax_payments
CREATE TABLE mtms.tax_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    obligation_id UUID NOT NULL,
    branch_id UUID,
    payment_date DATE NOT NULL,
    amount NUMERIC(20,2) NOT NULL,
    payment_method VARCHAR(24) NOT NULL DEFAULT 'BANK_TRANSFER',
    financial_account_id UUID,
    cash_ledger_entry_id UUID,
    receipt_number VARCHAR(100),
    note TEXT,
    evidence_file_id UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_tax_payments_obligation FOREIGN KEY (obligation_id) REFERENCES mtms.entity_tax_obligations(id),
    CONSTRAINT fk_tax_payments_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_tax_payments_account FOREIGN KEY (financial_account_id) REFERENCES mtms.financial_accounts(id),
    CONSTRAINT fk_tax_payments_ledger_entry FOREIGN KEY (cash_ledger_entry_id) REFERENCES mtms.cash_ledger_entries(id),
    CONSTRAINT fk_tax_payments_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT fk_tax_payments_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT ck_tax_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_tax_payments_method CHECK (payment_method IN ('BANK_TRANSFER', 'CASH'))
);

CREATE INDEX idx_tax_payments_obligation ON mtms.tax_payments (obligation_id);
CREATE INDEX idx_tax_payments_branch_date ON mtms.tax_payments (branch_id, payment_date);
