# MTMS — Implementation Handbook v1.0
**Date:** 07/11/2026 · **Status:** Planning artifact (not an implemented repo) · **Project:** MilkTea Management System
**Audience:** Solo Java Backend/Full-stack engineer using this handbook as a task-by-task build mentor. All requirements trace to `BR-*`, tasks to `P0/P1/P2-*`, tests to `TC-*`, decisions to `ADR-*`.

> **Hard boundaries:** No POS, no fabricated app cash settlements, no automatic tax/legal compliance, no live profit distribution in P0. All financial payout policies require signed business/legal/accounting gate before P1 activation.
## Quick navigation
- 1. Product boundaries and acceptance
- 2. Stack and repository
- 3. Domain contracts and invariants
- 4. API style and security
- 5. Data and transaction patterns
- 6. Onboarding and Excel migration
- 7. Delivery sequence and gates
- 8. Complete task specifications
- 9. Test matrix
- 10. Decision register
- 11. Risk register
- 12. Local development and deployment runbooks
- 13. Go-live checklist

## 1. Product boundaries and acceptance
**Purpose:** An actual multi-shop milk-tea owner can close a month and trust the *reported managerial* numbers; also acts as strong full-stack portfolio. Shops can belong to different registered household-business entities. Each shop has separate investor economics, bank account, payroll, inventory and business report. Outside same-brand shops have counterparty identities only and cannot log in.
**P0 successful when:** one real branch has a signed opening-balance snapshot, all operating days entered or CLOSED, manual items used with per-branch WAC, approved timesheets/payroll, cash reconciled, one monthly snapshot produced and restore tested. Managerial profit is provisional without depreciation and verification.
**P1 successful when:** transfers/loans, quantity debt and return variance reconcile; GPS and payroll change histories survive updates; investor terms effective dated, distribution single-full payment with funds gate; income/cash remain separate; multi-branch authorization negative tests green.
**P2 success:** optional AI components suggest only; every proposed expense, stock action and OCR item requires human review. No direct write permission into core financial tables.

## 2. Stack and repository (engineering proposal)
```text
mtms/
├── backend/                  # Java 21, Spring Boot, Maven, modular-monolith
│   └── src/main/java/com/mtms/{common,organization,identity,revenue,finance,hr,inventory,closing,capital,transfer,reporting}/
├── frontend/                 # React, TypeScript, Vite
├── infra/docker/             # PostgreSQL, app services, reverse proxy
├── docs/                     # BR baseline, ADR, Mermaid, runbooks
├── database/migrations/      # Flyway V001... sequential and immutable
├── scripts/import-templates/ # Import staging samples, NOT real private records
├── .github/workflows/        # CI build, unit, integration, migration checks
└── ai-service/               # P2 optional FastAPI service
```
Core libraries are **proposed**: Spring Security, Spring Data JDBC/JPA (choose per module after benchmark), Jakarta Validation, Testcontainers/PostgreSQL integration, Flyway, Micrometer. Prefer internal module interfaces over events for same-transaction business actions. Async outbox only when a real cross-service or notification need appears.
**Conventions:** domain and application code do not import controllers or persistence entities from other modules; use immutable DTOs for boundaries; `BigDecimal` for amounts and quantities; `UUID` identifiers; `Instant`/UTC audit timestamps; `LocalDate` for Asia/Ho_Chi_Minh business dates. All public mutations use optimistic locking or idempotency appropriately.

## 3. Domain contracts and invariants
- **ORG-INV-01:** A registered business entity can contain many branches; no branch belongs to two business entities concurrently.
- **SEC-INV-01:** Every request is authorized at action, branch scope and data-row levels; investors may not access payroll without explicit special right (default denied).
- **REV-INV-01:** A branch has at most one current business-day revenue report; CLOSED day is not an OPEN day with zero revenue.
- **REV-INV-02:** Delivery app NET belongs to reported sale day but cannot be auto-posted as bank funds.
- **FIN-INV-01:** Cash movements, profit, supplier debt payments and capital contributions are different financial events; source event posted once.
- **FIN-INV-02:** Inventory purchase increases stock/payable; only consumption affects ingredient COGS.
- **HR-INV-01:** Approved payable hours are separate from GPS; rates are effective-dated and snapshotted on payroll approval.
- **HR-INV-02:** No Manager self-approval; payroll cost belongs to month-end primary branch.
- **INV-INV-01:** Cannot post negative available stock; WAC is updated under row lock by branch/item.
- **INV-INV-02:** Tracked expired lot may not be consumed; an approved disposal may remove it.
- **MOV-INV-01:** Inter-entity moves are never silent internal transfers; partner loans carry quantity-based liabilities.
- **MOV-INV-02:** Sender/receiver reconcile short deliveries; actual accepted receipt can be posted before completion.
- **CAP-INV-01:** Profit entitlement is not equal to nominal/paid capital, and prior period snapshot survives ownership changes.
- **CAP-INV-02:** Each approved beneficiary amount has at most one successful **full** payout; insufficient cash remains unpaid.
- **CLS-INV-01:** A month cannot close until all OPEN dates have reports and payroll is approved.
- **CLS-INV-02:** Closed postings require approved reopen/adjustment, impact scan and retained immutable historical payments.
**Rounding engineering proposal:** store `NUMERIC(20,2)` monetary amounts, `NUMERIC(18,3)` stock quantities, `NUMERIC(20,6)` unit price; evaluate WAC with unrounded intermediate precision and round at booked line. Distribution-line rounding residual is assigned deterministically to a designated line so approved total equals allocated total. This policy remains `PROPOSED` (ADR-031) and must be tested before financial go-live.

## 4. API style and security
Path examples are listed in the ERD file. Request context always includes authorized `branchId` and use-case action; never trust a branchId from the JSON body alone. All list endpoints query branch-filtered rows, not fetch-all + filter. File metadata authorization is separate from storage URL. Prefer short-lived secure download tokens, hashed password credentials, refresh token revocation, CSRF protection if using cookies, rate limiting at authentication boundary and no sensitive values in logs.
**API status convention:** 200 GET/PATCH result, 201 created, 204 no content, 400 malformed, 401 unauthenticated, 403 forbidden, 404 absent/hidden, 409 duplicate/conflict/outdated state, 422 domain-validation failure if chosen consistently. `problem_code`, `correlation_id`, `message`, `field_errors` and `retryable` fields are recommended. All write endpoints accept `Idempotency-Key`; validate re-use with same payload hash; same key/new payload fails conflict.
**Permissions:** `system.admin`, `branch.owner`, `branch.manager`, `branch.finance`, `branch.employee`, `branch.investor`. Add granular report groups (`revenue`, `cost`, `profit`, `inventory`, `capital`) for investors. Do not grant investor access to HR documents, raw GPS, payroll details or other branches by default. Delegated Owner authority must carry `effective_from`, `effective_to`, proof document and principal authorization records.

## 5. Data and transaction patterns
### 5.1 Monetary ledger and revenue
Make cash/bank `cash_ledger_entries` append-only once posted. Link every entry by `source_type`, `source_id`, `source_leg`; unique index prevents double posting. Daily confirmed cash and transfer create *reported* cash/bank postings only according to confirmed source mapping; app NET stays revenue read-model not available bank cash. Actual app deposits are later recorded as a distinct reconciled receipt and must never create beverage revenue again. Balance changes and journal inserts commit atomically.
### 5.2 WAC and stock
Use `SELECT ... FOR UPDATE` on branch/item balance, ensure active ingredient and valid unexpired lot, qty available, then insert source-linked movement and update quantity/value with one commit. `new_avg = (old_value + receipt_value) / (old_qty + receipt_qty)` for receipt; consumption removes qty × current avg, storing the booked unit cost and value. On exact zero stock, normalize residual value to zero using documented rounding logic. Supplier pack ratio is snapshotted in receipt line; changing pack conversion later never mutates old receipts.
### 5.3 Payroll
Read immutable approved time entries, split time by `salary_policies.effective_from` and calculate `approved hours × effective hourly rate` (or monthly prorated workdays). Add signed manual adjustment rows and preserve per-line snapshots: rate, hours, working days, branch and calculation version. Approving payroll accrues expense into service month; payment moves funds in actual paid month and reduces payroll liability. No advances. Owner may edit approved payroll while month OPEN with a new revision; closed month must reopen via authorized request.
### 5.4 Capital and distribution
Use earning-period entitlements from signed terms, not current investor rows. One `retained_profit_lot` per branch/earning month/source close version; an approved distribution batch draws from eligible lots, reserves amount and writes immutable beneficiary lines. Owner must check available funds, liabilities, taxes, prior retained commitments and agreement conditions. `approved_amount` can remain `AWAITING_FUNDS`. Post payment only for full amount in a single successful transaction per beneficiary; dedupe on line and external bank reference. On closed-period restatement, book approved receivable/offset claims without modifying prior payment entries.
### 5.5 Reopen and period correction
Closed `monthly_closes` are snapshotted. An approved reopen creates a new revision and impact plan: identify all affected stock WAC, payroll expense, taxes, retention, investor distributions and future reports. Never silently mutate historical approval/payment. If current-period offset permitted, link back to source period and record rationale. This requires an explicit business-accounting signoff at G5.

