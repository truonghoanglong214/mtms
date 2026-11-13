# MTMS — Monthly NET Revenue & Manual Monthly Salary Expense Scope-Change Plan v1.1

- **Document status:** PROPOSED / REVIEW_ONLY
- **Owner decision date:** 2026-11-12
- **Current gate:** G0 NOT SIGNED
- **Parent change request:** `CHANGE-REQUEST-TBD`
- **Authorization:** Documentation change only; this document does not authorize code, migration, API/DB design, Tracker status changes, deployment, or implementation.
**Baseline position:** The Business Spec v1.0, Implementation Handbook v1.0, ERD v1.0, SQL Starter v1.0, and Tracker XLSX remain unchanged and authoritative until an approved change-control process publishes a replacement baseline.

## 1. Purpose and decision status

This proposal records the business-scope change selected by the Owner on 2026-11-12:

- replace daily revenue capture with one manually entered monthly aggregate NET beverage-revenue amount for each branch;
- replace attendance/payroll calculation with one manually entered monthly salary-expense amount for each employee;
- retain employee directory and branch assignment only as master data;
- use those monthly amounts, together with other approved costs, for month-end managerial P&L;
- preserve NO POS, provisional P&L, authorization, audit, revision, idempotency, privacy, and period-control safeguards.

The selected business direction is recorded as **APPROVED input to this proposal**, but this document as a whole remains **PROPOSED** because G0 is not signed and baseline BR/Task/TC/ADR records have not been changed.

## 2. Owner decision register — 2026-11-12

| Ref | Status | Owner-selected decision | Consequence for this proposal |
|---|---|---|---|
| `DEC-01 / 1A` | APPROVED | One aggregate NET amount for each branch and revenue month. | MTMS stores and reports one monthly amount, not an amount per source. |
| `DEC-02 / 2C` | APPROVED | There are multiple types of third-party sources; the source list is declared. | Expected-source checklist and source metadata are required, without source-level amounts. |
| `DEC-03 / 3B` | APPROVED | The monthly NET represents all beverage revenue. | The amount is aggregate beverage revenue, but it does not introduce POS/order-level scope. |
| `DEC-04 / 4B` | APPROVED | NET deductions are defined per source; MTMS does not calculate gross-to-NET. | Source-specific definitions are metadata/evidence used to validate the aggregate input. |
| `DEC-05 / 5A` | APPROVED | Revenue month is the sale/service month. | Statement date, report-received date, or settlement date must not silently determine the revenue month. |
| `DEC-06 / 6A` | APPROVED | Actual settlement is completely out of current scope. | Monthly revenue creates no cash/bank entry, receivable, or settlement state. |
| `DEC-07 / 7A` | APPROVED | The same authorized person may enter and directly confirm a monthly amount. | Owner-selected exception conflicts with the current no-self-approval guard; see `GAP-01 — BLOCKED_POLICY_CONFLICT`. |
| `DEC-08 / 8A` | APPROVED | One total salary expense for each employee/month; no components and no payment management. | MTMS neither calculates nor pays salary and does not track salary payable/paid balances. |
| `DEC-09 / 9A` | APPROVED | Employee is master data only, with no login or self-service. | Remove employee attendance, schedule, leave, payslip, and self-service flows. |
| `DEC-10 / 10C` | APPROVED | Entire monthly employee expense belongs to the primary branch designated by Owner. | No automatic split across branches and no end-of-month inference without Owner designation. |
| `DEC-11 / 11B` | APPROVED | Zero requires a reason; missing data blocks close; negative amounts are rejected; correction uses a separate flow. | Applies to monthly revenue and monthly salary expense, subject to the specific completeness rules below. |
| `DEC-12 / 12C` | APPROVED | A CLOSED month may use approved reopen or current-period adjustment depending on error type. | The selection rule is not decided and remains POLICY_VERIFY; see `GAP-02`. |
| `DEC-13 / 13C` | APPROVED | Before close, Owner confirms expected-employee and expected-source checklists. | Missing checklist items or monthly values block close. |
| `DEC-14 / 14B` | APPROVED | Exact decimal scale is 2. | Monetary amounts use exact decimal semantics with two fractional digits; no float or silent extra precision. |

## 3. Scope boundary

### 3.1 In scope

