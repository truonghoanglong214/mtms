-- P0-FIN-01 / P0-FIN-02 / BR-FIN-01 / BR-FIN-09 / TC-010 / TC-011
-- Forward-only migration for financial categories, branch financial accounts, internal transfers, and cash/bank ledger.

-- 1. Financial Categories (Taxonomy & Separation Rules BR-FIN-01)
CREATE TABLE mtms.financial_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_code VARCHAR(60) NOT NULL,
    category_name VARCHAR(160) NOT NULL,
    category_group VARCHAR(40) NOT NULL,
    flow_direction VARCHAR(20) NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_financial_categories_code UNIQUE (category_code),
    CONSTRAINT ck_financial_categories_group CHECK (category_group IN (
        'OPERATING_EXPENSE', 'CAPITAL_FLOW', 'INTERNAL_TRANSFER',
        'DEBT_SETTLEMENT', 'REVENUE', 'PAYROLL', 'OTHER'
    )),
    CONSTRAINT ck_financial_categories_flow CHECK (flow_direction IN ('INFLOW', 'OUTFLOW', 'BOTH'))
);

CREATE INDEX idx_financial_categories_group ON mtms.financial_categories (category_group, is_active);

-- Seed System Categories (13 core categories)
INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system, description) VALUES
('REV_CASH', 'Doanh thu bán hàng - Tiền mặt', 'REVENUE', 'INFLOW', TRUE, 'Tiền mặt thu từ khách hàng theo báo cáo ca/ngày'),
('REV_BANK', 'Doanh thu bán hàng - Chuyển khoản', 'REVENUE', 'INFLOW', TRUE, 'Tiền chuyển khoản ngân hàng theo báo cáo ca/ngày'),
('TRF_INTERNAL', 'Chuyển quỹ nội bộ (Tiền mặt <-> Ngân hàng)', 'INTERNAL_TRANSFER', 'BOTH', TRUE, 'Luân chuyển giữa quỹ tiền mặt và tài khoản ngân hàng chi nhánh (TC-010)'),
('DEBT_SUPPLIER', 'Chi trả nợ nhà cung cấp mua hàng', 'DEBT_SETTLEMENT', 'OUTFLOW', TRUE, 'Thanh toán tiền mua nguyên vật liệu đã ghi nợ (TC-009)'),
('PAY_SALARY', 'Chi trả lương nhân viên', 'PAYROLL', 'OUTFLOW', TRUE, 'Thanh toán tiền lương thực tế cho nhân sự'),
('CAP_INJECTION', 'Vốn góp chủ sở hữu / Nhà đầu tư', 'CAPITAL_FLOW', 'INFLOW', TRUE, 'Tiền vốn góp kinh doanh ban đầu hoặc bổ sung'),
('CAP_WITHDRAWAL', 'Rút vốn chủ sở hữu', 'CAPITAL_FLOW', 'OUTFLOW', TRUE, 'Rút vốn khỏi hoạt động kinh doanh'),
('OPEX_RENT', 'Chi phí thuê mặt bằng', 'OPERATING_EXPENSE', 'OUTFLOW', TRUE, 'Tiền thuê địa điểm kinh doanh chi nhánh'),
('OPEX_UTILITIES', 'Chi phí điện, nước, internet', 'OPERATING_EXPENSE', 'OUTFLOW', TRUE, 'Hóa đơn dịch vụ tiện ích quán'),
('OPEX_SUPPLIES', 'Chi phí vật tư, bao bì tiêu hao', 'OPERATING_EXPENSE', 'OUTFLOW', TRUE, 'Ly, ống hút, túi, giấy in nhiệt, nước rửa chén...'),
('OPEX_MARKETING', 'Chi phí marketing & quảng cáo', 'OPERATING_EXPENSE', 'OUTFLOW', TRUE, 'In ấn tờ rơi, chạy ads, bảng hiệu...'),
('OPEX_MAINTENANCE', 'Chi phí bảo trì, sửa chữa máy móc', 'OPERATING_EXPENSE', 'OUTFLOW', TRUE, 'Bảo dưỡng máy pha, máy dập nắp, tủ lạnh...'),
('OPEX_OTHER', 'Chi phí vận hành khác', 'OPERATING_EXPENSE', 'OUTFLOW', TRUE, 'Chi phí vận hành lặt vặt khác');

