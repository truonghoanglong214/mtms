# MTMS — ERD & System Architecture (Mermaid)
> Version **1.0**, 07/11/2026 | Architecture proposal based on approved business baseline; **not** proof of implemented software.
> **In scope:** internal beverage shops, daily revenue aggregates (not POS), per-branch P&L/cash, payroll, manual stock, capital and profit sharing. **Do not create a point-of-sale subsystem.**
> **Design policy:** `APPROVED` = user business decision; `PROPOSED` = engineering assumption; `POLICY_VERIFY` = must be checked with responsible accounting, legal or labor specialist prior to financial go-live.

## 1. Context and trust boundaries
```mermaid
flowchart TB
    A[System Admin] --> FE[React TypeScript / mobile-first web]
    O[Branch Owner] --> FE
    M[Manager] --> FE
    E[Employee] --> FE
    I[Investor: permitted reports] --> FE
    FE --> API[Spring Boot API / modular monolith]
    API --> DB[(PostgreSQL / Flyway)]
    API --> OBJ[(Private object storage)]
    API --> AUD[(Immutable audit/outbox)]
    API -. Optional advisory calls .-> AI[Python FastAPI - P2]
    P[Outside partner: no account] -. Represented as counterparty only .-> API
```
**Security boundary:** The API authenticates every action and verifies both business-entity/branch membership and per-report permissions. React filtering is convenience, not authorization. File download uses per-request server checks and short-lived access tokens; no public evidence buckets.

## 2. Bounded contexts / dependency direction
```mermaid
flowchart LR
    ORG[organization + IAM] --> REV[revenue]
    ORG --> FIN[finance + treasury]
    ORG --> HR[workforce + payroll]
    ORG --> INV[inventory + cost]
    ORG --> CAP[capital + investor terms]
    REV --> CLS[period closing]
    FIN --> CLS
    HR --> CLS
    INV --> CLS
    CLS --> CAP
    INV --> MOV[transfer + loans]
    MOV --> FIN
    FIN --> CAP
    SEC[audit + evidence + idempotency] -.cross-cutting.-> CLS
```
**Implementation rule:** cross-module writes only through published application contracts in one database transaction when possible; no random repository reads into another module’s private tables. Reports read via versioned read models or published projections. `closing` creates snapshots rather than overwriting source financial entries.

## 3. Organization and access ERD
```mermaid
erDiagram
  business_entities ||--o{ branches : owns
  business_entities ||--o{ entity_tax_obligations : owes
  users ||--o{ scoped_role_grants : receives
  branches ||--o{ scoped_role_grants : scopes
  branches ||--o{ branch_bank_accounts : has
  branches ||--o{ business_days : operates
  branches ||--o{ delegation_records : delegates
  users ||--o{ delegation_records : delegated_user
  branches ||--o{ branch_counterparties : works_with
  business_entities { uuid id PK string registered_name string registration_ref }
  branches { uuid id PK uuid business_entity_id FK string branch_code string timezone }
  scoped_role_grants { uuid id PK uuid user_id FK uuid branch_id FK string role_key string permission_group }
  delegation_records { uuid id PK uuid branch_id FK uuid user_id FK date effective_from date effective_to }
```
A global Admin grant can have `branch_id=NULL` only for predefined admin scopes. Owner approval is valid only with a delegation effective for the transaction’s approval time; legal authority must be backed by separate agreement and evidence.

## 4. Revenue, cash, debt and closing ERD
```mermaid
erDiagram
  branches ||--o{ daily_revenue_reports : reports
  branches ||--o{ business_days : dates
  branches ||--o{ financial_accounts : accounts
  financial_accounts ||--o{ cash_ledger_entries : holds
  branches ||--o{ expenses : incurs
  branches ||--o{ payables : owes
  payables ||--o{ payable_payments : settles
  branches ||--o{ cash_reconciliations : reconciles
  branches ||--o{ monthly_closes : closes
  monthly_closes ||--o{ period_reopen_requests : reopens
  monthly_closes ||--o{ close_components : snapshots
  business_entities ||--o{ entity_tax_obligations : owns
  entity_tax_obligations ||--o{ entity_cost_allocations : allocated
  daily_revenue_reports { uuid id PK uuid branch_id FK date business_date decimal cash_amount decimal bank_amount decimal delivery_net string status }
  financial_accounts { uuid id PK uuid branch_id FK string kind string name }
  cash_ledger_entries { uuid id PK uuid account_id FK decimal signed_amount string source_type uuid source_id }
  monthly_closes { uuid id PK uuid branch_id FK date month_start string status int revision }
```
**Revenue semantics:** Confirmed cash/transfer figures can create sourced ledger postings. `delivery_net` counts in approved managerial reporting for sale day but **must not automatically increase bank balance**. A later actual bank deposit must be recorded as an actual settlement/refined financial event with reconciliation, not a second retail sale.

