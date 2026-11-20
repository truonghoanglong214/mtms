# MTMS API & Data Contract — P0-FIN-05: Payable & Receivable Ledger & Partial Debt Settlement

**Task Reference:** `P0-FIN-05` (BR-FIN-02, TC-009)  
**Frontend Backlog Reference:** `FE-P0-13` (*Payables and receivables ledger UI*)  
**Dependencies:** `P0-FIN-02` (Cash & Bank Ledger), `P0-INV-03` (Goods Receipts & Payables), `P0-SEC-03` (Idempotency)  
**Status:** PROPOSED & READY FOR IMPLEMENTATION  
**Author:** MTMS Architect  

---

## 1. Domain Model & Business Invariants

### 1.1 Supplier Debt Settlement Invariants (`BR-FIN-02`, `TC-009`)
1. **TC-009 No Double Cost Invariant:**
   - Paying supplier debt reduces cash/bank balance and reduces outstanding payable liability.
   - **Zero additional operating expense or COGS on P&L**: Ingredient costs are booked only when goods are consumed in brewing/kitchen operations (`BR-INV-02` / `TC-020`). Settling supplier invoices does not duplicate cost.
2. **Partial & Full Debt Settlement:**
   - A payable can be settled across multiple installments ($1 \dots N$ payments).
   - Validation invariant: $\sum \text{payment\_amount} \le \text{original\_amount}$. Overpayment beyond remaining debt is strictly rejected with `400 Bad Request` (`urn:mtms:error:payment-exceeds-remaining-debt`).
   - When $\text{paid\_amount} = \text{original\_amount}$, status transitions from `OPEN` to `SETTLED`.
   - If linked to a purchase receipt (`mtms.purchase_receipts`), the receipt's `payment_status` updates synchronously:
     - `PAID` when payable status is `SETTLED`.
     - `PARTIALLY_PAID` when $0 < \text{paid\_amount} < \text{original\_amount}$.
3. **Cash Subledger Integration:**
   - Each payment creates an immutable `OUTFLOW` entry in `mtms.cash_ledger_entries`:
     - `account_id`: Selected branch cash or bank financial account.
     - `entry_type`: `OUTFLOW`.
     - `amount`: Payment amount.
     - `signed_amount`: `-amount`.
     - `category_id`: Category `DEBT_SUPPLIER` (`Chi trả nợ nhà cung cấp mua hàng` - group `DEBT_SETTLEMENT`).
     - `source_type`: `SUPPLIER_PAYMENT`.
     - `source_id`: `payable_payments.id`.
     - `source_leg`: `MAIN`.
     - `memo`: `"Thanh toán công nợ NCC: " + counterparty_label`.
     - `evidence_file_id`: Optional bank transfer receipt / payment voucher.
   - Idempotency constraint `UNIQUE (account_id, source_type, source_id, source_leg)` ensures duplicate payment requests never debit funds twice (`TC-011`).
4. **Pessimistic Concurrency Locking:**
   - Both `mtms.payables` and `mtms.financial_accounts` are locked with `SELECT ... FOR UPDATE` within the transaction to eliminate race conditions between concurrent payments or balance mutations.

---

## 2. Database Schema (Flyway V015)

```sql
-- 1. Payable Payments (Installments against mtms.payables)
CREATE TABLE mtms.payable_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payable_id UUID NOT NULL REFERENCES mtms.payables(id),
    account_id UUID NOT NULL REFERENCES mtms.financial_accounts(id),
    amount NUMERIC(20, 2) NOT NULL CHECK (amount > 0),
    paid_on DATE NOT NULL,
    payment_method VARCHAR(24) NOT NULL DEFAULT 'BANK_TRANSFER' CHECK (payment_method IN ('CASH', 'BANK_TRANSFER')),
    bank_reference VARCHAR(160),
    evidence_file_id UUID REFERENCES mtms.evidence_files(id),
    status VARCHAR(16) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED', 'REVERSED')),
    posted_by UUID NOT NULL REFERENCES mtms.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payable_payments_payable ON mtms.payable_payments (payable_id);
CREATE INDEX idx_payable_payments_account_date ON mtms.payable_payments (account_id, paid_on);

-- 2. Manual Receivables (Phải thu khác: bồi thường, thanh lý phế liệu, hoàn tiền đối tác...)
CREATE TABLE mtms.receivables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL REFERENCES mtms.business_entities(id),
    branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    counterparty_label VARCHAR(200) NOT NULL,
    source_type VARCHAR(48) NOT NULL DEFAULT 'MANUAL_RECEIVABLE',
    source_id UUID,
    incurred_date DATE NOT NULL,
    due_date DATE,
    original_amount NUMERIC(20, 2) NOT NULL CHECK (original_amount >= 0),
    received_amount NUMERIC(20, 2) NOT NULL DEFAULT 0 CHECK (received_amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'SETTLED', 'CANCELLED')),
    notes TEXT,
    created_by UUID NOT NULL REFERENCES mtms.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_receivables_received_le_original CHECK (received_amount <= original_amount)
);

CREATE INDEX idx_receivables_branch_status ON mtms.receivables (branch_id, status);

-- 3. Receivable Receipts (Thu tiền công nợ phải thu)
CREATE TABLE mtms.receivable_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receivable_id UUID NOT NULL REFERENCES mtms.receivables(id),
    account_id UUID NOT NULL REFERENCES mtms.financial_accounts(id),
    amount NUMERIC(20, 2) NOT NULL CHECK (amount > 0),
    received_on DATE NOT NULL,
    payment_method VARCHAR(24) NOT NULL DEFAULT 'BANK_TRANSFER' CHECK (payment_method IN ('CASH', 'BANK_TRANSFER')),
    bank_reference VARCHAR(160),
    evidence_file_id UUID REFERENCES mtms.evidence_files(id),
    status VARCHAR(16) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED', 'REVERSED')),
    posted_by UUID NOT NULL REFERENCES mtms.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_receivable_receipts_receivable ON mtms.receivable_receipts (receivable_id);
```