-- 2. Financial Accounts (Per branch cash/bank accounts)
CREATE TABLE mtms.financial_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL,
    bank_account_id UUID,
    account_name VARCHAR(160) NOT NULL,
    account_kind VARCHAR(16) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    current_balance NUMERIC(20,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_financial_accounts_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_financial_accounts_bank_account FOREIGN KEY (bank_account_id) REFERENCES mtms.branch_bank_accounts(id),
    CONSTRAINT uq_financial_accounts_branch_name UNIQUE (branch_id, account_name),
    CONSTRAINT ck_financial_accounts_kind CHECK (account_kind IN ('CASH', 'BANK')),
    CONSTRAINT ck_financial_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_financial_accounts_bank_ref CHECK ((account_kind = 'CASH' AND bank_account_id IS NULL) OR (account_kind = 'BANK'))
);

CREATE INDEX idx_financial_accounts_branch ON mtms.financial_accounts (branch_id, account_kind);

-- 3. Internal Transfers (Cash <-> Bank within branch TC-010)
CREATE TABLE mtms.internal_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL,
    from_account_id UUID NOT NULL,
    to_account_id UUID NOT NULL,
    amount NUMERIC(20,2) NOT NULL,
    business_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'COMPLETED',
    note TEXT,
    evidence_file_id UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_internal_transfers_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_internal_transfers_from FOREIGN KEY (from_account_id) REFERENCES mtms.financial_accounts(id),
    CONSTRAINT fk_internal_transfers_to FOREIGN KEY (to_account_id) REFERENCES mtms.financial_accounts(id),
    CONSTRAINT fk_internal_transfers_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT fk_internal_transfers_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT ck_internal_transfers_amount CHECK (amount > 0),
    CONSTRAINT ck_internal_transfers_accounts CHECK (from_account_id <> to_account_id),
    CONSTRAINT ck_internal_transfers_status CHECK (status IN ('COMPLETED', 'REVERSED'))
);

CREATE INDEX idx_internal_transfers_branch_date ON mtms.internal_transfers (branch_id, business_date);

-- 4. Cash Ledger Entries (Append-only subledger BR-FIN-09, TC-011)
CREATE TABLE mtms.cash_ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    business_date DATE NOT NULL,
    entry_type VARCHAR(16) NOT NULL,
    amount NUMERIC(20,2) NOT NULL,
    signed_amount NUMERIC(20,2) NOT NULL,
    running_balance NUMERIC(20,2) NOT NULL,
    category_id UUID NOT NULL,
    source_type VARCHAR(48) NOT NULL,
    source_id UUID NOT NULL,
    source_leg VARCHAR(24) NOT NULL DEFAULT 'MAIN',
    status VARCHAR(24) NOT NULL DEFAULT 'POSTED',
    reversal_of_id UUID,
    memo TEXT,
    evidence_file_id UUID,
    created_by UUID NOT NULL,
    posted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_cash_ledger_account FOREIGN KEY (account_id) REFERENCES mtms.financial_accounts(id),
    CONSTRAINT fk_cash_ledger_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_cash_ledger_category FOREIGN KEY (category_id) REFERENCES mtms.financial_categories(id),
    CONSTRAINT fk_cash_ledger_reversal_of FOREIGN KEY (reversal_of_id) REFERENCES mtms.cash_ledger_entries(id),
    CONSTRAINT fk_cash_ledger_evidence FOREIGN KEY (evidence_file_id) REFERENCES mtms.evidence_files(id),
    CONSTRAINT fk_cash_ledger_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT ck_cash_ledger_entry_type CHECK (entry_type IN ('INFLOW', 'OUTFLOW')),
    CONSTRAINT ck_cash_ledger_amount CHECK (amount > 0),
    CONSTRAINT ck_cash_ledger_signed_amount CHECK (signed_amount <> 0),
    CONSTRAINT ck_cash_ledger_source_type CHECK (source_type IN (
        'DAILY_REVENUE', 'SUPPLIER_PAYMENT', 'INTERNAL_TRANSFER',
        'MANUAL_EXPENSE', 'CAPITAL_FLOW', 'PAYROLL_PAYMENT', 'MANUAL_ADJUSTMENT'
    )),
    CONSTRAINT ck_cash_ledger_status CHECK (status IN ('POSTED', 'REVERSED')),
    -- TC-011: Source uniqueness prevents double posting
    CONSTRAINT uq_cash_ledger_source_leg UNIQUE (account_id, source_type, source_id, source_leg)
);

CREATE INDEX idx_cash_ledger_account_date ON mtms.cash_ledger_entries (account_id, business_date);
CREATE INDEX idx_cash_ledger_branch_date ON mtms.cash_ledger_entries (branch_id, business_date);
CREATE INDEX idx_cash_ledger_source ON mtms.cash_ledger_entries (source_type, source_id);
