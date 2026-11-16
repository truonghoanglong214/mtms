# P0-SEC-03 — Task Brief: Idempotency & Optimistic Locking Concurrency Policy

**Module:** `SEC` | **Requirement:** `BR-SEC-03` | **Test Case:** `TC-011` | **FE Subtask:** Idempotent client & Concurrency handling  
**Parent Epic:** Phase 0 Foundation | **Dependencies:** `P0-ORG-02`, `P0-ORG-05`  
**Execution Mode:** Single Agent Orchestrator with Dynamic Parallel Subagents (`BE` & `FE`)

---

## 1. Business Intent & Requirement
* **BR-SEC-03:** All financial/inventory/status changes logged with deduplication key on unsafe retries.
* **TC-011 (Concurrency & Deduplication):** Two workers confirm the same posting / simultaneous duplicate request never creates duplicate financial or state postings. The first request succeeds; the duplicate either returns the cached idempotent response or reports in-progress conflict (HTTP 409).
* **Optimistic Locking:** Sensitive entities guarded with `@Version` column. Concurrent modifications are safely detected and rejected with RFC 7807 `urn:mtms:error:version-conflict` (HTTP 409 Conflict) instead of silently overwriting.

---

## 2. Technical Architecture & Target Files

### A. Database (PostgreSQL 17 & Flyway)
* Target File: `database/migrations/V006__create_idempotency_records.sql`
* DDL Table: `mtms.idempotency_records`
  * `id` UUID PK DEFAULT gen_random_uuid()
  * `actor_user_id` UUID NOT NULL FK -> `mtms.users(id)`
  * `idempotency_key` VARCHAR(160) NOT NULL
  * `request_hash` CHAR(64) NOT NULL
  * `status` VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED'))
  * `response_status` INTEGER
  * `response_headers` JSONB
  * `response_body` TEXT
  * `expires_at` TIMESTAMPTZ NOT NULL
  * `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
  * Unique Constraint: `(actor_user_id, idempotency_key)`
  * Index: `idx_idempotency_expires_at ON mtms.idempotency_records(expires_at)`

### B. Backend (Spring Boot 3.3.5 / Java 21)
* Target Directory: `backend/src/main/java/com/mtms/common/idempotency/`
  * `IdempotencyRecord.java`: Domain entity
  * `IdempotencyRecordJpaEntity.java`, `IdempotencyRecordJpaRepository.java`
  * `IdempotencyService.java`: `tryAcquire`, `recordSuccess`, `recordFailure`
  * `Idempotent.java`: Custom annotation for endpoints requiring idempotency
  * `IdempotencyInterceptor.java` or `IdempotencyFilter.java`: Intercepts `Idempotency-Key` header, caches response, returns `Idempotent-Replayed: true` on replay
* Optimistic Locking & Error Translation:
  * `GlobalExceptionHandler.java`: Add handler for `OptimisticLockingFailureException` returning RFC 7807 HTTP 409 Conflict (`urn:mtms:error:version-conflict`)
* Integration Tests:
  * `backend/src/test/java/com/mtms/common/idempotency/IdempotencyIntegrationTest.java`:
    * Test TC-011: Simultaneous duplicate requests with same idempotency key produce only 1 mutation.
    * Replayed request returns cached 200/201 response with `Idempotent-Replayed: true`.
    * Request with same key but different body payload is rejected with 422 Mismatch.
    * Optimistic lock conflict test translates to 409 Conflict.

### C. Frontend (React 18 / TypeScript / Vite / Axios)
* Target Files:
  * `frontend/src/shared/utils/idempotency.ts`: UUID generator for `Idempotency-Key`, payload hashing helper
  * `frontend/src/shared/api/apiClient.ts`: Automatic injection of `Idempotency-Key` on mutating requests and handling of `Idempotent-Replayed` header
  * `frontend/src/shared/components/ConcurrencyConflictToast.tsx` or global error notification for version conflict (HTTP 409)

---

## 3. Definition of Done (DoD)
1. Flyway migration `V006` applies forward idempotently on PostgreSQL 17.
2. Concurrent duplicate requests with same `Idempotency-Key` execute business logic only once (TC-011 verified).
3. Payload mismatch with same key is rejected with RFC 7807 error.
4. Version conflicts throw 409 Conflict with descriptive message.
5. All backend tests pass (`mvn test`).
6. Frontend compiles cleanly (`npm run build`).
7. `python scripts/validate_workspace.py` passes with `SUMMARY: PASS`.