1. Employee directory and Owner-designated primary-branch assignment needed for master data and monthly salary-expense attribution.
2. A declared list of third-party revenue sources and a monthly expected-source checklist for each branch.
3. Source-specific descriptions of what each source means by NET, including the categories of deductions reflected in that source's figure.
4. One monthly aggregate NET beverage-revenue amount for each branch and sale/service month.
5. One monthly salary-expense amount for each employee and service month, attributed entirely to the Owner-designated primary branch.
6. Direct confirmation by the same authorized person, subject to the unresolved baseline policy conflict in `GAP-01`.
7. Owner confirmation of expected-source and expected-employee checklists before close.
8. Exact-decimal amounts at scale 2, zero reasons, missing-data blockers, rejection of negative amounts, revisions, separate correction flow, audit, and idempotency.
9. Month-end managerial P&L that uses approved monthly revenue and salary expense together with other approved P&L components.
10. Controlled correction for OPEN and CLOSED periods, without overwriting prior approved history.

### 3.2 Explicitly out of scope

- Attendance and timekeeping of every kind.
- GPS, geofence, check-in/check-out, location evidence, or location retention.
- Work shifts, work schedules, shift assignments, shift swaps, or leave requests.
- Payable hours/days, standard workdays, meal-time rules, rounding of time, overtime, or night work.
- Hourly, daily, or monthly salary-rate policies; rate histories; effective rates; default rates; prorating; or calculation from hours, days, rates, shifts, or leave.
- Salary components such as bonus, deduction, allowance, insurance, tax, or reimbursement as separately managed fields. MTMS receives one total monthly salary-expense amount only.
- Salary approval as a payroll process, payslips, salary liabilities, payday, salary payments, amount paid/unpaid, salary cash/bank posting, or salary reconciliation.
- Employee login, employee portal, or employee self-service.
- Automatic integration with attendance, payroll, delivery-platform, accounting, settlement, or banking systems.
- Actual third-party settlement, settlement scheduling, receivables created from revenue, cash collection, or bank posting.
- Gross-to-NET calculation in MTMS.
- POS, customer accounts, order/cup/cart/checkout data, individual customer payment, or automatic recipe/ingredient deduction.
- Real investor payout before G5 specialist verification.

## 4. Monthly aggregate NET beverage revenue

### 4.1 Business meaning

For each branch and sale/service month, MTMS records one aggregate NET amount representing all beverage revenue for that branch/month.

“All beverage revenue” is a managerial monthly total. It does **not** mean MTMS records orders, cups, carts, checkout transactions, individual customers, payment methods, or recipe consumption. The NO POS boundary remains unchanged.

### 4.2 Multiple sources without source-level amounts

Although there may be multiple third-party source types, MTMS records only one aggregate monthly amount. For each branch/month:

- Owner maintains or confirms the list of expected third-party sources.
- Each expected source has a source-specific NET definition and supporting metadata/evidence.
- The monthly checklist records whether each expected source was considered in preparing the aggregate, or records an approved reason when a source is not applicable.
- MTMS does **not** store or derive an amount for each source.
- MTMS does **not** sum source amounts to calculate the aggregate.
- The person entering the aggregate confirms that it covers the expected source checklist under the approved source definitions.

**Accepted trade-off:** choosing one aggregate amount loses source-level allocation, reconciliation, variance analysis, and proof that source amounts sum to the aggregate. The checklist and evidence can establish which sources were considered, but cannot reproduce or reconcile an amount per source. This proposal must not be interpreted as the previously considered “NET per source” option.

### 4.3 Source-specific NET definition

For each declared source type, business metadata/evidence must state which categories are already deducted from that source's figure. Examples may be listed only when supplied by Owner/source documentation; this proposal does not decide the deduction list.

MTMS uses the definitions to help the authorized person validate the externally prepared aggregate. MTMS must not calculate gross-to-NET, normalize different source definitions, or invent a deduction.

### 4.4 Revenue period and money separation

- Revenue belongs to the sale/service month chosen under the approved source evidence.
- The date on which a report is received does not silently change the revenue month.
- Settlement date does not determine the revenue month.
- Confirming monthly NET changes managerial revenue/P&L only.
- It creates no cash/bank entry, receivable, settlement event, or assumption that funds exist.
- If settlement is introduced later, it requires a separate Owner-approved change request and must not create beverage revenue a second time.

