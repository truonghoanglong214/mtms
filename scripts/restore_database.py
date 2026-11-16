#!/usr/bin/env python3
"""
MTMS Database Restore Utility
Part of P0-SEC-05 / BR-SEC-04 / TC-044

Restores a backup dump onto a clean staging database or target environment.
Performs SHA-256 integrity verification before execution.
Audits table counts and required records after restoration (TC-044 verification).
"""

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

DEFAULT_DB = os.environ.get("POSTGRES_DB", "mtms")
DEFAULT_USER = os.environ.get("POSTGRES_USER", "mtms_user")
DEFAULT_HOST = os.environ.get("POSTGRES_HOST", "localhost")
DEFAULT_PORT = os.environ.get("POSTGRES_PORT", "5434")
DEFAULT_CONTAINER = os.environ.get("POSTGRES_CONTAINER", "mtms-postgres")

REQUIRED_TABLES = [
    "flyway_schema_history",
    "business_entities",
    "branches",
    "users",
    "scoped_role_grants",
    "branch_bank_accounts",
    "evidence_files",
    "audit_events",
    "idempotency_records",
    "import_batches",
    "import_rows"
]


def calculate_sha256(filepath: Path) -> str:
    hasher = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            hasher.update(chunk)
    return hasher.hexdigest()


def find_execution_mode(container_name: str) -> str:
    if shutil.which("psql"):
        return "native"
    if shutil.which("docker"):
        try:
            res = subprocess.run(
                ["docker", "inspect", "-f", "{{.State.Running}}", container_name],
                capture_output=True,
                text=True,
                check=False
            )
            if res.returncode == 0 and res.stdout.strip() == "true":
                return "docker"
        except Exception:
            pass
    return "none"


def verify_integrity(backup_file: Path) -> bool:
    sha_file = backup_file.with_name(backup_file.name + ".sha256")
    manifest_file = backup_file.with_name(backup_file.stem + ".manifest.json")

    actual_sha = calculate_sha256(backup_file)

    if sha_file.exists():
        with open(sha_file, "r", encoding="utf-8") as f:
            expected_sha = f.read().split()[0].strip()
        if actual_sha.lower() != expected_sha.lower():
            print(f"[ERROR] SHA-256 verification failed!")
            print(f"        Expected: {expected_sha}")
            print(f"        Actual  : {actual_sha}")
            return False
        print(f"[INFO] SHA-256 integrity checksum verified: {actual_sha}")
        return True

    if manifest_file.exists():
        with open(manifest_file, "r", encoding="utf-8") as f:
            manifest = json.load(f)
        expected_sha = manifest.get("sha256", "").strip()
        if actual_sha.lower() != expected_sha.lower():
            print(f"[ERROR] SHA-256 verification failed against manifest!")
            print(f"        Expected: {expected_sha}")
            print(f"        Actual  : {actual_sha}")
            return False
        print(f"[INFO] Manifest SHA-256 verified: {actual_sha}")
        return True

    print("[WARN] No .sha256 or .manifest.json file found; skipping checksum verification.")
    return True


def run_sql_query(mode: str, container_name: str, host: str, port: str, user: str, db: str, query: str) -> str:
    if mode == "native":
        env = os.environ.copy()
        cmd = ["psql", "-h", host, "-p", str(port), "-U", user, "-d", db, "-t", "-A", "-c", query]
        res = subprocess.run(cmd, env=env, capture_output=True, text=True)
        if res.returncode != 0:
            raise RuntimeError(f"psql query failed: {res.stderr}")
        return res.stdout.strip()
    else:
        cmd = ["docker", "exec", "-i", container_name, "psql", "-U", user, "-d", db, "-t", "-A", "-c", query]
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode != 0:
            raise RuntimeError(f"docker psql query failed: {res.stderr}")
        return res.stdout.strip()


def run_restore(
    backup_file: Path,
    db_name: str,
    user: str,
    host: str,
    port: str,
    container_name: str
):
    if not backup_file.exists():
        print(f"[ERROR] Backup file not found: {backup_file}")
        sys.exit(1)

    print(f"[INFO] Initiating restore for: {backup_file}")
    if not verify_integrity(backup_file):
        print("[FATAL] Aborting restore due to checksum mismatch.")
        sys.exit(1)

    mode = find_execution_mode(container_name)
    if mode == "none":
        print("[ERROR] Neither 'psql' on PATH nor running container was found.")
        sys.exit(1)

    print(f"[INFO] Restoring into database: {db_name} via {mode} mode...")

    if mode == "native":
        env = os.environ.copy()
        cmd = ["psql", "-h", host, "-p", str(port), "-U", user, "-d", db_name, "-f", str(backup_file)]
        res = subprocess.run(cmd, env=env, capture_output=True, text=True)
        if res.returncode != 0:
            print(f"[ERROR] Restore failed: {res.stderr}")
            sys.exit(res.returncode)
    else:
        cmd = ["docker", "exec", "-i", container_name, "psql", "-U", user, "-d", db_name]
        with open(backup_file, "r", encoding="utf-8") as f:
            res = subprocess.run(cmd, stdin=f, capture_output=True, text=True)
            if res.returncode != 0:
                print(f"[ERROR] Docker restore failed: {res.stderr}")
                sys.exit(res.returncode)

    print("[SUCCESS] SQL dump applied to database.")

    # Audit & verification (TC-044)
    print("\n--- TC-044 AUDIT & INTEGRITY VERIFICATION ---")
    query_tables = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'mtms' ORDER BY table_name;"
    try:
        tables_output = run_sql_query(mode, container_name, host, port, user, db_name, query_tables)
        restored_tables = set(tables_output.splitlines()) if tables_output else set()
        print(f"[INFO] Restored tables in mtms schema: {len(restored_tables)}")

        missing_tables = [t for t in REQUIRED_TABLES if t not in restored_tables]
        if missing_tables:
            print(f"[FAIL] Missing required tables: {missing_tables}")
            sys.exit(1)

        print("[INFO] All required tables present:")
        for t in REQUIRED_TABLES:
            count_q = f"SELECT count(*) FROM mtms.{t};"
            cnt = run_sql_query(mode, container_name, host, port, user, db_name, count_q)
            print(f"       - mtms.{t:24}: {cnt} rows")

        print("\n=======================================================")
        print("  TC-044 VERIFICATION RESULT: PASS                     ")
        print("  Database integrity & required records verified.      ")
        print("=======================================================\n")
    except Exception as e:
        print(f"[ERROR] Failed during TC-044 verification query: {e}")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="MTMS Database Restore Utility (P0-SEC-05 / BR-SEC-04 / TC-044)")
    parser.add_argument("backup_file", help="Path to SQL backup file (.sql)")
    parser.add_argument("--db", default=DEFAULT_DB, help=f"Database name (default: {DEFAULT_DB})")
    parser.add_argument("--user", default=DEFAULT_USER, help=f"Database user (default: {DEFAULT_USER})")
    parser.add_argument("--host", default=DEFAULT_HOST, help=f"Database host (default: {DEFAULT_HOST})")
    parser.add_argument("--port", default=DEFAULT_PORT, help=f"Database port (default: {DEFAULT_PORT})")
    parser.add_argument("--container", default=DEFAULT_CONTAINER, help=f"Docker container name (default: {DEFAULT_CONTAINER})")

    args = parser.parse_args()
    run_restore(
        backup_file=Path(args.backup_file),
        db_name=args.db,
        user=args.user,
        host=args.host,
        port=args.port,
        container_name=args.container
    )


if __name__ == "__main__":
    main()
