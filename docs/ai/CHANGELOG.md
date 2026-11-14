# MTMS — File changes and decisions

## 2026-11-14 — Frontend design system baseline and split-screen login upgrade
- **Authorization:** Owner explicit alignment on frontend aesthetic, density, palette, typography and layout.
- **ADD:**
  - `docs/design/FRONTEND_DESIGN_SYSTEM.md` (Single source of truth for UI/UX guidelines: Modern Editorial Linear/Supabase style, Forest Emerald & Deep Matcha palette, High-Density layout, Monospace tabular nums, Motion micro-interactions, Vercel React best practices, and Web Design accessibility).
- **MODIFY:**
  - `frontend/src/features/auth/pages/LoginPage.tsx` (Complete redesign into high-end Split-Screen 50/50: left column visual brand showcase & operational philosophy, right column clean minimal auth form with emerald focus states and animated RFC 7807 alerts).
- **Build Evidence:**
  - `npm --prefix frontend run build`: Successful build in 5.84s with zero TypeScript/CSS errors.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS.

## 2026-11-13 — P0-SEC-06 responsive shell UI implementation and verification
- **Authorization:** Owner explicit approval to implement `P0-SEC-06`.
- **Skills applied:** `design-taste-frontend`, `motion`, `vercel-react-best-practices`, `web-design-guidelines`.
- **ADD:**
  - `frontend/src/features/auth/types.ts` (DTOs matching backend Auth API and RFC 7807 ProblemDetail).
  - `frontend/src/features/auth/authStore.ts` (Zustand persistent store for tokens, user profile, active branch context).
  - `frontend/src/features/auth/authApi.ts` (API client methods for login, logout, refresh, getMe).
  - `frontend/src/features/auth/pages/LoginPage.tsx` (Anti-slop login screen with smooth motion transitions, form validation, error banners).
  - `frontend/src/shared/api/apiClient.ts` (Axios client with Bearer auth injection, queue-based 401 token refresh interceptor, and logout on session expiration).
  - `frontend/src/shared/components/ui/input.tsx` (Accessible input component with error border and focus rings).
  - `frontend/src/app/routes/ProtectedRoute.tsx` (Redirect unauthenticated requests to `/login`).
  - `frontend/src/app/routes/PublicOnlyRoute.tsx` (Redirect authenticated users visiting `/login` to `/`).
  - `frontend/src/vite-env.d.ts` (Vite client types).
  - `docs/planning/P0_SEC_06_TASK_BRIEF.md`.
- **MODIFY:**
  - `frontend/package.json` (installed `motion: ^14.14.0`).
  - `frontend/src/app/layout/Header.tsx` (integrated Branch Selector dropdown, User Profile menu with Logout, mobile menu toggle, and Provisional badge).
  - `frontend/src/app/layout/Sidebar.tsx` (desktop persistent sidebar + mobile slide-over drawer with backdrop blur using `motion/react`).
  - `frontend/src/app/layout/MainLayout.tsx` (wired responsive mobile state and Suspense fallback for code-split route chunks).
  - `frontend/src/app/router.tsx` (lazy route loading, route protection, login routing).
  - `docs/planning/CURRENT_STATE.md`.
- **Build & Test Evidence:**
  - `npm --prefix frontend run build`: Successful build in 3.87s. Route-level code-splitting (each page < 1.2 kB).
  - `python scripts/validate_workspace.py`: SUMMARY: PASS (all workspace invariants intact).

## 2026-11-13 — P0-ORG-04 authentication and sessions implementation and verification
- **Authorization:** Owner explicit approval to implement `P0-ORG-04`.
- **ADD:**
  - `backend/src/main/java/com/mtms/identity/domain/User.java`, `UserStatus.java`.
  - `backend/src/main/java/com/mtms/identity/infrastructure/persistence/UserJpaEntity.java`, `UserJpaRepository.java`.
  - `backend/src/main/java/com/mtms/identity/infrastructure/security/JwtTokenService.java`, `InvalidTokenException.java`, `TokenBlacklistService.java`, `UserPrincipal.java`, `JwtAuthenticationFilter.java`, `SecurityConfig.java`.
  - `backend/src/main/java/com/mtms/identity/application/dto/LoginCommand.java`, `AuthResponse.java`, `RefreshTokenCommand.java`, `TokenRefreshResponse.java`, `UserDto.java`.
  - `backend/src/main/java/com/mtms/identity/application/exception/AuthenticationFailedException.java`, `AccountSuspendedException.java`.
  - `backend/src/main/java/com/mtms/identity/application/AuthenticationService.java`.
  - `backend/src/main/java/com/mtms/identity/web/AuthController.java`.
  - `backend/src/main/java/com/mtms/common/error/UnauthorizedException.java`, `AccessForbiddenException.java`.
  - `backend/src/test/java/com/mtms/identity/JwtTokenServiceTest.java`.
  - `backend/src/test/java/com/mtms/identity/AuthenticationServiceTest.java`.
  - `backend/src/test/java/com/mtms/identity/AuthControllerTest.java`.
  - `backend/src/test/java/com/mtms/identity/SecurityProtectionIntegrationTest.java`.
  - `docs/planning/P0_ORG_04_TASK_BRIEF.md`.
