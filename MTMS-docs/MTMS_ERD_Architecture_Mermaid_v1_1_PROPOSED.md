# MTMS ERD & System Architecture v1.1 — PROPOSED

> **PROPOSED / REVIEW_ONLY / NOT AN EXECUTABLE MIGRATION**  
> Date: 2026-11-12 · Parent: `CHANGE-REQUEST-TBD` · Gate: **G0 NOT SIGNED**  
> This document proposes a replacement architecture view for review. It does not modify or supersede the v1.0 baseline without formal change-control.

## 1. Context and trust boundaries

```mermaid
flowchart TB
    A[System Admin]
    O[Authorized Owner]
    M[Manager / Finance user]
    I[Investor: explicitly granted reports]
    FE[React TypeScript web]
    API[Java 21 Spring Boot modular monolith]
    DB[(PostgreSQL)]
    OBJ[(Private evidence storage)]
    AUD[(Append-only audit)]

    A --> FE
    O --> FE
    M --> FE
    I --> FE
    FE --> API
    API --> DB
    API --> OBJ
    API --> AUD
```

Employees are master-data subjects, not authenticated actors. There is no employee portal. API authorization is entity/branch/action/row scoped; frontend filtering is not a security boundary.

## 2. Bounded contexts

```mermaid
flowchart LR
    ORG[organization] --> IAM[identity/authorization]
    ORG --> REV[monthly revenue]
    ORG --> WF[employee master/salary expense]
    ORG --> FIN[finance]
    ORG --> INV[inventory]
    INV --> MOV[transfer/loans]
    REV --> CLS[monthly closing]
    WF --> CLS
    FIN --> CLS
    INV --> CLS
    CLS --> RPT[reporting]
    CLS --> CAP[capital: G5 gated]
    SEC[evidence/audit/idempotency] -. contracts .-> REV
    SEC -. contracts .-> WF
    SEC -. contracts .-> FIN
    SEC -. contracts .-> CLS
```

## 3. Organization, identity and security

```mermaid
erDiagram
  BUSINESS_ENTITIES ||--o{ BRANCHES : owns
  USERS ||--o{ SCOPED_ROLE_GRANTS : receives
  BUSINESS_ENTITIES ||--o{ SCOPED_ROLE_GRANTS : bounds
  BRANCHES ||--o{ SCOPED_ROLE_GRANTS : optionally_bounds
  BUSINESS_ENTITIES ||--o{ EVIDENCE_FILES : classifies
  BRANCHES ||--o{ EVIDENCE_FILES : optionally_classifies
  USERS ||--o{ AUDIT_EVENTS : acts
  BUSINESS_ENTITIES ||--o{ IDEMPOTENCY_RECORDS : scopes
  BRANCHES ||--o{ IDEMPOTENCY_RECORDS : optionally_scopes

  BUSINESS_ENTITIES {
    uuid id PK
    string entity_code UK
    string registered_name
  }
  BRANCHES {
    uuid id PK
    uuid business_entity_id FK
    string branch_code
    string time_zone
  }
  USERS {
    uuid id PK
    string login_identifier UK
    string status
  }
  SCOPED_ROLE_GRANTS {
    uuid id PK
    uuid business_entity_id FK
    uuid branch_id FK
    uuid user_id FK
    string role_key
    date valid_from
    date valid_to
  }
  EVIDENCE_FILES {
    uuid id PK
    uuid business_entity_id FK
    uuid branch_id FK
    string object_key UK
    string privacy_class
    string checksum
  }
  AUDIT_EVENTS {
    uuid id PK
    uuid actor_user_id FK
    uuid business_entity_id FK
    uuid branch_id FK
    string action
    string target_type
    uuid target_id
    datetime occurred_at
  }
  IDEMPOTENCY_RECORDS {
    uuid id PK
    uuid business_entity_id FK
    uuid branch_id FK
    uuid actor_user_id FK
    string operation_scope
    string idempotency_key
    string request_hash
  }
```

Composite `(business_entity_id, branch_id)` foreign keys are required wherever both appear. Evidence links are domain-specific so a salary or revenue record cannot reference another branch's evidence.

