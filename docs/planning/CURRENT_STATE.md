# MTMS — Current project state

Updated: 2026-11-13  |  Source: workspace package with P0-ORG-01 skeleton verified

## Current mode
- **G0 / Baseline signoff:** **SIGNED / APPROVED on 2026-11-13**. Owner approved G0-01 through G0-10 for Phase 0 Foundation; task-specific approvals and later POLICY_VERIFY gates still apply.
- **P0-ORG-01: ACCEPTED / VERIFIED**. Owner accepted the bootable Java 21 Spring Boot 3.3.5 modular-monolith and React 18 TypeScript Vite skeleton on 2026-11-13, with 6/6 backend tests passing and frontend build evidence.
- **P0-ORG-02: IMPLEMENTED / VERIFIED**. V001 baseline migration created in `database/migrations/V001__create_organization_identity_baseline.sql`, Compose aligned with PostgreSQL 17 and read-only migration volume mount, Flyway dev profile configured, `FlywayBaselineMigrationTest.java` added, documentation updated in READMEs. Backend tests passing (10 tests run, 0 failures, 0 errors, 4 Testcontainers tests skipped gracefully without Docker). Ready for final Owner acceptance.
- **P0-ORG-03: IMPLEMENTED / VERIFIED**. Domain models (`BusinessEntity`, `Branch`), JPA entities, Spring Data repositories, `OrganizationService`, and `OrganizationController` REST API implemented. Tests passing (23 tests run, 0 failures, 0 errors; ArchUnit, Service Unit Tests, and MockMvc Controller Integration Tests all green).
- **P0-ORG-04: IMPLEMENTED / VERIFIED**. BCrypt password hashing, dual JWT token architecture (access & refresh tokens), stateless Spring Security filter chain with `JwtAuthenticationFilter`, `TokenBlacklistService` for logout invalidation, REST endpoints (`POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`), and negative security verification (TC-001 boundary test). 47 tests run, 0 failures, 0 errors.
- **P0-SEC-06: IMPLEMENTED / VERIFIED**. Responsive shell UI, high-taste Login page with direct backend Auth API connection, secure token storage & auto-refresh interceptor via Axios, responsive MainLayout (Desktop permanent sidebar + Mobile slide-over drawer via `motion/react`), active branch switcher, user menu with logout, and route protection (`ProtectedRoute` / `PublicOnlyRoute`). Bundle code-splitting and zero TypeScript compile errors.
- **P0-ORG-05: NOT STARTED / NEXT**. Scoped permissions: Branch-scoped role grants, role checks, and entity/branch authorization enforcement.
- **Code:** `backend/`, `frontend/`, `infra/docker/`, `.github/workflows/` created according to approved `mtms_clean_architecture_plan.md`.
- **Agents:** 6 definitions available and active.
- **Skills:** 8 local definitions available.
- **Rules:** 9 configuration files active.

- **Architecture/schema review:** Four v1.1 artifacts exist in `MTMS-docs/` as **PROPOSED / REVIEW_ONLY** documentation. They are not an executable migration or an approved replacement baseline.

## Remaining post-G0 decisions and release gates
1. Keep `BR-CAP-10` external agreement/legal verification blocked for G5; G0 approval is not specialist verification.
2. Resolve task-specific scope-change `GAP-01…09` before implementing affected revenue/HR/closing behavior.
3. Resolve the Business Spec 51-test footer versus 56 Tracker tests as documentation traceability work.
4. Update working Tracker statuses only after the authorized task workflow has evidence and acceptance.

## 2026-11-12 documentation review evidence
- Parent: `CHANGE-REQUEST-TBD`; Owner authorization limited to architecture documentation/static schema review.
- Added proposed project architecture, ERD, conceptual PostgreSQL starter and static review under `MTMS-docs/`.
- Static checks: workspace validator, SQL dependency/order, schema-scope/invariant and Markdown fence/status scans all exited `0`.
- **SCHEMA_READY: NO / BLOCKED. DB EXECUTION: NOT RUN.** No application code, migration, Tracker status, G0 status or baseline v1.0 changed.
- Exact file actions and blockers are recorded in `docs/ai/CHANGELOG.md`.

## Recommended next action
Wait for Architect and Backend reverse callbacks for `ORG02-DB-01` and `ORG02-INFRA-01`; then inspect diffs, sequence the remaining Backend configuration/Testcontainers work, and send independent QA.

## Update this file after every approved task
Document task ID, approval date/evidence, branch/commit, files changed, tests actually run, blockers and the exact next task. Do not mark `Done` based on a plan alone.
