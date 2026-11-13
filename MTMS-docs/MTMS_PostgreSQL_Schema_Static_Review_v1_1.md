# MTMS PostgreSQL Schema Static Review v1.1

> **PROPOSED / REVIEW_ONLY / NOT AN EXECUTABLE MIGRATION**  
> Review date: 2026-11-12 · Parent: `CHANGE-REQUEST-TBD` · Gate: **G0 NOT SIGNED**  
> Review target: v1.0 conceptual starter versus the proposed v1.1 architecture/ERD/schema. No database was connected and no SQL was executed.

## 1. Conclusion

| Decision | Result |
|---|---|
| `SCHEMA_READY` | **NO / BLOCKED** |
| PostgreSQL execution | **NOT RUN** |
| Flyway/migration | **NOT CREATED / NOT RUN** |
| Production readiness | **NOT CLAIMED** |
| G0 | **NOT SIGNED** |

The proposed v1.1 starter corrects the reviewed textual defects at conceptual level, but readiness remains blocked by `GAP-01…09`, especially the confirmation-policy conflict and CLOSED-correction route policy. PostgreSQL grammar, DDL bootstrap, privilege-based immutability, concurrency and integration behavior have not been tested. This file is not approval to run the starter.

## 2. Sources reviewed

- `AGENTS.md`, `docs/ai/START_HERE.md`, `docs/planning/CURRENT_STATE.md`, `docs/ai/HOW_TO_ADD_FILES.md`.
- Entire `MTMS_HR_SCOPE_CHANGE_PROPOSAL_v1_1.md`.
- Business Spec v1.0 BR/ADR statements relevant to revenue, HR, finance, close, security and capital.
- Entire Implementation Handbook v1.0, ERD v1.0 and PostgreSQL Schema Starter v1.0.
- Proposed v1.1 Project Architecture, ERD and SQL starter created under this documentation authorization.

Baseline v1.0 remains unchanged and authoritative until formal change-control publishes a replacement. The v1.1 documents are review proposals only.

## 3. Finding register

