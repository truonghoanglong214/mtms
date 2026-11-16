# Task brief — P0-SEC-05 (Monitoring and CI baseline)

- **G0 status/evidence:** SIGNED on 2026-11-13 (Phase 0 Foundation approved).
- **Parent Task:** `P0-SEC-05` — Monitoring and CI baseline
- **FE child:** None (Operations / SEC infrastructure).
- **BR IDs:** `BR-SEC-04` (Operational monitoring, CI/CD, backup & disaster recovery)
- **TC IDs:** `TC-044` (Restore backup on clean staging: Integrity and required records verified)
- **ADR / POLICY_VERIFY:** None blocking. Backup and restore scripts use standard PostgreSQL tooling (`pg_dump` / `psql` / Python SQL dump) with strict schema separation (`mtms` schema) and zero secrets in source control.
- **Dependency IDs + verified status:**
  - `P0-ORG-01` (Modular monolith skeleton): VERIFIED.
  - `P0-ORG-02` (Flyway baseline migration V001): VERIFIED.
  - `P0-SEC-01` through `P0-SEC-04` (Storage, Audit, Idempotency, Import staging): VERIFIED.
- **Owner / reviewers:** Lead Orchestrator, DevOps/Backend Specialist (`mtms-spring-boot-backend`), QA Reviewer.
- **Task scope and explicit non-goals:**
  - Scope:
    1. Operational health and readiness monitoring endpoints (`/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, database & storage custom health checks).
    2. GitHub Actions CI pipeline (`.github/workflows/ci.yml`) covering backend tests, frontend build, workspace invariants, and migration verification.
    3. Docker containerization verification (`infra/docker/compose.yml`, health checks, network isolation).
    4. Database backup and restore tooling (`scripts/backup_database.py`, `scripts/restore_database.py`) and standard runbook (`docs/operations/BACKUP_RESTORE_RUNBOOK.md`).
    5. Automated test for `TC-044` verifying clean staging restore and record integrity.
  - Non-goals:
    - NO production cloud provisioning or real external cloud backups in Phase 0 (handled at G5).
    - NO POS or transactional changes.
- **Existing files to inspect:**
  - `.github/workflows/ci.yml`
  - `infra/docker/compose.yml`
  - `backend/src/main/resources/application.yml`
  - `scripts/validate_workspace.py`
- **Allowed file paths after approval:**
  - `.github/workflows/ci.yml`
  - `backend/src/main/java/com/mtms/common/health/**`
  - `backend/src/test/java/com/mtms/operations/**`
  - `scripts/backup_database.py`
  - `scripts/restore_database.py`
  - `docs/operations/BACKUP_RESTORE_RUNBOOK.md`
  - `docs/planning/P0_SEC_05_TASK_BRIEF.md`
  - `docs/planning/CURRENT_STATE.md`
  - `docs/ai/CHANGELOG.md`
- **API/DB contract changes:**
  - No database migration needed (operates on existing `mtms` schema V001-V007).
  - Health endpoints:
    - `GET /actuator/health`
    - `GET /actuator/health/liveness`
    - `GET /actuator/health/readiness`
- **Acceptance criteria:**
  1. Health & readiness probes operational and exposed via Spring Boot Actuator.
  2. Backup script creates a clean export of all `mtms` schema tables and data.
  3. Restore script cleans or creates a target staging schema, applies dump, and audits record counts and integrity.
  4. `TC-044` automated test verifies that backing up a database with realistic data and restoring into a clean target database results in 100% record and constraint fidelity.
  5. Runbook `docs/operations/BACKUP_RESTORE_RUNBOOK.md` documents backup schedules, RTO/RPO targets, manual & automated restore rehearsal steps.
  6. CI workflow `.github/workflows/ci.yml` is validated and all jobs pass locally.
- **Negative tests / concurrency / retry:**
  - Corrupt dump detection during restore.
  - Missing prerequisite table or schema handling.
  - Non-zero exit code if integrity verification fails.
- **Plan approval evidence:** APPROVED by User ("ok triển khai giúp tôi đi").
- **Build and test evidence:** 
  - Backup & restore tooling: `scripts/backup_database.py` and `scripts/restore_database.py` tested against live Docker PostgreSQL 17 container; backup SQL generated with SHA-256 and JSON manifest; restore executed cleanly and reported `TC-044 VERIFICATION RESULT: PASS` (all 11 tables verified with exact row counts).
  - Health & readiness: Spring Boot Actuator `/actuator/health` and `SystemReadinessHealthIndicator` verified.
  - Automated integration test: `BackupRestoreVerificationTest` executed (3/3 tests passed, verifying health endpoint, TC-044 staging restore integrity audit, and corrupted schema constraint rejection).
  - Full backend test suite: 116 tests run, 0 failures, 0 errors, 100% green.
  - CI workflow: `.github/workflows/ci.yml` updated with backup/restore CLI verification and workspace policy validation.
- **QA decision:** VERIFIED (All acceptance criteria and TC-044 operational drill verified).
- **User acceptance:** PENDING OWNER SIGN-OFF.
- **Tracker update proposal:** PROPOSE STATUS 'Done' FOR P0-SEC-05.
