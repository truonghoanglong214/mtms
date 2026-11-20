-- P0-HR-07 & P0-HR-08: Payroll Payments and Cash Ledger Linkage
-- Authoritative: docs/api/contracts/P0-HR-07-09.md
-- BR-HR-03, BR-HR-05, BR-HR-06, TC-015, TC-016, TC-017

CREATE TABLE mtms.payroll_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_line_id UUID NOT NULL REFERENCES mtms.payroll_lines(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES mtms.financial_accounts(id),
    amount NUMERIC(20,2) NOT NULL CHECK (amount > 0),
    paid_on DATE NOT NULL,
    payment_method VARCHAR(20) NOT NULL DEFAULT 'BANK_TRANSFER' CHECK (payment_method IN ('CASH', 'BANK_TRANSFER')),
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED', 'REVERSED')),
    external_reference VARCHAR(160),
    evidence_file_id UUID REFERENCES mtms.evidence_files(id),
    ledger_entry_id UUID REFERENCES mtms.cash_ledger_entries(id),
    note TEXT,
    confirmed_by UUID REFERENCES mtms.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payroll_payments_line ON mtms.payroll_payments(payroll_line_id);
CREATE INDEX idx_payroll_payments_account ON mtms.payroll_payments(account_id);
CREATE INDEX idx_payroll_payments_paid_on ON mtms.payroll_payments(paid_on);