## 5. Manual monthly salary expense

### 5.1 Business meaning

For each employee and service month, MTMS records one total monthly salary-expense amount supplied by the authorized user. MTMS does not calculate or decompose the amount.

The amount:

- is an expense of the employee's service month;
- is not evidence of salary payment;
- creates no salary payable/paid balance;
- creates no cash/bank movement;
- contains no separately managed hours, days, rates, bonus, deduction, allowance, tax, insurance, or other payroll components;
- must not be derived from schedule, attendance, GPS, time, leave, or rate data.

### 5.2 Employee and branch semantics

- Employee records exist only as master data.
- Employees do not log in and have no self-service view.
- Owner designates one primary branch for the employee's monthly expense attribution.
- The entire employee/month amount belongs to that designated primary branch.
- MTMS does not automatically split the amount when an employee works at or moves between branches.
- A missing or ambiguous Owner designation prevents confirmation/close for that employee/month.

### 5.3 Entry and direct confirmation

The Owner selected a flow in which the same authorized person may enter and confirm the monthly amount directly. Entity/branch/action/row authorization still applies, and direct confirmation must remain fully auditable and idempotent.

This selection contradicts the current baseline guard against self-approval. It is therefore recorded as `GAP-01 — BLOCKED_POLICY_CONFLICT`. The proposal does not by itself make the exception operationally valid. Baseline change-control must explicitly state which old guard is superseded, the exact scope of the exception, and the required authorization before any implementation plan may proceed.

## 6. Amount, completeness, revision, and period rules

### 6.1 Amount rules

- Revenue and salary-expense amounts use exact decimal scale 2.
- Negative amounts are rejected in normal entry and confirmation flows.
- Zero is permitted only with a reason.
- A correction is not represented by silently overwriting the current amount or by entering an undocumented negative amount.
- Reuse of the same idempotency request with the same business intent must not create duplicate revenue, expense, revision, or close effect.

### 6.2 OPEN-month revisions

For an OPEN month, a correction to a confirmed amount creates a new traceable revision with actor, time, reason, and previous/current values. Prior confirmed history is retained and must not be silently overwritten.

### 6.3 CLOSED-month corrections

For a CLOSED month, direct editing is prohibited. The Owner selected two possible controlled routes:

1. approved reopen and revised close; or
2. approved current-period adjustment that references the original period.

The business rule that determines which error type uses which route remains `POLICY_VERIFY / GAP-02`. This proposal does not assign error categories, accounting treatment, sign conventions, or financial impact rules to either route.

### 6.4 Expected checklists and close readiness

Before close, Owner confirms for each branch/month:

- the expected third-party source checklist; and
- the expected employee checklist, including each employee's designated primary branch.

A branch/month is not ready to close when:

- an expected source is not confirmed as considered or explicitly marked not applicable with a reason;
- the one aggregate monthly NET amount is missing;
- the aggregate is zero without a reason;
- an expected employee has no confirmed monthly salary-expense amount;
- an employee amount is zero without a reason;
- an employee's primary-branch designation is missing or ambiguous;
- any required value is negative;
- required revisions/corrections are unresolved; or
- the direct-confirmation policy conflict has not been resolved for the intended release.

Close does not require attendance, timesheet, work schedule, leave, payable hours, rate policy, calculated payroll, salary payment, source-level revenue amounts, or settlement data.

## 7. Month-end P&L boundary

The managerial formula remains conceptually:

```text
Monthly aggregate NET beverage revenue
- Approved ingredient COGS for the period
- Confirmed manual monthly salary expense
- Approved operating expenses for the period
- Recorded tax/obligations for the period
- Approved corrections/adjustments under the selected period policy
= Managerial result — TẠM TÍNH / PROVISIONAL
```

Mandatory boundaries:

- Revenue is not cash, bank balance, settlement, capital contribution, or investor funds.
- Salary expense is not salary payment or proof that an employee was paid.
- P&L must always display **TẠM TÍNH / PROVISIONAL**.
- P0 P&L is not an independent basis for real profit distribution.
- No real investor payout before G5 specialist/legal/accounting verification and the existing capital controls.

## 8. Proposed BR–Task–TC–ADR traceability

