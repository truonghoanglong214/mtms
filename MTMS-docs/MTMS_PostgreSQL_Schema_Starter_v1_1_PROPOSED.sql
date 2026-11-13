-- ============================================================================
-- MTMS PostgreSQL Schema Starter v1.1 - PROPOSED / REVIEW_ONLY
-- Date: 2026-11-12 | Parent: CHANGE-REQUEST-TBD | Gate: G0 NOT SIGNED
-- ============================================================================
-- NOT AN EXECUTABLE MIGRATION. DO NOT RUN THIS FILE AGAINST A DATABASE.
-- This is a conceptual PostgreSQL design for static review. It is not Flyway,
-- implementation approval, G0 signoff, production DDL or evidence of testing.
-- UUID values are application-generated in this draft to avoid extension setup.
-- All names, types and constraints require approved task-level migration design.
--
-- SCOPE CONTRACT:
-- * one aggregate NET beverage-revenue amount per branch/service month;
-- * source definitions/checklists/evidence are metadata and contain no amount;
-- * no POS, settlement, revenue receivable or automatic cash/bank posting;
-- * employee master has no login; no attendance/time/rate/payroll/payment model;
-- * one salary expense per employee/service month at the designated branch;
-- * OPEN revisions retain history; CLOSED snapshots are immutable by contract;
-- * GAP-01 blocks confirmation release; GAP-02 leaves correction route undecided;
-- * P&L is always TAM TINH / PROVISIONAL; payouts remain G5-gated.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS mtms;
SET search_path TO mtms, public;

-- A. ORGANIZATION / IDENTITY -------------------------------------------------
CREATE TABLE business_entities (
  id UUID PRIMARY KEY,
  entity_code VARCHAR(32) NOT NULL UNIQUE,
  registered_name VARCHAR(200) NOT NULL,
  registration_ref VARCHAR(100),
  status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE branches (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  branch_code VARCHAR(40) NOT NULL,
  name VARCHAR(160) NOT NULL,
  time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','CLOSED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (business_entity_id, branch_code),
  UNIQUE (business_entity_id, id)
);

CREATE TABLE users (
  id UUID PRIMARY KEY,
  login_identifier VARCHAR(200) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name VARCHAR(160) NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','DISABLED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE scoped_role_grants (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  branch_id UUID,
  user_id UUID NOT NULL REFERENCES users(id),
  role_key VARCHAR(40) NOT NULL,
  action_scope VARCHAR(80) NOT NULL,
  row_scope VARCHAR(80),
  valid_from DATE NOT NULL,
  valid_to DATE,
  granted_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  CHECK (valid_to IS NULL OR valid_to >= valid_from),
  CHECK (branch_id IS NOT NULL OR role_key = 'SYSTEM_ADMIN'),
  UNIQUE (business_entity_id, branch_id, user_id, role_key, action_scope, valid_from)
);

CREATE TABLE branch_bank_accounts (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  bank_name VARCHAR(160) NOT NULL,
  account_holder_name VARCHAR(160) NOT NULL,
  account_number_ciphertext TEXT NOT NULL,
  masked_account_number VARCHAR(40) NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE UNIQUE INDEX uq_branch_active_primary_bank
  ON branch_bank_accounts(business_entity_id, branch_id)
  WHERE is_primary AND status = 'ACTIVE';

-- B. SECURITY / EVIDENCE / AUDIT / IDEMPOTENCY ------------------------------
CREATE TABLE evidence_files (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  branch_id UUID,
  storage_object_key TEXT NOT NULL UNIQUE,
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  byte_size BIGINT NOT NULL CHECK (byte_size > 0),
  checksum_sha256 CHAR(64) NOT NULL,
  privacy_class VARCHAR(32) NOT NULL
    CHECK (privacy_class IN ('ENTITY_PRIVATE','BRANCH_PRIVATE','SALARY_RESTRICTED','CAPITAL_RESTRICTED')),
  retention_until DATE,
  uploaded_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE audit_events (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  branch_id UUID,
  actor_user_id UUID NOT NULL REFERENCES users(id),
  action_name VARCHAR(90) NOT NULL,
  target_type VARCHAR(80) NOT NULL,
  target_id UUID NOT NULL,
  business_month DATE,
  action_reason TEXT,
  correlation_id UUID NOT NULL,
  idempotency_record_id UUID,
  safe_before_digest CHAR(64),
  safe_after_digest CHAR(64),
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  CHECK (business_month IS NULL OR EXTRACT(DAY FROM business_month) = 1)
);

CREATE TABLE idempotency_records (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  branch_id UUID,
  actor_user_id UUID NOT NULL REFERENCES users(id),
  operation_scope VARCHAR(100) NOT NULL,
  idempotency_key VARCHAR(160) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  response_status INTEGER,
  target_type VARCHAR(80),
  target_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ,
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, actor_user_id, operation_scope, idempotency_key)
);

ALTER TABLE audit_events
  ADD CONSTRAINT fk_audit_idempotency
  FOREIGN KEY (idempotency_record_id) REFERENCES idempotency_records(id);

CREATE TABLE import_batches (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  branch_id UUID,
  import_kind VARCHAR(48) NOT NULL,
  source_filename VARCHAR(255) NOT NULL,
  source_sha256 CHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('PREVIEW','VALIDATED','REJECTED')),
  created_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, import_kind, source_sha256)
);

CREATE TABLE import_rows (
  id UUID PRIMARY KEY,
  import_batch_id UUID NOT NULL REFERENCES import_batches(id),
  row_number INTEGER NOT NULL CHECK (row_number > 0),
  raw_data JSONB NOT NULL,
  normalized_data JSONB,
  validation_errors JSONB NOT NULL DEFAULT '[]'::jsonb,
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','VALID','INVALID')),
  UNIQUE (import_batch_id, row_number)
  -- JSONB is limited to staging input/errors, not core financial invariants.
);

CREATE TABLE opening_balance_batches (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  cutover_date DATE NOT NULL,
  import_batch_id UUID REFERENCES import_batches(id),
  status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT','VALIDATED','BR_FIN_08_BLOCKED')),
  release_blocked BOOLEAN NOT NULL DEFAULT TRUE CHECK (release_blocked),
  prepared_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, cutover_date)
  -- BR-FIN-08 ownership/workflow remains unresolved; posting is unavailable.
);

-- C. MONTHLY AGGREGATE NET REVENUE -----------------------------------------
CREATE TABLE revenue_source_types (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  source_code VARCHAR(50) NOT NULL,
  display_name VARCHAR(160) NOT NULL,
  description TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (business_entity_id, source_code),
  UNIQUE (business_entity_id, id)
);

