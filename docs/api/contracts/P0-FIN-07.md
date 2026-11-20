# MTMS API & Data Contract — P0-FIN-07: Tax Manual Obligations

**Task Reference:** `P0-FIN-07` (`BR-FIN-04`, `TC-012`)  
**Frontend Reference:** `FE-P0-15` (*Tax manual obligations UI*)  
**Dependencies:** `P0-FIN-05` (Payable/Receivable Ledger), `P0-FIN-02` (Cash & Bank Ledger)  
**Status:** PROPOSED & READY FOR IMPLEMENTATION  
**Author:** MTMS Architect  

---

## 1. Domain Model & Business Invariants

### 1.1 Business Invariants (`BR-FIN-04`, `TC-012`)
1. **No Fake Automated Tax Computation:**
   - In accordance with `BR-FIN-04` and `Handbook P0-FIN-07`: MTMS does not invent fake automated tax computation or algorithmic rates. Tax declarations and obligations in Vietnam (Lệ phí môn bài, Thuế GTGT, Thuế TNDN, Thuế TNCN, v.v.) are assessed by certified accountants or tax authorities and entered manually by authorized Owners/Finance personnel with documented basis.
2. **Entity & Branch Scoping:**
   - Tax obligations are entered by legal entity (`business_entity_id`) and may optionally link to a specific branch (`branch_id`).
   - If `branch_id` is specified, the obligation is branch-specific (e.g. Thuế môn bài chi nhánh).
   - If `branch_id` is null, the obligation is entity-wide (e.g. Thuế TNDN, Thuế GTGT toàn công ty).
3. **Validation & Math Constraints:**
   - `amount_due >= 0`
   - `amount_paid >= 0` and `amount_paid <= amount_due`
   - `tax_period_end >= tax_period_start`
   - $\text{remaining\_liability} = \text{amount\_due} - \text{amount\_paid}$
   - Paying more than $\text{remaining\_liability}$ is strictly rejected with `400 Bad Request` (`urn:mtms:error:tax-payment-exceeds-remaining-liability`).
4. **Lifecycle & Status Transitions:**
   - `OPEN`: Created, no payments made yet ($\text{amount\_paid} = 0$).
   - `PARTIALLY_PAID`: $0 < \text{amount\_paid} < \text{amount\_due}$.
   - `PAID`: $\text{amount\_paid} = \text{amount\_due}$.
   - `CANCELLED`: Cancelled if entered erroneously; only permitted when $\text{amount\_paid} = 0$.
5. **Non-Duplicated Money/Stock Effects:**
   - When tax is paid from a branch cash/bank account (`financial_account_id`):
     - An `OUTFLOW` entry is posted in `mtms.cash_ledger_entries` under source type `TAX_PAYMENT`, category `TAX_PAYMENT`, reducing the account balance under pessimistic row lock.
     - **Zero effect on inventory or COGS**.
     - Atomic subledger link ensures no double posting or silent omission.

---

## 2. Database Schema Design (Flyway `V018`)

```sql
-- P0-FIN-07 / BR-FIN-04 / TC-012
-- Forward-only migration for tax manual obligations and tax payments.

-- 1. Alter cash_ledger_entries constraint to allow TAX_PAYMENT source_type
ALTER TABLE mtms.cash_ledger_entries DROP CONSTRAINT ck_cash_ledger_source_type;
ALTER TABLE mtms.cash_ledger_entries ADD CONSTRAINT ck_cash_ledger_source_type CHECK (source_type IN (
    'DAILY_REVENUE', 'SUPPLIER_PAYMENT', 'INTERNAL_TRANSFER',
    'MANUAL_EXPENSE', 'CAPITAL_FLOW', 'PAYROLL_PAYMENT', 'MANUAL_ADJUSTMENT',
    'TAX_PAYMENT'
));

-- 2. Seed System Category for Tax Payment
INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system, description) VALUES
('TAX_PAYMENT', 'Chi nộp thuế và nghĩa vụ nhà nước', 'OTHER', 'OUTFLOW', TRUE, 'Chi nộp thuế môn bài, thuế GTGT, thuế TNDN, thuế TNCN và các nghĩa vụ ngân sách nhà nước (BR-FIN-04)');

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
```