## 5. Workforce and payroll ERD
```mermaid
erDiagram
  branches ||--o{ employee_assignments : employs
  employees ||--o{ employee_assignments : has
  employees ||--o{ salary_policies : agrees
  employees ||--o{ timesheet_entries : records
  employees ||--o{ attendance_events : checks
  branches ||--o{ work_shifts : defines
  work_shifts ||--o{ shift_assignments : assigns
  employees ||--o{ leave_requests : requests
  branches ||--o{ payroll_periods : pays
  payroll_periods ||--o{ payroll_lines : includes
  employees ||--o{ payroll_lines : earns
  payroll_lines ||--o{ payroll_payments : paid_by
  payroll_lines ||--o{ payroll_adjustments : adjusts
  salary_policies { uuid id PK uuid employee_id FK date effective_from decimal rate string rate_type }
  timesheet_entries { uuid id PK uuid employee_id FK date work_date decimal approved_hours }
  payroll_periods { uuid id PK uuid branch_id FK date month_start string status }
```
Manager-approved *payable hours* are separate from GPS time. The hourly rate effective on the work date applies. Monthly wage proration uses configured standard/payable days. The full month of a transferred employee belongs to the primary branch effective at **month end**. Overnight shifts are excluded from current operations; the 22:00–22:30 period still needs legally checked night-work treatment.

## 6. Inventory, lots, moving WAC and movements ERD
```mermaid
erDiagram
  catalog_items ||--o{ supplier_pack_conversions : packaged_as
  suppliers ||--o{ supplier_pack_conversions : offers
  branches ||--o{ purchase_receipts : receives
  purchase_receipts ||--o{ purchase_receipt_lines : has
  catalog_items ||--o{ purchase_receipt_lines : item
  branches ||--o{ stock_balances : values
  catalog_items ||--o{ stock_balances : valued_at
  branches ||--o{ stock_lots : tracks
  stock_lots ||--o{ stock_lot_balances : has
  stock_documents ||--o{ stock_document_lines : contains
  stock_documents ||--o{ stock_movements : posts
  branches ||--o{ stock_counts : counts
  stock_counts ||--o{ stock_count_lines : measures
  stock_balances { uuid id PK uuid branch_id FK uuid item_id FK decimal quantity decimal inventory_value decimal avg_unit_cost int version }
  stock_movements { uuid id PK uuid branch_id FK uuid item_id FK decimal signed_quantity decimal signed_value string movement_type uuid document_id }
  stock_lots { uuid id PK uuid branch_id FK uuid item_id FK string lot_code date expires_on }
```
Cost rule: **per-branch moving weighted average**. Use canonical base units, supplier conversion snapshot, decimal arithmetic, row-level balance lock and append-only posting. `stock_lots` govern traceability/expiry; issue valuation remains at branch WAC rather than batch price. Never issue above available or from an expired lot for consumption. Backdated changes require documented correction; do not silently rewrite closed-month WAC.

## 7. Transfers and loans ERD
```mermaid
erDiagram
  stock_transfers ||--o{ stock_transfer_lines : includes
  branches ||--o{ stock_transfers : sender
  branches ||--o{ stock_transfers : receiver
  branches ||--o{ material_loans : controls
  branch_counterparties ||--o{ material_loans : partner
  material_loans ||--o{ material_loan_lines : requires
  material_loans ||--o{ material_loan_returns : repaid_by
  material_loan_returns ||--o{ material_loan_return_lines : contains
  internal_branch_balances }o--|| branches : owed_from
  internal_branch_balances }o--|| branches : owed_to
  stock_transfers { uuid id PK uuid from_branch_id FK uuid to_branch_id FK string status }
  material_loans { uuid id PK uuid branch_id FK uuid partner_id FK string direction string status }
  material_loan_lines { uuid id PK uuid loan_id FK uuid item_id FK decimal quantity_due decimal quantity_returned }
```
Same entity only: `stock_transfers` with sender WAC, receiver actual quantity, unresolved in-transit balance and mirrored internal due-to/due-from (eliminated at entity consolidation). Different registered entities: use **loan or material sale**, never silent internal transfer. Outside partner has no login. Partial loan return reduces in-kind due only after owner approves receipt; cost variance must remain traceable even if borrowed stock was already consumed.

