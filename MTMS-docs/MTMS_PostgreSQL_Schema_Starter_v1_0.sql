-- ============================================================================
-- MILKTEA MANAGEMENT SYSTEM (MTMS) - PostgreSQL Schema Starter v1.0
-- Date: 2026-11-07 | Target PostgreSQL 17 | Planning reference only
-- ============================================================================
-- NOT AN APPROVED PRODUCTION MIGRATION. This models P0 + planned P1. Split and
-- test into immutable, chronological Flyway migrations before first deployment.
-- This schema cannot by itself guarantee all business invariants, branch scopes,
-- full-payout conditions or legally compliant capital distributions. Implement
-- service-level transactional/authorization checks and integration tests.
--
-- PRINCIPLES:
-- * No POS or individual beverage order tables; aggregate daily revenue only.
-- * branch_id + business_entity_id must be consistent (app+DB validation).
-- * per-branch inventory moving WAC; no negative stock; expired-lot checks.
-- * amounts NUMERIC(20,2), quantities NUMERIC(18,3), prices NUMERIC(20,6).
-- * timestamps are TIMESTAMPTZ, local business dates are DATE in Asia/Ho_Chi_Minh.
-- * no destructive edits to confirmed ledger/stock/paid distribution events.
-- * physical cash / real bank transactions are distinct from delivery-app NET.
-- * background assumptions marked POLICY_VERIFY in companion docs.
-- ============================================================================
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS mtms;
SET LOCAL search_path TO mtms, public;

