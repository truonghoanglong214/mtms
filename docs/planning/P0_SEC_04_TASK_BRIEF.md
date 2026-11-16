# Task brief — P0-SEC-04 (Excel import staging)

- **G0 status/evidence:** SIGNED on 2026-11-13 (Phase 0 Foundation approved).
- **Parent Task:** `P0-SEC-04` — Excel import staging
- **FE child:** `FE-P0-07` — Excel import staging wizard (Upload > Preview & Validate > Row Errors > Commit once)
- **BR IDs:** `BR-SEC-02` (Excel & CSV import staging)
- **TC IDs:** `TC-045` (Excel import duplicate identifiers rejected with row-level error reports)
- **ADR / POLICY_VERIFY:** None blocking. All calculations use exact decimal/string comparisons. CSV formula injection protection applied.
- **Dependency IDs + verified status:**
  - `P0-ORG-05` (Scoped permissions & `@branchAuth`): VERIFIED.
  - `P0-SEC-01` (Private storage & file handling baseline): VERIFIED.
  - `P0-SEC-03` (Idempotency and concurrency control): VERIFIED.
- **Owner / reviewers:** Lead Orchestrator, Backend Specialist (`mtms-spring-boot-backend`), Frontend Specialist (`mtms-react-frontend`), QA Reviewer.
- **Task scope and explicit non-goals:**
  - Scope: Staging engine for Excel (XLSX, XLS) and CSV files. Upload -> Parse -> Formula Sanitization -> In-memory validation (duplicate identifiers, required fields, data types, units) -> Staged persistence in `mtms.import_batches` and `mtms.import_rows` -> Row-level error reporting -> Idempotent commit once verified.
  - Non-goals: NO POS operations, NO automatic inventory consumption, NO real ledger postings in Phase 0. Target schemas store validated normalized rows ready for domain consumers (e.g. `P0-INV-02` opening inventory).
- **Existing files to inspect:**
  - `docs/architecture/MTMS_PostgreSQL_Schema_Starter_v1_0.sql` (reference for `import_batches` and `import_rows`)
  - `backend/src/main/java/com/mtms/common/security/BranchAuthorizationService.java`
  - `backend/src/main/java/com/mtms/common/idempotency/IdempotencyFilter.java`
  - `frontend/src/features/organization/OrganizationAdminPage.tsx`
- **Allowed file paths after approval:**
  - `database/migrations/V007__create_excel_import_staging.sql`
  - `backend/src/main/java/com/mtms/importer/**`
  - `backend/src/test/java/com/mtms/importer/**`
  - `frontend/src/features/importer/**`
  - `frontend/src/features/organization/OrganizationAdminPage.tsx`
- **API/DB contract changes:**
  - Tables: `mtms.import_batches`, `mtms.import_rows`.
  - Endpoints:
    - `POST /api/v1/branches/{branchId}/imports/stage` (multipart file + `importKind`)
    - `GET /api/v1/branches/{branchId}/imports/{batchId}`
    - `GET /api/v1/branches/{branchId}/imports/{batchId}/rows` (with optional status filter)
    - `POST /api/v1/branches/{branchId}/imports/{batchId}/commit` (requires `@Idempotent`)
- **Acceptance criteria:**
  1. Support XLSX, XLS, and CSV parsing with Apache POI & CSV parser.
  2. CSV formula injection protection: neutralize cells starting with `=`, `+`, `-`, `@`.
  3. `TC-045`: Reject duplicate identifiers within the imported batch with clear row-level reasons (e.g., "Duplicate item_code 'CF-01' found in rows 2, 5").
  4. Unit & malformed data validation: Reject missing required fields, blank units, negative numbers.
  5. Branch security: `@branchAuth` enforced on all staging and commit endpoints; cross-branch access returns 403.
  6. Idempotent commit: Commit operation uses `@Idempotent` and cannot be committed multiple times.
  7. Frontend Wizard: Multi-step UI for upload, preview summary, row error inspector table, and commit.
- **Negative tests / concurrency / retry:**
  - TC-045 duplicate identifier rejection.
  - Cross-branch access prohibition (HTTP 403).
  - Malformed file / formula injection safety.
  - Replay of commit with duplicate idempotency key.
- **Plan approval evidence:** APPROVED by User ("tiếp tục lên kế hoạch và triển khai phần tiếp theo đi").
- **Build and test evidence:** 
  - Backend: `mvn test` executed on 2026-11-16 (113 tests run, 0 failures, 0 errors, 0 skipped).
    - `ImportIntegrationTest`: 6 tests passing (XLSX staging & idempotent commit, CSV staging, TC-045 duplicate identifier rejection, data validation & formula sanitization, cross-branch 403 access control, recommit conflict 409).
    - `FlywayBaselineMigrationTest`: V007 constraint tests passing.
    - `ModuleBoundaryTest`: ArchUnit module isolation passing.
  - Frontend: `npm run build` executed on 2026-11-16 (0 TypeScript compile errors, 2552 modules transformed cleanly).
- **QA decision:** VERIFIED (All acceptance criteria and TC-045 verified with automated test evidence).
- **User acceptance:** PENDING OWNER SIGN-OFF.
- **Tracker update proposal:** PROPOSE STATUS 'Done' FOR P0-SEC-04 AND FE-P0-07.