All mappings in this section are **PROPOSED**. They do not update the Tracker or baseline documents.

| BR / decision | Proposed task impact | Acceptance focus | Proposed TC | ADR impact | Status / next signoff |
|---|---|---|---|---|---|
| `BR-REV-01…03` | Revise `P0-REV-02…04`, `P0-REV-06`; supersede revenue use of `P0-REV-01`. | One branch/month aggregate; sale/service month; expected-source checklist; no daily report requirement. | `TC-057`, `TC-058`, `TC-063` | Supersede/revise `ADR-005`, `ADR-006`; propose `ADR-032`. | PROPOSED — Owner baseline change-control. |
| `BR-REV-04` | Revise `P0-REV-05`, `P0-FIN-08`, `P1-FIN-03`. | Revenue confirmation has no cash/bank/settlement effect. | `TC-059` | Keep `ADR-007`; revise `ADR-030`; propose `ADR-034`. | PROPOSED — Owner + Finance. |
| `BR-REV-05` | Retain `P1-MOV-10` boundary. | Raw-material/non-beverage sale must not be silently included twice in beverage revenue. | Revise `TC-052`. | Keep NO-double-count principle. | PROPOSED — Owner clarifies any inclusion exceptions. |
| Proposed `BR-REV-06` | Revise monthly revenue entry/review tasks and FE subtasks. | One aggregate amount even with multiple declared sources; no source amounts. | `TC-057`, `TC-058`. | Proposed `ADR-032`. | PROPOSED — Owner accepts wording. |
| Proposed `BR-REV-07` | Revise finance/P&L/close tasks. | Revenue month is service month; settlement entirely out of scope. | `TC-059`. | Proposed `ADR-034`. | PROPOSED — Owner + Finance. |
| `BR-HR-01`, `02`, `04`, `06…10` | Supersede the payroll, attendance, schedule, leave, GPS, rate, payment tasks listed in section 9. | No time/rate/schedule/leave/payroll/payment behavior remains. | Supersede `TC-013`, `014`, `017`, `018`, `019`, `048`, `056`; replace old meaning of `TC-016`. | Supersede `ADR-014…016`; revise `ADR-024`; remove `ADR-029` as a software-feature gate for removed scope. | PROPOSED — Owner baseline change-control. |
| `BR-HR-03` | Revise `P0-HR-01`; replace `P0-HR-09` semantics. | Employee master; Owner-designated primary branch receives entire employee/month expense. | `TC-061`. | Proposed `ADR-033`. | PROPOSED — Owner accepts wording. |
| `BR-HR-05` | Replace payroll approval task with direct monthly expense confirmation/correction task. | Same authorized person may enter/confirm, but conflict remains blocked pending explicit supersede. | `TC-062`, `TC-064`. | Proposed `ADR-035`. | GAP — `BLOCKED_POLICY_CONFLICT`. |
| Proposed `BR-HR-11` | Proposed `P0-HR-10`. | Employee directory/primary branch only; no employee login. | `TC-061`, `TC-065`. | Proposed `ADR-033`. | PROPOSED — Owner baseline change-control. |
| Proposed `BR-HR-12` | Proposed `P0-HR-11`. | One total salary expense/employee/service month; no components/payment. | `TC-060`, `TC-062`, `TC-063`. | Proposed `ADR-033`, `ADR-035`. | PROPOSED; policy conflict noted. |
| `BR-FIN-01`, `BR-FIN-03` | Revise `P0-CLS-02`, `P0-FIN-08`. | P&L uses monthly aggregate revenue and salary expense; both remain separate from cash. | `TC-059`, `TC-060`, `TC-066`. | Keep `ADR-007`; proposed `ADR-033/034`. | PROPOSED — Owner + Finance. |
| `BR-CLS-01…04` | Revise `P0-CLS-01…05`, `P1-CLS-01…03`. | Owner-confirmed expected checklists; missing/zero/negative rules; controlled CLOSED corrections. | `TC-064`, `TC-066`, `TC-067`. | Revise `ADR-017`; propose `ADR-036`. | POLICY_VERIFY for route-selection rule. |
| `BR-SEC-01`, `BR-SEC-03` | Keep/revise `P0-SEC-01…03`. | Entity/branch/action/row authorization, private salary data, audit, revision, idempotency. | `TC-063`, `TC-065`. | Existing security decisions remain. | PROPOSED mapping; security controls retained. |
| `ADR-001` | No task expansion. | NO POS, no order/cup/cart/checkout, no automatic stock deduction. | Preserve relevant NO-POS tests; add assertion to `TC-057`. | Keep `ADR-001`. | APPROVED boundary; G0 still unsigned. |
| `BR-CAP-10`, `ADR-028` | No scope reduction. | P&L remains provisional; no real payout before G5. | Existing G5 tests remain. | Keep POLICY_VERIFY. | POLICY_VERIFY — qualified specialist at G5. |