## 6. Onboarding and Excel migration
1. Register each business entity and branch; assign dedicated branch bank account metadata and authorized Owner.
2. Freeze a *cutover date* per branch; establish actual cash and bank balances by physical count/statement.
3. Import employees, suppliers, items, base units, supplier packaging and opening stock from validated Excel; do not bulk-load old transaction history.
4. Record opening payables, receivables, assets, committed/actual capital and uncleared financial obligations.
5. Run preview with row-numbered errors, duplicate detection and unit validation. Owner signs the opening snapshot before atomic commit.
6. Reconcile source Excel/Zalo against first two weeks of data; train Manager; confirm one complete month closes; log variance separately.
7. Keep original Excel archives as records with restricted access, **not** as a writable second source of truth.

## 7. Delivery phases and gates
- **P0:** MVP — Internal daily management and provisional monthly P&L. 52 planned tasks; 590 indicative person-hours *excluding support, rework, accounting/legal validation and contingency*. A one-person project at 3 hours/day should not be assigned a fabricated deadline.
- **P1:** V1 — Multi-branch operation, stock loans and approved profit distribution. 44 planned tasks; 630 indicative person-hours *excluding support, rework, accounting/legal validation and contingency*. A one-person project at 3 hours/day should not be assigned a fabricated deadline.
- **P2:** Enhancements — optional AI / integrations and advanced analytics. 8 planned tasks; 112 indicative person-hours *excluding support, rework, accounting/legal validation and contingency*. A one-person project at 3 hours/day should not be assigned a fabricated deadline.
- **G0 / P0:** Requirements signoff — Business owner accepts baseline + legal/tax/labor verify flags and no-POS boundary.
- **G1 / P0:** Foundation ready — Branch-scoped auth, base schema, audit, idempotency and CI verified.
- **G2 / P0:** Revenue/finance pilot — One branch enters 2 weeks daily revenue + expenses and reconciles cash.
- **G3 / P0:** MVP close — One real month closed; revenue dates & payroll complete; P&L flagged provisional; backup restored.
- **G4 / P1:** Stock/HR rollout — GPS policy, transfers/loans, lot controls and accounting approvals signed off.
- **G5 / P1:** Capital payout enablement — Accountant/legal agreement policy verified; approved payout test, no partial transfer, recover/offset tested.
- **G6 / P1:** V1 rollout — Multi-branch test, permissions, finance/cost and regression suite passed.
- **G7 / P2:** Optional AI release — Privacy/evaluation human-review gate and business ROI assessed.

## 8. Complete task specifications
The tracker is the source for editable status/priority/owner. Each numbered task below includes scope, rationale, acceptance and a test trace. Dependencies are directional. `Done` requires code + test + evidence link, not a status toggle alone.

### P0 — MVP — Internal daily management and provisional monthly P&L

