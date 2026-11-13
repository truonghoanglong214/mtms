# Task brief — P0-ORG-02 PostgreSQL and Flyway baseline (APPROVED / IN_PROGRESS)

- **Prepared:** 2026-11-13
- **Readiness:** `IN_PROGRESS` — Owner approved this exact brief, accepted P0-ORG-01, and signed G0-01 through G0-10 on 2026-11-13.
- **G0 status/evidence:** `G0 SIGNED / APPROVED — 2026-11-13`; recorded in `docs/planning/G0_SIGNOFF_CHECKLIST.md` and Owner confirmation message.
- **Parent Task:** `P0-ORG-02` — PostgreSQL and Flyway baseline.
- **FE child, if applicable:** None. No frontend scope.
- **BR IDs:** `BR-ORG-01` (`BASELINED`) — one administrative network may contain multiple registered business entities, each with one or more branches.
- **TC IDs:** Tracker links `TC-001`, but the actual cross-branch expense authorization scenario is **NOT RUN / NOT EXECUTABLE** in this task because `P0-ORG-04`, `P0-ORG-05`, the expense domain, and scoped authorization are not implemented. This task uses technical migration tests without claiming `TC-001` passed.
- **ADR / POLICY_VERIFY:** `ADR-025` direction (Java/Spring Boot modular monolith, PostgreSQL, Flyway and Docker) is accepted for P0-ORG-02 through approved `G0-08`. No task-local legal/accounting `POLICY_VERIFY` decision is introduced.
- **Dependency IDs + verified status:** `P0-ORG-01` is the only direct dependency in Handbook and Tracker. See the dependency audit below.
- **Owner / reviewers:** Business Owner (plan approval and acceptance), Tech Lead (scope/dependency), Architect (DDL/constraint review), Backend Engineer (implementation), QA Reviewer (independent PostgreSQL/Flyway verification).
- **Task scope and explicit non-goals:** Sections 2.1 and 2.2.
- **Existing files to inspect:** `backend/pom.xml`, the three Spring YAML profiles, `infra/docker/compose.yml`, `infra/docker/.env.example`, the migration placeholder README, Handbook task rows, Tracker task rows, ADR-025, and the proposed v1.1 schema/static review. These are inspection inputs, not blanket edit authorization.
- **Allowed file paths after approval:** Section 6; any path not listed there is forbidden unless the Owner approves a revised brief.
- **API/DB contract changes:** No API change. The bounded V001 database contract and Flyway location/configuration are in Sections 3 and 4.
- **Acceptance criteria:** Section 7.
- **Negative tests / concurrency / retry:** Section 8.

## 1. Source and dependency audit

### 1.1 Authoritative task mapping

| Source | P0-ORG-02 evidence |
|---|---|
| Business Spec v1.0 | `BR-ORG-01` is `BASELINED`; entity and branch are distinct concepts. |
| Implementation Handbook v1.0 | Module `ORG`, estimate `10h`, priority `Must`, dependency `P0-ORG-01`, test link `TC-001`; intent is local Compose DB and first forward-only migrations for entity/branch/user. |
| Tracker v1.1 working copy | `P0-ORG-02`, status `Not Started`, dependency `P0-ORG-01`, DoD “Clean DB migrates once, rerun safe”. Tracker remains an unsigned working artifact. |
| ADR-025 / G0-08 | PostgreSQL, Flyway and Docker direction is approved for this task by the Owner message dated 2026-11-13. |
| Proposed v1.1 schema review | Conceptual schema is `SCHEMA_READY: NO / BLOCKED`; it is an input for the minimal ORG slice only and must never be copied wholesale into V001. |

### 1.2 P0-ORG-01 dependency result

