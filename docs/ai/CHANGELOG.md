# MTMS — File changes and decisions

## 2026-11-16 — P0-SEC-03 idempotency & optimistic locking concurrency policy
- **Authorization:** Owner approval to implement `P0-SEC-03` using parallel subagent coordination.
- **Requirements & Rules:** `BR-SEC-03`, `TC-011`, `01-financial-data-safety.md`, `07-backend-quality.md`.
- **ADD:**
  - `database/migrations/V006__create_idempotency_records.sql` (bảng `mtms.idempotency_records`, unique constraint `(actor_user_id, idempotency_key)`, TTL index `idx_idempotency_expires_at`).
  - `backend/src/main/java/com/mtms/common/idempotency/IdempotencyStatus.java` (`IN_PROGRESS`, `COMPLETED`, `FAILED`).
  - `backend/src/main/java/com/mtms/common/idempotency/IdempotencyRecord.java` (domain entity).
  - `backend/src/main/java/com/mtms/common/idempotency/IdempotencyRecordJpaEntity.java` & `IdempotencyRecordJpaRepository.java`.
  - `backend/src/main/java/com/mtms/common/idempotency/AcquireStatus.java`, `AcquireResult.java`, `CurrentActorProvider.java`.
  - `backend/src/main/java/com/mtms/identity/infrastructure/security/SecurityContextActorProvider.java` (adapter).
  - `backend/src/main/java/com/mtms/common/idempotency/IdempotencyService.java` (`tryAcquire` with REQUIRES_NEW transaction, hash verification, `recordSuccess`, `recordFailure`).
  - `backend/src/main/java/com/mtms/common/idempotency/Idempotent.java` (custom controller annotation).
  - `backend/src/main/java/com/mtms/common/idempotency/CachedBodyHttpServletRequest.java` & `IdempotencyFilter.java` (request caching, replay with `Idempotent-Replayed: true`).
  - `backend/src/main/java/com/mtms/common/idempotency/IdempotencyConflictException.java` & `IdempotencyPayloadMismatchException.java`.
  - `backend/src/test/java/com/mtms/common/idempotency/IdempotencyIntegrationTest.java` (8 integration tests: TC-011 duplicate request deduplication, cache replay, payload mismatch 422, in-progress conflict 409, optimistic lock translation 409).
  - `frontend/src/shared/utils/idempotency.ts` (UUID v4 generator, mutation method checks, RFC 7807 error types).
  - `frontend/src/shared/stores/concurrencyNotificationStore.ts` (Zustand notification store with deduplication and timeout).
  - `frontend/src/shared/components/ConcurrencyNotification.tsx` & `ConcurrencyConflictToast.tsx` (Dark-mode UI, Spring animation, instant reload action).
  - `frontend/src/app/api/client.ts` (convenience alias).
  - `docs/planning/P0_SEC_03_TASK_BRIEF.md`.
- **MODIFY:**
  - `backend/src/main/java/com/mtms/common/error/GlobalExceptionHandler.java` (added RFC 7807 handlers for `OptimisticLockingFailureException`, `OptimisticLockException`, `IdempotencyConflictException`, and `IdempotencyPayloadMismatchException`).
  - `backend/src/main/java/com/mtms/organization/web/BranchBankController.java` (annotated `@Idempotent` on bank account creation and branch activation).
  - `backend/src/main/java/com/mtms/identity/web/ScopedRoleGrantController.java` (annotated `@Idempotent` on grant role).
  - `backend/src/test/java/com/mtms/database/FlywayBaselineMigrationTest.java` (updated for V006 bootstrap and constraint verification).
  - `frontend/src/shared/api/apiClient.ts` (Axios interceptors for mutation `Idempotency-Key` injection, replay detection, and 409/422 error handlers).
  - `frontend/src/app/providers.tsx` (mounted `<ConcurrencyNotification />`).
  - `docs/planning/CURRENT_STATE.md`.
- **Build & Test Evidence:**
  - `mvn test`: 106 tests run, 0 failures, 0 errors, 100% green.
  - `npm --prefix frontend run build`: Clean build in 22s, zero TypeScript errors.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS.

