# MTMS API & Data Contract — P0-FIN-08: Opening Balances Import & Cutover Batch

**Task Reference:** `P0-FIN-08` (`BR-FIN-08`, `GAP-01`, `ADR-009`, `TC-017`)  
**Frontend Reference:** `FE-P0-16` / `BR-FIN-08` (*Opening balances capture & cutover UI*)  
**Dependencies:** `P0-FIN-02` (Cash & Bank Ledger), `P0-INV-02` (Opening Inventory), `P0-FIN-05` (Payables & Receivables), `P0-FIN-07` (Tax Obligations)  
**Status:** PROPOSED & READY FOR IMPLEMENTATION  
**Author:** MTMS Architect  

---

## 1. Domain Model & Business Invariants

### 1.1 Business Invariants (`BR-FIN-08`, `GAP-01`, `ADR-009`)
1. **End-to-End Opening Balance Ownership (`GAP-01`):**
   - Opening balance cutover establishes the starting financial and operational baseline of a branch onboarding onto MTMS on a specific cutover date (`cutover_date`).
   - No historical reimport of transactions (`ADR-009`): Onboarding captures actual physical counts and verified ending balances from the prior accounting/external system.
2. **Unified Working Capital Reconciliation (`BR-FIN-08`):**
   - The opening snapshot unifies 5 financial pillars:
     $$\text{Net Working Capital} = (\text{Total Cash} + \text{Total Bank} + \text{Opening Inventory} + \text{Opening Receivables}) - (\text{Opening Payables} + \text{Opening Tax Liabilities})$$
   - Summary snapshot is preserved as an immutable JSON document (`summary_snapshot`) for audit and historical reconciliation.
3. **Atomic Posting to Cash Ledger:**
   - When the Owner confirms and posts the batch (`POSTED`):
     - For each financial account with `opening_balance > 0`, an `INFLOW` entry is posted to `mtms.cash_ledger_entries`:
       - `source_type`: `CAPITAL_FLOW`
       - `category`: `OPENING_BALANCE` ("Số dư tiền đầu kỳ", group `CAPITAL_FLOW`)
       - `source_id`: `batch_id`
       - `source_leg`: `OPENING_BALANCE`
       - `amount`: `opening_balance`
       - `business_date`: `cutover_date`
     - Updates `financial_accounts.current_balance` atomically under pessimistic row lock.
4. **Idempotency & Immutability:**
   - Only 1 opening balance batch can be posted per branch.
   - Once `POSTED`, opening balances cannot be deleted, wiped, or re-posted.
   - Any subsequent adjustments must go through standard authorized reconciliation (`P0-FIN-06` / `TC-012`).

---

## 2. Database Schema Design (Flyway `V019`)

```sql
-- P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009
-- Forward-only migration for opening balance batches and cutover reconciliations.

-- 1. Seed System Category for Opening Balance
INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system, description) VALUES
('OPENING_BALANCE', 'Số dư tiền đầu kỳ', 'CAPITAL_FLOW', 'INFLOW', TRUE, 'Số dư tiền mặt và ngân hàng đầu kỳ khi đưa chi nhánh vào vận hành hệ thống (BR-FIN-08)');

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
```

---

## 3. REST API Endpoints

### 3.1 Get Opening Balances Status & Active Batch
- **Endpoint:** `GET /api/v1/branches/{branchId}/finance/opening-balances/status`
- **Authorization:** `OWNER`, `MANAGER`, `FINANCE`
- **Response `200 OK`:**
```json
{
  "isPosted": true,
  "cutoverDate": "2026-01-01",
  "activeBatch": {
    "id": "b1b2b3b4-1111-2222-3333-444455556666",
    "branchId": "c1c2c3c4-1111-2222-3333-444455556666",
    "cutoverDate": "2026-01-01",
    "status": "POSTED",
    "totalCashAmount": 15000000.00,
    "totalBankAmount": 85000000.00,
    "totalInventoryValuation": 42000000.00,
    "totalPayableAmount": 20000000.00,
    "totalReceivableAmount": 5000000.00,
    "totalTaxLiabilityAmount": 4000000.00,
    "netWorkingCapital": 123000000.00,
    "note": "Chốt số dư chuyển giao hệ thống ngày 01/01/2026",
    "evidenceFileId": "e1e2e3e4-5555-6666-7777-888899990000",
    "evidenceDownloadUrl": null,
    "createdById": "u1u2u3u4-...",
    "createdByName": "Quản lý cơ sở",
    "postedById": "u1u2u3u4-...",
    "postedByName": "Chủ sở hữu",
    "postedAt": "2026-01-01T08:00:00Z",
    "createdAt": "2026-01-01T07:30:00Z",
    "accountBalances": [
      {
        "id": "a1a2a3a4-...",
        "accountId": "fa1...",
        "accountName": "Quỹ tiền mặt quầy",
        "accountKind": "CASH",
        "openingBalance": 15000000.00,
        "cashLedgerEntryId": "le1..."
      },
      {
        "id": "a2a3a4a5-...",
        "accountId": "fa2...",
        "accountName": "Tài khoản Vietcombank",
        "accountKind": "BANK",
        "openingBalance": 85000000.00,
        "cashLedgerEntryId": "le2..."
      }
    ]
  }
}
```

### 3.2 Create / Save Draft Opening Balance Batch
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/opening-balances`
- **Authorization:** `OWNER`, `MANAGER`
- **Request Body:**
```json
{
  "cutoverDate": "2026-01-01",
  "note": "Biên bản chốt số dư đầu kỳ khi vận hành phần mềm",
  "evidenceFileId": "e1e2e3e4-5555-6666-7777-888899990000",
  "accountBalances": [
    {
      "accountId": "fa1...",
      "openingBalance": 15000000.00
    },
    {
      "accountId": "fa2...",
      "openingBalance": 85000000.00
    }
  ]
}
```
- **Response `201 Created`:** Returns created `OpeningBalanceBatchDto`.

### 3.3 Post / Confirm Opening Balance Batch
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/opening-balances/{id}/post`
- **Authorization:** `OWNER`
- **Response `200 OK`:** Posts entries to cash ledger, updates account balances, transitions status to `POSTED`.

---

## 4. Error Semantics (RFC 7807)

| Status | Error Type URI | Condition |
| :--- | :--- | :--- |
| `404 Not Found` | `urn:mtms:error:opening-balance-batch-not-found` | Batch ID does not exist in branch |
| `409 Conflict` | `urn:mtms:error:opening-balance-already-exists` | Opening batch already exists for branch |
| `400 Bad Request` | `urn:mtms:error:opening-balance-already-posted` | Attempting to edit or re-post an already posted batch |
| `400 Bad Request` | `urn:mtms:error:invalid-cutover-date` | Cutover date is null or invalid |
