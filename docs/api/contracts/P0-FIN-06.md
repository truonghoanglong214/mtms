# MTMS API & Data Contract — P0-FIN-06: Cash & Bank Account Reconciliation

**Task Reference:** `P0-FIN-06` (`BR-FIN-06`, `TC-012`)  
**Frontend Backlog Reference:** `FE-P0-14` (*Cash & bank reconciliation UI*)  
**Dependencies:** `P0-FIN-02` (Cash & Bank Ledger), `P0-SEC-03` (Idempotency)  
**Status:** PROPOSED & READY FOR IMPLEMENTATION  
**Author:** MTMS Architect  

---

## 1. Domain Model & Business Invariants

### 1.1 Business Invariants (`BR-FIN-06`, `TC-012`)
1. **Discrepancy Cannot Disappear Silently (`TC-012`):**
   - Comparing system ledger balance (`recorded_balance`) with physical cash count or bank statement balance (`observed_balance`) calculates:
     $$\text{variance\_amount} = \text{observed\_balance} - \text{recorded\_balance}$$
   - When $\text{variance\_amount} \ne 0$, the reconciliation record is created in status `OPEN`.
   - Discrepancies cannot be deleted or wiped silently. They must follow an explicit audit trail with documented disposition:
     - `OPEN`: Discrepancy recorded, awaiting review/explanation.
     - `EXPLAINED`: Documented reason / evidence attached (e.g. pending bank transfer timing difference, unrecorded receipt).
     - `ADJUSTED`: Authorized adjusting journal entry posted to the cash ledger to synchronize recorded balance with observed balance.
     - `CLOSED`: Reconciliation verified and finalized.
2. **Authorized Adjustment Requirement (`TC-012`):**
   - Adjusting a variance requires explicit authorization by Branch Owner or authorized Manager.
   - Synchronous Cash Ledger Posting:
     - If $\text{variance\_amount} > 0$ (Thừa tiền / Surplus): Creates an `INFLOW` entry in `mtms.cash_ledger_entries` under source type `MANUAL_ADJUSTMENT`, category `ADJ_SURPLUS`, increasing the account's recorded balance to match physical count.
     - If $\text{variance\_amount} < 0$ (Thiếu tiền / Shortage): Creates an `OUTFLOW` entry in `mtms.cash_ledger_entries` under source type `MANUAL_ADJUSTMENT`, category `ADJ_SHORTAGE`, decreasing the account's recorded balance to match physical count.
   - Under row lock on `financial_accounts`, the account balance is updated atomically.
   - The reconciliation record transitions to `ADJUSTED`, recording `adjustment_ledger_entry_id`, `confirmed_by`, and `confirmed_at`.
3. **Account & Date Uniqueness:**
   - A financial account can only have one reconciliation snapshot per business date (`UNIQUE (account_id, reconciliation_date)`).
4. **Branch Isolation:**
   - Reconciliations are strictly scoped by `branch_id`. Users can only query or reconcile accounts belonging to branches they have active access to.

---

## 2. Database Schema Design (Flyway `V017`)

```sql
-- P0-FIN-06 / BR-FIN-06 / TC-012 / FE-P0-14
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
```

---

## 3. REST API Endpoints

### 3.1 List Reconciliations
- **Endpoint:** `GET /api/v1/branches/{branchId}/finance/reconciliations`
- **Authorization:** `OWNER`, `MANAGER`
- **Query Parameters:**
  - `accountId` (UUID, optional)
  - `startDate` (LocalDate, optional)
  - `endDate` (LocalDate, optional)
  - `disposition` (String, optional: `OPEN`, `EXPLAINED`, `ADJUSTED`, `CLOSED`)
  - `page` (int, default `0`), `size` (int, default `20`)