## 9. Proposed task and frontend impact

### 9.1 Supersede/remove from implementation scope

The following tasks remain unchanged in the current Tracker until authorized change-control, but this proposal recommends marking them `SCOPE REMOVED/SUPERSEDED`, not `Done`:

- `P0-HR-02` — effective salary policies.
- `P0-HR-03` — approved hours input.
- `P0-HR-04` — paid-time review.
- `P0-HR-05` — payroll calculation engine.
- `P0-HR-06` — payroll approval/snapshot under old payroll semantics.
- `P0-HR-07` — payroll payment record.
- `P0-HR-08` — payroll statement/employee view.
- `P0-HR-09` — end-of-month primary-branch payroll allocation under the old inferred rule.
- `P1-HR-01` — work schedule model.
- `P1-HR-02` — shift swap and leave workflow.
- `P1-HR-03` — GPS attendance.
- `P1-HR-04` — attendance correction.
- `P1-HR-05` — leave/proration policy.
- `P1-HR-06` — night-work flags.
- `P1-HR-07` — payroll correction under old payroll semantics.
- Revenue-close use of `P0-REV-01` — daily OPEN/CLOSED business calendar prerequisite.

Affected FE subtasks for schedule, leave, GPS, attendance, rate policy, payable hours, payroll preview, salary payment, payslip, and employee self-service are likewise proposed for supersede. No employee-facing replacement screen is proposed because employees do not log in.

### 9.2 Revise existing tasks

| Task group | Proposed business revision |
|---|---|
| `P0-HR-01` | Employee directory plus Owner-designated primary branch only. |
| `P0-REV-02…06` | Monthly aggregate NET entry, checklist/evidence, confirmation, revision, and no-money boundary. |
| `P0-FIN-08`, `P1-FIN-03` | Show revenue separately from cash/bank; settlement remains out of scope rather than a reconciliation feature. |
| `P0-CLS-01…05` | Expected-source/employee checklist, monthly values, provisional P&L, close snapshot, and release evidence. |
| `P1-CLS-01…03` | Controlled reopen/current-period adjustment impact without inventing the route-selection policy. |
| `P0-SEC-01…03` | Private salary data/evidence, audit, revision, authorization, and idempotency for the new monthly records. |

### 9.3 Proposed new tasks

| Proposed task | Parent | Business result | Status |
|---|---|---|---|
| `P0-HR-10` | `CHANGE-REQUEST-TBD`, proposed `BR-HR-11` | Employee master and Owner-designated primary branch, without login/scheduling/attendance. | PROPOSED |
| `P0-HR-11` | `CHANGE-REQUEST-TBD`, proposed `BR-HR-12` | Manual employee/month salary-expense entry, direct confirmation, revision, privacy, and close completeness; `GAP-01` blocks release until resolved. | PROPOSED |
| `P0-REV-07` | `CHANGE-REQUEST-TBD`, proposed `BR-REV-06/07` | Source list/NET definitions/expected checklist plus one aggregate branch/month amount. | PROPOSED |
| `P0-CLS-06` | `CHANGE-REQUEST-TBD`, proposed `BR-CLS-05` | Owner checklist confirmation and new close blockers. | PROPOSED |
| `P1-CLS-04` | `CHANGE-REQUEST-TBD`, `BR-CLS-03/04` | Controlled CLOSED correction route after policy selection. | POLICY_VERIFY |

Task IDs are placeholders until Tracker change-control approves them. No task status is changed by this document.

## 10. Proposed test impact

### 10.1 Supersede or rewrite existing tests