| ID | Severity | Evidence in v1.0 | Current defect/risk | Proposed v1.1 correction | Status |
|---|---|---|---|---|---|
| `SR-01` | CRITICAL | SQL v1.0 lines 168–219, 336–477, 524–619, 680–934 | Many rows carry `branch_id` beside a single-column FK to account, evidence, payable, employee, lot, counterparty, close or capital rows. IDs from another branch/entity can be combined unless every service check is perfect. | Carry `business_entity_id` and branch through parent unique keys and composite child FKs. The v1.1 revenue, salary, finance, stock, correction, snapshot and capital paths use composite scope keys. | Corrected conceptually; DB bootstrap not run. |
| `SR-02` | HIGH | SQL v1.0 lines 36–47, 336–358, 725–739 | `branch_code`, `staff_code` and `asset_code` are globally unique even though their natural administrative scope is entity/branch. Global scope was not an approved business rule. | v1.1 uses entity-scoped branch/staff code and branch-scoped asset code. Any future choice of truly global codes requires an explicit ADR. | Corrected conceptually. |
| `SR-03` | HIGH | SQL v1.0 line 338 | `employees.user_id` makes an employee optionally become a login identity, contrary to `DEC-09` and AC-HR-01. | v1.1 employee master contains no `user_id`, credentials, session or self-service relationship. | Corrected conceptually. |
| `SR-04` | CRITICAL | SQL v1.0 lines 125–186; ERD v1.0 sections 4 and 9 | Daily report stores cash, bank transfer and delivery-app amounts and can emit ledger postings. Under v1.1 this would duplicate/misstate the sole monthly beverage revenue and violates the no-settlement/no-money contract. | Replace with one `monthly_revenue_revisions.aggregate_net_amount` per branch/service month. Source catalog, definitions and checklist items contain no amount. There is no revenue FK to cash, bank, receivable or settlement. | Corrected conceptually. |
| `SR-05` | HIGH | SQL v1.0 partial indexes at lines 155, 451 and 776 | v1.0 demonstrates revision/current uniqueness but only for daily revenue, payroll periods and close. State/current flags can still diverge without an atomic supersession transaction. | v1.1 adds `(scope, month, revision)` unique constraints and partial unique current indexes for source definition, source/employee checklist, designation, revenue, salary and close. Atomic lock/supersession remains service-enforced. | DB constraint proposed; concurrency test not run. |
| `SR-06` | CRITICAL | SQL v1.0 lines 760–809 | Close rows/components can be updated directly; `close_components.provenance JSONB` has no FK to exact revenue/payroll/stock/finance inputs. A recomputation could silently change history. | v1.1 uses close revision + snapshot + normalized typed input link tables for exact revenue, salary, expense, stock, tax and correction rows. Snapshot mutation must be denied by service and future DB privileges/triggers. | Partly DB-structured; immutability execution test remains open. |
| `SR-07` | CRITICAL | SQL v1.0 lines 73–101 and many single `evidence_file_id` FKs | Evidence entity and branch can disagree with the domain row. Object existence/count/key can leak unless every caller rechecks scope. | v1.1 evidence metadata has composite scope keys; revenue and salary evidence join tables enforce entity/branch matching. Downloads still require server-side action/row authorization and short-lived access. | Corrected conceptually; authorization tests not run. |
| `SR-08` | HIGH | SQL v1.0 lines 323–334 | Idempotency uniqueness is only `(actor_user_id,key)`. A key has no operation/entity/branch scope, while a different actor can duplicate a business effect. | v1.1 keys are unique by entity, actor, operation and key, with request hash and target metadata. Domain/source uniqueness remains independently required. | Corrected conceptually; retry/concurrency tests not run. |
| `SR-09` | BLOCKER | Handbook `HR-INV-02`, `P0-HR-06`, `TC-017`; proposal `DEC-07`, `GAP-01` | Owner-selected same-actor confirmation conflicts with baseline no-self-approval. Enforcing either side would invent/silently replace policy. | Store entered/confirmed actors neutrally, set `release_state=BLOCKED_POLICY_CONFLICT`, keep `release_blocked=TRUE`, and make `CONFIRMED` impossible while blocked. | **OPEN — GAP-01.** |
| `SR-10` | BLOCKER | Handbook section 5.5, `BR-CLS-03/04`, `ADR-026`; proposal `DEC-12`, `GAP-02` | Both reopen and current-period adjustment are named, but no approved rule assigns error types, signs, periods or impacts. | `correction_cases` has typed origin references and nullable `selected_route`; both route tables exist but their approval states are structurally blocked under `POLICY_VERIFY`. No error category is assigned. | **OPEN — GAP-02.** |
| `SR-11` | MEDIUM | SQL v1.0 lines 186–220 and final indexes 965–974 | The delayed expense→payable FK is valid in order, but manual split-migration could orphan late indexes/comments after scope removal. | v1.1 declares parents before children, uses one deliberate `ALTER TABLE` only after both audit/idempotency parents, and contains no index targeting removed HR/payroll tables. | Static order scan passed; PostgreSQL parse not run. |
| `SR-12` | HIGH | SQL v1.0 lines 359–489, 966–967 | Salary policies, shifts, attendance, leave, timesheets, payroll periods/lines/adjustments/payments and their indexes become orphan/forbidden under v1.1. | All are absent from v1.1. Employee master, designation and manual salary-expense revision are the only workforce financial structures. | Static absence scan passed. |
| `SR-13` | HIGH | `business_days`, `daily_revenue_reports`, `CLS-INV-01`, `BR-CLS-01`, `ADR-017` | Close depends on daily OPEN/CLOSED reports and payroll approval, both superseded by the new contract. | Close readiness depends on Owner-confirmed expected-source/employee checklists, one aggregate revenue revision, salary revisions/designations and retained P&L inputs. | Corrected conceptually; completeness transaction test not run. |
| `SR-14` | HIGH | SQL v1.0 lines 921–932 | One unique payment row per distribution line prevents retrying a failed attempt and does not by itself prove full-only success. | v1.1 separates payment attempts and deliberately provides no successful/paid state before G5. The final success uniqueness/full-value contract requires later authorized design. | **BLOCKED by G5 / BR-CAP-10.** |
| `SR-15` | MEDIUM | SQL v1.0 uses JSONB for import, audit, payroll snapshots, close provenance and capital snapshots | JSONB can hide relational invariants if used as the only authoritative source. | v1.1 limits JSONB to import staging/error payloads. Revenue, salary, checklist, evidence and close provenance use normalized relationships and exact amounts. | Corrected conceptually. |
| `SR-16` | BLOCKER | No PostgreSQL server/parser was used in this review | Text scans cannot prove PostgreSQL accepts every composite FK/constraint, nor that a clean bootstrap succeeds. | After G0 and task approval, split into reviewed Flyway migrations and run clean/repeat bootstrap with Testcontainers. Do not execute this starter as one migration. | **OPEN — execution forbidden before approval.** |