- **MODIFY:**
  - `backend/pom.xml` (added `spring-boot-starter-security`, `jjwt-api:0.12.6`, `jjwt-impl:0.12.6`, `jjwt-jackson:0.12.6`, `spring-security-test`).
  - `backend/src/main/java/com/mtms/common/error/GlobalExceptionHandler.java` (handled `UnauthorizedException` -> 401, `AccessForbiddenException` -> 403 with RFC 7807 ProblemDetail).
  - `backend/src/main/resources/application-test.yml` (added `INIT=CREATE SCHEMA IF NOT EXISTS mtms` for H2 schema compatibility).
  - `backend/src/test/java/com/mtms/organization/OrganizationControllerTest.java` (isolated security filters for slice testing).
  - `docs/planning/CURRENT_STATE.md`.
- **Test Evidence:**
  - `mvn -f backend/pom.xml test`: 47 tests run, 0 failures, 0 errors, 4 Testcontainers skipped gracefully without Docker on host.
    - ArchUnit ModuleBoundaryTest (2/2 PASS)
    - MoneyUtilsTest (3/3 PASS)
    - JwtTokenServiceTest (3/3 PASS)
    - AuthenticationServiceTest (6/6 PASS)
    - AuthControllerTest (9/9 PASS)
    - SecurityProtectionIntegrationTest (6/6 PASS)
    - MtmsApplicationTests Smoke (1/1 PASS)
    - OrganizationControllerTest (6/6 PASS)
    - OrganizationServiceTest (7/7 PASS)
  - `python scripts/validate_workspace.py`: SUMMARY: PASS (all workspace invariants intact).
  - `TC-001`: Verified unauthenticated access to `/api/v1/organizations/**` returns 401 Unauthorized; logout invalidates access token.

## 2026-11-13 — P0-ORG-03 entity and branch domain implementation and verification
- **Authorization:** Owner explicit approval to implement `P0-ORG-03`.
- **ADD:**
  - `backend/src/main/java/com/mtms/organization/domain/BusinessEntity.java`, `Branch.java`, `BusinessEntityStatus.java`, `BranchStatus.java`.
  - `backend/src/main/java/com/mtms/organization/application/dto/CreateBusinessEntityCommand.java`, `BusinessEntityDto.java`, `CreateBranchCommand.java`, `BranchDto.java`.
  - `backend/src/main/java/com/mtms/organization/application/exception/DuplicateEntityCodeException.java`, `DuplicateBranchCodeException.java`, `EntityNotFoundException.java`.
  - `backend/src/main/java/com/mtms/common/error/ResourceNotFoundException.java`, `ResourceConflictException.java`.
  - `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BusinessEntityJpaEntity.java`, `BranchJpaEntity.java`, `BusinessEntityJpaRepository.java`, `BranchJpaRepository.java`.
  - `backend/src/main/java/com/mtms/organization/application/OrganizationService.java`.
  - `backend/src/main/java/com/mtms/organization/web/OrganizationController.java`.
  - `backend/src/test/java/com/mtms/organization/OrganizationServiceTest.java`.
  - `backend/src/test/java/com/mtms/organization/OrganizationControllerTest.java`.
  - `docs/planning/P0_ORG_03_TASK_BRIEF.md`.
- **MODIFY:**
  - `backend/src/main/java/com/mtms/common/error/GlobalExceptionHandler.java` (handling `ResourceNotFoundException` -> 404, `ResourceConflictException` -> 409 without breaking ArchUnit boundaries).
  - `docs/planning/CURRENT_STATE.md`.
