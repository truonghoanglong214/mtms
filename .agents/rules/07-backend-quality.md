---
trigger: model_decision
description: "Apply when planning Java Spring Boot modular monolith, APIs, persistence, validation and tests"
---

# Backend discipline
- Prefer module boundaries and published application contracts. No cross-module repository access into internal tables.
- Use Java 21, Spring Boot modular monolith proposal, validation, explicit DB transactions and Testcontainers for financial/stock integration.
- `BigDecimal` (no double money); `UUID`; `Instant` UTC; `LocalDate` business date.
- Version-controlled forward-only Flyway scripts after the relevant task authorization. Never run conceptual starter as one migration.
- Keep API idempotency/conflict behavior explicit and test failure/retry/concurrent cases.
