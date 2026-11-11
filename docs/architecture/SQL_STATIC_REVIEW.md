# PostgreSQL starter schema — static design review, NOT execution proof

Original `MTMS_PostgreSQL_Schema_Starter_v1_0.sql` explicitly identifies itself as a **planning reference**, not a production migration.

## Inspected facts
- **75 `CREATE TABLE` declarations**, PostgreSQL 17 oriented; `NUMERIC` for exact money/stock quantities, `TIMESTAMPTZ` audit time and `DATE` local business dates.
- Explicit `UNIQUE(branch_id,business_date,revision)` and partial unique current daily report; source-linked uniqueness for money/stock movements.
- `distribution_payments.distribution_line_id UNIQUE`, with comment requiring a service-level full-payment match and explicit failed-attempt recovery.

## Priority technical controls before G1/G3/G5
1. **Cross-branch FK consistency**: `stock_movements.branch_id` and `lot_id` reference tables independently; independent FKs do not enforce that the lot belongs to the same branch/item. Likewise `cash_ledger_entries.branch_id` and `account_id` must align. Plan compound FK constraints where practical and negative integration tests; otherwise service transactional validation plus DB protection. **Proposed, not patched in SQL.**
2. **Revenue source posting and real bank cash**: confirmed daily cash/bank mapped from report must be idempotent. Delivery-app NET must not create bank funds until actual settlement; verify reconciliation event is not a second beverage sale.
3. **Stock WAC concurrency**: row lock on `stock_balances` by (branch,item), source uniqueness, lot availability and no expiry consumption; test concurrent issue twice and rounding residual.
4. **Period closing**: verify every OPEN day reported, approved payroll, reconciliation and source snapshots before month close; do not rewrite old ledger after close.
5. **Distribution retry**: one successful *full* payout per line; distinguish payment record from attempts/history so FAILED/PENDING states and bank confirmation cannot create duplicate cash postings.
6. **Transfer same registered entity**: table checks only `from_branch_id <> to_branch_id`; legal-entity equality needs service check and, if suitable, DB-level guard.
7. **Policy gates**: ADR-027 (loan valuation), ADR-028 (distribution legality), ADR-029 (labor) must remain blocked until verified.

## Migration handoff
- Split conceptual schema by dependencies: org/identity -> evidence/audit -> finance/revenue -> inventory/HR -> closing -> capital and P1 additions.
- Use immutable Flyway V001... with test Postgres/Testcontainers. Never run the complete starter as a single production migration.
- Verify *applied migrations* with clean database + repeat startup + rollback/recovery procedure, and test cross-module transactions.

**Not yet performed:** SQL parse/execution against a running PostgreSQL instance, migrations, integration tests, security penetration tests.