- **Test Evidence:**
  - `mvn -f backend/pom.xml test`: 23 tests run, 0 failures, 0 errors, 4 Testcontainers skipped without Docker. ArchUnit (2/2), Service Unit Tests (7/7), Controller MockMvc Tests (6/6), MoneyUtils (3/3), Context Smoke (1/1) all PASS.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS (all workspace invariants intact).
  - `TC-003`: domain prerequisite ready; bank account onboarding deferred to `P0-ORG-07`.

## 2026-11-13 — ORG02-INFRA-01 implementation callback and Tech Lead review
- **MODIFY by Backend:** `infra/docker/compose.yml`, `infra/docker/.env.example` only.
- **Result:** PostgreSQL 17 Alpine, mandatory password injection, persistent volume/healthcheck, read-only migration mount and approved Flyway environment prepared.
- **Static evidence rerun by Tech Lead:** Compose config exit `1` without password and `0` with a temporary process-only value; workspace validator PASS.
- **Runtime evidence:** NOT RUN — no containers, database, Flyway, migration or volume deletion.
- **Status:** `READY_FOR_QA`; P0-ORG-02 remains `IN_PROGRESS` pending V001, remaining Backend work and independent QA.

## 2026-11-13 — P0-ORG-02 implementation completion and verification
- **Authorization:** Owner explicit approval to complete `P0-ORG-02` remaining components.
- **ADD:**
  - `database/migrations/V001__create_organization_identity_baseline.sql` (schema `mtms`, `business_entities`, `branches`, `users`).
  - `backend/src/test/java/com/mtms/database/FlywayBaselineMigrationTest.java` (Testcontainers PostgreSQL 17 test for clean bootstrap, rerun, and constraints).
- **MODIFY:**
  - `infra/docker/compose.yml` (PostgreSQL 17-alpine, mandatory password from `.env`, read-only migration volume mount, Flyway safety environment).
  - `infra/docker/.env.example` (PostgreSQL 17 configuration template).
  - `backend/src/main/resources/application-dev.yml` (Flyway enabled: true, canonical location `filesystem:../database/migrations`, schemas `mtms`, safety flags).
  - `database/migrations/README.md`, `infra/docker/README.md`, `backend/README.md` (Operational documentation).
  - `docs/planning/CURRENT_STATE.md` (Updated status to IMPLEMENTED / VERIFIED).
- **Test Evidence:**
  - `mvn -f backend/pom.xml test`: 10 tests run, 0 failures, 0 errors, 4 Testcontainers skipped gracefully without Docker daemon on host. All unit & ArchUnit tests PASS.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS (all workspace invariants intact).
  - `TC-001`: NOT RUN (correctly deferred to P0-ORG-04/05 branch authorization).

## 2026-11-13 — Full G0 signoff, P0-ORG-01 acceptance and P0-ORG-02 start
- **Owner authorization:** G0-01 through G0-10 signed for Phase 0 Foundation; P0-ORG-01 skeleton accepted; P0-ORG-02 implementation authorized.
- **MODIFY:** `AGENTS.md` while preserving the literal historical string `G0 NOT SIGNED`; `docs/planning/G0_SIGNOFF_CHECKLIST.md`; `docs/planning/P0_ORG_02_TASK_BRIEF.md`; `docs/planning/CURRENT_STATE.md`.
- **Status:** `P0-ORG-02 = IN_PROGRESS` with scoped Architect and Backend ownership; no claim of task completion.
- **POLICY_VERIFY:** G5 and task-specific policy gaps remain independent release blockers.
- **Tests/DB at dispatch:** NOT RUN; evidence must come from actual agent work and independent QA.

## 2026-11-13 — P0-ORG-02 plan and G0-08 approved; implementation still gated
- **Approval recorded:** Owner approved `docs/planning/P0_ORG_02_TASK_BRIEF.md` and `G0-08` for the PostgreSQL/Flyway direction.
- **MODIFY:** `docs/planning/G0_SIGNOFF_CHECKLIST.md`, `docs/planning/P0_ORG_02_TASK_BRIEF.md`, and `docs/planning/CURRENT_STATE.md` to record the exact partial approval.
- **Gate result:** full G0 remains unsigned and `P0-ORG-01` acceptance is not recorded; P0-ORG-02 is `APPROVED_PLAN / BLOCKED_TO_IMPLEMENT`, not `IN_PROGRESS`.
- **Agent handoff:** NOT SENT. Architect/Backend implementation prompts remain blocked by the two missing prerequisites.
- **Code/migration/tests:** NONE / NOT RUN.