| Check | Evidence | Result |
|---|---|---|
| Backend skeleton exists | Java 21 / Spring Boot 3.3.5 Maven project, PostgreSQL driver, Flyway core/PostgreSQL module and Testcontainers dependencies exist in `backend/pom.xml`. | Technically satisfied |
| Runtime configuration exists | `application.yml`, `application-dev.yml`, `application-test.yml` exist; Flyway is deliberately disabled in dev/test pending this task. | Technically satisfied |
| Infrastructure skeleton exists | `infra/docker/compose.yml` and `.env.example` exist; current Compose uses PostgreSQL 16 and must be aligned with the PostgreSQL 17 design target. | Technically satisfied with P0-ORG-02 changes required |
| P0-ORG-01 verification | `CURRENT_STATE.md` and `CHANGELOG.md` record 6/6 backend tests passing and frontend build success. | Evidence present |
| Owner acceptance | Owner explicitly accepted the verified P0-ORG-01 skeleton on 2026-11-13 (6/6 backend tests and frontend build evidence). | Satisfied |
| Tracker status | Working Tracker has not yet been updated; status update remains deferred until the authorized workflow records evidence. | Non-blocking documentation lag |
| G0 | Owner signed G0-01 through G0-10 on 2026-11-13. | Satisfied |

**Dependency conclusion:** P0-ORG-01 technical evidence and Owner acceptance are recorded; G0 and this task plan are approved. `P0-ORG-02` is `IN_PROGRESS`.

### 1.3 Downstream consumers

The V001 contract becomes a prerequisite for `P0-ORG-03` (entity/branch domain), `P0-ORG-04` (authentication/session persistence), and `P0-SEC-03` (idempotency/locking). V001 must not pre-implement their business behavior.

## 2. Task scope

### 2.1 In scope

1. Make the existing Compose PostgreSQL service a deterministic local PostgreSQL 17 development database with a persistent named volume, readiness healthcheck and credentials supplied only through a gitignored `.env`.
2. Select `database/migrations/` as the **single canonical migration source**. Do not duplicate V001 under `backend/src/main/resources/db/migration`.
3. Add one immutable forward-only migration: `database/migrations/V001__create_organization_identity_baseline.sql`.
4. V001 creates only the `mtms` schema and the minimal organization/identity tables: `business_entities`, `branches`, and `users`.
5. Enable and harden Flyway for the dev/Compose paths: validation on migrate, out-of-order disabled, clean disabled, schema `mtms`, and an explicit filesystem location.
6. Add a PostgreSQL Testcontainers test that runs the real V001 against a disposable PostgreSQL 17 instance, verifies constraints, then invokes Flyway again and proves no pending/duplicate migration is applied.
7. Document reproducible local start, migrate, verify and forward-fix procedures without storing real credentials or deleting a developer volume automatically.

### 2.2 Explicit non-goals

- No execution of either conceptual SQL starter as a migration.
- No tables for scoped role grants, sessions/tokens, audit, idempotency, evidence, bank accounts, employees, revenue, salary expense, finance, inventory, closing, capital or transfers.
- No login/authentication implementation; `users` is persistence groundwork for `P0-ORG-04` only.
- No entity/branch CRUD API, repository, JPA entity or service; those belong to `P0-ORG-03`.
- No seed data, especially no real shop, employee, investor, bank or credential data.
- No frontend changes.
- No production deployment, backup/restore implementation, cloud database, Kubernetes or CI redesign.
- No downgrade migration and no editing V001 after it has been applied. Any correction is a new forward migration (`V002+`).
- No Tracker status update, commit, or claim that `TC-001` passed before QA and owner acceptance.

## 3. Database contract for V001

The Architect must approve the exact DDL before Backend implementation. The minimal contract is:

### 3.1 `mtms.business_entities`

- `id UUID PRIMARY KEY` — generated by the application/test fixture; do not require a database extension solely for UUID generation.
- `entity_code VARCHAR(32) NOT NULL UNIQUE`.
- `registered_name VARCHAR(200) NOT NULL`.
- `registration_ref VARCHAR(100) NULL`.
- `status VARCHAR(20) NOT NULL` with allowed values `ACTIVE`, `INACTIVE`.
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`.

### 3.2 `mtms.branches`

- `id UUID PRIMARY KEY`.
- `business_entity_id UUID NOT NULL` referencing `business_entities(id)` with no cascading delete.
- `branch_code VARCHAR(40) NOT NULL`.
- `name VARCHAR(160) NOT NULL`.
- `time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh'`.
- `status VARCHAR(20) NOT NULL` with allowed values `DRAFT`, `ACTIVE`, `SUSPENDED`, `CLOSED`.
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`.
- `UNIQUE (business_entity_id, branch_code)` so the same branch code may exist in different entities but not twice in one entity.
- `UNIQUE (business_entity_id, id)` to support future composite scope foreign keys.
- Index on `business_entity_id` for entity-scoped branch queries.

### 3.3 `mtms.users`

- `id UUID PRIMARY KEY`.
- `login_identifier VARCHAR(200) NOT NULL UNIQUE`.
- `password_hash TEXT NOT NULL`; V001 stores no plaintext password and creates no user row.
- `display_name VARCHAR(160) NOT NULL`.
- `status VARCHAR(20) NOT NULL` with allowed values `ACTIVE`, `SUSPENDED`, `DISABLED`.
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`.