CREATE TABLE source_net_definition_revisions (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  source_type_id UUID NOT NULL,
  revision INTEGER NOT NULL CHECK (revision > 0),
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  definition_text TEXT NOT NULL CHECK (btrim(definition_text) <> ''),
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','CONFIRMED','SUPERSEDED')),
  entered_by UUID NOT NULL REFERENCES users(id),
  confirmed_by UUID REFERENCES users(id),
  confirmed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, source_type_id)
    REFERENCES revenue_source_types(business_entity_id, id),
  UNIQUE (business_entity_id, source_type_id, revision),
  UNIQUE (business_entity_id, id),
  UNIQUE (business_entity_id, source_type_id, id),
  CHECK (status <> 'CONFIRMED' OR (confirmed_by IS NOT NULL AND confirmed_at IS NOT NULL))
);

CREATE UNIQUE INDEX uq_source_definition_current
  ON source_net_definition_revisions(business_entity_id, source_type_id)
  WHERE is_current;

CREATE TABLE source_definition_evidence (
  business_entity_id UUID NOT NULL,
  definition_revision_id UUID NOT NULL,
  evidence_file_id UUID NOT NULL,
  evidence_role VARCHAR(30) NOT NULL DEFAULT 'NET_DEFINITION',
  FOREIGN KEY (business_entity_id, definition_revision_id)
    REFERENCES source_net_definition_revisions(business_entity_id, id),
  FOREIGN KEY (business_entity_id, evidence_file_id)
    REFERENCES evidence_files(business_entity_id, id),
  PRIMARY KEY (definition_revision_id, evidence_file_id)
);

CREATE TABLE branch_revenue_sources (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  source_type_id UUID NOT NULL,
  effective_from DATE NOT NULL,
  effective_to DATE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  declared_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  FOREIGN KEY (business_entity_id, source_type_id)
    REFERENCES revenue_source_types(business_entity_id, id),
  CHECK (effective_to IS NULL OR effective_to >= effective_from),
  UNIQUE (business_entity_id, branch_id, source_type_id, effective_from),
  UNIQUE (business_entity_id, branch_id, source_type_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE expected_source_checklists (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  service_month DATE NOT NULL CHECK (EXTRACT(DAY FROM service_month) = 1),
  revision INTEGER NOT NULL CHECK (revision > 0),
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','OWNER_CONFIRMED','SUPERSEDED')),
  prepared_by UUID NOT NULL REFERENCES users(id),
  owner_confirmed_by UUID REFERENCES users(id),
  owner_confirmed_at TIMESTAMPTZ,
  freeze_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, service_month, revision),
  UNIQUE (business_entity_id, branch_id, service_month, id),
  CHECK (status <> 'OWNER_CONFIRMED' OR
    (owner_confirmed_by IS NOT NULL AND owner_confirmed_at IS NOT NULL))
);

CREATE UNIQUE INDEX uq_expected_source_checklist_current
  ON expected_source_checklists(business_entity_id, branch_id, service_month)
  WHERE is_current;

CREATE TABLE expected_source_items (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  service_month DATE NOT NULL,
  checklist_id UUID NOT NULL,
  branch_source_id UUID NOT NULL,
  source_type_id UUID NOT NULL,
  definition_revision_id UUID NOT NULL,
  consideration_status VARCHAR(24) NOT NULL
    CHECK (consideration_status IN ('PENDING','CONSIDERED','NOT_APPLICABLE')),
  not_applicable_reason TEXT,
  considered_by UUID REFERENCES users(id),
  considered_at TIMESTAMPTZ,
  FOREIGN KEY (business_entity_id, branch_id, service_month, checklist_id)
    REFERENCES expected_source_checklists(business_entity_id, branch_id, service_month, id),
  FOREIGN KEY (business_entity_id, branch_id, source_type_id, branch_source_id)
    REFERENCES branch_revenue_sources(business_entity_id, branch_id, source_type_id, id),
  FOREIGN KEY (business_entity_id, source_type_id, definition_revision_id)
    REFERENCES source_net_definition_revisions(business_entity_id, source_type_id, id),
  UNIQUE (checklist_id, branch_source_id),
  CHECK (EXTRACT(DAY FROM service_month) = 1),
  CHECK ((consideration_status = 'NOT_APPLICABLE') =
    (not_applicable_reason IS NOT NULL AND btrim(not_applicable_reason) <> '')),
  CHECK ((consideration_status = 'PENDING') OR
    (considered_by IS NOT NULL AND considered_at IS NOT NULL))
);

CREATE TABLE monthly_revenue_revisions (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  service_month DATE NOT NULL CHECK (EXTRACT(DAY FROM service_month) = 1),
  revision INTEGER NOT NULL CHECK (revision > 0),
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  expected_source_checklist_id UUID NOT NULL,
  aggregate_net_amount NUMERIC(20,2) NOT NULL CHECK (aggregate_net_amount >= 0),
  zero_reason TEXT,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','CONFIRMED','SUPERSEDED','CANCELLED')),
  revision_reason TEXT NOT NULL,
  entered_by UUID NOT NULL REFERENCES users(id),
  confirmed_by UUID REFERENCES users(id),
  confirmed_at TIMESTAMPTZ,
  policy_conflict_code VARCHAR(20) NOT NULL DEFAULT 'GAP-01',
  release_state VARCHAR(40) NOT NULL DEFAULT 'BLOCKED_POLICY_CONFLICT',
  release_blocked BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id, service_month, expected_source_checklist_id)
    REFERENCES expected_source_checklists(business_entity_id, branch_id, service_month, id),
  UNIQUE (business_entity_id, branch_id, service_month, revision),
  UNIQUE (business_entity_id, branch_id, service_month, id),
  CHECK ((aggregate_net_amount <> 0) OR (zero_reason IS NOT NULL AND btrim(zero_reason) <> '')),
  CHECK (status <> 'CONFIRMED' OR (confirmed_by IS NOT NULL AND confirmed_at IS NOT NULL)),
  CHECK (policy_conflict_code = 'GAP-01'),
  CHECK (release_state = 'BLOCKED_POLICY_CONFLICT'),
  CHECK (release_blocked),
  CHECK (status <> 'CONFIRMED' OR NOT release_blocked)
);

CREATE UNIQUE INDEX uq_monthly_revenue_current
  ON monthly_revenue_revisions(business_entity_id, branch_id, service_month)
  WHERE is_current;

CREATE TABLE monthly_revenue_evidence (
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  service_month DATE NOT NULL,
  revenue_revision_id UUID NOT NULL,
  evidence_file_id UUID NOT NULL,
  evidence_role VARCHAR(40) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id, service_month, revenue_revision_id)
    REFERENCES monthly_revenue_revisions(business_entity_id, branch_id, service_month, id),
  FOREIGN KEY (business_entity_id, branch_id, evidence_file_id)
    REFERENCES evidence_files(business_entity_id, branch_id, id),
  PRIMARY KEY (revenue_revision_id, evidence_file_id)
);