## 2026-11-16 — P0-SEC-02 audit event spine & activity log
- **Authorization:** Owner approval to implement `P0-SEC-02` using parallel subagent coordination.
- **Requirements & Rules:** `BR-SEC-03`, `TC-041`, `03-branch-authorization.md`, `04-accounting-postings.md`.
- **ADD:**
  - `database/migrations/V005__create_audit_events.sql` (bảng `mtms.audit_events`, indices `idx_audit_target`, `idx_audit_branch`, `idx_audit_actor`).
  - `backend/src/main/java/com/mtms/audit/domain/AuditEvent.java` (immutable domain entity with JSON snapshots).
  - `backend/src/main/java/com/mtms/audit/domain/AuditEventRepository.java`.
  - `backend/src/main/java/com/mtms/audit/infrastructure/persistence/AuditEventJpaEntity.java` (`@JdbcTypeCode(SqlTypes.JSON)` for JSONB mapping).
  - `backend/src/main/java/com/mtms/audit/infrastructure/persistence/AuditEventJpaRepository.java`.
  - `backend/src/main/java/com/mtms/audit/infrastructure/persistence/AuditEventRepositoryImpl.java`.
  - `backend/src/main/java/com/mtms/audit/application/dto/AuditEventDto.java`, `RecordAuditEventCommand.java`.
  - `backend/src/main/java/com/mtms/audit/application/AuditLogService.java` (append-only recordEvent, listEventsByBranch, listGlobalEvents, findByTarget).
  - `backend/src/main/java/com/mtms/audit/web/AuditController.java` (`GET /api/v1/audit-events`, `GET /api/v1/branches/{id}/audit-events`, `GET /api/v1/audit-events/target`).
  - `backend/src/test/java/com/mtms/audit/AuditIntegrationTest.java` (9 integration tests: record & query, JSON snapshot preservation, branch isolation 403, system admin access, non-admin 403, sensitive event hooks).
  - `frontend/src/features/audit/types.ts`, `auditApi.ts`, `index.ts`.
  - `frontend/src/features/audit/components/AuditLogTimeline.tsx` (timeline, smart filters, event badges, actor info, Before/After Snapshot visual diff view & raw JSON view, pagination).
  - `docs/planning/P0_SEC_02_TASK_BRIEF.md`.
- **MODIFY:**
  - `backend/src/main/java/com/mtms/organization/application/BranchBankService.java` (hooked audit events: `BRANCH_ACTIVATED`, `BANK_ACCOUNT_ADDED`).
  - `backend/src/main/java/com/mtms/identity/application/UserService.java` (hooked audit event: `USER_STATUS_CHANGED`).
  - `backend/src/main/java/com/mtms/identity/application/ScopedPermissionService.java` (hooked audit events: `ROLE_GRANTED`, `ROLE_REVOKED`).
  - `backend/src/test/java/com/mtms/arch/ModuleBoundaryTest.java` (included `audit` in ArchUnit boundary tests).
  - `backend/src/test/java/com/mtms/database/FlywayBaselineMigrationTest.java` (added assertions for V005 audit events table, DDL, constraints).
  - `frontend/src/features/organization/pages/OrganizationAdminPage.tsx` (integrated Tab 3 "Nhật ký kiểm toán" and quick audit jump button).
  - `docs/planning/CURRENT_STATE.md`.
- **Build & Test Evidence:**
  - `mvn test`: 97 tests run, 0 failures, 0 errors, 100% green.
  - `npm --prefix frontend run build`: Clean build in 21s, zero TypeScript errors.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS.