### 3.4 Contract guards

- `business_entity` and `branch` remain separate tables; no denormalized “shop owner” or investor access field is introduced.
- No `ON DELETE CASCADE` on organization roots.
- All timestamps are `TIMESTAMPTZ`; no business date exists in this slice.
- No money/quantity columns exist in V001.
- All names are schema-qualified or the migration explicitly sets a safe `search_path` after creating `mtms`.
- Flyway history is stored consistently in the configured `mtms` schema.
- V001 contains DDL only; it has no `DROP`, destructive reset, real data, secret, or `COMMIT` wrapper controlled outside Flyway.

## 4. Migration-location and runtime decision

`database/migrations/` is the sole source of truth.

- Local Maven/dev default: `filesystem:../database/migrations` when commands are run from `backend/`.
- Compose backend path: mount `../../database/migrations` read-only at `/app/migrations` and set `SPRING_FLYWAY_LOCATIONS=filesystem:/app/migrations`.
- Testcontainers: resolve the repository migration directory explicitly and pass it to programmatic Flyway; do not copy V001 into test resources.
- `backend/src/main/resources/db/migration` remains absent so two copies cannot drift.

Required Flyway safety values:

```text
enabled=true
default-schema=mtms
schemas=mtms
create-schemas=true
validate-on-migrate=true
out-of-order=false
clean-disabled=true
baseline-on-migrate=false
```

## 5. Sub-task breakdown and model tier

Work is sequential at contract boundaries. Parallel work is allowed only where target files do not overlap.

| ID | Tier | Owner | Work | Target files | Exit condition |
|---|---|---|---|---|---|
| `ORG02-GATE` | **High** | Tech Lead | Recheck G0 evidence, P0-ORG-01 acceptance and explicit approval of this brief. | Read-only: Tracker, G0 checklist, current state | All three authorization references recorded; otherwise stop. |
| `ORG02-DB-01` | **High** | Architect → Backend Engineer | Review the minimal DDL against BR-ORG-01 and the proposed v1.1 scoped-key strategy; implement V001 only after approval. | **ADD** `database/migrations/V001__create_organization_identity_baseline.sql` | DDL contains only schema/entity/branch/user and passes static review. |
| `ORG02-INFRA-01` | **Low** | Backend/Infra Engineer | Align local Compose with PostgreSQL 17, mandatory password injection, healthcheck, read-only migration mount and Flyway environment. | **MODIFY** `infra/docker/compose.yml`; **MODIFY** `infra/docker/.env.example` | `docker compose config` succeeds without embedding a real password. |
| `ORG02-BE-01` | **High** | Backend Engineer | Enable safe Flyway dev configuration with one filesystem migration source; retain Hibernate validation and UTC handling. | **MODIFY** `backend/src/main/resources/application-dev.yml` | App configuration points only to canonical migration directory and has safe Flyway flags. |
| `ORG02-TEST-01` | **High** | Backend Engineer | Add PostgreSQL 17 Testcontainers + programmatic Flyway test for clean migrate, rerun, history and constraints. | **ADD** `backend/src/test/java/com/mtms/database/FlywayBaselineMigrationTest.java` | Test proves clean bootstrap and second migrate applies zero migrations. |
| `ORG02-DOC-01` | **Low** | Backend Engineer / Technical Writer | Document canonical path, local commands, secrets rule and forward-fix policy. | **MODIFY** `database/migrations/README.md`; **MODIFY** `infra/docker/README.md`; **MODIFY** `backend/README.md` | A new developer can reproduce the approved checks without guessing paths or credentials. |
| `ORG02-QA-01` | **High** | QA Reviewer | Independent diff/security/schema review and execution evidence. | Read-only review of all allowed files; evidence artifact only after implementation | No scope creep; commands, exit codes and failures recorded; `TC-001` remains NOT RUN. |

### 5.1 Tier rationale

