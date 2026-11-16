# MTMS Operations Runbook — Database Backup & Staging Restore

- **Task Reference:** `P0-SEC-05`
- **Requirement:** `BR-SEC-04` (Operational Monitoring, CI/CD, Backup & Disaster Recovery)
- **Test Case:** `TC-044` (Restore backup on clean staging: Integrity and required records verified)
- **Status:** APPROVED / OPERATIONAL
- **Updated:** 2026-11-16

---

## 1. Objectives & SLA Metrics

| Metric | Target | Description |
| :--- | :--- | :--- |
| **RPO (Recovery Point Objective)** | < 24 Hours | Nightly scheduled dumps of `mtms` schema; point-in-time recovery via WAL archiving in production. |
| **RTO (Recovery Time Objective)** | < 30 Minutes | Complete restoration of schema, tables, and verification on clean staging. |
| **Integrity Assurance** | 100% SHA-256 Match | Every backup generates a companion `.sha256` checksum and manifest; restores without valid checksum are aborted. |
| **Data Scope** | Schema `mtms` | All relational data, constraints, foreign keys, indices, and Flyway migration history. |

---

## 2. Tooling Architecture

MTMS provides two automated scripts under `scripts/`:

1. **`scripts/backup_database.py`**:
   - Detects execution environment automatically (`native` `pg_dump` on PATH or `docker exec` into container `mtms-postgres`).
   - Dumps `mtms` schema with `--clean --if-exists --no-owner --no-privileges`.
   - Computes SHA-256 hash and writes `mtms_backup_<TIMESTAMP>.sql.sha256`.
   - Generates JSON manifest `mtms_backup_<TIMESTAMP>.manifest.json` with file metadata.

2. **`scripts/restore_database.py`**:
   - Validates SHA-256 checksum before executing any database modifications.
   - Restores SQL dump into target database.
   - **TC-044 Audit Engine:** Automatically inspects the restored `mtms` schema, queries all required core tables, asserts non-empty schema, and verifies constraint consistency.

---

## 3. Standard Operating Procedures (SOP)

### A. Performing an On-Demand Backup
Run the backup script pointing to your desired backup directory:
```bash
python scripts/backup_database.py --output-dir backups
```
*Output files produced:*
- `backups/mtms_backup_<YYYYMMDD_HHMMSSZ>.sql`
- `backups/mtms_backup_<YYYYMMDD_HHMMSSZ>.sql.sha256`
- `backups/mtms_backup_<YYYYMMDD_HHMMSSZ>.manifest.json`

### B. Performing a Clean Staging Restore Drill (TC-044)
To verify a backup or restore onto a clean staging environment:
```bash
python scripts/restore_database.py backups/mtms_backup_<TIMESTAMP>.sql --db mtms
```
The script will output:
```text
[INFO] Initiating restore for: backups/mtms_backup_...sql
[INFO] SHA-256 integrity checksum verified: <hash>
[INFO] Restoring into database: mtms via docker mode...
[SUCCESS] SQL dump applied to database.

--- TC-044 AUDIT & INTEGRITY VERIFICATION ---
[INFO] Restored tables in mtms schema: 11
[INFO] All required tables present:
       - mtms.flyway_schema_history   : 8 rows
       - mtms.business_entities       : X rows
       - mtms.branches                : X rows
       - mtms.users                   : X rows
       - mtms.scoped_role_grants      : X rows
       - mtms.branch_bank_accounts    : X rows
       - mtms.evidence_files          : X rows
       - mtms.audit_events            : X rows
       - mtms.idempotency_records     : X rows
       - mtms.import_batches          : X rows
       - mtms.import_rows             : X rows

=======================================================
  TC-044 VERIFICATION RESULT: PASS                     
  Database integrity & required records verified.      
=======================================================
```

---

## 4. Health & Readiness Monitoring

Spring Boot Actuator endpoints are exposed under `/actuator`:
- **Readiness Probe:** `GET /actuator/health/readiness` (returns HTTP 200 `UP` when application is accepting traffic).
- **Liveness Probe:** `GET /actuator/health/liveness` (returns HTTP 200 `UP` indicating container is alive).
- **Deep Health & Diagnostics:** `GET /actuator/health` includes custom `systemReadiness` indicator:
  - Database connectivity and query response time (`dbResponseTimeMs`).
  - Storage directory accessibility and usable disk space (`usableDiskSpaceMb`).

---

## 5. Disaster Recovery Emergency Checklist

1. [ ] Identify incident scope and freeze writes to target database.
2. [ ] Retrieve latest verified backup `.sql` and corresponding `.sha256` file from secure storage.
3. [ ] Verify file integrity: `sha256sum -c mtms_backup_<TIMESTAMP>.sql.sha256`.
4. [ ] Boot clean PostgreSQL staging/recovery container: `docker compose up -d postgres`.
5. [ ] Execute restore: `python scripts/restore_database.py <BACKUP_PATH>`.
6. [ ] Confirm TC-044 audit output reports `PASS`.
7. [ ] Start backend service and verify `/actuator/health` returns `UP`.
8. [ ] Perform smoke test on administrative login.
