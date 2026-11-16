# P0-SEC-02 — Task Brief: Audit Event Spine & Immutable Activity Log

**Module:** `SEC` | **Requirement:** `BR-SEC-03` | **Test Case:** `TC-041` | **FE Subtask:** Audit Log Viewer  
**Parent Epic:** Phase 0 Foundation | **Dependencies:** `P0-ORG-05`, `P0-ORG-02`  
**Execution Mode:** Single Agent Orchestrator with Dynamic Parallel Subagents (`BE` & `FE`)

---

## 1. Business Intent & Requirement
* **BR-SEC-03:** All financial/inventory/status changes logged with who/when/why, append-only, deduplication key on unsafe retries.
* **P0-SEC-02 DoD:** Close, rate edits, expense approvals, status changes, and permission transitions leave searchable, immutable audit events. Includes actor, action, target, before-after snapshot digest, and reason.
* **Security & Invariants:**
  * Audit records are append-only; no updates or deletes allowed on `mtms.audit_events`.
  * Branch-scoped query isolation: Branch users can only view audit events belonging to their authorized branch (`@branchAuth.hasBranchAccess`), while System Admin can view cross-branch system audit events.

---

## 2. Technical Architecture & Target Files

### A. Database (PostgreSQL 17 & Flyway)
* Target File: `database/migrations/V005__create_audit_events.sql`
* DDL Table: `mtms.audit_events`
  * `id` UUID PK DEFAULT gen_random_uuid()
  * `branch_id` UUID FK -> `mtms.branches(id)` (NULL for global admin events)
  * `actor_user_id` UUID FK -> `mtms.users(id)`
  * `correlation_id` UUID
  * `target_type` VARCHAR(80) NOT NULL
  * `target_id` UUID NOT NULL
  * `event_name` VARCHAR(90) NOT NULL
  * `action_reason` TEXT
  * `old_snapshot` JSONB
  * `new_snapshot` JSONB
  * `occurred_at` TIMESTAMPTZ NOT NULL DEFAULT now()
  * Indices:
    * `idx_audit_target ON mtms.audit_events(target_type, target_id, occurred_at)`
    * `idx_audit_branch ON mtms.audit_events(branch_id, occurred_at)`
    * `idx_audit_actor ON mtms.audit_events(actor_user_id, occurred_at)`

### B. Backend (Spring Boot 3.3.5 / Java 21)
* Target Directory: `backend/src/main/java/com/mtms/audit/`
  * `domain/`: `AuditEvent.java`, `AuditEventRepository.java`
  * `infrastructure/persistence/`: `AuditEventJpaEntity.java`, `AuditEventJpaRepository.java`, `AuditEventRepositoryImpl.java`
  * `application/dto/`: `AuditEventDto.java`, `RecordAuditEventCommand.java`
  * `application/`: `AuditLogService.java`
  * `web/`: `AuditController.java` (`GET /api/v1/audit-events`, `GET /api/v1/branches/{branchId}/audit-events`)
  * `test/`: `backend/src/test/java/com/mtms/audit/AuditIntegrationTest.java` (Verifying audit event recording, searchability, branch isolation, and integration with sensitive events).

### C. Frontend (React 18 / TypeScript / Vite / Tailwind / Motion)
* Target Directory: `frontend/src/features/audit/`
  * `types.ts`: `AuditEventDto`, query parameters
  * `auditApi.ts`: `getGlobalAuditEvents`, `getBranchAuditEvents`
  * `components/AuditLogViewer.tsx`: High-density timeline view with event badges, actor info, reason, and interactive Before/After snapshot inspection
  * Integration into `OrganizationAdminPage.tsx`: Tab 3 ("Nhật ký kiểm toán") and branch action button.

---

## 3. Definition of Done (DoD)
1. Flyway migration `V005` applies cleanly and idempotently on PostgreSQL 17.
2. `AuditLogService` reliably persists immutable audit records with JSON snapshots.
3. Branch authorization properly isolates audit queries (Branch B user cannot read Branch A's audit events).
4. All existing and new backend tests pass (`mvn clean test`).
5. Frontend compiles cleanly (`npm run build`).
6. `python scripts/validate_workspace.py` passes with `SUMMARY: PASS`.