## 2026-11-16 — P0-SEC-01 private evidence storage & TC-043 Signed URL branch boundary
- **Authorization:** Owner approval to implement `P0-SEC-01` using parallel subagent coordination.
- **Requirements & Rules:** `BR-SEC-01`, `TC-043`, `FE-P0-06`, `03-branch-authorization.md`, `08-sensitive-data.md`.
- **ADD:**
  - `database/migrations/V004__create_evidence_files.sql` (bảng `mtms.evidence_files`, constraints visibility & byte size > 0, indices scope & entity).
  - `backend/src/main/java/com/mtms/evidence/domain/VisibilityClass.java` (`BRANCH_PRIVATE`, `ENTITY_PRIVATE`, `GLOBAL_ADMIN_ONLY`).
  - `backend/src/main/java/com/mtms/evidence/domain/EvidenceFile.java` (immutable domain entity with 5MB validation, MIME whitelist, SHA-256 integrity, path traversal protection).
  - `backend/src/main/java/com/mtms/evidence/domain/EvidenceFileRepository.java`.
  - `backend/src/main/java/com/mtms/evidence/infrastructure/storage/StorageService.java` & `LocalStorageService.java` (private storage outside webroot).
  - `backend/src/main/java/com/mtms/evidence/infrastructure/persistence/EvidenceFileJpaEntity.java`, `EvidenceFileJpaRepository.java`, `EvidenceFileRepositoryImpl.java`.
  - `backend/src/main/java/com/mtms/evidence/application/EvidenceTokenService.java` (HMAC-SHA256 signed download tokens with 15-min TTL, constant-time verification).
  - `backend/src/main/java/com/mtms/evidence/application/EvidenceService.java` (upload with SHA-256 checksum & MIME check, download token generation with branch check, content streaming).
  - `backend/src/main/java/com/mtms/evidence/application/dto/EvidenceFileDto.java`, `EvidenceUploadResponse.java`, `DownloadTokenResponse.java`, `FilePayload.java`.
  - `backend/src/main/java/com/mtms/evidence/web/EvidenceController.java` (`POST /branches/{id}/evidence`, `GET /branches/{id}/evidence`, `POST /evidence/{id}/token`, `GET /evidence/{id}/content?token=...`).
  - `backend/src/test/java/com/mtms/evidence/EvidenceIntegrationTest.java` (7 integration tests: PDF/PNG upload, 5MB limit, MIME reject, TC-043 cross-branch 403, tampered token 403, expired token 403, token mismatch 403, stream download).
  - `frontend/src/features/evidence/types.ts`, `evidenceApi.ts`, `utils.ts`, `index.ts`.
  - `frontend/src/features/evidence/components/EvidenceDropzone.tsx` (drag & drop, client validation 5MB / types, progress bar, toast).
  - `frontend/src/features/evidence/components/EvidencePreviewModal.tsx` (HD zoom/rotate image & secure PDF viewer with auto signed token fetch).
  - `frontend/src/features/evidence/components/BranchEvidenceManagerModal.tsx` (branch evidence management dialog).
  - `docs/planning/P0_SEC_01_TASK_BRIEF.md`.
- **MODIFY:**
  - `backend/src/main/java/com/mtms/common/error/GlobalExceptionHandler.java` (handled `MaxUploadSizeExceededException`).
  - `backend/src/main/java/com/mtms/identity/infrastructure/security/SecurityConfig.java` (permitted public signed token stream `/api/v1/evidence/*/content`).
  - `backend/src/test/java/com/mtms/arch/ModuleBoundaryTest.java` (included `evidence` in module architecture boundary checks).
  - `backend/src/test/java/com/mtms/database/FlywayBaselineMigrationTest.java` (added assertions for V004 evidence files table and constraints).
  - `frontend/src/features/organization/pages/OrganizationAdminPage.tsx` (added "Chứng từ" action button and modal integration).
  - `.gitignore` (ignored `**/mtms-storage/`).
  - `docs/planning/CURRENT_STATE.md`.
- **Build & Test Evidence:**
  - `mvn clean test`: 87 tests run, 0 failures, 0 errors, 0 skipped. All green (ArchUnit, Flyway 7/7, EvidenceIntegrationTest 7/7, TC-043 verified).
  - `npm --prefix frontend run build`: Clean build in 32s, zero TypeScript errors.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS.

## 2026-11-15 — P0-ORG-07 branch bank onboarding & TC-003 Go-live readiness gate
- **Authorization:** Owner approval to implement `P0-ORG-07` using parallel subagent coordination.
- **Requirements & Rules:** `BR-ORG-03`, `TC-003`, `FE-P0-05`, `03-branch-authorization.md`, `08-sensitive-data.md`.
- **ADD:**
  - `database/migrations/V003__create_branch_bank_accounts.sql` (bảng `mtms.branch_bank_accounts`, `ck_branch_bank_accounts_status`, `idx_branch_bank_accounts_branch_id`, partial unique index `uq_branch_primary_bank`).
  - `backend/src/main/java/com/mtms/organization/domain/BankAccountStatus.java` (`ACTIVE`, `INACTIVE`).
  - `backend/src/main/java/com/mtms/organization/domain/BranchBankAccount.java` (domain model with invariant validation).
  - `backend/src/main/java/com/mtms/organization/domain/BranchBankAccountRepository.java`.
  - `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BranchBankAccountJpaEntity.java`.
  - `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BranchBankAccountJpaRepository.java`.
  - `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BranchBankAccountRepositoryImpl.java`.
  - `backend/src/main/java/com/mtms/organization/application/dto/CreateBankAccountCommand.java`.
  - `backend/src/main/java/com/mtms/organization/application/dto/BranchBankAccountDto.java` (safe public DTO, masked account numbers).
  - `backend/src/main/java/com/mtms/organization/application/dto/BranchReadinessDto.java` (readiness status, missingRequirements).
  - `backend/src/main/java/com/mtms/organization/application/exception/BranchNotReadyException.java`.
  - `backend/src/main/java/com/mtms/organization/application/BankAccountCryptoUtils.java` (AES encryption at-rest, masking).
  - `backend/src/main/java/com/mtms/organization/application/BranchBankService.java` (add, list, set primary, readiness check, activate branch).
  - `backend/src/main/java/com/mtms/organization/web/BranchBankController.java` (`GET/POST /api/v1/branches/{id}/bank-accounts`, `PATCH .../primary`, `GET .../readiness`, `PATCH .../activate` with `@branchAuth`).
  - `backend/src/test/java/com/mtms/organization/BranchBankIntegrationTest.java` (7 integration tests: TC-003 missing bank blocks activation, adding bank allows activation, primary toggle, branch isolation 403, RBAC).
  - `frontend/src/features/organization/components/BranchBankModal.tsx` (accessible modal, Viet bank dropdown, masked numbers, primary badge, readiness alert & activation button).
  - `docs/planning/P0_ORG_07_TASK_BRIEF.md`.