-- D. EMPLOYEE MASTER / PRIMARY BRANCH / MANUAL SALARY EXPENSE --------------
CREATE TABLE employees (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  staff_code VARCHAR(50) NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  position_name VARCHAR(100),
  hired_on DATE NOT NULL,
  left_on DATE,
  status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (left_on IS NULL OR left_on >= hired_on),
  UNIQUE (business_entity_id, staff_code),
  UNIQUE (business_entity_id, id)
);

CREATE TABLE primary_branch_designations (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  employee_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  service_month DATE NOT NULL CHECK (EXTRACT(DAY FROM service_month) = 1),
  revision INTEGER NOT NULL CHECK (revision > 0),
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','OWNER_CONFIRMED','SUPERSEDED')),
  authority_reference TEXT NOT NULL CHECK (btrim(authority_reference) <> ''),
  designation_reason TEXT NOT NULL,
  designated_by UUID NOT NULL REFERENCES users(id),
  owner_confirmed_by UUID REFERENCES users(id),
  owner_confirmed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, employee_id)
    REFERENCES employees(business_entity_id, id),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, employee_id, service_month, revision),
  UNIQUE (business_entity_id, employee_id, branch_id, service_month, id),
  CHECK (status <> 'OWNER_CONFIRMED' OR
    (owner_confirmed_by IS NOT NULL AND owner_confirmed_at IS NOT NULL))
);

CREATE UNIQUE INDEX uq_primary_designation_current
  ON primary_branch_designations(business_entity_id, employee_id, service_month)
  WHERE is_current;

CREATE TABLE expected_employee_checklists (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  service_month DATE NOT NULL CHECK (EXTRACT(DAY FROM service_month) = 1),
  revision INTEGER NOT NULL CHECK (revision > 0),
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','OWNER_CONFIRMED','SUPERSEDED')),
  prepared_by UUID NOT NULL REFERENCES users(id),
  owner_confirmed_by UUID REFERENCES users(id),
  owner_confirmed_at TIMESTAMPTZ,
  freeze_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, service_month, revision),
  UNIQUE (business_entity_id, branch_id, service_month, id),
  CHECK (status <> 'OWNER_CONFIRMED' OR
    (owner_confirmed_by IS NOT NULL AND owner_confirmed_at IS NOT NULL))
);

CREATE UNIQUE INDEX uq_expected_employee_checklist_current
  ON expected_employee_checklists(business_entity_id, branch_id, service_month)
  WHERE is_current;

CREATE TABLE expected_employee_items (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  service_month DATE NOT NULL,
  checklist_id UUID NOT NULL,
  employee_id UUID NOT NULL,
  designation_id UUID NOT NULL,
  expectation_status VARCHAR(20) NOT NULL CHECK (expectation_status IN ('EXPECTED','EXCLUDED')),
  exclusion_reason TEXT,
  FOREIGN KEY (business_entity_id, branch_id, service_month, checklist_id)
    REFERENCES expected_employee_checklists(business_entity_id, branch_id, service_month, id),
  FOREIGN KEY (business_entity_id, employee_id, branch_id, service_month, designation_id)
    REFERENCES primary_branch_designations(business_entity_id, employee_id, branch_id, service_month, id),
  UNIQUE (checklist_id, employee_id),
  CHECK (EXTRACT(DAY FROM service_month) = 1),
  CHECK ((expectation_status = 'EXCLUDED') =
    (exclusion_reason IS NOT NULL AND btrim(exclusion_reason) <> ''))
);

CREATE TABLE monthly_salary_expense_revisions (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  employee_id UUID NOT NULL,
  expense_branch_id UUID NOT NULL,
  service_month DATE NOT NULL CHECK (EXTRACT(DAY FROM service_month) = 1),
  revision INTEGER NOT NULL CHECK (revision > 0),
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  designation_id UUID NOT NULL,
  expected_employee_checklist_id UUID NOT NULL,
  expense_amount NUMERIC(20,2) NOT NULL CHECK (expense_amount >= 0),
  zero_reason TEXT,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','CONFIRMED','SUPERSEDED','CANCELLED')),
  revision_reason TEXT NOT NULL,
  entered_by UUID NOT NULL REFERENCES users(id),
  confirmed_by UUID REFERENCES users(id),
  confirmed_at TIMESTAMPTZ,
  policy_conflict_code VARCHAR(20) NOT NULL DEFAULT 'GAP-01',
  release_state VARCHAR(40) NOT NULL DEFAULT 'BLOCKED_POLICY_CONFLICT',
  release_blocked BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, employee_id, expense_branch_id, service_month, designation_id)
    REFERENCES primary_branch_designations(business_entity_id, employee_id, branch_id, service_month, id),
  FOREIGN KEY (business_entity_id, expense_branch_id, service_month, expected_employee_checklist_id)
    REFERENCES expected_employee_checklists(business_entity_id, branch_id, service_month, id),
  UNIQUE (business_entity_id, employee_id, service_month, revision),
  UNIQUE (business_entity_id, employee_id, expense_branch_id, service_month, id),
  CHECK ((expense_amount <> 0) OR (zero_reason IS NOT NULL AND btrim(zero_reason) <> '')),
  CHECK (status <> 'CONFIRMED' OR (confirmed_by IS NOT NULL AND confirmed_at IS NOT NULL)),
  CHECK (policy_conflict_code = 'GAP-01'),
  CHECK (release_state = 'BLOCKED_POLICY_CONFLICT'),
  CHECK (release_blocked),
  CHECK (status <> 'CONFIRMED' OR NOT release_blocked)
);

CREATE UNIQUE INDEX uq_monthly_salary_expense_current
  ON monthly_salary_expense_revisions(business_entity_id, employee_id, service_month)
  WHERE is_current;

CREATE TABLE salary_expense_evidence (
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  service_month DATE NOT NULL,
  employee_id UUID NOT NULL,
  salary_expense_revision_id UUID NOT NULL,
  evidence_file_id UUID NOT NULL,
  evidence_role VARCHAR(40) NOT NULL,
  FOREIGN KEY (business_entity_id, employee_id, branch_id, service_month, salary_expense_revision_id)
    REFERENCES monthly_salary_expense_revisions(business_entity_id, employee_id, expense_branch_id, service_month, id),
  FOREIGN KEY (business_entity_id, branch_id, evidence_file_id)
    REFERENCES evidence_files(business_entity_id, branch_id, id),
  PRIMARY KEY (salary_expense_revision_id, evidence_file_id)
);

-- E. FINANCE / TREASURY (NO REVENUE SETTLEMENT) -----------------------------
CREATE TABLE financial_accounts (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  bank_account_id UUID,
  account_name VARCHAR(160) NOT NULL,
  account_kind VARCHAR(16) NOT NULL CHECK (account_kind IN ('CASH','BANK')),
  currency CHAR(3) NOT NULL DEFAULT 'VND',
  status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  FOREIGN KEY (business_entity_id, branch_id, bank_account_id)
    REFERENCES branch_bank_accounts(business_entity_id, branch_id, id),
  UNIQUE (business_entity_id, branch_id, id),
  CHECK ((account_kind = 'CASH' AND bank_account_id IS NULL) OR
         (account_kind = 'BANK' AND bank_account_id IS NOT NULL))
);

