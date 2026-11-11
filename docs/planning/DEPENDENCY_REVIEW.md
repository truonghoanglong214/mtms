# Dependency review — working copy, not signed

The original Task DAG is valid and contains no cycle. For v1.1 we added **19 technical prerequisite proposals** in the working workbook. Changes must be approved at G0. No estimated hours/status were changed.

| Task | Added prerequisite(s) | Reason |
|---|---|---|
| P0-REV-03 | P0-ORG-05; P0-SEC-03 | Need branch-scoped authorization and anti-duplicate writes for revenue confirmation. |
| P0-REV-05 | P0-REV-03; P0-SEC-03 | Revenue posting must depend on confirmed reporting and idempotency, not only finance ledger. |
| P0-FIN-02 | P0-ORG-05; P0-SEC-03 | Money ledger requires server-side authorization and unique source posting. |
| P0-FIN-03 | P0-ORG-05 | Expense operations must validate scoped permissions. |
| P0-HR-03 | P0-ORG-05 | Approved payable time must honor branch-scoped permissions. |
| P0-HR-05 | P0-HR-03 | Calculation cannot proceed without approved payable hours. |
| P0-HR-06 | P0-SEC-02 | Approval revisions require audit events. |
| P0-HR-07 | P0-FIN-02 | Payroll payment must flow through cash/bank posting. |
| P0-INV-02 | P0-INV-01 | Opening inventory must use validated item/base units. |
| P0-INV-04 | P0-SEC-03 | Posting WAC must be idempotent and safe under retries. |
| P0-INV-05 | P0-SEC-03 | Manual issue requires idempotency in addition to inventory posting. |
| P0-CAP-02 | P0-FIN-02 | Actual capital cash contribution must link separately to money ledger. |
| P0-CLS-01 | P0-REV-05; P0-HR-06; P0-FIN-06; P0-INV-07 | Close prerequisites require confirmed revenue, payroll, reconciled cash and COGS. |
| P0-CLS-02 | P0-REV-05; P0-FIN-03; P0-HR-06; P0-FIN-07 | P&L query draws from revenue, expenses, payroll accrual, tax and COGS. |
| P0-CLS-03 | P0-CLS-01; P0-SEC-02 | Immutable close snapshot requires prerequisite validation and audit trail. |
| P0-CLS-05 | P0-SEC-05; P0-ORG-05; P0-SEC-03 | MVP gate must verify CI/restore, permissions and idempotency. |
| P1-MOV-08 | P1-MOV-07 | Returns must handle both borrow-out and borrow-in valuation rules. |
| P1-CAP-07 | P1-CAP-05; P1-CAP-08; P1-CAP-02 | Payment requires approved cash/liability gate, retained lots and delegation. |
| P1-CLS-03 | P1-CLS-02; P1-MOV-09; P1-HR-07; P1-FIN-02 | V1 release rehearsal must include reopen, loans, payroll and consolidation controls. |

## Guardrails
- Treat the revised `Tasks!F` as a **proposed execution order** until G0 is accepted.
- The Implementation Handbook v1.0 still contains the **original** dependencies. Where they differ, record G0 review rather than quietly rewriting history.
- Before running a task, orchestrator must inspect direct AND transitive predecessors; missing Test Evidence blocks marking Done.
- After changing any edge: run DAG validation, reject missing task IDs/cycles and review whether extra dependencies affect project critical path.