- **MODIFY:**
  - `backend/src/main/java/com/mtms/identity/infrastructure/security/BranchAuthorizationService.java` (enhanced multi-role and action scope matching).
  - `backend/src/test/java/com/mtms/database/FlywayBaselineMigrationTest.java` (added assertions for V003 schema, constraints, and partial unique index).
  - `frontend/src/features/organization/types.ts` (added bank account and readiness DTOs).
  - `frontend/src/features/organization/organizationApi.ts` (added 5 bank and readiness endpoints).
  - `frontend/src/features/organization/pages/OrganizationAdminPage.tsx` (added bank action button and modal integration).
  - `docs/planning/CURRENT_STATE.md`.
- **Build & Test Evidence:**
  - `mvn clean test`: 78 tests run, 0 failures, 0 errors, 0 skipped. All green (ArchUnit, Flyway 6/6, BranchBankIntegrationTest 7/7, TC-003 verified).
  - `npm --prefix frontend run build`: Clean build in 21s, zero TypeScript errors.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS.

## 2026-11-14 — P0-ORG-06 org and users admin UI implementation and verification
- **Authorization:** Owner explicit approval to implement `P0-ORG-06`.
- **Requirements & Rules:** `BR-ORG-01`, `BR-ORG-04`, `TC-001`, `TC-003`, `FRONTEND_DESIGN_SYSTEM.md`, `03-branch-authorization.md`.
- **ADD:**
  - `backend/src/main/java/com/mtms/identity/application/dto/CreateUserCommand.java` (username validation, password min 8, display name).
  - `backend/src/main/java/com/mtms/identity/application/dto/UpdateUserStatusCommand.java` (status transition: ACTIVE / SUSPENDED).
  - `backend/src/main/java/com/mtms/identity/application/UserService.java` (user onboarding with BCrypt, listing with active role grants, status update).
  - `backend/src/main/java/com/mtms/identity/web/UserController.java` (`GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PATCH /api/v1/users/{id}/status`, protected by `@branchAuth.isSystemAdmin()`).
  - `backend/src/test/java/com/mtms/identity/UserControllerTest.java` (8 integration tests: unauthenticated 401, non-admin 403 TC-001 negative test, duplicate 409 conflict, validation 400 bad request, admin create/list/status 200/201).
  - `frontend/src/features/organization/types.ts` (Entity, Branch, User, ScopedGrant DTOs & command types).
  - `frontend/src/features/organization/organizationApi.ts` (REST client for entities, branches, users, and grants).
  - `frontend/src/features/organization/components/CreateEntityModal.tsx` (accessible modal for entity onboarding).
  - `frontend/src/features/organization/components/CreateBranchModal.tsx` (accessible modal for branch onboarding).
  - `frontend/src/features/organization/components/CreateUserModal.tsx` (accessible modal for employee/user onboarding).
  - `frontend/src/features/organization/components/GrantRoleModal.tsx` (interactive modal with **Permission Preview** matrix, role descriptions, and temporal constraints).
  - `frontend/src/features/organization/pages/OrganizationAdminPage.tsx` (high-density split tabs: Entities & Branches accordion, Users & Grants with role badges, status toggles, revoke grant actions, search & filters).
  - `docs/planning/P0_ORG_06_TASK_BRIEF.md`.