- Supersede old payroll/attendance tests: `TC-013`, `TC-014`, `TC-017`, `TC-018`, `TC-019`, `TC-048`, `TC-056`.
- Rewrite `TC-015` for Owner-designated primary-branch attribution.
- Rewrite `TC-016` to prove salary expense belongs to service month and has no payment/cash effect.
- Replace daily-revenue close semantics in `TC-004`, `TC-005`, `TC-006`, and `TC-040` with monthly aggregate/checklist semantics.
- Keep and adapt `TC-007`, `TC-041`, and `TC-042` for no-bank effect, closed-period protection, and controlled correction impact.
- Keep branch security, evidence privacy, audit, backup, and idempotency tests where their underlying controls remain applicable.

### 10.2 Proposed new tests

| TC | Scenario | Expected business result | Status |
|---|---|---|---|
| `TC-057` | Enter monthly beverage revenue for one branch/month while several expected sources exist. | Exactly one aggregate amount is current; no source-level amount is stored or calculated; NO POS data is requested. | PROPOSED |
| `TC-058` | One expected source lacks checklist confirmation or not-applicable reason. | Close is blocked even though the aggregate amount exists. | PROPOSED |
| `TC-059` | Confirm monthly NET for a sale/service month without settlement information. | P&L revenue changes once; cash, bank, receivable, and settlement state do not change. | PROPOSED |
| `TC-060` | Confirm one salary-expense total for an employee/service month. | Expense belongs to the service month and designated primary branch; no component, payable, payment, cash, or bank state is created. | PROPOSED |
| `TC-061` | Employee changes operational location but Owner designates one primary branch for the month. | Entire employee/month expense is attributed once to the designated branch; no automatic split. | PROPOSED |
| `TC-062` | Same authorized actor enters and confirms a salary amount. | Until `GAP-01` is resolved, release is blocked; after explicit policy supersede, action is audited and scope-authorized. | GAP |
| `TC-063` | Retry the same monthly revenue or salary request. | One business effect only; conflicting reuse is rejected; exact decimal scale 2 is preserved. | PROPOSED |
| `TC-064` | Zero, missing, or negative monthly value is submitted/closed. | Zero requires reason; missing blocks close; negative is rejected; correction uses separate flow. | PROPOSED |
| `TC-065` | Unauthorized branch user or investor attempts to read/write salary detail. | Access is denied without row/count/evidence leakage; salary data remains private. | PROPOSED |
| `TC-066` | Close a complete branch/month. | Owner-confirmed expected-source and expected-employee checklists plus all required monthly values allow close; result is labeled TẠM TÍNH / PROVISIONAL. | PROPOSED |
| `TC-067` | Correct a CLOSED month. | Direct edit is rejected; only approved reopen or approved current-period adjustment is possible after the selection policy is verified. | POLICY_VERIFY |

## 11. Proposed Gherkin Acceptance Criteria

### AC-REV-01 — One aggregate amount, multiple source checklist

```gherkin
Given a branch and sale/service month have an Owner-confirmed expected-source list
When an authorized actor confirms monthly beverage revenue
Then exactly one aggregate NET amount is current for that branch and month
And no amount is recorded or calculated per source
And the record references the expected-source checklist and approved NET-definition evidence
```

### AC-REV-02 — NO POS boundary

```gherkin
Given monthly NET represents all beverage revenue
When the amount is entered, reviewed, reported, or closed
Then MTMS does not require or create an order, cup, cart, checkout, customer-payment, or recipe-consumption record
```

### AC-REV-03 — Sale/service month and no settlement

```gherkin
Given a monthly NET report is received or could be settled in a later month
When revenue is confirmed for its approved sale/service month
Then managerial revenue belongs to that sale/service month
And cash, bank, receivable, and settlement state remain unchanged
```

### AC-REV-04 — NET-definition evidence

```gherkin
Given several declared third-party source types use different NET definitions
When the aggregate monthly amount is confirmed
Then each expected source has an approved definition/evidence reference or a not-applicable reason
And MTMS does not calculate gross-to-NET or invent a deduction
```

### AC-HR-01 — Employee master only

```gherkin
Given an employee exists in MTMS
When the employee record is used in the monthly process
Then it provides master identity and Owner-designated primary-branch information only
And the employee has no MTMS login, self-service, schedule, leave, GPS, attendance, payable-time, rate, or payroll-calculation flow
```

