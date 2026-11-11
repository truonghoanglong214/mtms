---
name: mtms-architecture-review
description: "Reviews MTMS modular monolith, PostgreSQL domain boundaries, branch isolation, transactions, database design and ADR proposals."
---

# Architecture review
1. Read original ERD and SQL as proposals, Handbook for invariants and proposed ADR-025–031 decisions.
2. Check module responsibilities: organization/identity, revenue/finance, workforce/payroll, inventory, closing, transfer/loan, capital, reporting/security.
3. Identify cross-branch FK mismatches, settlement-vs-revenue duplication, closing snapshot immutability and payout attempt/retry ambiguity.
4. Propose trade-offs with acceptance tests. Never create an applied migration or revise business behavior silently.
5. Write `docs/architecture/decisions/ADR-...-proposed.md` only with approval to edit planning files.
**Output:** selected option, alternatives, risks, domain invariants, impacted BR/TC, pending policy decisions.