- **MODIFY:**
  - `frontend/src/features/auth/types.ts` (added `RoleKey`, `ScopedRoleGrantDto`, and aligned `UserDto.grants`).
  - `frontend/src/app/layout/Sidebar.tsx` (added `Cơ cấu & Phân quyền` navigation item with `Building2` icon).
  - `frontend/src/app/router.tsx` (lazy code-split route for `/admin/organization`).
  - `docs/planning/CURRENT_STATE.md`.
- **Build & Test Evidence:**
  - `mvn clean test`: 70 tests run, 0 failures, 0 errors, 0 skipped. All green (ArchUnit 2/2, Flyway 10/10, UserControllerTest 8/8, TC-001 boundary tests all PASS).
  - `npm --prefix frontend run build`: Clean build in 6.6s, code-split chunk `OrganizationAdminPage-*.js` (44.61 kB), zero TypeScript errors.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS.

## 2026-11-14 — P0-ORG-05 scoped permissions implementation and verification
- **Authorization:** Owner explicit approval to implement `P0-ORG-05`.
- **Requirements & Rules:** `BR-ORG-04`, `TC-001`, `03-branch-authorization.md`, `01-financial-data-safety.md`.
- **ADD:**
  - `database/migrations/V002__create_scoped_role_grants.sql` (composite FK to branches, valid date range check, branch requirement check, lookup index).
  - `backend/src/main/java/com/mtms/identity/domain/RoleKey.java` (`SYSTEM_ADMIN`, `BRANCH_OWNER`, `MANAGER`, `FINANCE`, `EMPLOYEE`, `INVESTOR`).
  - `backend/src/main/java/com/mtms/identity/domain/ScopedRoleGrant.java` (pure domain entity with date range and branch invariants).
  - `backend/src/main/java/com/mtms/identity/infrastructure/persistence/ScopedRoleGrantJpaEntity.java` & `ScopedRoleGrantJpaRepository.java`.
  - `backend/src/main/java/com/mtms/identity/infrastructure/security/BranchAuthorizationService.java` (`@branchAuth` server-side authorization engine).
  - `backend/src/main/java/com/mtms/identity/application/ScopedPermissionService.java` (grant, revoke, list active grants).
  - `backend/src/main/java/com/mtms/identity/application/dto/GrantRoleCommand.java`, `ScopedRoleGrantDto.java`.
  - `backend/src/main/java/com/mtms/identity/web/ScopedRoleGrantController.java` (`POST /api/v1/auth/grants`, `GET /api/v1/auth/grants/user/{userId}`, `DELETE /api/v1/auth/grants/{grantId}`).
  - `backend/src/test/java/com/mtms/identity/ScopedPermissionServiceTest.java`.
  - `backend/src/test/java/com/mtms/identity/BranchAuthorizationSecurityTest.java` (TC-001 negative branch isolation & temporal expiry test suite).
  - `docs/planning/P0_ORG_05_TASK_BRIEF.md`.
- **MODIFY:**
  - `backend/src/main/java/com/mtms/identity/infrastructure/security/UserPrincipal.java` (loaded scoped granted authorities).
  - `backend/src/main/java/com/mtms/identity/infrastructure/security/JwtAuthenticationFilter.java` (resolved active role grants at request time).
  - `backend/src/main/java/com/mtms/identity/infrastructure/security/SecurityConfig.java` (`@EnableMethodSecurity`, `accessDeniedHandler` with RFC 7807 403 Forbidden).
  - `backend/src/main/java/com/mtms/identity/application/AuthenticationService.java` (loaded user grants into UserDto on login and profile).
  - `backend/src/main/java/com/mtms/identity/application/dto/UserDto.java` (added grants list).
  - `backend/src/main/java/com/mtms/common/error/GlobalExceptionHandler.java` (handled Spring Security `AccessDeniedException` with RFC 7807 `urn:mtms:error:forbidden`).
  - `backend/src/main/java/com/mtms/organization/web/OrganizationController.java` (applied `@PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'READ')")`).
  - `backend/src/test/java/com/mtms/database/FlywayBaselineMigrationTest.java` (added V002 schema migration & constraint tests).
  - `docs/planning/CURRENT_STATE.md`.
- **Build & Test Evidence:**
  - `mvn clean test`: 62 tests run, 0 failures, 0 errors (ArchUnit 2/2, Flyway Postgres 17 10/10, Service Unit Tests, TC-001 MockMvc Integration Tests all PASS).
  - `npm --prefix frontend run build`: Clean build in 6.2s, 0 TypeScript errors.
  - `python scripts/validate_workspace.py`: SUMMARY: PASS.

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
