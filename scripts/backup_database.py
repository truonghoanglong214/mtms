#!/usr/bin/env python3
"""
MTMS Database Backup Utility
Part of P0-SEC-05 / BR-SEC-04 / TC-044

Performs a consistent, auditable dump of the mtms schema and records.
Supports both native pg_dump and Docker containerized pg_dump.
Generates SHA-256 checksum and metadata manifest for restore verification.
"""

import argparse
import datetime
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


def calculate_sha256(filepath: Path) -> str:
    hasher = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            hasher.update(chunk)
    return hasher.hexdigest()


def find_execution_mode(container_name: str) -> str:
    """Determine whether to use native pg_dump or docker exec."""
    if shutil.which("pg_dump"):
        return "native"
    # Check if docker is available and container is running
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


def run_backup(
    output_dir: Path,
    db_name: str,
    user: str,
    host: str,
    port: str,
    container_name: str,
    schema: str = "mtms"
) -> Path:
    timestamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%d_%H%M%SZ")
    output_dir.mkdir(parents=True, exist_ok=True)
    backup_file = output_dir / f"mtms_backup_{timestamp}.sql"
    manifest_file = output_dir / f"mtms_backup_{timestamp}.manifest.json"
    sha_file = output_dir / f"mtms_backup_{timestamp}.sql.sha256"

    mode = find_execution_mode(container_name)
    if mode == "none":
        print("[ERROR] Neither 'pg_dump' on PATH nor a running Docker container was found.")
        print(f"        Ensure PostgreSQL client is installed or container '{container_name}' is running.")
        sys.exit(1)

    print(f"[INFO] Starting MTMS backup using mode: {mode}")
    print(f"[INFO] Target database: {db_name}, schema: {schema}")

    if mode == "native":
        env = os.environ.copy()
        cmd = [
            "pg_dump",
            "-h", host,
            "-p", str(port),
            "-U", user,
            "-d", db_name,
            "-n", schema,
            "--clean",
            "--if-exists",
            "--no-owner",
            "--no-privileges",
            "-f", str(backup_file)
        ]
        res = subprocess.run(cmd, env=env, capture_output=True, text=True)
        if res.returncode != 0:
            print(f"[ERROR] Native pg_dump failed: {res.stderr}")
            sys.exit(res.returncode)
    else:  # docker mode
        cmd = [
            "docker", "exec",
            "-i",
            container_name,
            "pg_dump",
            "-U", user,
            "-d", db_name,
            "-n", schema,
            "--clean",
            "--if-exists",
            "--no-owner",
            "--no-privileges"
        ]
        with open(backup_file, "w", encoding="utf-8") as out:
            res = subprocess.run(cmd, stdout=out, stderr=subprocess.PIPE, text=True)
            if res.returncode != 0:
                print(f"[ERROR] Docker pg_dump failed: {res.stderr}")
                if backup_file.exists():
                    backup_file.unlink()
                sys.exit(res.returncode)

    file_size = backup_file.stat().st_size
    sha256_hash = calculate_sha256(backup_file)

    with open(sha_file, "w", encoding="utf-8") as f:
        f.write(f"{sha256_hash}  {backup_file.name}\n")

    manifest = {
        "backup_file": backup_file.name,
        "timestamp": timestamp,
        "schema": schema,
        "database": db_name,
        "file_size_bytes": file_size,
        "sha256": sha256_hash,
        "mode": mode,
        "status": "COMPLETED"
    }
    with open(manifest_file, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)

    print(f"[SUCCESS] Backup created successfully:")
    print(f"          SQL Dump : {backup_file} ({file_size:,} bytes)")
    print(f"          SHA-256  : {sha256_hash}")
    print(f"          Manifest : {manifest_file}")
    return backup_file


def main():
    parser = argparse.ArgumentParser(description="MTMS Database Backup Utility (P0-SEC-05 / BR-SEC-04)")
    parser.add_argument("--output-dir", default="backups", help="Directory where backup files will be saved")
    parser.add_argument("--db", default=DEFAULT_DB, help=f"Database name (default: {DEFAULT_DB})")
    parser.add_argument("--user", default=DEFAULT_USER, help=f"Database user (default: {DEFAULT_USER})")
    parser.add_argument("--host", default=DEFAULT_HOST, help=f"Database host (default: {DEFAULT_HOST})")
    parser.add_argument("--port", default=DEFAULT_PORT, help=f"Database port (default: {DEFAULT_PORT})")
    parser.add_argument("--container", default=DEFAULT_CONTAINER, help=f"Docker container name (default: {DEFAULT_CONTAINER})")
    parser.add_argument("--schema", default="mtms", help="Schema to dump (default: mtms)")

    args = parser.parse_args()
    run_backup(
        output_dir=Path(args.output_dir),
        db_name=args.db,
        user=args.user,
        host=args.host,
        port=args.port,
        container_name=args.container,
        schema=args.schema
    )


if __name__ == "__main__":
    main()