### AC-HR-02 — One manual monthly salary expense

```gherkin
Given an expected employee and service month
When an authorized actor confirms the employee's monthly salary expense
Then one exact-decimal total at scale 2 is used as the expense for that employee and month
And the entire amount belongs to the Owner-designated primary branch
And no hours, days, rates, components, payable, payment, cash, or bank entry is created
```

### AC-HR-03 — Direct confirmation conflict guard

```gherkin
Given the Owner selected the same authorized actor to enter and confirm an amount
When the flow is evaluated before implementation or release
Then it remains blocked while GAP-01 conflicts with the current no-self-approval baseline
And the proposal alone is not treated as evidence that the exception is operationally valid
```

### AC-AUTH-01 — Scoped authorization and salary privacy

```gherkin
Given an actor lacks the required entity, branch, action, or row scope
When the actor attempts to view, enter, confirm, revise, export, or access evidence for monthly salary data
Then access is denied without leaking rows, counts, amounts, employee identity, or evidence
```

### AC-DATA-01 — Zero, missing, negative, and duplicate handling

```gherkin
Given a monthly revenue or salary-expense value is being submitted or checked for close
When the value is zero, missing, negative, or a retry
Then zero requires a reason
And missing required data blocks close
And a negative amount is rejected
And an idempotent retry creates only one business effect
```

### AC-REVISION-01 — OPEN-month revision

```gherkin
Given a confirmed monthly amount belongs to an OPEN month
When an authorized correction is made
Then a new revision records actor, time, reason, prior value, and current value
And the prior confirmed history is not overwritten
```

### AC-REVISION-02 — CLOSED-month correction

```gherkin
Given a monthly amount belongs to a CLOSED month
When a correction is requested
Then direct edit is rejected
And the correction uses either approved reopen or approved current-period adjustment
And the route is not selected until the GAP/POLICY_VERIFY rule for the error type is approved
```

### AC-CLOSE-01 — Owner-confirmed completeness

```gherkin
Given Owner has confirmed expected sources and expected employees for a branch and month
When close readiness is evaluated
Then every expected source is confirmed considered or has a reasoned not-applicable status
And one valid aggregate NET amount exists
And every expected employee has one valid monthly expense and a designated primary branch
And unresolved missing, zero-without-reason, negative, or correction items block close
```

### AC-PL-01 — Provisional P&L

```gherkin
Given a monthly P&L is viewed, exported, or closed
When monthly revenue and approved costs are presented
Then the report prominently displays TẠM TÍNH / PROVISIONAL
And it does not authorize a real investor payout before G5
```

### AC-AUDIT-01 — Audit without sensitive leakage

```gherkin
Given a monthly revenue, salary-expense, checklist, revision, correction, or close action occurs
When the action completes or is rejected
Then audit captures actor, authorized scope, event time, business month, action, reason, revision, and correlation/idempotency reference
And sensitive salary payload is not exposed to unauthorized logs or users
```

## 12. GAP and POLICY_VERIFY register

| ID | Status | Open conflict/question | Required decision/signoff |
|---|---|---|---|
| `GAP-01` | GAP — `BLOCKED_POLICY_CONFLICT` | `DEC-07` allows the same person to enter and confirm, while the current baseline contains no-self-approval controls. | Owner must explicitly supersede or narrow the old guard through change-control and state the authorized roles/scope. Tech Lead/QA must preserve audit and negative authorization coverage. |
| `GAP-02` | POLICY_VERIFY | `DEC-12` allows reopen and current-period adjustment, but no rule assigns error types to routes. | Owner + qualified Finance/Accounting adviser must approve route-selection, period, sign, impact, and evidence rules before Architect design. |
| `GAP-03` | GAP | Source-specific NET deduction definitions have not been enumerated in this proposal. | Owner/Finance must supply the approved source list and definition/evidence for each source before G0 signoff. |
| `GAP-04` | GAP | One aggregate amount cannot be reconciled or allocated by source. | Owner must accept this documented loss of source-level control; Architect must not silently add source amounts. |
| `GAP-05` | GAP | The authority that designates each employee's primary branch and its effective month needs operational wording. | Owner must define who can designate/change it and how a disputed or late change affects close/correction. |
| `GAP-06` | GAP | Expected-source and expected-employee checklist preparation/confirmation timing is not fully described. | Owner must define who prepares the checklist and when it is frozen for close; this proposal fixes only that Owner confirms it. |
| `GAP-07` | GAP | Existing BR/Task/TC/ADR/Gate estimates and dependencies still describe daily revenue, attendance, schedules, and payroll. | Tech Lead must prepare a proposed re-baseline after Owner accepts this plan; Tracker must remain unchanged until authorized. |
| `GAP-08` | GAP | `BR-FIN-08` opening-balance ownership remains unresolved from readiness review. | Owner + Finance decision remains required for G0; this change does not resolve it. |
| `GAP-09` | POLICY_VERIFY | `BR-CAP-10`/G5 external agreement and payout verification remain unresolved. | Qualified specialist signoff remains required before real payout. |