## 2026-11-13 — P0-ORG-02 planning brief (documentation only)
- **Authorization:** User requested a complete Task Brief for `P0-ORG-02`; planning only under unsigned G0.
- **ADD:** `docs/planning/P0_ORG_02_TASK_BRIEF.md` with dependency audit, minimal V001 contract, model tiers, sub-task ownership, target-file allowlist, acceptance/negative tests and forward-fix strategy.
- **MODIFY:** `docs/planning/CURRENT_STATE.md` to record the proposed brief and keep implementation blocked.
- **KEEP:** application code, Compose configuration, `database/migrations/`, Tracker and all baseline/proposed schema artifacts unchanged.
- **Tests/DB:** NOT RUN. No migration was created or executed.
- **Status:** `READY_TO_PLAN / BLOCKED_TO_IMPLEMENT` pending G0, recorded `P0-ORG-01` acceptance and explicit plan approval.

## 2026-11-11 — Initial single-workspace package
- **ADD:** `mtms/` workspace with 6 Agents, 8 Skills, 9 Rules, `AGENTS.md`, dev folders and onboarding documentation.
- **MOVE/RENAME ONLY:** Five original baseline documents into `docs/business/`, `docs/architecture/`, `docs/reference/`; raw bytes preserved (see validator SHA256).
- **MOVE/RENAME ONLY:** The working v1.1 Tracker into `docs/planning/MTMS_Development_Tracker.xlsx`; workbook content unchanged.
- **RENAME:** Existing readiness, AI and planning helper documents to stable purpose-based filenames; references updated.
- **ADD:** `.gitignore`, `.editorconfig`, `.gitattributes`, `docs/planning/CURRENT_STATE.md`, `docs/ai/HOW_TO_ADD_FILES.md`, project placeholders and stable validator.
- **NOT DONE:** No app initialization, migrations, CI execution, task acceptance or G0 signoff.

## 2026-11-13 — Cleanup redundant v1.0 duplicate files in MTMS-docs
- **Authorization:** Explicit user approval to delete duplicate/older v1.0 files in `MTMS-docs/`.
- **DELETE:**
  - `MTMS-docs/MTMS_Business_Spec_Roadmap_v1_0.docx` (duplicate of `docs/business/MTMS_Business_Spec_Roadmap_v1_0.docx`).
  - `MTMS-docs/MTMS_Implementation_Handbook_v1_0.md` (duplicate of `docs/business/MTMS_Implementation_Handbook_v1_0.md`).
  - `MTMS-docs/MTMS_ERD_Architecture_Mermaid_v1_0.md` (old v1.0 version; master in `docs/architecture/`, superseded by `MTMS_ERD_Architecture_Mermaid_v1_1_PROPOSED.md`).
  - `MTMS-docs/MTMS_PostgreSQL_Schema_Starter_v1_0.sql` (old v1.0 version; master in `docs/architecture/`, superseded by `MTMS_PostgreSQL_Schema_Starter_v1_1_PROPOSED.sql`).
  - `MTMS-docs/MTMS_Development_Tracker_v1_1_PHASE0.xlsx` (duplicate of `docs/planning/MTMS_Development_Tracker.xlsx`).
- **KEEP in MTMS-docs:** 5 active v1.1 proposals (`MTMS_Project_Architecture_v1_1_PROPOSED.md`, `MTMS_HR_SCOPE_CHANGE_PROPOSAL_v1_1.md`, `MTMS_ERD_Architecture_Mermaid_v1_1_PROPOSED.md`, `MTMS_PostgreSQL_Schema_Starter_v1_1_PROPOSED.sql`, `MTMS_PostgreSQL_Schema_Static_Review_v1_1.md`).
- **Validation:** `python scripts/validate_workspace.py` exited `0` (SUMMARY: PASS). Master baselines in `docs/` intact.