## 4. Monthly aggregate NET revenue

```mermaid
erDiagram
  BUSINESS_ENTITIES ||--o{ REVENUE_SOURCE_TYPES : defines
  REVENUE_SOURCE_TYPES ||--o{ SOURCE_NET_DEFINITION_REVISIONS : documents
  SOURCE_NET_DEFINITION_REVISIONS ||--o{ SOURCE_DEFINITION_EVIDENCE : supported_by
  EVIDENCE_FILES ||--o{ SOURCE_DEFINITION_EVIDENCE : file
  BRANCHES ||--o{ BRANCH_REVENUE_SOURCES : declares
  REVENUE_SOURCE_TYPES ||--o{ BRANCH_REVENUE_SOURCES : source
  BRANCHES ||--o{ EXPECTED_SOURCE_CHECKLISTS : prepares
  EXPECTED_SOURCE_CHECKLISTS ||--o{ EXPECTED_SOURCE_ITEMS : contains
  BRANCH_REVENUE_SOURCES ||--o{ EXPECTED_SOURCE_ITEMS : expects
  SOURCE_NET_DEFINITION_REVISIONS ||--o{ EXPECTED_SOURCE_ITEMS : uses_definition
  EXPECTED_SOURCE_CHECKLISTS ||--o{ MONTHLY_REVENUE_REVISIONS : validates
  BRANCHES ||--o{ MONTHLY_REVENUE_REVISIONS : reports
  MONTHLY_REVENUE_REVISIONS ||--o{ MONTHLY_REVENUE_EVIDENCE : supported_by
  EVIDENCE_FILES ||--o{ MONTHLY_REVENUE_EVIDENCE : file

  REVENUE_SOURCE_TYPES {
    uuid id PK
    uuid business_entity_id FK
    string source_code
    string display_name
    boolean active
  }
  SOURCE_NET_DEFINITION_REVISIONS {
    uuid id PK
    uuid source_type_id FK
    int revision
    boolean is_current
    string definition_text
    string status
  }
  BRANCH_REVENUE_SOURCES {
    uuid id PK
    uuid business_entity_id FK
    uuid branch_id FK
    uuid source_type_id FK
    date effective_from
    date effective_to
  }
  EXPECTED_SOURCE_CHECKLISTS {
    uuid id PK
    uuid business_entity_id FK
    uuid branch_id FK
    date service_month
    int revision
    boolean is_current
    string status
    uuid owner_confirmed_by FK
  }
  EXPECTED_SOURCE_ITEMS {
    uuid id PK
    uuid checklist_id FK
    uuid branch_source_id FK
    uuid definition_revision_id FK
    string consideration_status
    string not_applicable_reason
  }
  MONTHLY_REVENUE_REVISIONS {
    uuid id PK
    uuid business_entity_id FK
    uuid branch_id FK
    date service_month
    int revision
    boolean is_current
    decimal aggregate_net_amount
    string zero_reason
    string status
    uuid entered_by FK
    uuid confirmed_by FK
  }
```

There is deliberately no amount on source type, source definition, branch source or checklist item. The sole beverage-revenue amount is `MONTHLY_REVENUE_REVISIONS.aggregate_net_amount`. It creates no relation to financial accounts, cash ledger, receivable or settlement.

## 5. Employee master and monthly salary expense