## 8. Capital and distribution ERD
```mermaid
erDiagram
  branches ||--o{ investor_agreements : contracted
  investors ||--o{ investor_agreements : party
  investor_agreements ||--o{ capital_commitments : promises
  capital_commitments ||--o{ capital_contributions : settles
  investor_agreements ||--o{ profit_entitlement_terms : sets
  branches ||--o{ retained_profit_lots : accumulates
  retained_profit_lots ||--o{ retained_profit_entitlements : owned_by
  investors ||--o{ retained_profit_entitlements : entitled
  branches ||--o{ distribution_batches : approves
  distribution_batches ||--o{ distribution_lines : includes
  investors ||--o{ distribution_lines : receives
  distribution_lines ||--o| distribution_payments : settles
  branches ||--o{ capital_withdrawal_requests : requests
  investor_agreements { uuid id PK uuid branch_id FK uuid investor_id FK date effective_from string status }
  profit_entitlement_terms { uuid id PK uuid agreement_id FK date effective_from decimal share_ratio }
  retained_profit_lots { uuid id PK uuid branch_id FK date earning_month decimal retained_amount }
  distribution_lines { uuid id PK uuid batch_id FK uuid investor_id FK decimal approved_amount string status }
```
Entitlement ratios are **not** derived automatically from commitments. Snapshot ratios/eligible investor set for the earning month; keep unresolved prior-period entitlements after capital withdrawals. `distribution_payments` max one successful payment per line, full amount only, no partial payout. Approved-but-unpaid line remains `AWAITING_FUNDS` or `APPROVED`. Following period reopen use approved recovery/offset claims, not deletion of historical payments.

## 9. Example sequence: daily business report and bank
```mermaid
sequenceDiagram
  actor M as Manager
  participant R as Revenue module
  participant F as Finance module
  participant D as PostgreSQL
  M->>R: Confirm report (cash, bank, delivery NET) + request key
  R->>D: Lock branch/day unique report
  R->>F: Post sourced cash + bank entries (not delivery NET)
  F->>D: Insert immutable ledger entries with source uniqueness
  D-->>R: Commit atomically
  R-->>M: Confirmed report + summary
  Note over F,D: Actual delivery deposit must be verified separately
```

## 10. Example sequence: stock issue at WAC
```mermaid
sequenceDiagram
  actor M as Manager
  participant I as Inventory service
  participant DB as PostgreSQL
  M->>I: Confirm issue(item, qty, lot, requestKey)
  I->>DB: Lock stock_balances(branch,item) FOR UPDATE
  DB-->>I: available qty, avg cost, version
  alt Expired lot or qty insufficient
    I-->>M: Conflict; no postings
  else Valid issue
    I->>DB: Insert movement with WAC snapshot
    I->>DB: Update balances/lot and source ledger once
    DB-->>I: Atomic commit
    I-->>M: Posted document
  end
```

## 11. State machines (proposed)
```mermaid
stateDiagram-v2
  [*] --> DRAFT
  DRAFT --> CONFIRMED: authorized confirm
  CONFIRMED --> ADJUSTMENT_PENDING: correction needed
  ADJUSTMENT_PENDING --> CONFIRMED: approved corrective entry
  DRAFT --> CANCELLED
```
Daily report or stock document statuses must not be confused with monthly period status. Closed financial period requires a different approval state and version:
```mermaid
stateDiagram-v2
  [*] --> OPEN
  OPEN --> CLOSING: prerequisites satisfied
  CLOSING --> CLOSED: Owner confirms snapshot
  CLOSED --> REOPEN_REQUESTED: correction request
  REOPEN_REQUESTED --> REOPENED: approved by authority
  REOPEN_REQUESTED --> CLOSED: rejected
  REOPENED --> CLOSED: recalc and close new revision
```
Loan states: `DRAFT -> DISPATCHED/RECEIVED -> PARTIALLY_RETURNED -> SETTLED`; transfer states: `DRAFT -> DISPATCHED -> RECEIPT_PARTIAL/DISPUTED -> COMPLETED`. Approved distributions: `DRAFT -> APPROVED/AWAITING_FUNDS -> PAYMENT_PENDING -> PAID`, with `FAILED` and `CANCELLED` controlled. No money disappears on retries.