---

## 3. REST API Endpoints

### 3.1 List Tax Obligations
- **Endpoint:** `GET /api/v1/branches/{branchId}/finance/tax-obligations`
- **Authorization:** `OWNER`, `MANAGER`, `FINANCE`
- **Query Parameters:**
  - `obligationType` (String, optional: `MON_BAI`, `VAT`, `CIT`, `PIT`, `OTHER`)
  - `status` (String, optional: `OPEN`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`)
  - `startDate` (LocalDate, optional)
  - `endDate` (LocalDate, optional)
  - `page` (int, default `0`), `size` (int, default `20`)
- **Response `200 OK`:** Paginated `TaxObligationDto`.

### 3.2 Create Tax Obligation
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/tax-obligations`
- **Authorization:** `OWNER`, `FINANCE`
- **Request Body:**
```json
{
  "obligationCode": "TAX-2026-Q1-VAT",
  "obligationType": "VAT",
  "taxAuthorityName": "Chi cục Thuế Quận 1",
  "taxPeriodStart": "2026-01-01",
  "taxPeriodEnd": "2026-03-31",
  "dueDate": "2026-04-30",
  "amountDue": 12500000.00,
  "isBranchSpecific": true,
  "note": "Thuế GTGT tạm tính Q1/2026 theo tờ khai",
  "evidenceFileId": "e1e2e3e4-5555-6666-7777-888899990000"
}
```
- **Response `201 Created`:** Returns created `TaxObligationDto`.

### 3.3 Get Tax Obligation Detail
- **Endpoint:** `GET /api/v1/branches/{branchId}/finance/tax-obligations/{id}`
- **Response `200 OK`:** Returns `TaxObligationDetailDto` including `payments: TaxPaymentDto[]`.

### 3.4 Pay Tax Obligation
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay`
- **Authorization:** `OWNER`, `FINANCE`
- **Request Body:**
```json
{
  "paymentDate": "2026-04-15",
  "amount": 12500000.00,
  "paymentMethod": "BANK_TRANSFER",
  "financialAccountId": "a1a2a3a4-1111-2222-3333-444455556666",
  "receiptNumber": "UNC-20260415-099",
  "note": "Ủy nhiệm chi nộp thuế GTGT Q1 vào KBNN",
  "evidenceFileId": "e5e6e7e8-5555-6666-7777-888899990000"
}
```
- **Response `200 OK`:** Returns updated `TaxObligationDetailDto`.

### 3.5 Cancel Tax Obligation
- **Endpoint:** `POST /api/v1/branches/{branchId}/finance/tax-obligations/{id}/cancel`
- **Authorization:** `OWNER`
- **Response `200 OK`:** Sets status to `CANCELLED` (only if `amount_paid == 0`).

---

## 4. Error Semantics (RFC 7807)

| Status | Error Type URI | Condition |
| :--- | :--- | :--- |
| `404 Not Found` | `urn:mtms:error:tax-obligation-not-found` | Tax obligation ID does not exist |
| `409 Conflict` | `urn:mtms:error:tax-obligation-already-exists` | Obligation code already exists in entity |
| `400 Bad Request` | `urn:mtms:error:tax-payment-exceeds-remaining-liability` | Payment amount > remaining liability |
| `400 Bad Request` | `urn:mtms:error:tax-obligation-already-paid` | Attempting to pay an already settled tax obligation |
| `400 Bad Request` | `urn:mtms:error:tax-obligation-cancelled` | Attempting to pay or modify a cancelled obligation |
| `400 Bad Request` | `urn:mtms:error:cannot-cancel-partially-paid-tax` | Attempting to cancel an obligation with recorded payments |
| `400 Bad Request` | `urn:mtms:error:insufficient-account-balance` | Financial account balance insufficient for tax payment |