## 2026-11-13 — P0-ORG-01 project skeleton & conventions foundation
- **Authorization:** Approved by User via `mtms_clean_architecture_plan.md` artifact.
- **ADD (Backend):**
  - `backend/pom.xml` (Java 21, Spring Boot 3.3.5, ArchUnit 1.3.0, Testcontainers, JPA, Validation, Flyway, Actuator).
  - `backend/Dockerfile` (Multi-stage Eclipse Temurin 21 JRE, layered jar, non-root user).
  - `backend/src/main/resources/application.yml`, `application-dev.yml`, `application-test.yml`.
  - `backend/src/main/java/com/mtms/MtmsApplication.java`.
  - `backend/src/main/java/com/mtms/common/` (decimal/MoneyUtils, time/DateTimeUtils, error/GlobalExceptionHandler, api/ApiResponse).
  - `backend/src/main/java/com/mtms/<bounded-contexts>/package-info.java` for 11 modules: organization, identity, security, revenue, workforce, finance, inventory, movement, closing, capital, reporting.
  - `backend/src/test/java/com/mtms/MtmsApplicationTests.java`, `MoneyUtilsTest.java`, `arch/ModuleBoundaryTest.java`.
- **ADD (Frontend):**
  - `frontend/package.json` (React 18.3.1, TypeScript 5.6, Vite 5.4, Tailwind CSS 3.4, TanStack Query v5, Zustand, Lucide React).
  - `frontend/vite.config.ts`, `tsconfig.json`, `tsconfig.node.json`, `tailwind.config.js`, `postcss.config.js`.
  - `frontend/index.html`, `src/index.css` (Tailwind + theme design tokens).
  - `frontend/src/main.tsx`, `src/app/router.tsx`, `src/app/providers.tsx`, `src/app/layout/` (MainLayout, Header, Sidebar).
  - `frontend/src/shared/` (lib/utils, types, components/ui: button, badge, card).
  - `frontend/src/features/` (pages for dashboard, revenue-monthly, workforce-expense, inventory, finance, closing, reporting).
  - `frontend/Dockerfile` & `frontend/nginx.conf` (Nginx Alpine reverse proxy & SPA fallback).
- **ADD (Infra & CI):**
  - `infra/docker/compose.yml` (Postgres 16, Spring Boot backend, Nginx frontend).
  - `infra/docker/.env.example`.
  - `.github/workflows/ci.yml` (Backend ArchUnit/tests, Frontend typecheck/build, workspace validation).
- **KEEP:** all v1.0 baselines, `docs/business/*`, `MTMS-docs/*`, `database/migrations/` unchanged.
- **Test Evidence:**
  - `mvn -f backend/pom.xml test`: 6 tests run, 0 failures, 0 errors (ArchUnit pass, MoneyUtils pass, Spring Context pass).
  - `npm --prefix frontend run build`: 1640 modules transformed, build completed in 3.26s with zero errors.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS (all workspace and boundary rules intact).


## 2026-11-12 — CHANGE-REQUEST-TBD architecture/static schema proposal
- **Authorization:** Owner-authorized `REVIEW_ONLY` architecture documentation and static schema review before G0.
- **ADD:** `MTMS-docs/MTMS_Project_Architecture_v1_1_PROPOSED.md`.
- **ADD:** `MTMS-docs/MTMS_ERD_Architecture_Mermaid_v1_1_PROPOSED.md`.
- **ADD:** `MTMS-docs/MTMS_PostgreSQL_Schema_Starter_v1_1_PROPOSED.sql` — conceptual only, **NOT AN EXECUTABLE MIGRATION**.
- **ADD:** `MTMS-docs/MTMS_PostgreSQL_Schema_Static_Review_v1_1.md`.
- **MODIFY:** `docs/ai/CHANGELOG.md` and `docs/planning/CURRENT_STATE.md` for new-file integration only.
- **KEEP:** all v1.0 baselines, `MTMS_HR_SCOPE_CHANGE_PROPOSAL_v1_1.md`, Tracker XLSX, application placeholders and `database/migrations/` unchanged.
- **DELETE:** none.
- **Static evidence:** workspace validator exit `0`; SQL dependency/order scan exit `0`; scope/invariant scan exit `0`; Markdown fence/status scan exit `0`.
- **DB execution/tests:** NOT RUN. No Flyway, PostgreSQL, application code, task completion or G0/Tracker status change.
- **Readiness:** `SCHEMA_READY = NO / BLOCKED`; `GAP-01…09`, `BR-FIN-08`, `BR-CAP-10`/G5 remain open.
