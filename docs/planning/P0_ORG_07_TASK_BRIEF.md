# P0-ORG-07 — Task Brief: Branch Bank Onboarding & Operational Readiness

**Module:** `ORG` | **Requirement:** `BR-ORG-03` | **Test Case:** `TC-003` | **FE Subtask:** `FE-P0-05`  
**Parent Epic:** Phase 0 Foundation | **Dependencies:** `P0-ORG-03`, `P0-ORG-05`  
**Execution Mode:** Single Agent Orchestrator with Dynamic Parallel Subagents (`BE` & `FE`)

---

## 1. Business Intent & Requirement
* **BR-ORG-03:** Each branch requires at least one dedicated bank account in operational use; legal account-holder validation is out of system scope.
* **TC-003 (Go-live readiness gate):** A branch cannot pass readiness or be transitioned to `ACTIVE` without at least one active dedicated bank account. If missing, activation is blocked with a clear violation reason.
* **Security & Invariants:**
  * Exact bank account numbers must be protected at rest (masked representation `**** **** 1234` for UI/reporting, ciphertext/encrypted representation for system records).
  * Unique constraint on active primary bank account per branch (`uq_branch_primary_bank`).
  * Branch-scoped authorization: Only `ADMIN` or `OWNER` of the branch entity may onboard or change bank accounts.

---

## 2. Technical Architecture & Target Files

### A. Database (PostgreSQL 17 & Flyway)
* Target File: `database/migrations/V003__create_branch_bank_accounts.sql`
* DDL Table: `mtms.branch_bank_accounts`
  * `id` UUID PK
  * `branch_id` UUID NOT NULL FK -> `mtms.branches(id)`
  * `bank_name` VARCHAR(160) NOT NULL
  * `account_holder_name` VARCHAR(160) NOT NULL
  * `account_number_ciphertext` TEXT NOT NULL
  * `masked_account_number` VARCHAR(40) NOT NULL
  * `is_primary` BOOLEAN NOT NULL DEFAULT FALSE
  * `status` VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' CHECK (`ACTIVE`, `INACTIVE`)
  * `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
  * Unique partial index: `(branch_id)` WHERE `is_primary = TRUE AND status = 'ACTIVE'`

### B. Backend (Spring Boot 3.3.5 / Java 21)
* Target Files:
  * `backend/src/main/java/com/mtms/organization/domain/BranchBankAccount.java`
  * `backend/src/main/java/com/mtms/organization/domain/BranchBankAccountRepository.java`
  * `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BranchBankAccountJpaEntity.java`
  * `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BranchBankAccountJpaRepository.java`
  * `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BranchBankAccountRepositoryImpl.java`
  * `backend/src/main/java/com/mtms/organization/application/dto/CreateBankAccountCommand.java`
  * `backend/src/main/java/com/mtms/organization/application/dto/BranchBankAccountDto.java`
  * `backend/src/main/java/com/mtms/organization/application/dto/BranchReadinessDto.java`
  * `backend/src/main/java/com/mtms/organization/application/exception/BranchNotReadyException.java`
  * `backend/src/main/java/com/mtms/organization/application/BranchBankService.java`
  * `backend/src/main/java/com/mtms/organization/web/BranchBankController.java`
  * `backend/src/test/java/com/mtms/organization/BranchBankIntegrationTest.java` (Verification of TC-003 and security scopes)

### C. Frontend (React 18 / TypeScript / Vite / Tailwind / Motion)
* Target Files:
  * `frontend/src/features/organization/types.ts` (Add `BranchBankAccountDto`, `BranchReadinessDto`, `CreateBankAccountCommand`)
  * `frontend/src/features/organization/organizationApi.ts` (Add API functions)
  * `frontend/src/features/organization/components/BranchBankModal.tsx` (Manage bank accounts, set primary, add new)
  * `frontend/src/features/organization/pages/OrganizationAdminPage.tsx` (Integrate Readiness badge & bank accounts action)

---

## 3. Definition of Done (DoD)
1. Flyway migration `V003` runs forward idempotently on PostgreSQL 17.
2. Backend REST endpoints are secured by `@branchAuth`.
3. TC-003 integration test proves that a branch without a bank account cannot pass readiness, and successfully passes once a bank account is onboarded.
4. All existing + new backend tests pass (`mvn clean test`).
5. Frontend compiles cleanly with `npm run build` without TypeScript errors.
6. `python scripts/validate_workspace.py` passes with `SUMMARY: PASS`.