CREATE TABLE cash_ledger_entries (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  account_id UUID NOT NULL,
  business_date DATE NOT NULL,
  signed_amount NUMERIC(20,2) NOT NULL CHECK (signed_amount <> 0),
  source_type VARCHAR(48) NOT NULL,
  source_id UUID NOT NULL,
  source_leg VARCHAR(24) NOT NULL,
  ledger_category VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('POSTED','REVERSED')),
  reversal_of_id UUID REFERENCES cash_ledger_entries(id),
  posted_by UUID NOT NULL REFERENCES users(id),
  posted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id, account_id)
    REFERENCES financial_accounts(business_entity_id, branch_id, id),
  UNIQUE (business_entity_id, branch_id, account_id, source_type, source_id, source_leg)
);

CREATE TABLE payables (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  counterparty_label VARCHAR(200) NOT NULL,
  source_type VARCHAR(48) NOT NULL,
  source_id UUID,
  incurred_date DATE NOT NULL,
  due_date DATE,
  original_amount NUMERIC(20,2) NOT NULL CHECK (original_amount >= 0),
  paid_amount NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
  status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','SETTLED','DISPUTED','CANCELLED')),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, id),
  CHECK (paid_amount <= original_amount)
);

CREATE TABLE expenses (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  expense_date DATE NOT NULL,
  category_code VARCHAR(60) NOT NULL,
  amount NUMERIC(20,2) NOT NULL CHECK (amount > 0),
  payment_account_id UUID,
  payable_id UUID,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','CONFIRMED','ADJUSTED','VOIDED')),
  reason TEXT,
  created_by UUID NOT NULL REFERENCES users(id),
  confirmed_by UUID REFERENCES users(id),
  confirmed_at TIMESTAMPTZ,
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  FOREIGN KEY (business_entity_id, branch_id, payment_account_id)
    REFERENCES financial_accounts(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, branch_id, payable_id)
    REFERENCES payables(business_entity_id, branch_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE payable_payments (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  payable_id UUID NOT NULL,
  account_id UUID NOT NULL,
  amount NUMERIC(20,2) NOT NULL CHECK (amount > 0),
  paid_on DATE NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('POSTED','REVERSED')),
  posted_by UUID NOT NULL REFERENCES users(id),
  FOREIGN KEY (business_entity_id, branch_id, payable_id)
    REFERENCES payables(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, branch_id, account_id)
    REFERENCES financial_accounts(business_entity_id, branch_id, id)
);

CREATE TABLE cash_reconciliations (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  account_id UUID NOT NULL,
  reconciliation_date DATE NOT NULL,
  recorded_balance NUMERIC(20,2) NOT NULL,
  observed_balance NUMERIC(20,2) NOT NULL,
  variance_amount NUMERIC(20,2) NOT NULL,
  disposition VARCHAR(20) NOT NULL CHECK (disposition IN ('OPEN','EXPLAINED','ADJUSTED','CLOSED')),
  confirmed_by UUID REFERENCES users(id),
  FOREIGN KEY (business_entity_id, branch_id, account_id)
    REFERENCES financial_accounts(business_entity_id, branch_id, id),
  UNIQUE (account_id, reconciliation_date),
  CHECK (variance_amount = observed_balance - recorded_balance)
);

CREATE TABLE entity_tax_obligations (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  obligation_type VARCHAR(70) NOT NULL,
  amount_due NUMERIC(20,2) NOT NULL CHECK (amount_due >= 0),
  amount_paid NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
  status VARCHAR(20) NOT NULL,
  CHECK (period_end >= period_start),
  CHECK (amount_paid <= amount_due),
  UNIQUE (business_entity_id, id)
);

CREATE TABLE entity_cost_allocations (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  obligation_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  allocated_amount NUMERIC(20,2) NOT NULL CHECK (allocated_amount >= 0),
  allocation_basis TEXT NOT NULL,
  approved_by UUID NOT NULL REFERENCES users(id),
  FOREIGN KEY (business_entity_id, obligation_id)
    REFERENCES entity_tax_obligations(business_entity_id, id),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (obligation_id, branch_id),
  UNIQUE (business_entity_id, branch_id, id)
);

-- F. INVENTORY / MOVING WAC -------------------------------------------------
CREATE TABLE suppliers (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  supplier_code VARCHAR(50) NOT NULL,
  name VARCHAR(200) NOT NULL,
  status VARCHAR(20) NOT NULL,
  UNIQUE (business_entity_id, supplier_code),
  UNIQUE (business_entity_id, id)
);

CREATE TABLE units (
  id UUID PRIMARY KEY,
  unit_code VARCHAR(20) NOT NULL UNIQUE,
  unit_name VARCHAR(70) NOT NULL,
  dimension VARCHAR(16) NOT NULL CHECK (dimension IN ('MASS','VOLUME','COUNT','OTHER'))
);

CREATE TABLE catalog_items (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  item_code VARCHAR(50) NOT NULL,
  name VARCHAR(160) NOT NULL,
  base_unit_id UUID NOT NULL REFERENCES units(id),
  item_kind VARCHAR(24) NOT NULL,
  lot_tracking_required BOOLEAN NOT NULL DEFAULT FALSE,
  expiration_tracking_required BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL,
  UNIQUE (business_entity_id, item_code),
  UNIQUE (business_entity_id, id),
  CHECK (NOT expiration_tracking_required OR lot_tracking_required)
);

CREATE TABLE supplier_pack_conversions (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  supplier_id UUID NOT NULL,
  item_id UUID NOT NULL,
  pack_name VARCHAR(100) NOT NULL,
  base_units_per_pack NUMERIC(18,3) NOT NULL CHECK (base_units_per_pack > 0),
  effective_from DATE NOT NULL,
  effective_to DATE,
  FOREIGN KEY (business_entity_id, supplier_id)
    REFERENCES suppliers(business_entity_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE purchase_receipts (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  supplier_id UUID,
  received_on DATE NOT NULL,
  invoice_number VARCHAR(120),
  invoice_total NUMERIC(20,2) CHECK (invoice_total >= 0),
  payable_id UUID,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','POSTED','VOIDED')),
  posted_by UUID REFERENCES users(id),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  FOREIGN KEY (business_entity_id, supplier_id)
    REFERENCES suppliers(business_entity_id, id),
  FOREIGN KEY (business_entity_id, branch_id, payable_id)
    REFERENCES payables(business_entity_id, branch_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE purchase_receipt_lines (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  receipt_id UUID NOT NULL,
  line_no INTEGER NOT NULL CHECK (line_no > 0),
  item_id UUID NOT NULL,
  received_base_quantity NUMERIC(18,3) NOT NULL CHECK (received_base_quantity > 0),
  conversion_snapshot NUMERIC(18,6) NOT NULL CHECK (conversion_snapshot > 0),
  base_unit_cost NUMERIC(20,6) NOT NULL CHECK (base_unit_cost >= 0),
  line_total NUMERIC(20,2) NOT NULL CHECK (line_total >= 0),
  lot_code VARCHAR(80),
  expires_on DATE,
  FOREIGN KEY (business_entity_id, branch_id, receipt_id)
    REFERENCES purchase_receipts(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  UNIQUE (receipt_id, line_no)
);

CREATE TABLE stock_balances (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  item_id UUID NOT NULL,
  quantity NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  inventory_value NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (inventory_value >= 0),
  avg_unit_cost NUMERIC(20,6) NOT NULL DEFAULT 0 CHECK (avg_unit_cost >= 0),
  version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, item_id)
);

CREATE TABLE stock_lots (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  item_id UUID NOT NULL,
  lot_code VARCHAR(80) NOT NULL,
  expires_on DATE,
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, item_id, lot_code),
  UNIQUE (business_entity_id, branch_id, item_id, id)
);

CREATE TABLE stock_lot_balances (
  lot_id UUID PRIMARY KEY REFERENCES stock_lots(id),
  quantity NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE TABLE stock_documents (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  document_type VARCHAR(28) NOT NULL,
  document_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','POSTED','CANCELLED')),
  reason TEXT,
  created_by UUID NOT NULL REFERENCES users(id),
  approved_by UUID REFERENCES users(id),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE stock_document_lines (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  stock_document_id UUID NOT NULL,
  line_no INTEGER NOT NULL CHECK (line_no > 0),
  item_id UUID NOT NULL,
  lot_id UUID,
  base_quantity NUMERIC(18,3) NOT NULL CHECK (base_quantity > 0),
  booked_unit_cost NUMERIC(20,6),
  booked_total NUMERIC(22,2),
  FOREIGN KEY (business_entity_id, branch_id, stock_document_id)
    REFERENCES stock_documents(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  FOREIGN KEY (business_entity_id, branch_id, item_id, lot_id)
    REFERENCES stock_lots(business_entity_id, branch_id, item_id, id),
  UNIQUE (stock_document_id, line_no)
);

CREATE TABLE stock_movements (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  item_id UUID NOT NULL,
  lot_id UUID,
  business_date DATE NOT NULL,
  movement_type VARCHAR(28) NOT NULL,
  signed_quantity NUMERIC(18,3) NOT NULL CHECK (signed_quantity <> 0),
  signed_value NUMERIC(22,2) NOT NULL,
  booked_avg_cost NUMERIC(20,6) NOT NULL CHECK (booked_avg_cost >= 0),
  source_type VARCHAR(50) NOT NULL,
  source_id UUID NOT NULL,
  source_line_no INTEGER NOT NULL,
  posting_leg VARCHAR(24) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  FOREIGN KEY (business_entity_id, branch_id, item_id, lot_id)
    REFERENCES stock_lots(business_entity_id, branch_id, item_id, id),
  UNIQUE (business_entity_id, branch_id, source_type, source_id, source_line_no, posting_leg),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE stock_counts (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  counted_on DATE NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','POSTED','REJECTED')),
  reason TEXT NOT NULL,
  approved_by UUID REFERENCES users(id),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE stock_count_lines (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  stock_count_id UUID NOT NULL,
  item_id UUID NOT NULL,
  expected_qty NUMERIC(18,3) NOT NULL,
  counted_qty NUMERIC(18,3) NOT NULL CHECK (counted_qty >= 0),
  variance_value NUMERIC(22,2),
  reason TEXT NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id, stock_count_id)
    REFERENCES stock_counts(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  UNIQUE (stock_count_id, item_id)
);

-- G. TRANSFERS / EXTERNAL MATERIAL LOANS -----------------------------------
CREATE TABLE branch_counterparties (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  counterparty_name VARCHAR(160) NOT NULL,
  partner_type VARCHAR(32) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE stock_transfers (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL REFERENCES business_entities(id),
  from_branch_id UUID NOT NULL,
  to_branch_id UUID NOT NULL,
  requested_on DATE NOT NULL,
  status VARCHAR(24) NOT NULL,
  requested_by UUID NOT NULL REFERENCES users(id),
  FOREIGN KEY (business_entity_id, from_branch_id)
    REFERENCES branches(business_entity_id, id),
  FOREIGN KEY (business_entity_id, to_branch_id)
    REFERENCES branches(business_entity_id, id),
  CHECK (from_branch_id <> to_branch_id),
  UNIQUE (business_entity_id, id)
);

CREATE TABLE stock_transfer_lines (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  transfer_id UUID NOT NULL,
  item_id UUID NOT NULL,
  dispatched_base_qty NUMERIC(18,3) NOT NULL CHECK (dispatched_base_qty > 0),
  received_base_qty NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (received_base_qty >= 0),
  resolved_difference_qty NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (resolved_difference_qty >= 0),
  sender_unit_cost NUMERIC(20,6),
  FOREIGN KEY (business_entity_id, transfer_id)
    REFERENCES stock_transfers(business_entity_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  UNIQUE (transfer_id, item_id),
  CHECK (received_base_qty + resolved_difference_qty <= dispatched_base_qty)
);

CREATE TABLE material_loans (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  counterparty_id UUID NOT NULL,
  direction VARCHAR(12) NOT NULL CHECK (direction IN ('BORROW_IN','LEND_OUT')),
  loan_date DATE NOT NULL,
  status VARCHAR(24) NOT NULL,
  created_by UUID NOT NULL REFERENCES users(id),
  FOREIGN KEY (business_entity_id, branch_id, counterparty_id)
    REFERENCES branch_counterparties(business_entity_id, branch_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE material_loan_lines (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  loan_id UUID NOT NULL,
  item_id UUID NOT NULL,
  quantity_due NUMERIC(18,3) NOT NULL CHECK (quantity_due > 0),
  quantity_returned NUMERIC(18,3) NOT NULL DEFAULT 0 CHECK (quantity_returned >= 0),
  initial_unit_cost NUMERIC(20,6) NOT NULL CHECK (initial_unit_cost >= 0),
  FOREIGN KEY (business_entity_id, branch_id, loan_id)
    REFERENCES material_loans(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  UNIQUE (loan_id, item_id),
  CHECK (quantity_returned <= quantity_due)
);

-- H. ASSETS -----------------------------------------------------------------
CREATE TABLE equipment_assets (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  asset_code VARCHAR(60) NOT NULL,
  name VARCHAR(160) NOT NULL,
  purchased_on DATE,
  acquisition_cost NUMERIC(20,2) NOT NULL DEFAULT 0 CHECK (acquisition_cost >= 0),
  status VARCHAR(20) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, asset_code),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE recipes (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  recipe_name VARCHAR(160) NOT NULL,
  version INTEGER NOT NULL CHECK (version > 0),
  status VARCHAR(20) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, recipe_name, version),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE recipe_lines (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  recipe_id UUID NOT NULL,
  item_id UUID NOT NULL,
  base_quantity NUMERIC(18,3) NOT NULL CHECK (base_quantity > 0),
  FOREIGN KEY (business_entity_id, branch_id, recipe_id)
    REFERENCES recipes(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, item_id)
    REFERENCES catalog_items(business_entity_id, id),
  UNIQUE (recipe_id, item_id)
  -- Theoretical only. Never posts stock from beverage sales.
);

-- I. PERIOD CONTROL / IMMUTABLE CLOSE / CORRECTION REFERENCES --------------
CREATE TABLE branch_periods (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL CHECK (EXTRACT(DAY FROM month_start) = 1),
  status VARCHAR(24) NOT NULL CHECK (status IN ('OPEN','CLOSING','CLOSED','REOPEN_REQUESTED','REOPENED')),
  version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  UNIQUE (business_entity_id, branch_id, month_start),
  UNIQUE (business_entity_id, branch_id, month_start, id)
);

CREATE TABLE monthly_close_revisions (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL,
  branch_period_id UUID NOT NULL,
  revision INTEGER NOT NULL CHECK (revision > 0),
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  status VARCHAR(20) NOT NULL CHECK (status IN ('CLOSING','CLOSED','SUPERSEDED')),
  managerial_result NUMERIC(22,2) NOT NULL,
  is_provisional BOOLEAN NOT NULL DEFAULT TRUE,
  display_label VARCHAR(40) NOT NULL DEFAULT 'TẠM TÍNH / PROVISIONAL',
  closed_by UUID NOT NULL REFERENCES users(id),
  closed_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id, month_start, branch_period_id)
    REFERENCES branch_periods(business_entity_id, branch_id, month_start, id),
  UNIQUE (business_entity_id, branch_id, month_start, revision),
  UNIQUE (business_entity_id, branch_id, month_start, id),
  CHECK (EXTRACT(DAY FROM month_start) = 1),
  CHECK (is_provisional),
  CHECK (display_label = 'TẠM TÍNH / PROVISIONAL')
);

CREATE UNIQUE INDEX uq_monthly_close_current
  ON monthly_close_revisions(business_entity_id, branch_id, month_start)
  WHERE is_current;

CREATE TABLE close_snapshots (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL,
  close_revision_id UUID NOT NULL UNIQUE,
  captured_by UUID NOT NULL REFERENCES users(id),
  captured_at TIMESTAMPTZ NOT NULL,
  content_digest CHAR(64) NOT NULL UNIQUE,
  FOREIGN KEY (business_entity_id, branch_id, month_start, close_revision_id)
    REFERENCES monthly_close_revisions(business_entity_id, branch_id, month_start, id),
  UNIQUE (business_entity_id, branch_id, month_start, id),
  CHECK (EXTRACT(DAY FROM month_start) = 1)
  -- Immutable after insert by service contract and future DB privilege policy.
);

CREATE TABLE close_components (
  id UUID PRIMARY KEY,
  close_snapshot_id UUID NOT NULL REFERENCES close_snapshots(id),
  component_code VARCHAR(50) NOT NULL,
  signed_amount NUMERIC(22,2) NOT NULL,
  UNIQUE (close_snapshot_id, component_code)
);

CREATE TABLE close_revenue_inputs (
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL,
  close_snapshot_id UUID PRIMARY KEY,
  revenue_revision_id UUID NOT NULL UNIQUE,
  amount_snapshot NUMERIC(20,2) NOT NULL CHECK (amount_snapshot >= 0),
  FOREIGN KEY (business_entity_id, branch_id, month_start, close_snapshot_id)
    REFERENCES close_snapshots(business_entity_id, branch_id, month_start, id),
  FOREIGN KEY (business_entity_id, branch_id, month_start, revenue_revision_id)
    REFERENCES monthly_revenue_revisions(business_entity_id, branch_id, service_month, id)
);

CREATE TABLE close_salary_inputs (
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL,
  close_snapshot_id UUID NOT NULL,
  salary_expense_revision_id UUID NOT NULL,
  employee_id UUID NOT NULL,
  amount_snapshot NUMERIC(20,2) NOT NULL CHECK (amount_snapshot >= 0),
  FOREIGN KEY (business_entity_id, branch_id, month_start, close_snapshot_id)
    REFERENCES close_snapshots(business_entity_id, branch_id, month_start, id),
  FOREIGN KEY (business_entity_id, employee_id, branch_id, month_start, salary_expense_revision_id)
    REFERENCES monthly_salary_expense_revisions(business_entity_id, employee_id, expense_branch_id, service_month, id),
  PRIMARY KEY (close_snapshot_id, salary_expense_revision_id),
  UNIQUE (close_snapshot_id, employee_id)
);

CREATE TABLE close_expense_inputs (
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL,
  close_snapshot_id UUID NOT NULL,
  expense_id UUID NOT NULL,
  signed_amount_snapshot NUMERIC(22,2) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id, month_start, close_snapshot_id)
    REFERENCES close_snapshots(business_entity_id, branch_id, month_start, id),
  FOREIGN KEY (business_entity_id, branch_id, expense_id)
    REFERENCES expenses(business_entity_id, branch_id, id),
  PRIMARY KEY (close_snapshot_id, expense_id)
);

CREATE TABLE close_inventory_inputs (
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL,
  close_snapshot_id UUID NOT NULL,
  stock_movement_id UUID NOT NULL,
  signed_amount_snapshot NUMERIC(22,2) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id, month_start, close_snapshot_id)
    REFERENCES close_snapshots(business_entity_id, branch_id, month_start, id),
  FOREIGN KEY (business_entity_id, branch_id, stock_movement_id)
    REFERENCES stock_movements(business_entity_id, branch_id, id),
  PRIMARY KEY (close_snapshot_id, stock_movement_id)
);

CREATE TABLE close_tax_allocation_inputs (
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL,
  close_snapshot_id UUID NOT NULL,
  entity_cost_allocation_id UUID NOT NULL,
  signed_amount_snapshot NUMERIC(22,2) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id, month_start, close_snapshot_id)
    REFERENCES close_snapshots(business_entity_id, branch_id, month_start, id),
  FOREIGN KEY (business_entity_id, branch_id, entity_cost_allocation_id)
    REFERENCES entity_cost_allocations(business_entity_id, branch_id, id),
  PRIMARY KEY (close_snapshot_id, entity_cost_allocation_id)
);

CREATE TABLE correction_cases (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  source_month DATE NOT NULL,
  origin_close_revision_id UUID NOT NULL,
  revenue_revision_id UUID,
  employee_id UUID,
  salary_expense_revision_id UUID,
  expense_id UUID,
  stock_movement_id UUID,
  requested_reason TEXT NOT NULL,
  selected_route VARCHAR(32) CHECK (selected_route IN ('REOPEN','CURRENT_PERIOD_ADJUSTMENT')),
  policy_state VARCHAR(30) NOT NULL DEFAULT 'POLICY_VERIFY',
  status VARCHAR(24) NOT NULL CHECK (status IN ('OPEN','POLICY_BLOCKED','APPROVED','COMPLETED','REJECTED')),
  requested_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  FOREIGN KEY (business_entity_id, branch_id, source_month, origin_close_revision_id)
    REFERENCES monthly_close_revisions(business_entity_id, branch_id, month_start, id),
  FOREIGN KEY (business_entity_id, branch_id, source_month, revenue_revision_id)
    REFERENCES monthly_revenue_revisions(business_entity_id, branch_id, service_month, id),
  FOREIGN KEY (business_entity_id, employee_id, branch_id, source_month, salary_expense_revision_id)
    REFERENCES monthly_salary_expense_revisions(business_entity_id, employee_id, expense_branch_id, service_month, id),
  FOREIGN KEY (business_entity_id, branch_id, expense_id)
    REFERENCES expenses(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, branch_id, stock_movement_id)
    REFERENCES stock_movements(business_entity_id, branch_id, id),
  UNIQUE (business_entity_id, branch_id, source_month, id),
  CHECK (EXTRACT(DAY FROM source_month) = 1),
  CHECK (num_nonnulls(revenue_revision_id, salary_expense_revision_id,
    expense_id, stock_movement_id) = 1),
  CHECK ((salary_expense_revision_id IS NULL) = (employee_id IS NULL)),
  CHECK (policy_state = 'POLICY_VERIFY'),
  CHECK (status <> 'APPROVED' AND status <> 'COMPLETED')
  -- GAP-02: deliberately cannot become approved/completed in this proposal.
);

CREATE TABLE reopen_requests (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  source_month DATE NOT NULL,
  correction_case_id UUID NOT NULL UNIQUE,
  origin_close_revision_id UUID NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','REJECTED','POLICY_BLOCKED')),
  approved_by UUID,
  approved_at TIMESTAMPTZ,
  resulting_close_revision_id UUID REFERENCES monthly_close_revisions(id),
  FOREIGN KEY (business_entity_id, branch_id, source_month, correction_case_id)
    REFERENCES correction_cases(business_entity_id, branch_id, source_month, id),
  FOREIGN KEY (business_entity_id, branch_id, source_month, origin_close_revision_id)
    REFERENCES monthly_close_revisions(business_entity_id, branch_id, month_start, id),
  CHECK (approved_by IS NULL AND approved_at IS NULL AND resulting_close_revision_id IS NULL)
  -- GAP-02 blocks approval until a later authorized design supersedes this guard.
);

CREATE TABLE current_period_adjustments (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  source_month DATE NOT NULL,
  booking_month DATE NOT NULL,
  correction_case_id UUID NOT NULL UNIQUE,
  origin_close_revision_id UUID NOT NULL,
  booking_period_id UUID NOT NULL,
  signed_amount NUMERIC(22,2) NOT NULL CHECK (signed_amount <> 0),
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','POLICY_BLOCKED','REJECTED')),
  approved_by UUID,
  approved_at TIMESTAMPTZ,
  FOREIGN KEY (business_entity_id, branch_id, source_month, correction_case_id)
    REFERENCES correction_cases(business_entity_id, branch_id, source_month, id),
  FOREIGN KEY (business_entity_id, branch_id, source_month, origin_close_revision_id)
    REFERENCES monthly_close_revisions(business_entity_id, branch_id, month_start, id),
  FOREIGN KEY (business_entity_id, branch_id, booking_month, booking_period_id)
    REFERENCES branch_periods(business_entity_id, branch_id, month_start, id),
  UNIQUE (business_entity_id, branch_id, booking_month, id),
  CHECK (EXTRACT(DAY FROM source_month) = 1),
  CHECK (EXTRACT(DAY FROM booking_month) = 1),
  CHECK (approved_by IS NULL AND approved_at IS NULL)
  -- GAP-02 blocks approval and assigns no error category or sign treatment.
);

CREATE TABLE close_correction_inputs (
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  month_start DATE NOT NULL,
  close_snapshot_id UUID NOT NULL,
  current_period_adjustment_id UUID NOT NULL,
  signed_amount_snapshot NUMERIC(22,2) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id, month_start, close_snapshot_id)
    REFERENCES close_snapshots(business_entity_id, branch_id, month_start, id),
  FOREIGN KEY (business_entity_id, branch_id, month_start, current_period_adjustment_id)
    REFERENCES current_period_adjustments(business_entity_id, branch_id, booking_month, id),
  PRIMARY KEY (close_snapshot_id, current_period_adjustment_id)
);

-- J. CAPITAL / INVESTOR TERMS / G5-GATED DISTRIBUTION -----------------------
CREATE TABLE investors (
  id UUID PRIMARY KEY,
  display_name VARCHAR(160) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE investor_agreements (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  investor_id UUID NOT NULL REFERENCES investors(id),
  agreement_ref VARCHAR(120),
  effective_from DATE NOT NULL,
  effective_to DATE,
  status VARCHAR(20) NOT NULL,
  FOREIGN KEY (business_entity_id, branch_id)
    REFERENCES branches(business_entity_id, id),
  CHECK (effective_to IS NULL OR effective_to >= effective_from),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE capital_commitments (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  agreement_id UUID NOT NULL,
  committed_value NUMERIC(22,2) NOT NULL CHECK (committed_value >= 0),
  recognized_non_cash_value NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (recognized_non_cash_value >= 0),
  FOREIGN KEY (business_entity_id, branch_id, agreement_id)
    REFERENCES investor_agreements(business_entity_id, branch_id, id),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE capital_contributions (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  commitment_id UUID NOT NULL,
  contributed_on DATE NOT NULL,
  contribution_type VARCHAR(20) NOT NULL CHECK (contribution_type IN ('CASH','ASSET','WORK')),
  recognized_value NUMERIC(22,2) NOT NULL CHECK (recognized_value >= 0),
  account_id UUID,
  asset_id UUID,
  FOREIGN KEY (business_entity_id, branch_id, commitment_id)
    REFERENCES capital_commitments(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, branch_id, account_id)
    REFERENCES financial_accounts(business_entity_id, branch_id, id),
  FOREIGN KEY (business_entity_id, branch_id, asset_id)
    REFERENCES equipment_assets(business_entity_id, branch_id, id),
  CHECK ((contribution_type = 'CASH' AND account_id IS NOT NULL AND asset_id IS NULL) OR
         (contribution_type = 'ASSET' AND account_id IS NULL AND asset_id IS NOT NULL) OR
         (contribution_type = 'WORK' AND account_id IS NULL AND asset_id IS NULL))
);

CREATE TABLE profit_entitlement_terms (
  id UUID PRIMARY KEY,
  agreement_id UUID NOT NULL REFERENCES investor_agreements(id),
  effective_from DATE NOT NULL,
  effective_to DATE,
  profit_share_ratio NUMERIC(9,6) NOT NULL CHECK (profit_share_ratio BETWEEN 0 AND 1),
  approved_by UUID NOT NULL REFERENCES users(id),
  CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE retained_profit_lots (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  source_close_revision_id UUID NOT NULL UNIQUE,
  earning_month DATE NOT NULL CHECK (EXTRACT(DAY FROM earning_month) = 1),
  retained_amount NUMERIC(22,2) NOT NULL CHECK (retained_amount >= 0),
  allocated_amount NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (allocated_amount >= 0),
  g5_policy_verified BOOLEAN NOT NULL DEFAULT FALSE,
  FOREIGN KEY (business_entity_id, branch_id, earning_month, source_close_revision_id)
    REFERENCES monthly_close_revisions(business_entity_id, branch_id, month_start, id),
  CHECK (allocated_amount <= retained_amount),
  UNIQUE (business_entity_id, branch_id, id)
);

CREATE TABLE retained_profit_entitlements (
  retained_lot_id UUID NOT NULL REFERENCES retained_profit_lots(id),
  investor_id UUID NOT NULL REFERENCES investors(id),
  entitlement_ratio_snapshot NUMERIC(9,6) NOT NULL CHECK (entitlement_ratio_snapshot BETWEEN 0 AND 1),
  terms_digest CHAR(64) NOT NULL,
  PRIMARY KEY (retained_lot_id, investor_id)
);

CREATE TABLE distribution_batches (
  id UUID PRIMARY KEY,
  business_entity_id UUID NOT NULL,
  branch_id UUID NOT NULL,
  source_retained_lot_id UUID NOT NULL,
  proposed_on DATE NOT NULL,
  approved_total NUMERIC(22,2) NOT NULL DEFAULT 0 CHECK (approved_total >= 0),
  status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT','G5_BLOCKED','CANCELLED')),
  g5_verified_at TIMESTAMPTZ,
  FOREIGN KEY (business_entity_id, branch_id, source_retained_lot_id)
    REFERENCES retained_profit_lots(business_entity_id, branch_id, id),
  CHECK (g5_verified_at IS NULL)
  -- This proposal cannot enable approval/payment before G5.
);

CREATE TABLE distribution_lines (
  id UUID PRIMARY KEY,
  batch_id UUID NOT NULL REFERENCES distribution_batches(id),
  investor_id UUID NOT NULL REFERENCES investors(id),
  approved_amount NUMERIC(22,2) NOT NULL CHECK (approved_amount > 0),
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','G5_BLOCKED','CANCELLED')),
  UNIQUE (batch_id, investor_id)
);

CREATE TABLE distribution_payment_attempts (
  id UUID PRIMARY KEY,
  distribution_line_id UUID NOT NULL REFERENCES distribution_lines(id),
  attempt_no INTEGER NOT NULL CHECK (attempt_no > 0),
  attempted_amount NUMERIC(22,2) NOT NULL CHECK (attempted_amount > 0),
  status VARCHAR(20) NOT NULL CHECK (status IN ('G5_BLOCKED','FAILED','CANCELLED')),
  external_reference VARCHAR(160),
  UNIQUE (distribution_line_id, attempt_no),
  UNIQUE (external_reference)
  -- No successful state exists until a separately approved G5 contract.
);

-- K. SUPPORTING INDEXES -----------------------------------------------------
CREATE INDEX idx_role_grant_lookup
  ON scoped_role_grants(user_id, business_entity_id, branch_id, valid_from, valid_to);
CREATE INDEX idx_evidence_scope
  ON evidence_files(business_entity_id, branch_id, privacy_class, created_at);
CREATE INDEX idx_audit_target
  ON audit_events(business_entity_id, branch_id, target_type, target_id, occurred_at);
CREATE INDEX idx_source_checklist_month
  ON expected_source_checklists(business_entity_id, branch_id, service_month, status);
CREATE INDEX idx_employee_checklist_month
  ON expected_employee_checklists(business_entity_id, branch_id, service_month, status);
CREATE INDEX idx_salary_branch_month
  ON monthly_salary_expense_revisions(business_entity_id, expense_branch_id, service_month, status);
CREATE INDEX idx_cash_account_date
  ON cash_ledger_entries(business_entity_id, branch_id, account_id, business_date);
CREATE INDEX idx_expense_branch_date
  ON expenses(business_entity_id, branch_id, expense_date, status);
CREATE INDEX idx_stock_movement_branch_date
  ON stock_movements(business_entity_id, branch_id, item_id, business_date);
CREATE INDEX idx_stock_lot_expiry
  ON stock_lots(business_entity_id, branch_id, item_id, expires_on);
CREATE INDEX idx_close_month_status
  ON monthly_close_revisions(business_entity_id, branch_id, month_start, status);
CREATE INDEX idx_correction_origin
  ON correction_cases(origin_close_revision_id, status, policy_state);

-- L. INVARIANTS REQUIRING TRANSACTION/SERVICE ENFORCEMENT -------------------
--  1) GAP-01: confirmation/release remains blocked until explicit supersession
--     of the no-self-approval baseline. entered_by/confirmed_by are neutral.
--  2) GAP-02: route selection, error classification, sign, target period and
--     accounting impact require Owner + qualified Finance policy approval.
--  3) Source-definition content, checklist preparer/freeze authority and
--     primary-branch designation authority remain GAP-03/GAP-05/GAP-06.
--  4) A close transaction must verify every expected source/employee item,
--     current confirmed revenue/salary revision and unresolved correction.
--  5) Current-row supersession plus revision insert must be atomic and locked.
--  6) CLOSED snapshot tables must be INSERT-only through application and DB
--     privileges; the conceptual starter does not grant/revoke privileges.
--  7) Evidence download requires fresh entity/branch/action/row authorization;
--     generic evidence metadata must not leak existence, count or object key.
--  8) Idempotency records complement, not replace, business/source uniqueness.
--  9) Stock balance/lot/movement and WAC update in one locked transaction;
--     expired lots cannot be consumed and available stock cannot be negative.
-- 10) Cross-module cash/debt/stock postings use published application contracts
--     and one transaction; monthly revenue/salary never emit money postings.
-- 11) P&L display/export always includes TAM TINH / PROVISIONAL.
-- 12) Distribution remains unavailable until G5 and BR-CAP-10 verification.

-- END OF CONCEPTUAL STARTER. DB EXECUTION: NOT RUN.
