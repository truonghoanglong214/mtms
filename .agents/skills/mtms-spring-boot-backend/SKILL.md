---
name: mtms-spring-boot-backend
description: "Implements approved Java 21 Spring Boot modular monolith APIs and domain use cases using branch scopes, validation, idempotency and tests."
---

# Spring Boot backend task delivery
**Hard gate:** before G0 and task approval, plan only. Do not scaffold or modify app code.
After approval:
1. Read contract/BR/TC, search code patterns and module boundaries before creating classes.
2. Build a small vertical slice: DTO validation -> application service -> domain rules -> persistence -> controller.
3. Enforce branch/entity ownership in service and DB query; do not rely on FE filters.
4. Use transaction boundaries and exact decimal arithmetic; handle duplicate Idempotency-Key and version conflicts.
5. Write unit and Testcontainers integration tests for happy path, invalid access, retry and concurrent writes when applicable.
6. Run actual Maven tests; report commands and observed outputs, don't fabricate.
**Output:** changed files, design decisions, tests, limitations, blocker references.