## 4. v1.1 invariant coverage

| Invariant | Proposed database control | Remaining service/transaction control |
|---|---|---|
| one current revenue per branch/month | unique revision + partial unique current index | lock old current, insert/supersede atomically |
| no source amount | no numeric amount column in source/definition/checklist tables | validate external aggregate coverage without fabricating reconciliation |
| revenue is not money | no revenue FK/contract to finance tables | forbid side-effect calls from revenue confirmation |
| exact scale 2/nonnegative/zero reason | `NUMERIC(20,2)`, `>=0`, conditional nonblank reason | reject extra input precision consistently at API boundary |
| employee has no login | no user FK in employee | do not expose employee auth/session endpoints |
| one salary expense/employee/month | unique revision + partial current index | current-row locking and status transition |
| entire salary goes to designated branch | composite designation FK includes employee/month/branch | designation authority and late/disputed change policy (`GAP-05`) |
| close completeness | typed checklist/input tables | set-completeness query under branch-period lock (`GAP-06`) |
| OPEN history retained | insert revisions; prior rows remain referenced | prohibit destructive update/delete paths |
| CLOSED immutable | snapshot and typed exact-source references | DB privileges/trigger plus service negative tests |
| correction traceable | typed origin source + original close + possible route tables | route/error/sign/impact decision (`GAP-02`) |
| evidence private | scoped evidence keys and link FKs | authorization, opaque 404/403 behavior, short-lived download |
| retry safe | scoped key/hash plus business unique constraints | replay/conflict behavior and concurrency tests |
| provisional P&L/no payout | required label; distribution successful state absent | UI/export label and G5 release configuration |

## 5. Orphan/removal review

The v1.1 starter has no `CREATE TABLE` or supporting index for:

- `daily_revenue_reports`;
- `salary_policies`;
- `work_shifts`, `shift_assignments`;
- `attendance_events`, `leave_requests`, `timesheet_entries`;
- `payroll_periods`, `payroll_lines`, `payroll_adjustments`, `payroll_payments`.

It also contains no source-level revenue amount, employee login link, revenue settlement, revenue receivable or salary payment state. Mentions in comments are explanatory removal guards, not schema objects.

## 6. Open GAP and policy register

| Gap | Status and schema posture |
|---|---|
| `GAP-01` | `BLOCKED_POLICY_CONFLICT`; confirmation/release disabled. |
| `GAP-02` | `POLICY_VERIFY`; both correction references modeled, neither can be approved. |
| `GAP-03` | Source-specific NET definitions not supplied; catalog supports revisions/evidence but invents no content. |
| `GAP-04` | Aggregate cannot reconcile by source; no source amount added. Owner acceptance still required. |
| `GAP-05` | Designation actor/authority wording unresolved; schema stores actor/reference and does not infer branch. |
| `GAP-06` | Checklist preparer/freeze timing unresolved; schema records both but service release remains blocked. |
| `GAP-07` | BR/Task/TC/ADR/Tracker re-baseline not authorized; no status changed. |
| `GAP-08` | `BR-FIN-08` unresolved; opening-balance batch has no posted/approved state. |
| `GAP-09` | `BR-CAP-10`/G5 unresolved; distribution has no successful state. |

