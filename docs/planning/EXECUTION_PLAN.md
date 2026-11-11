# MTMS — Execution plan (the task tracker is authoritative for task status)

The overall delivery order from the unchanged business baseline is **P0 (MVP) → P1 (V1) → P2 (optional enhancements)**. These are product-release/task identifiers, not names of workspace folders.

- **Before G0:** audit, source review, dependencies/FE backlog signoff; no feature code.
- **First approved work:** `P0-ORG-01` (Spring Boot + React repositories), then `P0-ORG-02` (local PostgreSQL/Flyway) after respective acceptance.
- **Security foundation:** entity/branch scope, auth, permissions, audit and idempotency before money-moving features.
- **First vertical slice:** daily business calendar → aggregated revenue → cash ledger (without fabricating app deposits) → owner report.
- **MVP:** inventory manual WAC, approved attendance/payroll, monthly closing and provisional P&L; restore backup and branch-scope negative tests.
- **V1:** multi-branch operations, transfers/loans, investor entitlements and G5-gated actual distributions.
- **P2:** advisory AI and advanced capabilities only after separate G7 evaluation.

**Project execution procedure:** Read BR + linked Task/TC → dependency check → scoped plan → user approval → scoped implementation → tests → QA → evidence → acceptance → tracker update.

**Source of detailed task specifications:** `docs/business/MTMS_Implementation_Handbook_v1_0.md`, and status/links: `docs/planning/MTMS_Development_Tracker.xlsx`. Dependency changes and 69 frontend subtasks are still proposals pending G0. See `docs/planning/DEPENDENCY_REVIEW.md` and `docs/planning/FRONTEND_EXECUTION_PLAN.md`.