- **High:** DDL, Flyway configuration, migration location, Testcontainers and constraint tests can corrupt the baseline or create future immutable-history problems if wrong.
- **Low:** Compose/env and documentation edits are mechanical only after the High-tier database contract and exact variables/paths are locked.

## 6. Allowed target files after approval

### ADD

- `database/migrations/V001__create_organization_identity_baseline.sql`
- `backend/src/test/java/com/mtms/database/FlywayBaselineMigrationTest.java`

### MODIFY

- `infra/docker/compose.yml`
- `infra/docker/.env.example`
- `backend/src/main/resources/application-dev.yml`
- `database/migrations/README.md`
- `infra/docker/README.md`
- `backend/README.md`
- `docs/planning/CURRENT_STATE.md` — only after QA/owner acceptance, with actual evidence.
- `docs/ai/CHANGELOG.md` — only after QA/owner acceptance, with actual file actions/evidence.

### INSPECT / KEEP unless the approved implementation proves a necessary change

- `backend/pom.xml` — PostgreSQL, Flyway and Testcontainers dependencies already exist.
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml` — H2 may remain for the existing smoke context, but it must not be used as migration proof.
- `.github/workflows/ci.yml` — existing `mvn ... test` should discover the new `*Test`; modify only through a separately approved plan if CI cannot run Testcontainers.
- `backend/Dockerfile`, frontend files and all business/architecture baseline documents.

### FORBIDDEN

- `docs/architecture/MTMS_PostgreSQL_Schema_Starter_v1_0.sql`
- `MTMS-docs/MTMS_PostgreSQL_Schema_Starter_v1_1_PROPOSED.sql` (conceptual review input only)
- Any `V002+` migration, application/domain code, frontend file, Tracker status, `.env`, secret, dump or real data.

### DELETE

- None.

## 7. Acceptance criteria

1. **Gate:** implementation does not start without recorded G0, accepted `P0-ORG-01`, and explicit approval of this brief.
2. **Canonical source:** exactly one V001 exists and it is under `database/migrations/`; no duplicate exists in backend resources.
3. **Local database:** Compose configuration resolves with PostgreSQL 17, persistent volume and healthcheck; no committed real password/default secret is present.
4. **Clean bootstrap:** a fresh disposable PostgreSQL 17 database receives exactly one successful Flyway migration and reports schema version `1`/`001` according to Flyway normalization.
5. **Safe rerun:** a second `migrate()` reports zero pending/executed migrations, with the original checksum/history unchanged.
6. **Schema:** only `business_entities`, `branches`, `users` and Flyway history are introduced by V001 in schema `mtms`.
7. **Entity/branch integrity:** a branch cannot reference a missing entity; duplicate `(entity, branch_code)` is rejected; the same `branch_code` in a different entity is accepted.
8. **User integrity:** duplicate login identifiers and invalid status values are rejected; no plaintext password or seed user exists.
9. **Safety:** no cascade delete of organization roots, no destructive DDL/reset, no conceptual starter execution and no real data.
10. **Regression:** existing backend unit/architecture tests remain green.
11. **Traceability:** technical test evidence is recorded separately; `TC-001` is explicitly `NOT RUN`, not falsely passed.
12. **Recovery:** documentation states that an applied V001 is immutable and defects are repaired with a forward-only V002; disposable Testcontainers are used for clean tests.

## 8. Negative tests / retry / concurrency

### Required automated tests

- Duplicate `business_entities.entity_code` → constraint violation.
- Missing `branches.business_entity_id` parent → foreign-key violation.
- Duplicate `(business_entity_id, branch_code)` → constraint violation.
- Same `branch_code` under two entities → allowed.
- Invalid entity/branch/user status → check-constraint violation.
- Duplicate `users.login_identifier` → constraint violation.
- Second Flyway invocation → zero migrations executed.
- Migration checksum mismatch test is optional in code but QA must verify the documented rule: never edit an applied V001.

### Concurrency boundary

V001 has no money/stock posting use case and adds no application mutation API. Domain-level concurrency tests are out of scope. Flyway schema-history locking is exercised implicitly by standard Flyway behavior; a deliberate parallel-start test may be added only if it remains within the two approved test files and is stable in CI.

### TC-001 boundary

`TC-001` (“Branch A user requests Branch B expense”) requires authentication, scoped role grants, an expense endpoint/query and denial behavior. P0-ORG-02 provides none of those. The correct result for this task is `NOT RUN — blocked by P0-ORG-04/P0-ORG-05 and finance prerequisites`.

## 9. Commands to run only after authorization

Commands are evidence targets, not evidence already obtained:

```powershell
docker compose --env-file infra/docker/.env -f infra/docker/compose.yml config
docker compose --env-file infra/docker/.env -f infra/docker/compose.yml up -d postgres
mvn -f backend/pom.xml test
docker compose --env-file infra/docker/.env -f infra/docker/compose.yml ps postgres
python scripts/validate_workspace.py
```

Additional QA queries must run only against the disposable Testcontainers database or the explicitly approved local development database. Do not run `docker compose down -v` automatically; volume deletion requires explicit confirmation because it destroys local data.

## 10. Risks and controls

| Risk | Control |
|---|---|
| Conceptual v1.1 starter copied wholesale into V001 | V001 allowlist is only schema + three tables; Architect and QA diff review. |
| Two migration sources drift | Canonical `database/migrations/`; filesystem mount/config only; no resource copy. |
| PostgreSQL 16/17 mismatch | Compose and Testcontainers both use PostgreSQL 17 to match the documented design target. |
| Secret committed through Compose/application YAML | Password is required from gitignored `.env`; repository scan and QA check. |
| H2 gives false confidence | Migration proof must use PostgreSQL Testcontainers; H2 remains smoke-only. |
| V001 later edited | Forward-only rule; checksum verification; correction through V002+. |
| Scope creep into auth/roles/domain | Table and target-file allowlists; no application code. |
| Tracker status contradicts implementation | Do not hand off until P0-ORG-01 acceptance and G0 evidence are recorded. |

## 11. Handoff and evidence state

- **Plan approval evidence:** `APPROVED — Owner message dated 2026-11-13`
- **Implementation authorization:** `APPROVED / IN_PROGRESS — Owner confirmation dated 2026-11-13`
- **Build and test evidence:** `PARTIAL` — ORG02-INFRA-01 static Compose validation recorded below; build, migration and Testcontainers evidence remain NOT RUN.
- **Database execution:** `NOT RUN`
- **QA decision:** `PENDING`
- **User acceptance:** `PENDING`
- **Tracker update proposal:** `BLOCKED` until G0, dependency acceptance, implementation evidence, independent QA and user acceptance.
- **Status recommendation:** `IN_PROGRESS`

### 11.1 ORG02-INFRA-01 evidence — 2026-11-13

- **Status:** `READY_FOR_QA` after Tech Lead static review; not independently accepted yet.
- **Files:** `infra/docker/compose.yml`, `infra/docker/.env.example` only.
- **Verified:** PostgreSQL `17-alpine`, named persistent volume, `pg_isready` healthcheck, mandatory `POSTGRES_PASSWORD`, non-secret example placeholder, read-only `../../database/migrations:/app/migrations`, and approved Flyway filesystem/safety environment.
- **Independent static rerun:** `docker compose -f infra/docker/compose.yml config --quiet` returned exit `1` without `POSTGRES_PASSWORD` and exit `0` with process-only value `temporary-static-validation-only`.
- **Workspace validation:** `python scripts/validate_workspace.py` returned `SUMMARY: PASS`.
- **Not run:** containers, PostgreSQL connection, Flyway, migration, volume deletion, build and QA suite.
- **Review note:** Docker emitted an access warning for the local user-level Docker config, but Compose interpolation/validation produced the required exit codes; no repository defect was inferred from that host warning.

## 12. Approval checklist before Dev handoff

- [x] G0 signoff reference is recorded — G0-01 through G0-10 approved 2026-11-13.
- [x] `P0-ORG-01` Owner acceptance/evidence is recorded — accepted 2026-11-13; Tracker update remains a documentation follow-up.
- [x] `G0-08` accepts ADR-025/database migration direction — approved 2026-11-13.
- [x] Owner explicitly approves this exact scope, target-file allowlist and V001 contract — approved 2026-11-13.
- [ ] Architect approves the V001 DDL contract.
- [ ] Backend Engineer and QA Reviewer accept file ownership and evidence commands.

Architect and Backend may now execute only their assigned target files. Remaining unchecked boxes are completion/handoff evidence, not authorization to expand scope.