## 7. BR–TC–ADR traceability

| Requirement/decision | Proposed impact | Verification target | Status |
|---|---|---|---|
| `BR-REV-01…04`, `ADR-005/006/030` | monthly aggregate, service month, no-money boundary | `TC-057…059`, `TC-063` | PROPOSED |
| proposed `BR-REV-06/07`, `ADR-032/034` | source metadata/checklist without amount | `TC-057/058` | PROPOSED |
| `BR-HR-01…10`, `ADR-014…016/029` | supersede time/rate/payroll/payment | absence scan; rewritten `TC-015/016` | PROPOSED supersession |
| `BR-HR-03`, proposed `BR-HR-11/12`, `ADR-033/035` | employee master, designation, manual salary expense | `TC-060…065` | PROPOSED; `TC-062` blocked |
| `BR-FIN-01/03`, `ADR-007` | accrual inputs separate from cash | `TC-059/060/066` | Retained/revised |
| `BR-CLS-01…04`, `ADR-017/026/036` | checklist close and controlled correction | `TC-064/066/067` | PROPOSED; route POLICY_VERIFY |
| `BR-SEC-01/03` | evidence privacy, audit, idempotency | `TC-063/065` | Retained/revised |
| `ADR-001` | NO POS | `TC-057` | Retained |
| `BR-CAP-10`, `ADR-028` | no real payout before G5 | existing G5 suite | POLICY_VERIFY |

## 8. ACTUAL TESTS / STATIC CHECKS

Only read-only/static checks were run.

| Command/check | Result | Exit code |
|---|---|---:|
| `python scripts\validate_workspace.py` | Workspace rules, six agents/eight skills/nine rules, baseline hashes and G0 safeguard passed. | `0` |
| PowerShell SQL dependency/order scanner over `MTMS_PostgreSQL_Schema_Starter_v1_1_PROPOSED.sql` | 78 tables; unresolved references: none; forward inline references: none; indexes without tables: none; non-comment parentheses `917/917`. | `0` |
| PowerShell v1.1 scope/invariant scanner | Removed-table creates: none; employee login link: false; source metadata amount: false; revenue money link: false; required scale/current/provisional/GAP markers present; duplicate table/index names: none. | `0` |
| PowerShell Markdown fence/status scanner | Architecture fences `6`, Mermaid blocks `2`; ERD fences `20`, Mermaid blocks `10`; fences balanced; PROPOSED/G0/review guards present. | `0` |
| `py -3 scripts\validate_workspace.py` | Not run: Windows `py` launcher is unavailable; the same script was run through `python`. | N/A |

Scanner logic was intentionally textual: it removed `--` comments before dependency/parenthesis checks, enumerated `CREATE TABLE`, `REFERENCES`, and `CREATE INDEX` targets, and inspected the relevant table bodies for forbidden columns/links. It is not a PostgreSQL grammar or runtime validator.

### DB EXECUTION

**NOT RUN.** No PostgreSQL connection, transaction, DDL, migration, Flyway, Testcontainers or real data operation was performed.

## 9. Required next approvals

1. Owner change-control resolves or explicitly supersedes the baseline for `GAP-01`.
2. Owner + qualified Finance/Accounting adviser resolves `GAP-02`.
3. Owner/Finance supplies source definitions and accepts the no-source-reconciliation trade-off.
4. Owner defines designation authority and checklist preparation/freeze timing.
5. G0 resolves `BR-FIN-08` and accepts the proposed architecture direction.
6. Only after G0 and a separately approved task plan may an Architect split an approved subset into Flyway migrations and QA run PostgreSQL/Testcontainers tests.