## 13. Business impacts and questions for Architect after Owner acceptance

This BA proposal intentionally contains no ERD, SQL, migration, API path, DTO, index, constraint, or technology design. After Owner accepts the business plan and resolves the blocking policy questions, Architect must address only the following business impacts/questions:

1. How will the solution enforce one current aggregate revenue result per branch/service month without creating source-level amounts?
2. How will source list, NET-definition evidence, and expected-source checklist remain traceable to the aggregate revision?
3. How will one employee/month amount be attributed to the Owner-designated primary branch without automatic split or implicit end-of-month inference?
4. How will entity/branch/action/row authorization prevent cross-branch reads, writes, counts, exports, and evidence leaks?
5. How will direct confirmation be blocked or enabled according to the final `GAP-01` change-control decision?
6. How will exact decimal scale 2, zero reason, negative rejection, missing-data blocking, idempotency, and revisions be enforced consistently?
7. How will OPEN revisions preserve history and CLOSED corrections support the selected route without overwriting prior close snapshots?
8. How will managerial P&L consume revenue and salary expense without producing cash/bank/settlement/payroll-payment effects?
9. How will private salary evidence and audit metadata remain accessible only to authorized roles without logging sensitive values?
10. Which existing proposed technical elements become obsolete after the business baseline is formally changed?

Architect must not answer `GAP-01`, `GAP-02`, source NET definitions, or checklist ownership by inventing business policy.

## 14. Baseline and file disposition

| Artifact | Disposition in this authorization |
|---|---|
| `MTMS_HR_SCOPE_CHANGE_PROPOSAL_v1_1.md` | **MODIFY** — this proposal only. |
| Business Spec v1.0 | **KEEP** — unchanged; do not overwrite. |
| Implementation Handbook v1.0 | **KEEP** — unchanged. |
| ERD v1.0 | **KEEP** — unchanged. |
| SQL Starter v1.0 | **KEEP** — unchanged; never execute as migration. |
| Tracker XLSX | **KEEP** — unchanged; no task/gate/status update. |
| Any other file | **KEEP** — unchanged. |
| Deleted files | **NONE**. |

## 15. Review and execution status

- **Document:** PROPOSED
- **Mode:** REVIEW_ONLY
- **Current gate:** G0 NOT SIGNED
- **Implementation authorization:** NONE
- **Tracker status changes:** NONE
- **Actual tests:** NOT RUN
- **Test result:** NOT CLAIMED
- **Files changed by this authorization:** `MTMS-docs/MTMS_HR_SCOPE_CHANGE_PROPOSAL_v1_1.md` only
- **Files deleted:** NONE

## 16. Change log

| Date | Version | Change | File action | Other artifacts |
|---|---|---|---|---|
| 2026-11-12 | v1.1 | Replaced the earlier HR-only draft with a consistent monthly aggregate NET revenue + manual monthly salary-expense scope-change plan. Removed scheduling/leave and all time/rate/payroll/payment scope; recorded Owner decisions 1A–14B; documented the aggregate-with-multiple-source trade-off; added BR–Task–TC–ADR impact, close criteria, Gherkin acceptance, policy conflicts, GAPs, and Architect business-impact questions. | **MODIFY:** `MTMS_HR_SCOPE_CHANGE_PROPOSAL_v1_1.md` | **KEEP:** Business Spec, Handbook, ERD, SQL Starter, Tracker, and all other files. **DELETE:** none. |