---

## 3. REST API Specifications

### 3.1 Payables Management API
- `GET /api/v1/branches/{branchId}/payables`
  - Secured by `@branchAuth.hasBranchAccess(#branchId)`.
  - Query params: `status` (`ALL`, `OPEN`, `SETTLED`), `search`, `page`, `size`.
  - Response: `200 OK` paginated list of `PayableDto` (including `originalAmount`, `paidAmount`, `remainingDebt`, `status`).

- `POST /api/v1/branches/{branchId}/payables`
  - Creates manual payable (e.g. equipment installment, facility repairs).
  - Body:
    ```json
    {
      "counterpartyLabel": "Công ty TNHH Cơ điện Lạnh ABC",
      "incurredDate": "2026-11-18",
      "dueDate": "2026-12-15",
      "originalAmount": "12000000.00",
      "notes": "Tiền bảo dưỡng và lắp đặt hệ thống máy lạnh chi nhánh"
    }
    ```
  - Response: `201 Created`

- `GET /api/v1/branches/{branchId}/payables/{payableId}`
  - Detail of a single payable with its installment payment history (`payments` array).

- `POST /api/v1/branches/{branchId}/payables/{payableId}/payments`
  - Executes partial or full debt settlement (`TC-009`).
  - Supports `Idempotency-Key` header.
  - Body:
    ```json
    {
      "accountId": "uuid-financial-account",
      "amount": "5000000.00",
      "paidOn": "2026-11-18",
      "paymentMethod": "BANK_TRANSFER",
      "bankReference": "FT2611189999",
      "evidenceFileId": "optional-uuid-evidence"
    }
    ```
  - Validation:
    - `amount > 0`
    - `amount <= (original_amount - paid_amount)`
    - `financialAccount.currentBalance >= amount` (or overdraft check)
  - Response: `201 Created` with `PayablePaymentResponseDto`.

### 3.2 Receivables Management API
- `GET /api/v1/branches/{branchId}/receivables`
  - Query params: `status`, `search`, `page`, `size`.
- `POST /api/v1/branches/{branchId}/receivables`
  - Create manual receivable.
- `POST /api/v1/branches/{branchId}/receivables/{receivableId}/receipts`
  - Record payment collection against receivable.

---

## 4. Error Codes & RFC 7807 Problem Details

| HTTP Status | Error Code (`type`) | Description |
|---|---|---|
| 400 Bad Request | `urn:mtms:error:payment-exceeds-remaining-debt` | Số tiền thanh toán vượt quá số nợ còn lại |
| 400 Bad Request | `urn:mtms:error:insufficient-account-balance` | Số dư tài khoản thanh toán không đủ |
| 400 Bad Request | `urn:mtms:error:payable-already-settled` | Khoản nợ này đã được thanh toán toàn bộ trước đó |
| 404 Not Found | `urn:mtms:error:payable-not-found` | Không tìm thấy khoản nợ phải trả |
| 404 Not Found | `urn:mtms:error:financial-account-not-found` | Không tìm thấy tài khoản tài chính để trích tiền |
| 409 Conflict | `urn:mtms:error:duplicate-ledger-posting` | Bút toán thanh toán đã được ghi nhận trước đó (TC-011) |
| 403 Forbidden | `urn:mtms:error:forbidden` | Người dùng không có quyền truy cập chi nhánh |