```mermaid
erDiagram
  BUSINESS_ENTITIES ||--o{ EMPLOYEES : maintains
  EMPLOYEES ||--o{ PRIMARY_BRANCH_DESIGNATIONS : designated
  BRANCHES ||--o{ PRIMARY_BRANCH_DESIGNATIONS : receives_full_expense
  BRANCHES ||--o{ EXPECTED_EMPLOYEE_CHECKLISTS : prepares
  EXPECTED_EMPLOYEE_CHECKLISTS ||--o{ EXPECTED_EMPLOYEE_ITEMS : contains
  EMPLOYEES ||--o{ EXPECTED_EMPLOYEE_ITEMS : expects
  PRIMARY_BRANCH_DESIGNATIONS ||--o{ EXPECTED_EMPLOYEE_ITEMS : freezes_designation
  EMPLOYEES ||--o{ MONTHLY_SALARY_EXPENSE_REVISIONS : incurs
  PRIMARY_BRANCH_DESIGNATIONS ||--o{ MONTHLY_SALARY_EXPENSE_REVISIONS : attributes
  EXPECTED_EMPLOYEE_CHECKLISTS ||--o{ MONTHLY_SALARY_EXPENSE_REVISIONS : validates
  MONTHLY_SALARY_EXPENSE_REVISIONS ||--o{ SALARY_EXPENSE_EVIDENCE : supported_by
  EVIDENCE_FILES ||--o{ SALARY_EXPENSE_EVIDENCE : file

  EMPLOYEES {
    uuid id PK
    uuid business_entity_id FK
    string staff_code
    string full_name
    date hired_on
    date left_on
    string status
  }
  PRIMARY_BRANCH_DESIGNATIONS {
    uuid id PK
    uuid business_entity_id FK
    uuid employee_id FK
    uuid branch_id FK
    date service_month
    int revision
    boolean is_current
    string authority_reference
    uuid designated_by FK
  }
  EXPECTED_EMPLOYEE_CHECKLISTS {
    uuid id PK
    uuid business_entity_id FK
    uuid branch_id FK
    date service_month
    int revision
    boolean is_current
    string status
    uuid owner_confirmed_by FK
  }
  EXPECTED_EMPLOYEE_ITEMS {
    uuid id PK
    uuid checklist_id FK
    uuid employee_id FK
    uuid designation_id FK
    string expectation_status
    string exclusion_reason
  }
  MONTHLY_SALARY_EXPENSE_REVISIONS {
    uuid id PK
    uuid business_entity_id FK
    uuid employee_id FK
    uuid expense_branch_id FK
    uuid designation_id FK
    date service_month
    int revision
    boolean is_current
    decimal expense_amount
    string zero_reason
    string status
    uuid entered_by FK
    uuid confirmed_by FK
  }
```

`EMPLOYEES` has no user/login foreign key. The composite designation reference forces employee, service month and expense branch to match. The model contains no attendance, GPS, shift, schedule, leave, payable time, rate, payroll component, payable or payment entity.

## 6. Finance and inventory retained scope

```mermaid
erDiagram
  BRANCHES ||--o{ FINANCIAL_ACCOUNTS : owns
  FINANCIAL_ACCOUNTS ||--o{ CASH_LEDGER_ENTRIES : posts
  BRANCHES ||--o{ EXPENSES : incurs
  BRANCHES ||--o{ PAYABLES : owes
  PAYABLES ||--o{ PAYABLE_PAYMENTS : settles
  BRANCHES ||--o{ CASH_RECONCILIATIONS : reconciles
  BUSINESS_ENTITIES ||--o{ ENTITY_TAX_OBLIGATIONS : owes
  ENTITY_TAX_OBLIGATIONS ||--o{ ENTITY_COST_ALLOCATIONS : allocates
  BRANCHES ||--o{ ENTITY_COST_ALLOCATIONS : receives

  BRANCHES ||--o{ PURCHASE_RECEIPTS : receives
  PURCHASE_RECEIPTS ||--o{ PURCHASE_RECEIPT_LINES : contains
  CATALOG_ITEMS ||--o{ PURCHASE_RECEIPT_LINES : item
  BRANCHES ||--o{ STOCK_BALANCES : owns
  CATALOG_ITEMS ||--o{ STOCK_BALANCES : item
  BRANCHES ||--o{ STOCK_LOTS : tracks
  STOCK_LOTS ||--|| STOCK_LOT_BALANCES : balances
  STOCK_DOCUMENTS ||--o{ STOCK_DOCUMENT_LINES : contains
  STOCK_DOCUMENTS ||--o{ STOCK_MOVEMENTS : posts
  BRANCHES ||--o{ STOCK_COUNTS : counts
```

