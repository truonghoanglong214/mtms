---
name: mtms-postgresql-flyway
description: "Designs and verifies approved PostgreSQL migrations and integrity constraints for MTMS using Flyway with rollback-safe practices."
---

# PostgreSQL & Flyway
**Do not execute starter SQL as one migration.** Before G0/task approval: review-only.
1. Check dependency on `P0-ORG-02`, ADR approvals and original SQL constraints.
2. Convert only assigned schema segment into sequential immutable Flyway migration once authorized.
3. Test clean bootstrap and already-migrated re-run against disposable PostgreSQL (Testcontainers); no real DB writes without user permission.
4. Validate unique per-branch/day active report, unique source legs, nonnegative stock, branch/lot consistency and appropriate indexes.
5. For historical correction, use forward-only migration and recorded strategy, never edit an applied migration.
**Output:** migration name, dependency, safety check, real test result and backout/forward-fix plan.

