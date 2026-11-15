# P0-SEC-01 — Task Brief: Private Evidence Storage & Secure Time-Limited Access

**Module:** `SEC` | **Requirement:** `BR-SEC-01` | **Test Case:** `TC-043` | **FE Subtask:** `FE-P0-06`  
**Parent Epic:** Phase 0 Foundation | **Dependencies:** `P0-ORG-05`, `P0-ORG-03`  
**Execution Mode:** Single Agent Orchestrator with Dynamic Parallel Subagents (`BE` & `FE`)

---

## 1. Business Intent & Requirement
* **BR-SEC-01:** Private file storage abstraction, attachment metadata, branch-level access control, max-size/type validation, and access via authorized short-lived links with TTL.
* **TC-043 (Branch Security Boundary):** A signed evidence download link/token generated for Branch A cannot be used or accessed by a user belonging only to Branch B (reject with 403 Forbidden or opaque 404).
* **Security & Invariants:**
  * Files are stored in private storage (never public S3 or public webroot).
  * Max size: 5 MB. Allowed content types: `image/jpeg`, `image/png`, `image/webp`, `application/pdf`.
  * Computes SHA-256 checksum on ingestion to guarantee integrity.
  * Time-limited signed token (TTL: 15 minutes) using HMAC-SHA256 secret.

---

## 2. Technical Architecture & Target Files

### A. Database (PostgreSQL 17 & Flyway)
* Target File: `database/migrations/V004__create_evidence_files.sql`
* DDL Table: `mtms.evidence_files`
  * `id` UUID PK
  * `business_entity_id` UUID FK -> `mtms.business_entities(id)`
  * `branch_id` UUID FK -> `mtms.branches(id)`
  * `storage_object_key` TEXT NOT NULL UNIQUE
  * `original_filename` VARCHAR(255) NOT NULL
  * `content_type` VARCHAR(120) NOT NULL
  * `byte_size` BIGINT NOT NULL CHECK (byte_size > 0)
  * `checksum_sha256` CHAR(64) NOT NULL
  * `visibility_class` VARCHAR(32) NOT NULL DEFAULT 'BRANCH_PRIVATE' CHECK (`BRANCH_PRIVATE`, `ENTITY_PRIVATE`, `GLOBAL_ADMIN_ONLY`)
  * `retention_until` DATE
  * `uploaded_by` UUID NOT NULL FK -> `mtms.users(id)`
  * `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
  * Index: `idx_evidence_scope ON mtms.evidence_files (branch_id, created_at)`

### B. Backend (Spring Boot 3.3.5 / Java 21)
* Target Directory: `backend/src/main/java/com/mtms/evidence/`
  * `domain/`: `EvidenceFile.java`, `EvidenceFileRepository.java`, `VisibilityClass.java`
  * `infrastructure/storage/`: `StorageService.java`, `LocalStorageService.java`
  * `infrastructure/persistence/`: `EvidenceFileJpaEntity.java`, `EvidenceFileJpaRepository.java`, `EvidenceFileRepositoryImpl.java`
  * `application/dto/`: `EvidenceUploadResponse.java`, `EvidenceFileDto.java`, `DownloadTokenResponse.java`
  * `application/`: `EvidenceService.java`, `EvidenceTokenService.java`
  * `web/`: `EvidenceController.java`
  * `test/`: `backend/src/test/java/com/mtms/evidence/EvidenceIntegrationTest.java` (Verifying TC-043 negative branch test, size/type validation, token expiration).

### C. Frontend (React 18 / TypeScript / Vite / Tailwind / Lucide)
* Target Directory: `frontend/src/features/evidence/`
  * `types.ts`: `EvidenceFileDto`, `EvidenceUploadResponse`, `DownloadTokenResponse`
  * `evidenceApi.ts`: REST API client for uploading and fetching download tokens
  * `components/EvidenceDropzone.tsx`: Drag & drop file upload with size/type client validation
  * `components/EvidencePreviewModal.tsx`: Secure preview for images and PDF with signed time-limited link
  * Integrated into Organization Branch details or evidence manager

---

## 3. Definition of Done (DoD)
1. Flyway migration `V004` runs forward idempotently.
2. Max file size (5MB) and MIME type checks enforced server-side.
3. Time-limited signed token expires and rejects invalid/tampered signatures.
4. TC-043 security test verifies that Branch B user cannot access Branch A's evidence.
5. All backend tests pass (`mvn clean test`).
6. Frontend build succeeds (`npm run build`).
7. `python scripts/validate_workspace.py` passes with `SUMMARY: PASS`.