Finance remains actual-money/debt state only. Monthly aggregate revenue and salary expense do not create finance rows. Inventory remains manual per-branch moving WAC with no negative available stock and no expired-lot consumption.

## 7. Transfers, loans, assets and capital retained scope

```mermaid
erDiagram
  BUSINESS_ENTITIES ||--o{ STOCK_TRANSFERS : bounds
  BRANCHES ||--o{ STOCK_TRANSFERS : sends
  BRANCHES ||--o{ STOCK_TRANSFERS : receives
  STOCK_TRANSFERS ||--o{ STOCK_TRANSFER_LINES : contains
  BRANCHES ||--o{ MATERIAL_LOANS : controls
  BRANCH_COUNTERPARTIES ||--o{ MATERIAL_LOANS : partner
  MATERIAL_LOANS ||--o{ MATERIAL_LOAN_LINES : contains
  MATERIAL_LOANS ||--o{ MATERIAL_LOAN_RETURNS : returns
  BRANCHES ||--o{ EQUIPMENT_ASSETS : owns

  BRANCHES ||--o{ INVESTOR_AGREEMENTS : contracts
  INVESTORS ||--o{ INVESTOR_AGREEMENTS : party
  INVESTOR_AGREEMENTS ||--o{ PROFIT_ENTITLEMENT_TERMS : sets
  MONTHLY_CLOSE_REVISIONS ||--o| RETAINED_PROFIT_LOTS : sources
  RETAINED_PROFIT_LOTS ||--o{ RETAINED_PROFIT_ENTITLEMENTS : freezes
  DISTRIBUTION_BATCHES ||--o{ DISTRIBUTION_LINES : contains
  DISTRIBUTION_LINES ||--o{ DISTRIBUTION_PAYMENT_ATTEMPTS : attempts
```

Transfer branch references share one business entity. Capital and distribution remain separate from software roles. Distribution activation requires G5 verification; a provisional close is not payout authorization.

## 8. Period close, immutable snapshot and correction references

```mermaid
erDiagram
  BRANCHES ||--o{ BRANCH_PERIODS : controls
  BRANCH_PERIODS ||--o{ MONTHLY_CLOSE_REVISIONS : versions
  MONTHLY_CLOSE_REVISIONS ||--|| CLOSE_SNAPSHOTS : freezes
  CLOSE_SNAPSHOTS ||--o{ CLOSE_COMPONENTS : totals
  CLOSE_SNAPSHOTS ||--|| CLOSE_REVENUE_INPUTS : revenue_input
  MONTHLY_REVENUE_REVISIONS ||--o| CLOSE_REVENUE_INPUTS : exact_revision
  CLOSE_SNAPSHOTS ||--o{ CLOSE_SALARY_INPUTS : salary_inputs
  MONTHLY_SALARY_EXPENSE_REVISIONS ||--o| CLOSE_SALARY_INPUTS : exact_revision
  CLOSE_SNAPSHOTS ||--o{ CLOSE_EXPENSE_INPUTS : expense_inputs
  EXPENSES ||--o{ CLOSE_EXPENSE_INPUTS : exact_expense
  CLOSE_SNAPSHOTS ||--o{ CLOSE_INVENTORY_INPUTS : inventory_inputs
  STOCK_MOVEMENTS ||--o{ CLOSE_INVENTORY_INPUTS : exact_movement
  CLOSE_SNAPSHOTS ||--o{ CLOSE_TAX_ALLOCATION_INPUTS : tax_inputs
  ENTITY_COST_ALLOCATIONS ||--o{ CLOSE_TAX_ALLOCATION_INPUTS : exact_allocation
  MONTHLY_CLOSE_REVISIONS ||--o{ CORRECTION_CASES : origin
  CORRECTION_CASES ||--o| REOPEN_REQUESTS : possible_route
  CORRECTION_CASES ||--o| CURRENT_PERIOD_ADJUSTMENTS : possible_route
  REOPEN_REQUESTS ||--o| MONTHLY_CLOSE_REVISIONS : resulting_close

  BRANCH_PERIODS {
    uuid id PK
    uuid business_entity_id FK
    uuid branch_id FK
    date month_start
    string status
  }
  MONTHLY_CLOSE_REVISIONS {
    uuid id PK
    uuid branch_period_id FK
    int revision
    boolean is_current
    string status
    decimal managerial_result
    boolean provisional
    string display_label
  }
  CLOSE_SNAPSHOTS {
    uuid id PK
    uuid close_revision_id FK
    datetime captured_at
    uuid captured_by FK
    string content_digest
  }
  CLOSE_COMPONENTS {
    uuid id PK
    uuid snapshot_id FK
    string component_code
    decimal signed_amount
  }
  CORRECTION_CASES {
    uuid id PK
    uuid origin_close_revision_id FK
    uuid revenue_revision_id FK
    uuid salary_expense_revision_id FK
    uuid expense_id FK
    uuid stock_movement_id FK
    string selected_route
    string policy_state
    string reason
  }
  REOPEN_REQUESTS {
    uuid id PK
    uuid correction_case_id FK
    uuid origin_close_revision_id FK
    string status
  }
  CURRENT_PERIOD_ADJUSTMENTS {
    uuid id PK
    uuid correction_case_id FK
    uuid origin_close_revision_id FK
    uuid booking_period_id FK
    decimal signed_amount
  }
```

