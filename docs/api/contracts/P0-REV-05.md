# MTMS API & Data Contract — P0-REV-05: Revenue Cash Linkage

**Task Reference:** `P0-REV-05` (BR-REV-04, TC-007)  
**Dependencies:** `P0-FIN-02` (Cash & Bank Ledger), `P0-REV-03` (Revenue Capture API), `P0-SEC-03` (Idempotency)  
**Status:** PROPOSED & READY FOR IMPLEMENTATION  
**Author:** MTMS Architect  

---

## 1. Domain Model & Business Invariants

### 1.1 Revenue Cash Linkage Rules (`BR-REV-04`)
1. **Cash Component Posting (`CASH_LEG`):**
   - When a daily revenue report is `CONFIRMED`, if `cash_amount > 0`:
     - Creates an `INFLOW` entry in `mtms.cash_ledger_entries` for the branch's primary `CASH` financial account.
     - Category: `REV_CASH` (`Doanh thu bán hàng - Tiền mặt`).
     - `source_type`: `DAILY_REVENUE`.
     - `source_id`: `daily_revenue_reports.id`.
     - `source_leg`: `CASH_LEG`.
     - `amount`: `cash_amount`.
     - Memo: `"Doanh thu tiền mặt ngày YYYY-MM-DD"`.

2. **Bank Transfer Component Posting (`BANK_LEG`):**
   - When a daily revenue report is `CONFIRMED`, if `bank_transfer_amount > 0`:
     - Creates an `INFLOW` entry in `mtms.cash_ledger_entries` for the branch's active `BANK` financial account.
     - Category: `REV_BANK` (`Doanh thu bán hàng - Chuyển khoản`).
     - `source_type`: `DAILY_REVENUE`.
     - `source_id`: `daily_revenue_reports.id`.
     - `source_leg`: `BANK_LEG`.
     - `amount`: `bank_transfer_amount`.
     - Memo: `"Doanh thu chuyển khoản ngày YYYY-MM-DD"`.

3. **Delivery App NET Separation (`TC-007` Invariant):**
   - `delivery_app_net_amount` is **NEVER** posted to cash or bank accounts upon revenue report confirmation.
   - Bank cash balance remains completely unchanged by `delivery_app_net_amount`.
   - Delivery app net amount is stored only in `mtms.daily_revenue_reports` and feeds directly into managerial P&L (`BR-FIN-03`). Actual bank receipts from delivery platforms will be reconciled independently when settled (`P0-FIN-06`).

4. **Revision Adjustment & Reversal:**
   - When an existing `CONFIRMED` report is superseded by Revision $N+1$:
     - All active ledger entries linked to the superseded revision (`source_id = oldReportId`) are reversed (`status = 'REVERSED'` with offsetting reversal entries).
     - New ledger entries are posted for Revision $N+1$ (`source_id = newReportId`).
     - This maintains an immutable audit trail with exact balance adjustments.

5. **Idempotency & Concurrency (`TC-011` / `TC-006`):**
   - Unique database constraint: `UNIQUE (account_id, source_type, source_id, source_leg)`.
   - Confirming an already confirmed report is idempotent: no duplicate ledger entries or double balance increases.

---

## 2. REST API Specifications

### 2.1 Linked Ledger Entries Endpoint
- `GET /api/v1/branches/{branchId}/daily-revenue/{reportId}/ledger-entries`
  - Secured by `@PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")`.
  - Response: `200 OK`
    ```json
    [
      {
        "id": "uuid",
        "accountId": "uuid",
        "accountName": "Quỹ tiền mặt chi nhánh",
        "businessDate": "2026-11-17",
        "entryType": "INFLOW",
        "amount": "5000000.00",
        "signedAmount": "5000000.00",
        "runningBalance": "15000000.00",
        "categoryCode": "REV_CASH",
        "categoryName": "Doanh thu bán hàng - Tiền mặt",
        "sourceType": "DAILY_REVENUE",
        "sourceId": "uuid",
        "sourceLeg": "CASH_LEG",
        "status": "POSTED",
        "memo": "Doanh thu tiền mặt ngày 2026-11-17",
        "postedAt": "2026-11-17T14:30:00Z"
      },
      {
        "id": "uuid",
        "accountId": "uuid",
        "accountName": "Techcombank 1903... (Chính)",
        "businessDate": "2026-11-17",
        "entryType": "INFLOW",
        "amount": "3000000.00",
        "signedAmount": "3000000.00",
        "runningBalance": "8000000.00",
        "categoryCode": "REV_BANK",
        "categoryName": "Doanh thu bán hàng - Chuyển khoản",
        "sourceType": "DAILY_REVENUE",
        "sourceId": "uuid",
        "sourceLeg": "BANK_LEG",
        "status": "POSTED",
        "memo": "Doanh thu chuyển khoản ngày 2026-11-17",
        "postedAt": "2026-11-17T14:30:00Z"
      }
    ]
    ```

### 2.2 Confirmation Trigger
- `POST /api/v1/branches/{branchId}/daily-revenue/{reportId}/confirm`
  - Existing confirmation endpoint will now automatically and synchronously trigger the `RevenueCashLinkageService` within the same database transaction `@Transactional`.