- **Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "e4f8c2b1-5e72-4b2a-89a1-77890abcdef1",
      "branchId": "b1b2b3b4-1111-2222-3333-444455556666",
      "accountId": "a1a2a3a4-1111-2222-3333-444455556666",
      "accountName": "Quỹ tiền mặt quầy",
      "accountKind": "CASH",
      "currency": "VND",
      "reconciliationDate": "2026-11-18",
      "recordedBalance": 15000000.00,
      "observedBalance": 14950000.00,
      "varianceAmount": -50000.00,
      "disposition": "OPEN",
      "note": "Kiểm đếm cuối ca chiều thiếu 50k",
      "evidenceFileId": null,
      "evidenceDownloadUrl": null,
      "adjustmentLedgerEntryId": null,
      "createdById": "u1u2u3u4-1111-2222-3333-444455556666",
      "createdByName": "Quản lý ca",
      "confirmedById": null,
      "confirmedByName": null,
      "confirmedAt": null,
      "createdAt": "2026-11-18T17:30:00Z"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 3.2 Create Reconciliation Snapshot
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/reconciliations`
- **Authorization:** `OWNER`, `MANAGER`
- **Request Body:**
```json
{
  "accountId": "a1a2a3a4-1111-2222-3333-444455556666",
  "reconciliationDate": "2026-11-18",
  "observedBalance": 14950000.00,
  "note": "Kiểm đếm quỹ tiền mặt cuối ngày",
  "evidenceFileId": "e1e2e3e4-5555-6666-7777-888899990000"
}
```
- **Response `201 Created`:** Returns created `CashReconciliationDto`.
- **Validation Rules:**
  - `accountId`, `reconciliationDate`, `observedBalance` must not be null.
  - If a reconciliation already exists for `(accountId, reconciliationDate)`, returns `409 Conflict` (`urn:mtms:error:reconciliation-already-exists`).

### 3.3 Explain Discrepancy
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/reconciliations/{id}/explain`
- **Authorization:** `OWNER`, `MANAGER`
- **Request Body:**
```json
{
  "note": "Khách quẹt thẻ nhưng tiền chưa nổi về tài khoản ngân hàng lúc 23h",
  "evidenceFileId": "e1e2e3e4-5555-6666-7777-888899990000"
}
```
- **Response `200 OK`:** Updates `disposition` to `EXPLAINED`.

### 3.4 Authorized Adjustment (`TC-012`)
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/reconciliations/{id}/adjust`
- **Authorization:** `OWNER`
- **Request Body:**
```json
{
  "reason": "Chấp thuận bù thiếu quỹ tiền mặt ca tối do thối nhầm tiền lẻ"
}
```
- **Response `200 OK`:**
  - If `varianceAmount < 0`: Posts OUTFLOW entry with category `ADJ_SHORTAGE`.
  - If `varianceAmount > 0`: Posts INFLOW entry with category `ADJ_SURPLUS`.
  - Links `adjustmentLedgerEntryId`.
  - Sets `disposition = 'ADJUSTED'`, `confirmed_by = userId`, `confirmed_at = now()`.
  - Returns updated `CashReconciliationDto`.

### 3.5 Close Reconciliation
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/reconciliations/{id}/close`
- **Authorization:** `OWNER`
- **Response `200 OK`:** Sets `disposition = 'CLOSED'`.

---

## 4. Error Semantics (RFC 7807)

| Status | Error Type URI | Condition |
| :--- | :--- | :--- |
| `404 Not Found` | `urn:mtms:error:reconciliation-not-found` | Reconciliation ID does not exist in branch |
| `409 Conflict` | `urn:mtms:error:reconciliation-already-exists` | Reconciliation already exists for account and date |
| `400 Bad Request` | `urn:mtms:error:reconciliation-already-adjusted` | Attempting to adjust an already adjusted or closed reconciliation |
| `400 Bad Request` | `urn:mtms:error:invalid-reconciliation-status` | Attempting an invalid disposition transition |
| `400 Bad Request` | `urn:mtms:error:no-variance-to-adjust` | Attempting to post adjustment when variance is 0 |