#### P0-ORG-01 — Project skeleton, conventions
- **Module / Req:** `ORG` · `BR-ORG-01` · **Estimate:** 8h · **Priority:** Must · **Dependency:** `None` · **Tests:** `TC-001`
- **Business intent:** Create mono-repo backend/frontend/infra/docs, Java 21, Maven, React TS and agreed conventions.
- **Definition of Done:** Both services boot; lint and unit smoke test pass. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-ORG-02 — PostgreSQL and Flyway baseline
- **Module / Req:** `ORG` · `BR-ORG-01` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-ORG-01` · **Tests:** `TC-001`
- **Business intent:** Build compose local DB and first forward-only migrations for entity/branch/user.
- **Definition of Done:** Clean DB migrates once, rerun safe. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-ORG-03 — Entity and branch domain
- **Module / Req:** `ORG` · `BR-ORG-02` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-ORG-02` · **Tests:** `TC-003`
- **Business intent:** Separate registered entity from branch; operational bank readiness.
- **Definition of Done:** Create two entities and several branches with correct FK constraints. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-ORG-04 — Authentication and sessions
- **Module / Req:** `ORG` · `BR-ORG-04` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-ORG-02` · **Tests:** `TC-001`
- **Business intent:** Implement login, password hashing, refresh/logout, secure cookie/token pattern.
- **Definition of Done:** Unauthorized request rejected; logout invalidates access. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-ORG-05 — Scoped permissions
- **Module / Req:** `ORG` · `BR-ORG-04` · **Estimate:** 14h · **Priority:** Must · **Dependency:** `P0-ORG-04` · **Tests:** `TC-001`
- **Business intent:** Implement role grants by branch, permission checks in app services and queries.
- **Definition of Done:** Denied cross-branch employee/expense access with negative test. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-ORG-06 — Org and users admin UI
- **Module / Req:** `ORG` · `BR-ORG-01` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-ORG-05` · **Tests:** `TC-001`
- **Business intent:** CRUD workflows for branches, employees and memberships with permission previews.
- **Definition of Done:** Admin can onboard a branch without leaked unauthorized data. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-ORG-07 — Branch bank onboarding
- **Module / Req:** `ORG` · `BR-ORG-03` · **Estimate:** 6h · **Priority:** Must · **Dependency:** `P0-ORG-03` · **Tests:** `TC-003`
- **Business intent:** Require dedicated bank account metadata and cash-box configuration.
- **Definition of Done:** Active branch cannot pass readiness without account. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-SEC-01 — Private evidence upload
- **Module / Req:** `SEC` · `BR-SEC-01` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-ORG-05` · **Tests:** `TC-043`
- **Business intent:** Private storage abstraction, attachment metadata, max-size/type validation.
- **Definition of Done:** Only authorized branch may request time-limited download. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-SEC-02 — Audit event spine
- **Module / Req:** `SEC` · `BR-SEC-03` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-ORG-05` · **Tests:** `TC-041`
- **Business intent:** Append actor/action/target/before-after digest and reason for sensitive transitions.
- **Definition of Done:** Close, rate edits and expense approvals leave searchable audit events. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-SEC-03 — Idempotency and optimistic locking
- **Module / Req:** `SEC` · `BR-SEC-03` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-ORG-02` · **Tests:** `TC-011`
- **Business intent:** Implement request keys, unique source constraints and version conflict policy.
- **Definition of Done:** Duplicate request never makes duplicate financial postings. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-SEC-04 — Excel import staging
- **Module / Req:** `SEC` · `BR-SEC-02` · **Estimate:** 14h · **Priority:** Must · **Dependency:** `P0-ORG-05` · **Tests:** `TC-045`
- **Business intent:** Upload XLSX/CSV, preview mappings, validate, store row error report and commit once.
- **Definition of Done:** Duplicate IDs and malformed units rejected before commit. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-SEC-05 — Monitoring and CI baseline
- **Module / Req:** `SEC` · `BR-SEC-04` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-ORG-01` · **Tests:** `TC-044`
- **Business intent:** GitHub Actions test/build, Docker, health/readiness, backup+restore runbook.
- **Definition of Done:** Automated test pipeline and staging restore proof. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-SEC-06 — Responsive shell UI
- **Module / Req:** `SEC` · `BR-ORG-04` · **Estimate:** 8h · **Priority:** Must · **Dependency:** `P0-ORG-04` · **Tests:** `TC-001`
- **Business intent:** Layout for Admin/Owner/Manager and employee phone views.
- **Definition of Done:** Common flows usable on mobile and dashboard on desktop. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-REV-01 — Business calendar
- **Module / Req:** `REV` · `BR-REV-02` · **Estimate:** 8h · **Priority:** Must · **Dependency:** `P0-ORG-03` · **Tests:** `TC-005`
- **Business intent:** Branch day opening/closed status, timezone and activation logic.
- **Definition of Done:** CLOSED day exempt, missing OPEN date visible. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-REV-02 — Daily revenue report model
- **Module / Req:** `REV` · `BR-REV-01` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-REV-01` · **Tests:** `TC-006`
- **Business intent:** Create one versioned daily aggregate/report with cash, transfer, app NET.
- **Definition of Done:** Unique branch/date active report, 0 or more evidence. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-REV-03 — Revenue capture API
- **Module / Req:** `REV` · `BR-REV-03` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-REV-02` · **Tests:** `TC-006`
- **Business intent:** Manager create/update/confirm daily report; Owner correction reason captured.
- **Definition of Done:** Retry confirmation does not double post. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-REV-04 — Revenue entry UI
- **Module / Req:** `REV` · `BR-REV-01` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-REV-03` · **Tests:** `TC-004`
- **Business intent:** Mobile form with component sum, proof upload and preview.
- **Definition of Done:** Operator can record, edit draft and submit a day. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-REV-05 — Revenue cash linkage
- **Module / Req:** `REV` · `BR-REV-04` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-FIN-02` · **Tests:** `TC-007`
- **Business intent:** Post confirmed cash/transfer to correct subledger without auto-cashing app NET.
- **Definition of Done:** App revenue affects P&L, not bank balance by itself. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-REV-06 — Daily overview UI
- **Module / Req:** `REV` · `BR-REV-02` · **Estimate:** 8h · **Priority:** Must · **Dependency:** `P0-REV-04` · **Tests:** `TC-004`
- **Business intent:** Calendar showing missing, closed, confirmed and exceptions.
- **Definition of Done:** Owner can identify incomplete business dates instantly. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-FIN-01 — Financial chart of categories
- **Module / Req:** `FIN` · `BR-FIN-01` · **Estimate:** 8h · **Priority:** Must · **Dependency:** `P0-ORG-03` · **Tests:** `TC-010`
- **Business intent:** Separate operating expenses, capital flows, internal transfers and debt settlements.
- **Definition of Done:** Category cannot incorrectly create retail revenue from capital. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-FIN-02 — Cash and bank ledger
- **Module / Req:** `FIN` · `BR-FIN-09` · **Estimate:** 14h · **Priority:** Must · **Dependency:** `P0-FIN-01` · **Tests:** `TC-011`
- **Business intent:** Append-only account entries with source uniqueness, cash/bank per branch.
- **Definition of Done:** Retries safe, deterministic ledger balance. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-FIN-03 — Expense model and approval
- **Module / Req:** `FIN` · `BR-FIN-07` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-FIN-02` · **Tests:** `TC-011`
- **Business intent:** Expense draft/confirm, receipt reference, period assignment and threshold alert.
- **Definition of Done:** Manager can confirm; Owner sees abnormal expense alert. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-FIN-04 — Expense mobile UI
- **Module / Req:** `FIN` · `BR-FIN-07` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-FIN-03` · **Tests:** `TC-011`
- **Business intent:** Capture expense amount, category, payment method, document and notes.
- **Definition of Done:** Manager enters and owner reviews by branch. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-FIN-05 — Payable/receivable ledger
- **Module / Req:** `FIN` · `BR-FIN-02` · **Estimate:** 14h · **Priority:** Must · **Dependency:** `P0-FIN-02` · **Tests:** `TC-009`
- **Business intent:** Track suppliers, manual receivables and payables with partial debt settlement.
- **Definition of Done:** Debt and bank moves post once and reconcile. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-FIN-06 — Cash/bank reconciliation
- **Module / Req:** `FIN` · `BR-FIN-06` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-FIN-02` · **Tests:** `TC-012`
- **Business intent:** Ledger-vs-actual snapshot, variance, approval and journal correction.
- **Definition of Done:** Discrepancy cannot disappear silently. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-FIN-07 — Tax manual obligations
- **Module / Req:** `FIN` · `BR-FIN-04` · **Estimate:** 8h · **Priority:** Must · **Dependency:** `P0-FIN-05` · **Tests:** `TC-012`
- **Business intent:** Enter tax due, period, paid amount and remaining liability by entity or branch.
- **Definition of Done:** No fake automated tax computation. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-FIN-08 — Cash flow and expense dashboard
- **Module / Req:** `FIN` · `BR-FIN-01` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-FIN-06` · **Tests:** `TC-007`
- **Business intent:** Separately graph cash movement, payable aging and operating expenses.
- **Definition of Done:** App net not incorrectly presented as bank cash. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-01 — Employee and branch assignments
- **Module / Req:** `HR` · `BR-HR-03` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-ORG-03` · **Tests:** `TC-015`
- **Business intent:** Create employee, primary branch history and position.
- **Definition of Done:** End-of-month primary branch known with no overlap ambiguity. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-02 — Effective salary policies
- **Module / Req:** `HR` · `BR-HR-01` · **Estimate:** 14h · **Priority:** Must · **Dependency:** `P0-HR-01` · **Tests:** `TC-013`
- **Business intent:** Hourly/monthly policy, branch defaults and individual overrides with start date.
- **Definition of Done:** Old rates retained; no conflicting intervals. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-03 — Approved hours input
- **Module / Req:** `HR` · `BR-HR-02` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-HR-01` · **Tests:** `TC-013`
- **Business intent:** Manager inputs paid hours/days per date independent of GPS.
- **Definition of Done:** Approved data has actor/time and is used by payroll. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-04 — Paid-time review UI
- **Module / Req:** `HR` · `BR-HR-02` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-HR-03` · **Tests:** `TC-017`
- **Business intent:** Mobile editable timesheet and Manager review queue.
- **Definition of Done:** Approved hours visible with reason and who approved. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-05 — Payroll calculation engine
- **Module / Req:** `HR` · `BR-HR-10` · **Estimate:** 16h · **Priority:** Must · **Dependency:** `P0-HR-02` · **Tests:** `TC-013`
- **Business intent:** Effective rate slices, monthly proration, manual additions/deductions.
- **Definition of Done:** Midmonth raise accurate and decimal-safe. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-06 — Payroll approval and snapshot
- **Module / Req:** `HR` · `BR-HR-05` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-HR-05` · **Tests:** `TC-017`
- **Business intent:** Payroll draft/approve with branch cost snapshot and recalculation guard.
- **Definition of Done:** Owner authorizes; self-approval denied; closed changes blocked. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-07 — Payroll payment record
- **Module / Req:** `HR` · `BR-HR-06` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-HR-06` · **Tests:** `TC-016`
- **Business intent:** Branch payday, full/partial actual paid amount records, cash/bank posting, due tracking.
- **Definition of Done:** November salary paid December reduces December cash only. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-08 — Payroll statement UI
- **Module / Req:** `HR` · `BR-HR-05` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-HR-06` · **Tests:** `TC-017`
- **Business intent:** Owner can inspect rates/hours/adjustments and pay summary; employee privacy.
- **Definition of Done:** Owner approves; no cross-branch disclosure. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-HR-09 — Primary-branch allocation test
- **Module / Req:** `HR` · `BR-HR-03` · **Estimate:** 6h · **Priority:** Must · **Dependency:** `P0-HR-05` · **Tests:** `TC-015`
- **Business intent:** Apply end-of-month primary branch rule with reassignment history.
- **Definition of Done:** Payroll expense charged to correct branch. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-INV-01 — Item and base units catalog
- **Module / Req:** `INV` · `BR-INV-04` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-ORG-03` · **Tests:** `TC-023`
- **Business intent:** Items, UOM and supplier-specific package conversions with immutable receipt snapshot.
- **Definition of Done:** 2 boxes of 24 maps to 48 pieces. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-INV-02 — Opening inventory and valuation
- **Module / Req:** `INV` · `BR-INV-08` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-SEC-04` · **Tests:** `TC-024`
- **Business intent:** Physical count + Excel import with verified qty/value seed per branch.
- **Definition of Done:** Post once, WAC computed and origin auditable. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-INV-03 — Supplier and goods receipt
- **Module / Req:** `INV` · `BR-INV-05` · **Estimate:** 14h · **Priority:** Must · **Dependency:** `P0-INV-01` · **Tests:** `TC-008`
- **Business intent:** Direct receive without PO, actual qty, price, discrepancy and payable link.
- **Definition of Done:** Receipt raises stock and debt, not COGS. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-INV-04 — Per-branch moving WAC posting
- **Module / Req:** `INV` · `BR-INV-02` · **Estimate:** 18h · **Priority:** Must · **Dependency:** `P0-INV-03` · **Tests:** `TC-020`
- **Business intent:** Single transactional posting engine updates balance qty/value and movement snapshot.
- **Definition of Done:** 10@100k + 10@120k -> WAC=110k. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-INV-05 — Manual consumption issue
- **Module / Req:** `INV` · `BR-INV-03` · **Estimate:** 14h · **Priority:** Must · **Dependency:** `P0-INV-04` · **Tests:** `TC-022`
- **Business intent:** One/multiple daily documents; valid qty and no-negative enforcement.
- **Definition of Done:** Concurrent final issues never result in negative stock. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-INV-06 — Inventory UI
- **Module / Req:** `INV` · `BR-INV-01` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-INV-05` · **Tests:** `TC-021`
- **Business intent:** Receive, issue, on-hand and valuation summary on mobile/desktop.
- **Definition of Done:** Owner sees quantity and value per branch. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-INV-07 — Inventory-to-COGS period read model
- **Module / Req:** `INV` · `BR-INV-02` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-INV-05` · **Tests:** `TC-020`
- **Business intent:** Inventory consumption posts period COGS separate from purchases.
- **Definition of Done:** P&L uses issue value and no double cost. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-CAP-01 — Investor registry and agreements
- **Module / Req:** `CAP` · `BR-CAP-01` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-ORG-03` · **Tests:** `TC-032`
- **Business intent:** Per-branch investor and agreement history separate from app role.
- **Definition of Done:** Investor in A not automatically entitled to B. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-CAP-02 — Commitment and contributions
- **Module / Req:** `CAP` · `BR-CAP-01` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-CAP-01` · **Tests:** `TC-032`
- **Business intent:** Record promises, cash/assets/work and actual contribution; link financial flows properly.
- **Definition of Done:** 100m commitment, 60m paid, 40m pending. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-CAP-03 — Effective profit term viewer
- **Module / Req:** `CAP` · `BR-CAP-01` · **Estimate:** 8h · **Priority:** Must · **Dependency:** `P0-CAP-02` · **Tests:** `TC-032`
- **Business intent:** View configured ratio and date, no actual distribution in MVP.
- **Definition of Done:** Terms visible, marked information only. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-CLS-01 — Period state and prerequisites
- **Module / Req:** `CLS` · `BR-CLS-01` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-REV-06` · **Tests:** `TC-040`
- **Business intent:** OPEN/CLOSING/CLOSED control with all business days reported and payroll approved.
- **Definition of Done:** Missing revenue/payroll blocks close. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-CLS-02 — Managerial P&L query
- **Module / Req:** `CLS` · `BR-FIN-01` · **Estimate:** 18h · **Priority:** Must · **Dependency:** `P0-INV-07` · **Tests:** `TC-016`
- **Business intent:** Revenue + issued COGS + payroll accrual + expenses + manually assigned tax.
- **Definition of Done:** Components reconcile to transactions; depreciation caveat. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-CLS-03 — Close snapshot
- **Module / Req:** `CLS` · `BR-CLS-02` · **Estimate:** 12h · **Priority:** Must · **Dependency:** `P0-CLS-02` · **Tests:** `TC-041`
- **Business intent:** Store immutable component totals/inputs, revision, actor and timestamp.
- **Definition of Done:** Closed period cannot be edited by Manager. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-CLS-04 — Owner dashboard and close UI
- **Module / Req:** `CLS` · `BR-CLS-01` · **Estimate:** 14h · **Priority:** Must · **Dependency:** `P0-CLS-03` · **Tests:** `TC-040`
- **Business intent:** Missing-data check, provisional profit, confirm closure and downloadable summary.
- **Definition of Done:** Owner reviews all prechecks and closed snapshot. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-CLS-05 — MVP release gate
- **Module / Req:** `CLS` · `BR-SEC-04` · **Estimate:** 16h · **Priority:** Must · **Dependency:** `P0-CLS-04` · **Tests:** `TC-044`
- **Business intent:** End-to-end real-branch trial with anonymized data and backup restore.
- **Definition of Done:** One closed sample month reconciles to source Excel. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P0-AST-01 — Equipment baseline register
- **Module / Req:** `AST` · `BR-AST-01` · **Estimate:** 10h · **Priority:** Must · **Dependency:** `P0-ORG-03` · **Tests:** `TC-051`
- **Business intent:** Register equipment by branch, acquisition cost, location and warranty; no automatic depreciation in MVP.
- **Definition of Done:** Asset visible; P0 P&L discloses exclusion of depreciation. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

### P1 — V1 — Multi-branch operation, stock loans and approved profit distribution

#### P1-ORG-01 — Investor report grants UI
- **Module / Req:** `ORG` · `BR-ORG-05` · **Estimate:** 10h · **Priority:** Should · **Dependency:** `P0-ORG-05` · **Tests:** `TC-002`
- **Business intent:** Owner toggles permitted report groups per investor and branch.
- **Definition of Done:** Investor cannot access hidden payroll/evidence. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-ORG-02 — Delegated Owner authority
- **Module / Req:** `ORG` · `BR-ORG-06` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P0-CAP-01` · **Tests:** `TC-053`
- **Business intent:** Effective-dated delegation and evidence + app authorization checks.
- **Definition of Done:** Expired Owner cannot authorize new distribution. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-HR-01 — Work schedule model
- **Module / Req:** `HR` · `BR-HR-07` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P0-HR-01` · **Tests:** `TC-019`
- **Business intent:** Shift patterns/assignment and weekly/monthly planning.
- **Definition of Done:** No overlapping scheduled shifts unless flagged. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-HR-02 — Shift swap and leave workflow
- **Module / Req:** `HR` · `BR-HR-07` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P1-HR-01` · **Tests:** `TC-019`
- **Business intent:** Employee request, Manager approve, audit and schedule update.
- **Definition of Done:** Rejected/unapproved swap not active. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-HR-03 — GPS attendance capture
- **Module / Req:** `HR` · `BR-HR-08` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P1-HR-01` · **Tests:** `TC-018`
- **Business intent:** Mobile geofence, timestamp, consent, anomaly flag, limited retention.
- **Definition of Done:** Out-of-geofence kept for review not auto-paid. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-HR-04 — Attendance correction workflow
- **Module / Req:** `HR` · `BR-HR-08` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P1-HR-03` · **Tests:** `TC-018`
- **Business intent:** Employee request correction; Manager approves; Manager self-record handled by Owner.
- **Definition of Done:** Evidence and old/new values persisted. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-HR-05 — Leave and monthly wage policy
- **Module / Req:** `HR` · `BR-HR-09` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P1-HR-02` · **Tests:** `TC-056`
- **Business intent:** Paid/unpaid leave mapping; configurable rounding/meal policies.
- **Definition of Done:** Manual adjustments preserved; proration consistent. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-HR-06 — Night-work compliance flags
- **Module / Req:** `HR` · `BR-HR-09` · **Estimate:** 8h · **Priority:** Should · **Dependency:** `P1-HR-03` · **Tests:** `TC-048`
- **Business intent:** Record actual after-22:00 minutes, alert human reviewer; no implicit legal compliance claim.
- **Definition of Done:** Shift ending 22:30 flagged when applicable. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-HR-07 — Payroll corrections and audit
- **Module / Req:** `HR` · `BR-HR-05` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P0-HR-06` · **Tests:** `TC-041`
- **Business intent:** Owner modifies approved-but-open payroll via version; close requires approved reopen.
- **Definition of Done:** Old payroll and payments preserved. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-INV-01 — Lot and expiry catalog
- **Module / Req:** `INV` · `BR-INV-06` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P0-INV-05` · **Tests:** `TC-025`
- **Business intent:** Per receipt lot, optional expiration per tracked item, FEFO candidate selection.
- **Definition of Done:** Expired lot cannot be consumed. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-INV-02 — Stock count draft and approval
- **Module / Req:** `INV` · `BR-INV-07` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P1-INV-01` · **Tests:** `TC-026`
- **Business intent:** Count actual qty, delta qty/value, reason; Owner approval creates immutable adjustment.
- **Definition of Done:** No change until approved. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-INV-03 — Disposal workflow
- **Module / Req:** `INV` · `BR-INV-07` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P1-INV-01` · **Tests:** `TC-025`
- **Business intent:** Expired/damaged item disposal request with evidence, Owner approval and COGS adjustment class.
- **Definition of Done:** Cannot use expired goods in drinks. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-INV-04 — Backdate and cost impact analysis
- **Module / Req:** `INV` · `BR-INV-09` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P0-CLS-03` · **Tests:** `TC-042`
- **Business intent:** Disallow silent repost of old WAC; adjustment planner and period impact scan.
- **Definition of Done:** History/reopen traceable, no silent valuation change. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-INV-05 — Supplier credit detail
- **Module / Req:** `INV` · `BR-INV-05` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P0-FIN-05` · **Tests:** `TC-008`
- **Business intent:** Supplier invoices, partial receipt discrepancy and proper payable association.
- **Definition of Done:** Invoice 10 units, received 8 => debt on 8, shortage recorded. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-01 — Transfer request and route
- **Module / Req:** `MOV` · `BR-MOV-01` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P0-INV-04` · **Tests:** `TC-028`
- **Business intent:** Draft/dispatch transfer in same registered entity only, sender WAC snapshot.
- **Definition of Done:** Inter-entity internal transfer forbidden. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-02 — Receiver partial acceptance
- **Module / Req:** `MOV` · `BR-MOV-02` · **Estimate:** 16h · **Priority:** Should · **Dependency:** `P1-MOV-01` · **Tests:** `TC-027`
- **Business intent:** Actual qty received, partial inventory post, in-transit remainder.
- **Definition of Done:** 20 sent/18 received retains two pending. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-03 — Transfer discrepancy resolution
- **Module / Req:** `MOV` · `BR-MOV-02` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P1-MOV-02` · **Tests:** `TC-027`
- **Business intent:** Bilateral confirmation/resolution before COMPLETED.
- **Definition of Done:** No unaccounted transit quantity. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-04 — Internal interbranch balances
- **Module / Req:** `MOV` · `BR-MOV-03` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P1-MOV-02` · **Tests:** `TC-047`
- **Business intent:** Due-from/due-to branch mirrors at sender WAC; exclude entity consolidated external balance.
- **Definition of Done:** Consolidated internal offset zero. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-05 — Outside partner directory
- **Module / Req:** `MOV` · `BR-MOV-04` · **Estimate:** 8h · **Priority:** Should · **Dependency:** `P0-ORG-03` · **Tests:** `TC-029`
- **Business intent:** Outside same-brand shops are counterparties, not logged-in branches.
- **Definition of Done:** Partner has no system account. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-06 — Borrow-out and due balance
- **Module / Req:** `MOV` · `BR-MOV-04` · **Estimate:** 16h · **Priority:** Should · **Dependency:** `P1-MOV-05` · **Tests:** `TC-029`
- **Business intent:** Loan issues decrease stock, create quantity owed, no COGS.
- **Definition of Done:** Borrow 10 -> due 10. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-07 — Borrow-in and approved valuation
- **Module / Req:** `MOV` · `BR-MOV-06` · **Estimate:** 16h · **Priority:** Should · **Dependency:** `P1-MOV-05` · **Tests:** `TC-029`
- **Business intent:** Borrow receipts increase stock and in-kind liability with branch reference WAC.
- **Definition of Done:** Approved missing-price fallback recorded. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-08 — Partial loan returns
- **Module / Req:** `MOV` · `BR-MOV-05` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P1-MOV-06` · **Tests:** `TC-030`
- **Business intent:** Return line/link, in-kind outstanding update; Owner approves partner-return receipt.
- **Definition of Done:** 10 borrowed/4 returned => due 6; idempotent. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-09 — Loan valuation variance policy
- **Module / Req:** `MOV` · `BR-MOV-06` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P1-MOV-08` · **Tests:** `TC-055`
- **Business intent:** Compare receipt/return value, record authorized adjustment without reversing consumed COGS.
- **Definition of Done:** Cost difference not erased or double counted. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-MOV-10 — Raw material sales linkage
- **Module / Req:** `MOV` · `BR-REV-05` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P0-FIN-05` · **Tests:** `TC-052`
- **Business intent:** Stock sale reference and manually entered finance revenue with duplicate controls.
- **Definition of Done:** Separate sales do not duplicate beverage report. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-AST-01 — Equipment upkeep
- **Module / Req:** `AST` · `BR-AST-01` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P0-AST-01` · **Tests:** `TC-051`
- **Business intent:** Asset register with warranty, repair, transfer and branch owner.
- **Definition of Done:** Transfer preserves original price and history. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-AST-02 — Depreciation policy implementation
- **Module / Req:** `AST` · `BR-AST-02` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P1-AST-01` · **Tests:** `TC-054`
- **Business intent:** Approved useful life, monthly expense postings and retrospective impact policy.
- **Definition of Done:** P&L disclosure updated; rule reviewed before active. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-AST-03 — Recipe theoretical cost
- **Module / Req:** `AST` · `BR-AST-03` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P0-INV-01` · **Tests:** `TC-020`
- **Business intent:** Bill of materials by drink/size/topping with no stock posting.
- **Definition of Done:** Theoretical cost computed, not actual cups sold. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-FIN-01 — Entity tax and expense allocation
- **Module / Req:** `FIN` · `BR-FIN-05` · **Estimate:** 16h · **Priority:** Should · **Dependency:** `P0-FIN-07` · **Tests:** `TC-046`
- **Business intent:** Book common obligations at entity, allocate with signed rationale to branches.
- **Definition of Done:** Entity total equals sum branch allocations. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-FIN-02 — Interbranch elimination views
- **Module / Req:** `FIN` · `BR-MOV-03` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P1-MOV-04` · **Tests:** `TC-047`
- **Business intent:** Report due-from/due-to separately, exclude on entity consolidated report.
- **Definition of Done:** No external receivable inflation. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-FIN-03 — Advanced cash control
- **Module / Req:** `FIN` · `BR-FIN-06` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P0-FIN-06` · **Tests:** `TC-007`
- **Business intent:** Account statements vs manually posted settlements, correction and variance review.
- **Definition of Done:** Reported app NET not auto bank cash. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-FIN-04 — Accounting review of distribution inputs
- **Module / Req:** `FIN` · `BR-CAP-04` · **Estimate:** 12h · **Priority:** Should · **Dependency:** `P1-AST-02` · **Tests:** `TC-035`
- **Business intent:** Config of eligible profit, depreciation/tax/liability checks and hard gate.
- **Definition of Done:** Owner warned/blocked pending compliance policy signoff. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-01 — Profit eligibility term versions
- **Module / Req:** `CAP` · `BR-CAP-02` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P0-CAP-03` · **Tests:** `TC-034`
- **Business intent:** Effective dating, conditions for partly paid promises and asset/work participation.
- **Definition of Done:** Profit share not derived solely from paid capital. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-02 — Owner delegation for distributions
- **Module / Req:** `CAP` · `BR-CAP-04` · **Estimate:** 10h · **Priority:** Should · **Dependency:** `P1-ORG-02` · **Tests:** `TC-053`
- **Business intent:** Validate admin-assigned authority and effective period/evidence.
- **Definition of Done:** Expired authority rejected. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-03 — Capital withdrawal workflow
- **Module / Req:** `CAP` · `BR-CAP-09` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P1-CAP-01` · **Tests:** `TC-039`
- **Business intent:** Request, Owner approval, actual return and next-period ratio effective change.
- **Definition of Done:** Old period terms immutable. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-04 — Earning-period entitlement snapshots
- **Module / Req:** `CAP` · `BR-CAP-03` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P1-CAP-01` · **Tests:** `TC-033`
- **Business intent:** Freeze investor ratio/terms for profit source period; include retained lots.
- **Definition of Done:** New investor not entitled to old retained by default. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-05 — Profit funds/liability gate
- **Module / Req:** `CAP` · `BR-CAP-04` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P1-FIN-04` · **Tests:** `TC-035`
- **Business intent:** Compute candidate from adjusted verified P&L, cash availability and obligations.
- **Definition of Done:** Insufficient funds -> no fake payment. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-06 — Distribution proposal and approval
- **Module / Req:** `CAP` · `BR-CAP-04` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P1-CAP-05` · **Tests:** `TC-034`
- **Business intent:** Owner proposes reserve, approves each beneficiary share and immutable lines.
- **Definition of Done:** Sum allocated consistent incl rounding residual. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-07 — Single-full payment per investor
- **Module / Req:** `CAP` · `BR-CAP-05` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P1-CAP-06` · **Tests:** `TC-036`
- **Business intent:** AWAITING_FUNDS -> PAYMENT_PENDING -> PAID exactly once for full beneficiary amount.
- **Definition of Done:** Partial cash not marked PAID; duplicate retry safe. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-08 — Retained profit ledger
- **Module / Req:** `CAP` · `BR-CAP-07` · **Estimate:** 16h · **Priority:** Should · **Dependency:** `P1-CAP-04` · **Tests:** `TC-033`
- **Business intent:** Track undistributed earning-period balances and later eligible distribution.
- **Definition of Done:** No automatic use of current investor ratios for old lot. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-09 — Withdrawal/recover/offset claims
- **Module / Req:** `CAP` · `BR-CAP-08` · **Estimate:** 16h · **Priority:** Should · **Dependency:** `P1-CAP-07` · **Tests:** `TC-038`
- **Business intent:** Approve claim/offset after reopen; preserve original successful payment.
- **Definition of Done:** Recovery never deletes prior money movement. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CAP-10 — Investor visibility portal
- **Module / Req:** `CAP` · `BR-ORG-05` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P1-ORG-01` · **Tests:** `TC-002`
- **Business intent:** Allowed per-branch report summaries, distribution statement and download scope.
- **Definition of Done:** Cannot cross-branch access through object ID. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CLS-01 — Reopen period approval workflow
- **Module / Req:** `CLS` · `BR-CLS-03` · **Estimate:** 14h · **Priority:** Should · **Dependency:** `P0-CLS-03` · **Tests:** `TC-041`
- **Business intent:** Owner request, delegated authority check, reason and new revision.
- **Definition of Done:** No direct edit of closed period. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CLS-02 — Recalculation dependency graph
- **Module / Req:** `CLS` · `BR-CLS-04` · **Estimate:** 18h · **Priority:** Should · **Dependency:** `P1-CLS-01` · **Tests:** `TC-042`
- **Business intent:** Run impact report over COGS, payroll, financial P&L, capital and distributions.
- **Definition of Done:** Affected later periods and payouts enumerated. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P1-CLS-03 — P1 release and audit rehearsal
- **Module / Req:** `CLS` · `BR-SEC-05` · **Estimate:** 20h · **Priority:** Should · **Dependency:** `P1-CAP-09` · **Tests:** `TC-038`
- **Business intent:** Run realistic scenario across branches, retained capital and loans; resolve compliance gates.
- **Definition of Done:** Finance signoff, data restore and negative authorization tests green. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use service-layer transaction plus unique request/source reference and concurrency tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

### P2 — Enhancements — optional AI / integrations and advanced analytics

#### P2-AI-01 — FastAPI separate service skeleton
- **Module / Req:** `AI` · `BR-AI-01` · **Estimate:** 10h · **Priority:** Could · **Dependency:** `P1-CLS-03` · **Tests:** `TC-049`
- **Business intent:** Python service with signed service-to-service auth, strict schemas and health.
- **Definition of Done:** No database write privileges to financial core. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P2-AI-02 — OCR invoice extract
- **Module / Req:** `AI` · `BR-AI-01` · **Estimate:** 18h · **Priority:** Could · **Dependency:** `P2-AI-01` · **Tests:** `TC-049`
- **Business intent:** Upload through private owner-controlled flow; return proposed item/amount/date only.
- **Definition of Done:** Review required before posting. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P2-AI-03 — OCR review screen
- **Module / Req:** `AI` · `BR-AI-01` · **Estimate:** 12h · **Priority:** Could · **Dependency:** `P2-AI-02` · **Tests:** `TC-049`
- **Business intent:** Human preview, map units/vendors and approve proposed fields.
- **Definition of Done:** OCR cannot directly create confirmed stock or expense. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P2-AI-04 — Spending anomaly rules
- **Module / Req:** `AI` · `BR-AI-02` · **Estimate:** 14h · **Priority:** Could · **Dependency:** `P2-AI-01` · **Tests:** `TC-049`
- **Business intent:** Explainable baseline on normalized reviewed expenses, non-blocking alert.
- **Definition of Done:** No auto-post or silent AI approval. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P2-AI-05 — Stock anomaly insight
- **Module / Req:** `AI` · `BR-AI-02` · **Estimate:** 12h · **Priority:** Could · **Dependency:** `P2-AI-04` · **Tests:** `TC-049`
- **Business intent:** Detect issue/count volatility with evidence and sample-size caution.
- **Definition of Done:** Human can inspect source movements. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P2-AI-06 — Demand forecasting prototype
- **Module / Req:** `AI` · `BR-AI-03` · **Estimate:** 18h · **Priority:** Could · **Dependency:** `P2-AI-01` · **Tests:** `TC-050`
- **Business intent:** Approved historical issues and exogenous variables only; no POS fiction.
- **Definition of Done:** Insufficient history returns warning, not precise forecast. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P2-AI-07 — Optional import connector
- **Module / Req:** `AI` · `BR-AI-03` · **Estimate:** 12h · **Priority:** Could · **Dependency:** `P2-AI-06` · **Tests:** `TC-050`
- **Business intent:** CSV mapping of external sales counts into analytics-only records.
- **Definition of Done:** Does not create POS or overwrite daily revenue. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

#### P2-AI-08 — AI security/evaluation gate
- **Module / Req:** `AI` · `BR-SEC-05` · **Estimate:** 16h · **Priority:** Could · **Dependency:** `P2-AI-07` · **Tests:** `TC-049`
- **Business intent:** Benchmark extraction accuracy, prompt injection, PII and cost ceilings.
- **Definition of Done:** All AI features remain advisory. Include authorization checks, validation, nonduplicated money/stock effects when applicable, frontend usability if relevant, and a short PR/demo evidence link.
- **Implementation guidance:** Use explicit module boundaries, validation and negative authorization tests. If affected by `POLICY_VERIFY` decisions, keep feature behind a release gate until signed off.

## 9. Test matrix (run these and record evidence in Tracker)
| ID | Phase | Type | Scenario | Expected result |
|---|---|---|---|---|
| TC-001 | P0 | Security | User from Branch A requests Branch B expense | 403/404 without data leak |
| TC-002 | P0 | Security | Investor with no branch grant requests payroll | Denied; report permission groups enforced |
| TC-003 | P0 | Integration | Branch bank account missing during go-live readiness | Readiness gate blocks activation |
| TC-004 | P0 | Integration | Open business day no report, attempt monthly close | Close rejected with missing date list |
| TC-005 | P0 | Integration | Closed day no revenue report, close | Allowed if other prerequisites met |
| TC-006 | P0 | Concurrency | Same daily report submitted twice | No duplicate active report or ledger post |
| TC-007 | P0 | Integration | Delivery app NET reported, no bank receipt | P&L includes declared net, bank cash remains unchanged |
| TC-008 | P0 | Integration | Unpaid 5m goods receipt | Inventory + payable, cash unchanged, COGS unchanged |
| TC-009 | P0 | Integration | Supplier debt paid later | Payable and cash reduced once, no extra expense |
| TC-010 | P0 | Integration | Transfer between branch cash and branch bank | No new revenue or P&L effect |
| TC-011 | P0 | Concurrency | Two workers confirm same cash posting | One ledger posting only |
| TC-012 | P0 | Integration | Cash count differs from ledger | Exception recorded; adjustment requires authorized posting |
| TC-013 | P0 | Unit | Rate increase mid-month, 40h old + 50h new | 40×old + 50×new, no repricing old hours |
| TC-014 | P0 | Unit | Monthly salary, unpaid leave and partial month | Uses configured standard vs payable days |
| TC-015 | P0 | Integration | Employee changes primary branch during period | Entire month cost allocated to end-of-month primary branch |
| TC-016 | P0 | Integration | Salary accrued November, paid December | Nov P&L expense; Dec cash out only |
| TC-017 | P0 | Security | Manager approves own payable hours | Denied; Owner required |
| TC-018 | P1 | Integration | GPS out-of-geofence check-in | Stored as anomaly; review required before paid-hours approval |
| TC-019 | P1 | Integration | Shift outside schedule without preapproval | Not eligible until authorized exception |
| TC-020 | P0 | Unit | 10kg@100k + 10kg@120k, issue 5kg | Per-branch WAC=110k, issue value=550k |
| TC-021 | P0 | Integration | Issue more than balance | Rejected without stock mutation |
| TC-022 | P0 | Concurrency | Two concurrent issues of last 5kg | At most 5kg issued, never negative |
| TC-023 | P0 | Unit | Supplier packs 1 box=24 units, receipt 2 boxes | 48 base units with traceable conversion snapshot |
| TC-024 | P0 | Integration | Excel opening stock import retried | Single approved opening balance |
| TC-025 | P1 | Integration | Expired lot used for brewing | Rejected; disposal route permitted |
| TC-026 | P1 | Security | Stock count discrepancy without Owner approval | No posted stock change |
| TC-027 | P1 | Integration | Transfer 20 units, receiver accepts 18 | 18 received, 2 in transit/variance; no vanish |
| TC-028 | P1 | Security | Transfer across separate legal entities using internal type | Rejected; loan/sale required |
| TC-029 | P1 | Integration | Outside partner borrows 10kg, returns 4kg | In-kind 6kg outstanding; Owner-approved receipt |
| TC-030 | P1 | Concurrency | Return submitted twice | Inventory and obligation updated once |
| TC-031 | P1 | Integration | Return item differs from loan item | Rejected unless separate approved agreement |
| TC-032 | P0 | Unit | Committal 100m, paid 60m | Committed=100m, actual=60m, outstanding=40m |
| TC-033 | P1 | Integration | New investor joins after prior earning month | Prior month retained entitlement not recalculated |
| TC-034 | P1 | Unit | Profit ratios differ from contribution ratios | Distribution follows effective profit terms |
| TC-035 | P1 | Integration | Approved investor payout lacks cash | AWAITING_FUNDS; no paid journal |
| TC-036 | P1 | Integration | Pay approved 10m with only 6m | Disallow partial PAID; single full-value payment required |
| TC-037 | P1 | Concurrency | Submit payout twice | One payment and ledger effect only |
| TC-038 | P1 | Integration | Reopen post-paid month, profit decreases | Approved recover/offset record; old transfer immutable |
| TC-039 | P1 | Integration | Investor rights from future period used to backdate | Denied; entitlement effective terms enforced |
| TC-040 | P0 | Integration | All OPEN dates reported but payroll draft | Close blocked |
| TC-041 | P0 | Security | Month closed and Manager edits revenue | Denied until approved reopen/correction |
| TC-042 | P1 | Integration | Approved reopen with cost change | Recompute dependent snapshot and impact register |
| TC-043 | P0 | Security | Signed evidence URL attempted by another branch | Denied or opaque not accessible |
| TC-044 | P0 | Operations | Restore backup on clean staging | Integrity and required records verified |
| TC-045 | P0 | Integration | Excel import has duplicate identifiers | Duplicate rows rejected with row-level reasons |
| TC-046 | P1 | Integration | Entity expense allocated A/B | Entity total unchanged; branch sum equals total |
| TC-047 | P1 | Integration | Internal A-B transfers consolidated | Due-to/from offset excluded from entity external balances |
| TC-048 | P1 | Unit | Shift ending 22:30 has night-hour component | Recorded accurately, policy warning shown |
| TC-049 | P2 | Integration | OCR returns wrong amount | Only a proposed draft, not a posted expense |
| TC-050 | P2 | Unit | Insufficient data for forecast | Warning and no false precise claim |
| TC-051 | P0 | Integration | Asset acquired in MVP | Register asset but P&L clearly excludes depreciation |
| TC-052 | P1 | Integration | Separate raw-material sale to partner | Outbound stock links to manually entered sale finance income exactly once, not daily drinks turnover |
| TC-053 | P1 | Security | Delegated Owner loses approval authority | Expired or revoked delegation rejects distribution/reopen approval without altering historic audit |
| TC-054 | P1 | Integration | Approved asset depreciation policy posts period expense | Approved policy changes monthly expense and P&L without retroactive silent alteration |
| TC-055 | P1 | Integration | Borrowed item returned at higher purchase price | Loan quantity reaches zero and approved value difference remains traceable |
| TC-056 | P1 | Unit | Leave plus branch meal/rounding policy applied | Approved leave mapping and payable minutes rounded under effective branch policy |

## 10. Decision register
`APPROVED` means user-agreed scope/business rule, not necessarily audited legal/accounting policy. `PROPOSED` means architecture or calculation detail still to verify. `POLICY_VERIFY` gates actual money movement or regulated claims.
| ID | Status | Phase | Decision | Rule |
|---|---|---|---|---|
| ADR-001 | APPROVED | P0 | No POS | Only daily aggregate sales; no customer orders, checkout or recipe auto-consumption. |
| ADR-002 | APPROVED | P0 | Mixed household-business topology | Model registered entities separately from branches; entity can own several branches. |
| ADR-003 | APPROVED | P0 | Separate investor agreements | Different people can invest in different branches; entitlement differs from capital ratio. |
| ADR-004 | APPROVED | P0 | Branch-local banking | Each branch must register dedicated bank account; do not assume bank account is a separate legal person. |
| ADR-005 | APPROVED | P0 | Revenue channel semantics | Daily cash/transfer values plus delivery-app NET by sale day, no app settlement scheduling. |
| ADR-006 | APPROVED | P0 | Day closed vs missing | CLOSED business day exempt from reporting; active day requires daily report. |
| ADR-007 | APPROVED | P0 | Accrual vs cash | Managerial P&L and actual cash positions are distinct reports. |
| ADR-008 | APPROVED | P0 | Per-branch moving WAC | Material valuation is moving weighted average within each branch. |
| ADR-009 | APPROVED | P0 | Opening balances | Onboarding uses actual physical count + Excel master/opening data; retains old transaction history externally. |
| ADR-010 | APPROVED | P0 | No negative stock | No confirmed inventory issue beyond available quantity. |
| ADR-011 | APPROVED | P0 | Manual issue + counts | Manual consumptions and periodic stock counts; Owner approves quantity-value corrections. |
| ADR-012 | APPROVED | P1 | Intra-entity transfer | Only same business entity; receiver confirms, reconciliation required for shortages. |
| ADR-013 | APPROVED | P1 | External partner loans | Partner no login; in-kind quantity balance; partial return; Owner approves receipt. |
| ADR-014 | APPROVED | P0 | Individual salary rates | Rates individual and branch-specific; effective-dated; cooks/managers may be monthly. |
| ADR-015 | APPROVED | P0 | Approved payable hours | GPS is evidence only; Manager approves paid hours; Manager own hours approved by Owner. |
| ADR-016 | APPROVED | P0 | Payroll date and channel | Branch-specific payday, cash/bank; no salary advances. |
| ADR-017 | APPROVED | P0 | Monthly close prerequisites | Every OPEN business day reported and payroll approved; Owner controls close and reopen. |
| ADR-018 | APPROVED | P0 | Capital is not turnover | Contributions, returned capital, investor profit settlements cannot be booked as retail revenue or operating expense. |
| ADR-019 | APPROVED | P1 | Profit payout atomic | Per beneficiary, approved amount must be settled in one full transaction; can await funds. |
| ADR-020 | APPROVED | P1 | Profit period snapshots | Prior period entitlement preserved when ownership changes; retained profit distributable later subject to review. |
| ADR-021 | APPROVED | P1 | Delegated owner | Admin delegates branch Owner, period + evidence, app permissions separate from legal authority. |
| ADR-022 | APPROVED | P0 | Tax manual | No automated tax calculation; manually entered due/payment, owner allocations across same entity. |
| ADR-023 | APPROVED | P0 | Asset depreciation staging | P0 tracks equipment but excludes depreciation; P1 includes subject to approved policy. |
| ADR-024 | APPROVED | P0 | Online responsive | Phone-first attendance/data entry, desktop reports; online-only in first release. |
| ADR-025 | PROPOSED | P0 | Stack and deployment | Java/Spring Boot modular monolith, PostgreSQL, React TypeScript, Flyway and Docker. |
| ADR-026 | PROPOSED | P1 | Stock backdating | Closed period and prior WAC values can be corrected only by controlled re-open or posted current adjustment, never silent edit. |
| ADR-027 | POLICY_VERIFY | P1 | Loans cost variance | Borrowed item uses receiving branch reference WAC or approved value; financial difference tracked on in-kind repayment. |
| ADR-028 | POLICY_VERIFY | P1 | Profit distribution compliance | Final distribution conditions must be verified against actual household agreement, tax and liabilities. |
| ADR-029 | POLICY_VERIFY | P1 | Labour compliance | Night work after 22:00, rest breaks, overtime and payroll deductions need legal-policy validation. |
| ADR-030 | PROPOSED | P0 | Delivery app cash posting | App net in daily report must not auto-post as bank funds; when actual funds arrive manually reconcile without duplicate sales. |
| ADR-031 | PROPOSED | P1 | Monetary rounding | Use Decimal NUMERIC, record amounts to 2 decimals; payout residual cent assignment deterministic. |

## 11. Risk register
| ID | Severity | Owner | Risk | Mitigation |
|---|---|---|---|---|
| R-001 | Critical | Business Owner | Household business / sharing rights: Distribution based on software figures may not match executed contract or mandatory obligations | Keep business entity, investor and delegated approver separate; verify legal agreement before financial go-live |
| R-002 | Critical | Owner/BA | Profit MVP provisional: No asset depreciation and possibly incomplete obligations, misleading profit for actual payouts | Clearly mark provisional; no payout in P0; approval gate before P1 payout |
| R-003 | High | Owner/BA | App net not cash settled: Reported revenue counted as bank cash before receipt | Keep separate reporting/ledger; explicit manual receipt and reconciliation |
| R-004 | High | Owner/BA | Opening-balance inaccuracies: First-month WAC, cash and profit all wrong | Count physically, Excel preview, dual review and sign-off |
| R-005 | Critical | Owner/BA | Backdated WAC: Editing posted inputs silently revalues historic consumption | Period locks, snapshots, controlled adjustment and impact report |
| R-006 | High | Owner/HR | Employee legal pay: 22:00-22:30 working hours, rest, deductions not compliant | Policy review; record timestamps, human alerts and signed contract policies |
| R-007 | Critical | Owner/BA | Investor entitlements after ratio change: Old retained profits incorrectly paid to newcomers | Snapshot effective terms per earning period and approved departure settlement |
| R-008 | High | Owner/BA | Inter-branch double counts: Intra-entity transfer inflates consolidated turnover or receivables | Separate internal due-to/due-from from third party, eliminate on entity consolidation |
| R-009 | High | Owner/BA | Borrowed item repayment valuation: Loan return different cost misstates profit or stock | Quantity contract, record valuation on both legs and approved difference policy |
| R-010 | Critical | Owner/BA | Unauthorized cross-branch access: Investor/employee sees other branch payroll or documents | Branch-scope guard, negative tests, private signed URLs |
| R-011 | High | Owner/BA | Payroll override / GPS spoof: Paid hours manipulated or self-approved | Approve payable hours, audit, GPS flags, two-person control for managers |
| R-012 | Critical | Owner/BA | Double posting due to retries: Duplicate payment, stock or distribution records | Idempotency key, unique source refs, row-level locking and concurrency tests |
| R-013 | Critical | Owner/BA | Data loss / file leak: Sensitive documents or balance data lost or public | Private storage, backups, tested restore, secret rotation |
| R-014 | High | Owner/BA | Excel dirty import: Duplicate employees/items and inconsistent base units | Mapping preview, validation, rejection log, import-dedupe |
| R-015 | High | Owner/BA | Manual tax obligations: Missing tax or improper period allocation | Tax register, entity-level allocation evidence and external accounting review |
| R-016 | High | Owner/BA | Scope growth: Too much V1 design delays use with real shop | P0 thin vertical slices, no POS and strict change control |
| R-017 | High | Owner/BA | Overstated balances from app: Actual cash and managerial revenue diverge without settlement history | Bank recon status and unknown-cash caveat |
| R-018 | High | Owner/BA | Single full investor payout: No partial allowed; failed/bank pending transaction could falsely mark paid | Payment state machine with failure/void/refund idempotency and actual receipt check |
| R-019 | Medium | Owner/BA | Receivable from raw-material sale double-booked: Ingredient sale appears both daily beverage report and finance receipt | External sale source ref + finance category, unique revenue linkage |
| R-020 | Medium | Owner/BA | Wrong branch cost for mobile employee: Salary charged to wrong branch after assignment change | Month-end primary branch snapshot and payroll review |

## 12. Local development and deployment runbooks
### Local environment checklist
1. Start Postgres with Compose with persistent volumes and credentials only in gitignored `.env`.
2. Apply *forward-only* Flyway migrations from scratch. The SQL Starter is a **design draft**, not `V001` ready to run.
3. Start Spring Boot and React, verify `/actuator/health` and login.
4. Seed **fictional** household entities, branches, staff, ingredients and investors; never commit real shop records.
5. Run unit and Testcontainers integration suite, include serial concurrent issues, payout double-submit and cross-branch access denials.
6. Verify no secret, GPS location, raw evidence or real investor bank data in logs.
7. Capture implementation evidence and link it to task rows in the tracker.
### Release and recovery checklist
- Automated tests, migration dry-run on restored staging snapshot, rollback to previous image (DB forward fix, never delete applied migration), encrypted backup and successful restore exercise.
- Server-side authorization at branch + row level, approval delegation verified.
- Day-close/payroll-close/profit-close integrity and cash reconciliation reports passed.
- Monitor failed posting, negative available quantity attempts, overdue inventory loans, unreconciled bank differences and expired evidence tokens.
- Human business owner signs the real branch pilot; before P1 payout, accounting/legal/HR rules signed by qualified parties.

## 13. Go-live acceptance checklists
**G3 MVP:** [ ] complete operating day calendar [ ] month revenue confirmed [ ] cash/bank reconciled [ ] payroll approved [ ] stock positive and WAC tested [ ] opening balances signed [ ] managerial P&L labeled provisional [ ] backup restored [ ] cross-branch test passes.
**G5 Profit distribution:** [ ] agreement reviewed [ ] legal decision authority documented [ ] tax/liability checks configured [ ] depreciation policy approved [ ] retained entitlements snapshotted [ ] no partial payment test [ ] post-payout reopen/recovery tested [ ] actual bank posting reconciled.
**User learning loop:** For each vertical slice: explain the business rule → implement failing test → build domain/app API → migration → web form → integration/e2e test → document design trade-offs as an ADR. Record only new knowledge into learning notes; do not rewrite generic OOP material each time.

---
**Linked deliverables:** `MTMS_Business_Spec_Roadmap_v1_0.docx` (policy narrative), `MTMS_Development_Tracker_v1_0.xlsx` (statuses and gates), `MTMS_ERD_Architecture_Mermaid_v1_0.md` (models and sequences), `MTMS_PostgreSQL_Schema_Starter_v1_0.sql` (schema draft).