## 12. API contract conventions (proposed)
| Area | Example endpoints | Important controls |
|---|---|---|
| `/api/v1/entities /branches` | `GET, POST, PATCH` | Admin + entity/branch scope |
| `/api/v1/branches/{branchId}/business-days` | `GET, PUT` | CLOSED vs OPEN day reason |
| `/api/v1/branches/{branchId}/daily-revenue` | `GET, POST, /{id}/confirm` | One report/day, request key |
| `/api/v1/branches/{branchId}/cash-entries` | `GET, POST, /reconcile` | Actual funds; source unique |
| `/api/v1/branches/{branchId}/expenses` | `GET, POST, /{id}/confirm` | Manager confirms, Owner review |
| `/api/v1/branches/{branchId}/employees` | `GET, POST, PATCH` | Policy effective date |
| `/api/v1/branches/{branchId}/payroll` | `GET, POST, /{id}/approve` | Approved hours and snapshot |
| `/api/v1/branches/{branchId}/stock/receipts` | `GET, POST, /{id}/post` | Base units + WAC |
| `/api/v1/branches/{branchId}/stock/issues` | `GET, POST, /{id}/post` | No negative stock |
| `/api/v1/branches/{branchId}/monthly-close` | `GET, POST, /reopen-request` | Prerequisites, reason, revision |
| `/api/v1/branches/{branchId}/transfers` | `GET, POST, /receive` | Only intra-entity (P1) |
| `/api/v1/branches/{branchId}/material-loans` | `GET, POST, /returns` | Qty due, partial return (P1) |
| `/api/v1/branches/{branchId}/investor-agreements` | `GET, POST, PATCH` | Terms effective-date (P1) |
| `/api/v1/branches/{branchId}/distributions` | `GET, POST, /approve, /pay` | Full-single payout and funds gate (P1) |
Common: `Authorization`, `Idempotency-Key` on unsafe write, `If-Match` or version token for mutable drafts, RFC-7807-like problem response, local business date `YYYY-MM-DD`, timestamp stored in UTC. No API should accept arbitrary `branchId` without cross-checking membership and target ownership. All V1 endpoints are planned, not implemented.

## 13. Traceability and open policies
| Policy | Status | Design consequence |
|---|---|---|
| ADR-025 — Stack and deployment | PROPOSED | Java/Spring Boot modular monolith, PostgreSQL, React TypeScript, Flyway and Docker. |
| ADR-026 — Stock backdating | PROPOSED | Closed period and prior WAC values can be corrected only by controlled re-open or posted current adjustment, never silent edit. |
| ADR-027 — Loans cost variance | POLICY_VERIFY | Borrowed item uses receiving branch reference WAC or approved value; financial difference tracked on in-kind repayment. |
| ADR-028 — Profit distribution compliance | POLICY_VERIFY | Final distribution conditions must be verified against actual household agreement, tax and liabilities. |
| ADR-029 — Labour compliance | POLICY_VERIFY | Night work after 22:00, rest breaks, overtime and payroll deductions need legal-policy validation. |
| ADR-030 — Delivery app cash posting | PROPOSED | App net in daily report must not auto-post as bank funds; when actual funds arrive manually reconcile without duplicate sales. |
| ADR-031 — Monetary rounding | PROPOSED | Use Decimal NUMERIC, record amounts to 2 decimals; payout residual cent assignment deterministic. |

## 14. SQL mapping and split-migration strategy
The companion `MTMS_PostgreSQL_Schema_Starter_v1_0.sql` includes a **proposed conceptual starter** with core P0 and future P1 structures; it is **not** a production migration or evidence of executed schema. Before coding, split by dependency into immutable Flyway `V001__...` and onward, verify constraint names/order, add integration tests, and never modify an applied migration. At minimum, install constraints for one branch/day report, source posting uniqueness, nonnegative balances, one successful full payout per entitlement line, effective-date collision checks and one branch–bank account go-live gate. Cross-module accounting integrity remains transaction/service responsibility; a schema by itself is insufficient.

## 15. Gate alignment
- **G0 (P0)** Requirements signoff: Business owner accepts baseline + legal/tax/labor verify flags and no-POS boundary.
- **G1 (P0)** Foundation ready: Branch-scoped auth, base schema, audit, idempotency and CI verified.
- **G2 (P0)** Revenue/finance pilot: One branch enters 2 weeks daily revenue + expenses and reconciles cash.
- **G3 (P0)** MVP close: One real month closed; revenue dates & payroll complete; P&L flagged provisional; backup restored.
- **G4 (P1)** Stock/HR rollout: GPS policy, transfers/loans, lot controls and accounting approvals signed off.
- **G5 (P1)** Capital payout enablement: Accountant/legal agreement policy verified; approved payout test, no partial transfer, recover/offset tested.
- **G6 (P1)** V1 rollout: Multi-branch test, permissions, finance/cost and regression suite passed.
- **G7 (P2)** Optional AI release: Privacy/evaluation human-review gate and business ROI assessed.