`selected_route` is nullable while `GAP-02` is unresolved. Both reference paths exist, but this ERD assigns no error type to either route. A close snapshot points to exact input revisions, never to a mutable “current” query.

## 9. State models

```mermaid
stateDiagram-v2
  [*] --> DRAFT
  DRAFT --> CONFIRMED: authorized confirmation after GAP-01 resolution
  CONFIRMED --> SUPERSEDED: OPEN-period revision
  SUPERSEDED --> [*]
```

The transition to `CONFIRMED` is a documented capability, not release enablement. `GAP-01` blocks it until explicit policy change-control.

```mermaid
stateDiagram-v2
  [*] --> OPEN
  OPEN --> CLOSING: completeness transaction
  CLOSING --> CLOSED: immutable snapshot
  CLOSED --> REOPEN_REQUESTED: approved request path proposed
  REOPEN_REQUESTED --> REOPENED: after GAP-02 policy and approval
  REOPENED --> CLOSED: new close revision
  CLOSED --> CLOSED: referenced current-period adjustment after GAP-02 policy
```

## 10. Required relational invariants

1. Every branch-scoped child carries a composite FK to `(business_entity_id, branch_id)`.
2. Every service month is the first day of a month.
3. Monetary normal-entry values are exact scale 2 and nonnegative; zero requires a nonblank reason.
4. One current revenue revision per branch/month and one current salary revision per employee/month use partial unique indexes.
5. Expected-source items have no amount column.
6. Salary expense branch equals the exact primary-branch designation revision.
7. Checklist headers/items and aggregate/salary revisions share entity, branch and month through composite FKs.
8. Close inputs reference exact revisions, not current aliases or JSON-only provenance.
9. Immutable CLOSED snapshots require restrictive DB privileges/service guards plus negative tests.
10. Idempotency uniqueness includes entity, actor, operation and key; business uniqueness remains separately enforced.
11. Evidence is private and linked through scope-matching join tables.
12. Capital payout remains disabled until G5; P&L always displays **TẠM TÍNH / PROVISIONAL**.

## 11. Explicitly removed entities

The v1.1 proposal contains none of the following: `salary_policies`, `work_shifts`, `shift_assignments`, `attendance_events`, `leave_requests`, `timesheet_entries`, `payroll_periods`, `payroll_lines`, `payroll_adjustments`, `payroll_payments`, daily revenue reports, app settlement, salary payable/payment or employee login link.

## 12. Review status

- Architecture: **PROPOSED**.
- Schema readiness: **NO / BLOCKED**.
- G0: **NOT SIGNED**.
- Open: `GAP-01…09`, `BR-FIN-08`, `BR-CAP-10`/G5.
- Application code, migration and database execution: **NOT AUTHORIZED / NOT RUN**.