-- A. ORGANIZATION / IDENTITY -------------------------------------------------
CREATE TABLE business_entities (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 entity_code VARCHAR(32) NOT NULL UNIQUE,
 registered_name VARCHAR(200) NOT NULL,
 registration_ref VARCHAR(100),
 entity_type VARCHAR(32) NOT NULL DEFAULT 'HOUSEHOLD_BUSINESS',
 status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE branches (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 business_entity_id UUID NOT NULL REFERENCES business_entities(id),
 branch_code VARCHAR(40) NOT NULL UNIQUE,
 name VARCHAR(160) NOT NULL,
 address_text TEXT,
 time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
 opens_at TIME NOT NULL DEFAULT TIME '06:00',
 closes_at TIME NOT NULL DEFAULT TIME '22:30',
 status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','CLOSED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (business_entity_id, id)
);
CREATE TABLE users (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 email VARCHAR(200) UNIQUE,
 phone VARCHAR(30) UNIQUE,
 password_hash TEXT NOT NULL,
 display_name VARCHAR(160) NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','DISABLED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (email IS NOT NULL OR phone IS NOT NULL)
);
CREATE TABLE scoped_role_grants (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id UUID NOT NULL REFERENCES users(id),
 branch_id UUID REFERENCES branches(id),
 role_key VARCHAR(40) NOT NULL CHECK (role_key IN ('SYSTEM_ADMIN','BRANCH_OWNER','MANAGER','FINANCE','EMPLOYEE','INVESTOR')),
 report_permission_group VARCHAR(50),
 valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
 valid_to DATE,
 granted_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (valid_to IS NULL OR valid_to >= valid_from),
 CHECK (role_key = 'SYSTEM_ADMIN' OR branch_id IS NOT NULL)
);
CREATE INDEX idx_role_scope ON scoped_role_grants(user_id,branch_id,role_key,valid_from);
CREATE TABLE evidence_files (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 business_entity_id UUID REFERENCES business_entities(id),
 branch_id UUID REFERENCES branches(id),
 storage_object_key TEXT NOT NULL UNIQUE,
 original_filename VARCHAR(255) NOT NULL,
 content_type VARCHAR(120) NOT NULL,
 byte_size BIGINT NOT NULL CHECK (byte_size > 0),
 checksum_sha256 CHAR(64),
 visibility_class VARCHAR(32) NOT NULL DEFAULT 'BRANCH_PRIVATE',
 retention_until DATE,
 uploaded_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE delegation_records (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 user_id UUID NOT NULL REFERENCES users(id),
 delegated_by UUID NOT NULL REFERENCES users(id),
 purpose VARCHAR(50) NOT NULL DEFAULT 'PROFIT_DISTRIBUTION',
 evidence_file_id UUID REFERENCES evidence_files(id),
 effective_from DATE NOT NULL,
 effective_to DATE,
 revoked_at TIMESTAMPTZ,
 rationale TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX idx_delegations_lookup ON delegation_records(branch_id,user_id,effective_from,effective_to);
CREATE TABLE branch_bank_accounts (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 bank_name VARCHAR(160) NOT NULL,
 account_holder_name VARCHAR(160) NOT NULL,
 account_number_ciphertext TEXT NOT NULL,
 masked_account_number VARCHAR(40) NOT NULL,
 is_primary BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_branch_primary_bank ON branch_bank_accounts(branch_id) WHERE is_primary AND status='ACTIVE';
CREATE TABLE branch_counterparties (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 counterparty_name VARCHAR(160) NOT NULL,
 contact_note VARCHAR(255),
 partner_type VARCHAR(32) NOT NULL DEFAULT 'OTHER_SAME_BRAND_SHOP',
 is_active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- B. BUSINESS DAYS, REVENUE, FINANCE / TREASURY ------------------------------
CREATE TABLE business_days (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 business_date DATE NOT NULL,
 operation_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (operation_status IN ('OPEN','CLOSED')),
 close_reason TEXT,
 recorded_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (branch_id,business_date),
 CHECK (operation_status <> 'CLOSED' OR close_reason IS NOT NULL)
);
CREATE TABLE daily_revenue_reports (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 business_date DATE NOT NULL,
 revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
 is_current BOOLEAN NOT NULL DEFAULT TRUE,
 cash_amount NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (cash_amount >= 0),
 bank_transfer_amount NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (bank_transfer_amount >= 0),
 delivery_app_net_amount NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (delivery_app_net_amount >= 0),
 other_adjustment_amount NUMERIC(20,2) NOT NULL DEFAULT 0,
 status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','SUPERSEDED','CANCELLED')),
 submitted_by UUID NOT NULL REFERENCES users(id),
 confirmed_by UUID REFERENCES users(id),
 confirmed_at TIMESTAMPTZ,
 adjustment_reason TEXT,
 evidence_file_id UUID REFERENCES evidence_files(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (branch_id,business_date,revision)
);
CREATE UNIQUE INDEX uq_daily_current ON daily_revenue_reports(branch_id,business_date) WHERE is_current;
CREATE INDEX idx_daily_month ON daily_revenue_reports(branch_id,business_date,status);
CREATE TABLE financial_accounts (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 bank_account_id UUID REFERENCES branch_bank_accounts(id),
 account_name VARCHAR(160) NOT NULL,
 account_kind VARCHAR(16) NOT NULL CHECK (account_kind IN ('CASH','BANK')),
 currency CHAR(3) NOT NULL DEFAULT 'VND',
 status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK ((account_kind = 'CASH' AND bank_account_id IS NULL) OR account_kind = 'BANK')
);
CREATE TABLE cash_ledger_entries (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 account_id UUID NOT NULL REFERENCES financial_accounts(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 business_date DATE NOT NULL,
 signed_amount NUMERIC(20,2) NOT NULL CHECK (signed_amount <> 0),
 source_type VARCHAR(48) NOT NULL,
 source_id UUID NOT NULL,
 source_leg VARCHAR(24) NOT NULL DEFAULT 'MAIN',
 ledger_category VARCHAR(50) NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED','REVERSED')),
 reversal_of_id UUID REFERENCES cash_ledger_entries(id),
 memo TEXT,
 created_by UUID NOT NULL REFERENCES users(id),
 posted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (account_id,source_type,source_id,source_leg)
);
CREATE INDEX idx_cash_account_date ON cash_ledger_entries(account_id,business_date);
CREATE TABLE expenses (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 expense_date DATE NOT NULL,
 category_code VARCHAR(60) NOT NULL,
 amount NUMERIC(20,2) NOT NULL CHECK (amount > 0),
 is_cash_paid BOOLEAN NOT NULL DEFAULT FALSE,
 payment_account_id UUID REFERENCES financial_accounts(id),
 payable_id UUID,
 evidence_file_id UUID REFERENCES evidence_files(id),
 status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','ADJUSTED','VOIDED')),
 review_flag BOOLEAN NOT NULL DEFAULT FALSE,
 reason TEXT,
 created_by UUID NOT NULL REFERENCES users(id),
 confirmed_by UUID REFERENCES users(id),
 confirmed_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_expenses_branch_date ON expenses(branch_id,expense_date,status);
CREATE TABLE payables (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 counterparty_label VARCHAR(200) NOT NULL,
 source_type VARCHAR(48) NOT NULL,
 source_id UUID,
 incurred_date DATE NOT NULL,
 due_date DATE,
 original_amount NUMERIC(20,2) NOT NULL CHECK (original_amount >= 0),
 paid_amount NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
 status VARCHAR(24) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','SETTLED','DISPUTED','CANCELLED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (paid_amount <= original_amount)
);
ALTER TABLE expenses ADD CONSTRAINT fk_expense_payable FOREIGN KEY (payable_id) REFERENCES payables(id);
CREATE TABLE payable_payments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 payable_id UUID NOT NULL REFERENCES payables(id),
 account_id UUID NOT NULL REFERENCES financial_accounts(id),
 amount NUMERIC(20,2) NOT NULL CHECK (amount > 0),
 paid_on DATE NOT NULL,
 bank_reference VARCHAR(160),
 evidence_file_id UUID REFERENCES evidence_files(id),
 status VARCHAR(16) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED','REVERSED')),
 posted_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE cash_reconciliations (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 account_id UUID NOT NULL REFERENCES financial_accounts(id),
 reconciliation_date DATE NOT NULL,
 recorded_balance NUMERIC(20,2) NOT NULL,
 observed_balance NUMERIC(20,2) NOT NULL,
 variance_amount NUMERIC(20,2) NOT NULL,
 disposition VARCHAR(24) NOT NULL DEFAULT 'OPEN' CHECK (disposition IN ('OPEN','EXPLAINED','ADJUSTED','CLOSED')),
 note TEXT,
 evidence_file_id UUID REFERENCES evidence_files(id),
 confirmed_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (account_id,reconciliation_date)
);
CREATE TABLE entity_tax_obligations (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 business_entity_id UUID NOT NULL REFERENCES business_entities(id),
 obligation_period_start DATE NOT NULL,
 obligation_period_end DATE NOT NULL,
 obligation_type VARCHAR(70) NOT NULL,
 amount_due NUMERIC(20,2) NOT NULL CHECK (amount_due >= 0),
 amount_paid NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
 amount_allocated NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (amount_allocated >= 0),
 entered_by UUID NOT NULL REFERENCES users(id),
 status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (obligation_period_end >= obligation_period_start),
 CHECK (amount_paid <= amount_due),
 CHECK (amount_allocated <= amount_due)
);
CREATE TABLE entity_cost_allocations (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 obligation_id UUID NOT NULL REFERENCES entity_tax_obligations(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 allocated_amount NUMERIC(20,2) NOT NULL CHECK (allocated_amount >= 0),
 allocation_basis TEXT NOT NULL,
 approved_by UUID NOT NULL REFERENCES users(id),
 approved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (obligation_id,branch_id)
);

-- C. MIGRATION / EVIDENCE / AUDIT / DEDUP ------------------------------------
CREATE TABLE import_batches (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID REFERENCES branches(id),
 import_kind VARCHAR(48) NOT NULL,
 source_filename VARCHAR(255) NOT NULL,
 source_sha256 CHAR(64) NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'PREVIEW' CHECK (status IN ('PREVIEW','VALIDATED','COMMITTED','REJECTED')),
 preview_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
 committed_by UUID REFERENCES users(id),
 committed_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (import_kind,branch_id,source_sha256)
);
CREATE TABLE import_rows (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 import_batch_id UUID NOT NULL REFERENCES import_batches(id),
 row_number INTEGER NOT NULL CHECK (row_number > 0),
 raw_data JSONB NOT NULL,
 normalized_data JSONB,
 errors JSONB NOT NULL DEFAULT '[]'::jsonb,
 status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
 UNIQUE (import_batch_id,row_number)
);
CREATE TABLE opening_balance_batches (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 cutover_date DATE NOT NULL,
 import_batch_id UUID REFERENCES import_batches(id),
 summary_snapshot JSONB NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','APPROVED','POSTED')),
 approved_by UUID REFERENCES users(id),
 approved_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (branch_id,cutover_date)
);
CREATE TABLE audit_events (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID REFERENCES branches(id),
 actor_user_id UUID REFERENCES users(id),
 correlation_id UUID,
 target_type VARCHAR(80) NOT NULL,
 target_id UUID NOT NULL,
 event_name VARCHAR(90) NOT NULL,
 action_reason TEXT,
 old_snapshot JSONB,
 new_snapshot JSONB,
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_target ON audit_events(target_type,target_id,occurred_at);
CREATE TABLE idempotency_records (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 actor_user_id UUID NOT NULL REFERENCES users(id),
 key VARCHAR(160) NOT NULL,
 request_hash CHAR(64) NOT NULL,
 response_status INTEGER,
 response_snapshot JSONB,
 expires_at TIMESTAMPTZ NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (actor_user_id,key)
);

-- D. WORKFORCE / TIMESHEETS / PAYROLL ---------------------------------------
CREATE TABLE employees (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id UUID UNIQUE REFERENCES users(id),
 staff_code VARCHAR(50) NOT NULL UNIQUE,
 full_name VARCHAR(160) NOT NULL,
 position_name VARCHAR(100),
 hired_on DATE NOT NULL,
 left_on DATE,
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (left_on IS NULL OR left_on >= hired_on)
);
CREATE TABLE employee_assignments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 employee_id UUID NOT NULL REFERENCES employees(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 is_primary BOOLEAN NOT NULL DEFAULT TRUE,
 effective_from DATE NOT NULL,
 effective_to DATE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX idx_assignment_effective ON employee_assignments(employee_id,effective_from,effective_to);
CREATE TABLE salary_policies (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 employee_id UUID REFERENCES employees(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 rate_type VARCHAR(16) NOT NULL CHECK (rate_type IN ('HOURLY','MONTHLY')),
 rate_amount NUMERIC(20,2) NOT NULL CHECK (rate_amount >= 0),
 standard_workdays NUMERIC(7,2),
 paid_minutes_rounding_block SMALLINT,
 effective_from DATE NOT NULL,
 effective_to DATE,
 approved_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (standard_workdays IS NULL OR standard_workdays > 0),
 CHECK (paid_minutes_rounding_block IS NULL OR paid_minutes_rounding_block IN (1,5,10,15,30,60)),
 CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE TABLE work_shifts (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 shift_name VARCHAR(80) NOT NULL,
 starts_at TIME NOT NULL,
 ends_at TIME NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
 CHECK (ends_at > starts_at) -- Current stores have no overnight shifts
);
CREATE TABLE shift_assignments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 employee_id UUID NOT NULL REFERENCES employees(id),
 work_shift_id UUID NOT NULL REFERENCES work_shifts(id),
 work_date DATE NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
 approved_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (employee_id,work_shift_id,work_date)
);
CREATE TABLE attendance_events (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 employee_id UUID NOT NULL REFERENCES employees(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 event_type VARCHAR(12) NOT NULL CHECK (event_type IN ('CHECK_IN','CHECK_OUT')),
 occurred_at TIMESTAMPTZ NOT NULL,
 latitude NUMERIC(10,7),
 longitude NUMERIC(10,7),
 accuracy_m NUMERIC(10,2),
 is_location_anomaly BOOLEAN NOT NULL DEFAULT FALSE,
 review_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (latitude IS NULL OR (latitude BETWEEN -90 AND 90)),
 CHECK (longitude IS NULL OR (longitude BETWEEN -180 AND 180))
);
CREATE TABLE leave_requests (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 employee_id UUID NOT NULL REFERENCES employees(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 start_date DATE NOT NULL,
 end_date DATE NOT NULL,
 is_paid BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 approved_by UUID REFERENCES users(id),
 reason TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (end_date >= start_date)
);
CREATE TABLE timesheet_entries (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 employee_id UUID NOT NULL REFERENCES employees(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 work_date DATE NOT NULL,
 raw_minutes INTEGER CHECK (raw_minutes >= 0),
 approved_minutes INTEGER NOT NULL DEFAULT 0 CHECK (approved_minutes >= 0),
 payable_workdays NUMERIC(6,2) NOT NULL DEFAULT 0 CHECK (payable_workdays >= 0),
 source_type VARCHAR(24) NOT NULL DEFAULT 'MANUAL',
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 approved_by UUID REFERENCES users(id),
 approved_at TIMESTAMPTZ,
 approval_note TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (employee_id,branch_id,work_date)
);
CREATE TABLE payroll_periods (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 month_start DATE NOT NULL,
 revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
 is_current BOOLEAN NOT NULL DEFAULT TRUE,
 status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','APPROVED','SUPERSEDED','VOIDED')),
 approved_by UUID REFERENCES users(id),
 approved_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (branch_id,month_start,revision),
 CHECK (EXTRACT(DAY FROM month_start) = 1)
);
CREATE UNIQUE INDEX uq_payroll_current ON payroll_periods(branch_id,month_start) WHERE is_current;
CREATE TABLE payroll_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 payroll_period_id UUID NOT NULL REFERENCES payroll_periods(id),
 employee_id UUID NOT NULL REFERENCES employees(id),
 expense_branch_id UUID NOT NULL REFERENCES branches(id),
 approved_minutes INTEGER NOT NULL DEFAULT 0 CHECK (approved_minutes >= 0),
 payable_workdays NUMERIC(7,2) NOT NULL DEFAULT 0,
 base_salary NUMERIC(20,2) NOT NULL DEFAULT 0,
 adjustments_total NUMERIC(20,2) NOT NULL DEFAULT 0,
 gross_salary NUMERIC(20,2) NOT NULL CHECK (gross_salary >= 0),
 paid_amount NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
 policy_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
 calculation_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
 UNIQUE (payroll_period_id,employee_id),
 CHECK (paid_amount <= gross_salary)
);
CREATE TABLE payroll_adjustments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 payroll_line_id UUID NOT NULL REFERENCES payroll_lines(id),
 adjustment_code VARCHAR(50) NOT NULL,
 signed_amount NUMERIC(20,2) NOT NULL CHECK (signed_amount <> 0),
 explanation TEXT NOT NULL,
 approved_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE payroll_payments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 payroll_line_id UUID NOT NULL REFERENCES payroll_lines(id),
 account_id UUID NOT NULL REFERENCES financial_accounts(id),
 amount NUMERIC(20,2) NOT NULL CHECK (amount > 0),
 paid_on DATE NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED','REVERSED')),
 external_reference VARCHAR(160),
 confirmed_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- E. SUPPLIER, ITEM, STOCK, PER-BRANCH WAC ----------------------------------
CREATE TABLE suppliers (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 business_entity_id UUID REFERENCES business_entities(id),
 name VARCHAR(200) NOT NULL,
 contact_note TEXT,
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);
CREATE TABLE units (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 unit_code VARCHAR(20) NOT NULL UNIQUE,
 unit_name VARCHAR(70) NOT NULL,
 dimension VARCHAR(16) NOT NULL CHECK (dimension IN ('MASS','VOLUME','COUNT','OTHER'))
);
CREATE TABLE catalog_items (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 item_code VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(160) NOT NULL,
 base_unit_id UUID NOT NULL REFERENCES units(id),
 item_kind VARCHAR(24) NOT NULL CHECK (item_kind IN ('INGREDIENT','CONSUMABLE','EQUIPMENT','OTHER')),
 lot_tracking_required BOOLEAN NOT NULL DEFAULT FALSE,
 expiration_tracking_required BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
 CHECK (NOT expiration_tracking_required OR lot_tracking_required)
);
CREATE TABLE supplier_pack_conversions (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 supplier_id UUID NOT NULL REFERENCES suppliers(id),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 pack_name VARCHAR(100) NOT NULL,
 base_units_per_pack NUMERIC(18,3) NOT NULL CHECK (base_units_per_pack > 0),
 effective_from DATE NOT NULL,
 effective_to DATE,
 CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE TABLE purchase_receipts (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 supplier_id UUID REFERENCES suppliers(id),
 received_on DATE NOT NULL,
 invoice_number VARCHAR(120),
 invoice_total NUMERIC(20,2),
 documented_discrepancy TEXT,
 payable_id UUID REFERENCES payables(id),
 evidence_file_id UUID REFERENCES evidence_files(id),
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','POSTED','VOIDED')),
 posted_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE purchase_receipt_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 receipt_id UUID NOT NULL REFERENCES purchase_receipts(id),
 line_no INTEGER NOT NULL CHECK (line_no > 0),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 declared_pack_quantity NUMERIC(18,3),
 received_base_quantity NUMERIC(18,3) NOT NULL CHECK (received_base_quantity > 0),
 conversion_snapshot NUMERIC(18,6) NOT NULL DEFAULT 1 CHECK (conversion_snapshot > 0),
 base_unit_cost NUMERIC(20,6) NOT NULL CHECK (base_unit_cost >= 0),
 line_total NUMERIC(20,2) NOT NULL CHECK (line_total >= 0),
 lot_code VARCHAR(80),
 expires_on DATE,
 UNIQUE (receipt_id,line_no)
);
CREATE TABLE stock_balances (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 quantity NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
 inventory_value NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (inventory_value >= 0),
 avg_unit_cost NUMERIC(20,6) NOT NULL DEFAULT 0 CHECK (avg_unit_cost >= 0),
 version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (branch_id,item_id)
);
CREATE TABLE stock_lots (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 lot_code VARCHAR(80) NOT NULL,
 expires_on DATE,
 supplier_id UUID REFERENCES suppliers(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (branch_id,item_id,lot_code)
);
CREATE TABLE stock_lot_balances (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 lot_id UUID NOT NULL UNIQUE REFERENCES stock_lots(id),
 quantity NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
 version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE stock_documents (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 document_type VARCHAR(28) NOT NULL CHECK (document_type IN ('CONSUMPTION','DISPOSAL','COUNT_ADJUSTMENT','OPENING','OTHER')), 
 document_date DATE NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','POSTED','CANCELLED')),
 reason TEXT,
 created_by UUID NOT NULL REFERENCES users(id),
 approved_by UUID REFERENCES users(id),
 posted_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE stock_document_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 stock_document_id UUID NOT NULL REFERENCES stock_documents(id),
 line_no INTEGER NOT NULL CHECK (line_no > 0),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 lot_id UUID REFERENCES stock_lots(id),
 base_quantity NUMERIC(18,3) NOT NULL CHECK (base_quantity > 0),
 booked_unit_cost NUMERIC(20,6),
 booked_total NUMERIC(22,2),
 UNIQUE (stock_document_id,line_no)
);
CREATE TABLE stock_movements (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 lot_id UUID REFERENCES stock_lots(id),
 business_date DATE NOT NULL,
 movement_type VARCHAR(28) NOT NULL,
 signed_quantity NUMERIC(18,3) NOT NULL CHECK (signed_quantity <> 0),
 signed_value NUMERIC(22,2) NOT NULL,
 booked_avg_cost NUMERIC(20,6) NOT NULL CHECK (booked_avg_cost >= 0),
 source_type VARCHAR(50) NOT NULL,
 source_id UUID NOT NULL,
 source_line_no INTEGER NOT NULL DEFAULT 1,
 posting_leg VARCHAR(24) NOT NULL DEFAULT 'MAIN',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (branch_id,source_type,source_id,source_line_no,posting_leg)
);
CREATE INDEX idx_stock_movements_branch_date ON stock_movements(branch_id,item_id,business_date);
CREATE TABLE stock_counts (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 counted_on DATE NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','POSTED','REJECTED')),
 reason TEXT NOT NULL,
 submitted_by UUID REFERENCES users(id),
 approved_by UUID REFERENCES users(id),
 approved_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE stock_count_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 stock_count_id UUID NOT NULL REFERENCES stock_counts(id),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 expected_qty NUMERIC(18,3) NOT NULL,
 counted_qty NUMERIC(18,3) NOT NULL CHECK (counted_qty >= 0),
 variance_value NUMERIC(22,2),
 reason TEXT NOT NULL,
 UNIQUE (stock_count_id,item_id)
);

-- F. TRANSFERS / IN-KIND LOANS / INTERNAL BRANCH BALANCES -------------------
CREATE TABLE stock_transfers (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 from_branch_id UUID NOT NULL REFERENCES branches(id),
 to_branch_id UUID NOT NULL REFERENCES branches(id),
 requested_on DATE NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','DISPATCHED','PARTIAL_RECEIPT','DISPUTED','COMPLETED','CANCELLED')),
 requested_by UUID NOT NULL REFERENCES users(id),
 dispatched_by UUID REFERENCES users(id),
 completed_at TIMESTAMPTZ,
 note TEXT,
 CHECK (from_branch_id <> to_branch_id)
 -- BUSINESS RULE: both branches must belong to same business entity, enforce by service/trigger.
);
CREATE TABLE stock_transfer_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 transfer_id UUID NOT NULL REFERENCES stock_transfers(id),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 dispatched_base_qty NUMERIC(18,3) NOT NULL CHECK (dispatched_base_qty > 0),
 received_base_qty NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (received_base_qty >= 0),
 resolved_difference_qty NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (resolved_difference_qty >= 0),
 sender_unit_cost NUMERIC(20,6),
 note TEXT,
 UNIQUE (transfer_id,item_id),
 CHECK (received_base_qty + resolved_difference_qty <= dispatched_base_qty)
);
CREATE TABLE internal_branch_balances (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 transfer_id UUID NOT NULL REFERENCES stock_transfers(id),
 from_branch_id UUID NOT NULL REFERENCES branches(id),
 to_branch_id UUID NOT NULL REFERENCES branches(id),
 amount NUMERIC(20,2) NOT NULL CHECK (amount >= 0),
 settlement_status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (transfer_id),
 CHECK (from_branch_id <> to_branch_id)
 -- Mirror entries are separate internal accounts in application financial read model.
);
CREATE TABLE material_loans (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 counterparty_id UUID NOT NULL REFERENCES branch_counterparties(id),
 direction VARCHAR(12) NOT NULL CHECK (direction IN ('BORROW_IN','LEND_OUT')),
 loan_date DATE NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','CONFIRMED','PARTIALLY_RETURNED','SETTLED','CANCELLED')),
 note TEXT,
 created_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE material_loan_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 loan_id UUID NOT NULL REFERENCES material_loans(id),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 quantity_due NUMERIC(18,3) NOT NULL CHECK (quantity_due > 0),
 quantity_returned NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (quantity_returned >= 0),
 initial_unit_cost NUMERIC(20,6) NOT NULL CHECK (initial_unit_cost >= 0),
 valuation_evidence_file_id UUID REFERENCES evidence_files(id),
 UNIQUE (loan_id,item_id),
 CHECK (quantity_returned <= quantity_due)
);
CREATE TABLE material_loan_returns (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 loan_id UUID NOT NULL REFERENCES material_loans(id),
 returned_on DATE NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED','APPROVED','POSTED','REJECTED')),
 confirmed_by UUID REFERENCES users(id),
 approved_by UUID REFERENCES users(id),
 approved_at TIMESTAMPTZ,
 evidence_file_id UUID REFERENCES evidence_files(id),
 note TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE material_loan_return_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 return_id UUID NOT NULL REFERENCES material_loan_returns(id),
 loan_line_id UUID NOT NULL REFERENCES material_loan_lines(id),
 returned_base_qty NUMERIC(18,3) NOT NULL CHECK (returned_base_qty > 0),
 returned_unit_cost NUMERIC(20,6) NOT NULL CHECK (returned_unit_cost >= 0),
 value_variance NUMERIC(22,2) NOT NULL DEFAULT 0,
 UNIQUE (return_id,loan_line_id)
);

-- G. ASSETS, RECIPES --------------------------------------------------------
CREATE TABLE equipment_assets (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 asset_code VARCHAR(60) NOT NULL UNIQUE,
 name VARCHAR(160) NOT NULL,
 purchased_on DATE,
 acquisition_cost NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (acquisition_cost >= 0),
 warranty_until DATE,
 maintenance_note TEXT,
 depreciation_policy JSONB,
 status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
 -- P0 depreciation not applied to P&L; V1 requires signed policy.
);
CREATE TABLE recipes (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 recipe_name VARCHAR(160) NOT NULL,
 size_label VARCHAR(30),
 sugar_label VARCHAR(30),
 ice_label VARCHAR(30),
 version INTEGER NOT NULL DEFAULT 1,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 UNIQUE (branch_id,recipe_name,size_label,sugar_label,ice_label,version)
);
CREATE TABLE recipe_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 recipe_id UUID NOT NULL REFERENCES recipes(id),
 item_id UUID NOT NULL REFERENCES catalog_items(id),
 base_quantity NUMERIC(18,3) NOT NULL CHECK (base_quantity > 0),
 UNIQUE (recipe_id,item_id)
 -- Theoretical only; never posts a real stock movement.
);

-- H. MONTHLY CLOSE / REOPEN / SNAPSHOT --------------------------------------
CREATE TABLE monthly_closes (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 month_start DATE NOT NULL,
 revision INTEGER NOT NULL DEFAULT 1 CHECK (revision > 0),
 is_current BOOLEAN NOT NULL DEFAULT TRUE,
 status VARCHAR(28) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSING','CLOSED','REOPEN_REQUESTED','REOPENED','SUPERSEDED')),
 managerial_profit NUMERIC(22,2),
 is_provisional BOOLEAN NOT NULL DEFAULT TRUE,
 accounting_policy_version VARCHAR(40),
 closed_by UUID REFERENCES users(id),
 closed_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (branch_id,month_start,revision),
 CHECK (EXTRACT(DAY FROM month_start) = 1)
);
CREATE UNIQUE INDEX uq_current_close ON monthly_closes(branch_id,month_start) WHERE is_current;
CREATE TABLE close_components (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 monthly_close_id UUID NOT NULL REFERENCES monthly_closes(id),
 component_code VARCHAR(50) NOT NULL,
 signed_amount NUMERIC(22,2) NOT NULL,
 provenance JSONB NOT NULL DEFAULT '{}'::jsonb,
 UNIQUE (monthly_close_id,component_code)
);
CREATE TABLE period_reopen_requests (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 monthly_close_id UUID NOT NULL REFERENCES monthly_closes(id),
 requested_by UUID NOT NULL REFERENCES users(id),
 reason TEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED')),
 approved_by UUID REFERENCES users(id),
 approved_at TIMESTAMPTZ,
 impact_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE period_adjustments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 reopen_request_id UUID REFERENCES period_reopen_requests(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 source_period DATE NOT NULL,
 booked_period DATE NOT NULL,
 adjustment_type VARCHAR(50) NOT NULL,
 amount NUMERIC(22,2) NOT NULL,
 source_reference TEXT NOT NULL,
 approved_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- I. INVESTORS, COMMITMENTS, ENTITLEMENTS, PAYMENTS --------------------------
CREATE TABLE investors (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 display_name VARCHAR(160) NOT NULL,
 contact_note VARCHAR(200),
 contact_file_id UUID REFERENCES evidence_files(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE investor_agreements (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 investor_id UUID NOT NULL REFERENCES investors(id),
 branch_id UUID NOT NULL REFERENCES branches(id),
 agreement_ref VARCHAR(120),
 contribution_type VARCHAR(20) NOT NULL CHECK (contribution_type IN ('CASH','ASSET','WORK','MIXED')),
 effective_from DATE NOT NULL,
 effective_to DATE,
 eligibility_terms TEXT,
 evidence_file_id UUID REFERENCES evidence_files(id),
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE TABLE capital_commitments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 agreement_id UUID NOT NULL REFERENCES investor_agreements(id),
 committed_value NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (committed_value >= 0),
 recognized_non_cash_value NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (recognized_non_cash_value >= 0),
 commitment_due_date DATE,
 approved_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE capital_contributions (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 commitment_id UUID NOT NULL REFERENCES capital_commitments(id),
 contributed_on DATE NOT NULL,
 contribution_type VARCHAR(20) NOT NULL CHECK (contribution_type IN ('CASH','ASSET','WORK')),
 recognized_value NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (recognized_value >= 0),
 account_id UUID REFERENCES financial_accounts(id),
 asset_id UUID REFERENCES equipment_assets(id),
 evidence_file_id UUID REFERENCES evidence_files(id),
 approved_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE profit_entitlement_terms (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 agreement_id UUID NOT NULL REFERENCES investor_agreements(id),
 effective_from DATE NOT NULL,
 effective_to DATE,
 profit_share_ratio NUMERIC(9,6) NOT NULL CHECK (profit_share_ratio >= 0 AND profit_share_ratio <= 1),
 eligibility_conditions TEXT,
 approved_by UUID NOT NULL REFERENCES users(id),
 evidence_file_id UUID REFERENCES evidence_files(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE TABLE capital_withdrawal_requests (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 agreement_id UUID NOT NULL REFERENCES investor_agreements(id),
 requested_on DATE NOT NULL,
 approved_effective_month DATE,
 requested_value NUMERIC(22,2) NOT NULL CHECK (requested_value >= 0),
 returned_value NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (returned_value >= 0),
 settlement_terms TEXT,
 status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
 approved_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CHECK (returned_value <= requested_value)
);
CREATE TABLE retained_profit_lots (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 source_close_id UUID NOT NULL REFERENCES monthly_closes(id),
 earning_month DATE NOT NULL,
 retained_amount NUMERIC(22,2) NOT NULL CHECK (retained_amount >= 0),
 allocated_amount NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (allocated_amount >= 0),
 policy_verified BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (source_close_id),
 CHECK (allocated_amount <= retained_amount)
);
CREATE TABLE retained_profit_entitlements (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 retained_lot_id UUID NOT NULL REFERENCES retained_profit_lots(id),
 investor_id UUID NOT NULL REFERENCES investors(id),
 entitlement_ratio_snapshot NUMERIC(9,6) NOT NULL CHECK (entitlement_ratio_snapshot >= 0 AND entitlement_ratio_snapshot <= 1),
 terms_snapshot JSONB NOT NULL,
 UNIQUE (retained_lot_id,investor_id)
);
CREATE TABLE distribution_batches (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 branch_id UUID NOT NULL REFERENCES branches(id),
 source_retained_lot_id UUID REFERENCES retained_profit_lots(id),
 proposed_on DATE NOT NULL,
 approved_total NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (approved_total >= 0),
 reserve_amount NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (reserve_amount >= 0),
 available_cash_snapshot NUMERIC(22,2),
 liability_snapshot JSONB,
 status VARCHAR(28) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','APPROVED','AWAITING_FUNDS','PARTLY_SETTLED','SETTLED','CANCELLED')),
 delegated_owner_id UUID REFERENCES users(id),
 approved_at TIMESTAMPTZ,
 policy_signoff_evidence_id UUID REFERENCES evidence_files(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE distribution_lines (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 batch_id UUID NOT NULL REFERENCES distribution_batches(id),
 investor_id UUID NOT NULL REFERENCES investors(id),
 approved_amount NUMERIC(22,2) NOT NULL CHECK (approved_amount > 0),
 ratio_snapshot NUMERIC(9,6) NOT NULL CHECK (ratio_snapshot >= 0 AND ratio_snapshot <= 1),
 status VARCHAR(24) NOT NULL DEFAULT 'APPROVED' CHECK (status IN ('APPROVED','AWAITING_FUNDS','PAYMENT_PENDING','PAID','CANCELLED')),
 UNIQUE (batch_id,investor_id)
);
CREATE TABLE distribution_payments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 distribution_line_id UUID NOT NULL UNIQUE REFERENCES distribution_lines(id),
 account_id UUID NOT NULL REFERENCES financial_accounts(id),
 paid_amount NUMERIC(22,2) NOT NULL CHECK (paid_amount > 0),
 payment_date DATE NOT NULL,
 bank_reference VARCHAR(160),
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','FAILED','REVERSED')),
 confirmed_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
 -- CONTRACT: amount must equal line.approved_amount before CONFIRMED (service transaction).
 -- Due to UNIQUE(line), retry a FAILED attempt via explicit recovery process, not a second successful payment.
);
CREATE TABLE distribution_recovery_claims (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 origin_distribution_line_id UUID NOT NULL REFERENCES distribution_lines(id),
 impact_close_id UUID NOT NULL REFERENCES monthly_closes(id),
 claim_kind VARCHAR(20) NOT NULL CHECK (claim_kind IN ('RECOVERY','FUTURE_OFFSET')),
 amount NUMERIC(22,2) NOT NULL CHECK (amount > 0),
 status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
 rationale TEXT NOT NULL,
 approved_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- J. CONSTRAINT/INDEX NOTES -------------------------------------------------
-- IMPORTANT: the following invariants require application transaction/trigger work:
--  1) branch_id/account_id/bank_account_id and all cross-branch child references
--     must belong to the same authorized branch/entity.
--  2) one active branch bank account before branch ACTIVE, and owner delegation.
--  3) effective-rate/role/entitlement intervals may not overlap incompatibly.
--  4) month close must confirm all OPEN business_days and payroll approval.
--  5) stock lot quantity and stock_balances agree after every posting.
--  6) confirmed distribution payment equals approved_amount; does not overdraw
--     available funds or bypass verified legal accounting policy gates.
--  7) `cash_reconciliations.variance_amount = observed_balance-recorded_balance`.
--  8) source ledger entry/cash transfer/debt settlement are atomically consistent.
--  9) changing one WAC cost snapshot cannot silently revalue CLOSED months.
-- 10) payroll rates and investors' ratios are effective-dated and snapshotted.
-- 11) any `evidence_files` access must be authorized per branch/owner and TTL.
-- 12) importer must reject unsafe formula or CSV injections before export.
-- 13) exact bank account numbers encrypted at rest, not plaintext.
-- 14) material sale finance source links to outbound stock once, no duplicates.
-- 15) P0 P&L is provisional until depreciation and validated policies implemented.
CREATE INDEX idx_business_days_month ON business_days(branch_id,business_date);
CREATE INDEX idx_salary_employee_dates ON salary_policies(employee_id,branch_id,effective_from,effective_to);
CREATE INDEX idx_payroll_lines_employee ON payroll_lines(employee_id,payroll_period_id);
CREATE INDEX idx_stock_lot_expiry ON stock_lots(branch_id,item_id,expires_on);
CREATE INDEX idx_transfer_pending ON stock_transfers(from_branch_id,to_branch_id,status);
CREATE INDEX idx_material_loan_due ON material_loans(branch_id,counterparty_id,status);
CREATE INDEX idx_terms_effective ON profit_entitlement_terms(agreement_id,effective_from,effective_to);
CREATE INDEX idx_distribution_status ON distribution_batches(branch_id,status,proposed_on);
CREATE INDEX idx_evidence_scope ON evidence_files(branch_id,created_at);
CREATE INDEX idx_reopen_month ON period_reopen_requests(monthly_close_id,status);
COMMIT;
