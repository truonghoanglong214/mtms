package com.mtms.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P0-ORG-02 / ORG02-TEST-01
 * Integration test proving Flyway baseline bootstrap, safe rerun, and PostgreSQL constraints.
 * Uses a disposable PostgreSQL 17 Testcontainer to mirror production environment target.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlywayBaselineMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("mtms")
            .withUsername("mtms_user")
            .withPassword("mtms_test_pass");

    private static Path resolveMigrationDir() {
        Path fromBackend = Paths.get("../database/migrations").toAbsolutePath().normalize();
        if (Files.isDirectory(fromBackend)) {
            return fromBackend;
        }
        Path fromRoot = Paths.get("database/migrations").toAbsolutePath().normalize();
        if (Files.isDirectory(fromRoot)) {
            return fromRoot;
        }
        throw new IllegalStateException("Could not resolve canonical database/migrations directory");
    }

    private Flyway createFlyway() {
        Path migrationDir = resolveMigrationDir();
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .defaultSchema("mtms")
                .schemas("mtms")
                .createSchemas(true)
                .locations("filesystem:" + migrationDir.toString())
                .cleanDisabled(true)
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }

    @Test
    @Order(1)
    @DisplayName("Clean bootstrap: Flyway applies migrations (V001 through V023) in mtms schema")
    void testCleanBootstrap() {
        Flyway flyway = createFlyway();
        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(23);
        assertThat(result.targetSchemaVersion).isIn("23", "023");
    }

    @Test
    @Order(2)
    @DisplayName("Safe rerun: Second migrate invocation executes 0 migrations")
    void testSafeRerun() {
        Flyway flyway = createFlyway();
        MigrateResult rerunResult = flyway.migrate();

        assertThat(rerunResult.success).isTrue();
        assertThat(rerunResult.migrationsExecuted).isZero();
    }

    @Test
    @Order(3)
    @DisplayName("Constraint verification: Business entity and branch integrity")
    void testEntityAndBranchConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityA = UUID.randomUUID();
            UUID entityB = UUID.randomUUID();

            // Insert valid business entity A
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) " +
                    "VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityA);
                stmt.setString(2, "ENT_A");
                stmt.setString(3, "Entity Alpha Co., Ltd");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            // Duplicate entity_code must be rejected
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) " +
                    "VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, "ENT_A"); // Duplicate
                stmt.setString(3, "Entity Duplicate");
                stmt.setString(4, "ACTIVE");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // Invalid entity status must be rejected by check constraint
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) " +
                    "VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, "ENT_INVALID_STATUS");
                stmt.setString(3, "Entity Bad Status");
                stmt.setString(4, "DELETED"); // Invalid status
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // Insert valid branch under entity A
            UUID branch1 = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branch1);
                stmt.setObject(2, entityA);
                stmt.setString(3, "BR_01");
                stmt.setString(4, "Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // Branch referencing nonexistent entity must be rejected
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, UUID.randomUUID()); // Nonexistent entity
                stmt.setString(3, "BR_ORPHAN");
                stmt.setString(4, "Orphan Branch");
                stmt.setString(5, "ACTIVE");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // Duplicate branch_code in the same entity must be rejected
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityA);
                stmt.setString(3, "BR_01"); // Duplicate for entity A
                stmt.setString(4, "Branch 1 Duplicate");
                stmt.setString(5, "ACTIVE");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // Insert second valid entity B
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) " +
                    "VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityB);
                stmt.setString(2, "ENT_B");
                stmt.setString(3, "Entity Beta Co., Ltd");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            // Same branch_code (BR_01) under entity B must be ALLOWED
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityB);
                stmt.setString(3, "BR_01"); // Same code but under ENT_B
                stmt.setString(4, "Entity B Branch 1");
                stmt.setString(5, "ACTIVE");
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Invalid branch status must be rejected
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityA);
                stmt.setString(3, "BR_BAD_STATUS");
                stmt.setString(4, "Branch Bad Status");
                stmt.setString(5, "DELETED"); // Invalid branch status
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Constraint verification: User login identifier and status integrity")
    void testUserConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID user1 = UUID.randomUUID();

            // Insert valid user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, user1);
                stmt.setString(2, "admin@mtms.local");
                stmt.setString(3, "$2a$12$e8x...dummyhash");
                stmt.setString(4, "Administrator");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // Duplicate login_identifier must be rejected
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, "admin@mtms.local"); // Duplicate
                stmt.setString(3, "$2a$12$anotherhash");
                stmt.setString(4, "Admin Duplicate");
                stmt.setString(5, "ACTIVE");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // Invalid user status must be rejected
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) " +
                    "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, "user_invalid@mtms.local");
                stmt.setString(3, "$2a$12$anotherhash");
                stmt.setString(4, "User Invalid");
                stmt.setString(5, "BANNED"); // Invalid status
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("P0-ORG-05 / V002 Constraint verification: Scoped role grants integrity")
    void testScopedRoleGrantsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            // Insert base entity, branch, and users
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_SCOPE");
                stmt.setString(3, "Scope Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_SCOPE_01");
                stmt.setString(4, "Scope Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "scoped_user@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "Scoped User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();

                stmt.setObject(1, adminId);
                stmt.setString(2, "grant_admin@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "Grant Admin");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 1. Insert valid scoped grant
            UUID validGrantId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.scoped_role_grants (id, business_entity_id, branch_id, user_id, role_key, action_scope, valid_from, valid_to, granted_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, validGrantId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                stmt.setString(5, "BRANCH_OWNER");
                stmt.setString(6, "ALL");
                stmt.setObject(7, java.sql.Date.valueOf("2026-01-01"));
                stmt.setObject(8, java.sql.Date.valueOf("2026-12-31"));
                stmt.setObject(9, adminId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 2. Reject invalid role_key
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.scoped_role_grants (id, business_entity_id, branch_id, user_id, role_key, valid_from, granted_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                stmt.setString(5, "INVALID_ROLE"); // Not in allowed enum list
                stmt.setObject(6, java.sql.Date.valueOf("2026-01-01"));
                stmt.setObject(7, adminId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Reject invalid date range (valid_to < valid_from)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.scoped_role_grants (id, business_entity_id, branch_id, user_id, role_key, valid_from, valid_to, granted_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                stmt.setString(5, "MANAGER");
                stmt.setObject(6, java.sql.Date.valueOf("2026-12-31"));
                stmt.setObject(7, java.sql.Date.valueOf("2026-01-01")); // valid_to < valid_from
                stmt.setObject(8, adminId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject non-system-admin role with NULL branch_id
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.scoped_role_grants (id, business_entity_id, branch_id, user_id, role_key, valid_from, granted_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setNull(3, java.sql.Types.OTHER); // branch_id is null for MANAGER
                stmt.setObject(4, userId);
                stmt.setString(5, "MANAGER");
                stmt.setObject(6, java.sql.Date.valueOf("2026-01-01"));
                stmt.setObject(7, adminId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("P0-ORG-07 / V003 Constraint verification: Branch bank accounts integrity")
    void testBranchBankAccountsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();

            // Insert business entity and branch
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_BANK_TEST");
                stmt.setString(3, "Bank Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_BANK_01");
                stmt.setString(4, "Bank Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 1. Insert valid primary bank account
            UUID account1 = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branch_bank_accounts (id, branch_id, bank_name, account_holder_name, account_number_ciphertext, masked_account_number, is_primary, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, account1);
                stmt.setObject(2, branchId);
                stmt.setString(3, "Vietcombank");
                stmt.setString(4, "CTY LUC TRA TINH HOA");
                stmt.setString(5, "cipher_12345");
                stmt.setString(6, "**** **** 1234");
                stmt.setBoolean(7, true);
                stmt.setString(8, "ACTIVE");
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 2. Insert second primary bank account on SAME branch with ACTIVE status -> violates partial unique index uq_branch_primary_bank
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branch_bank_accounts (id, branch_id, bank_name, account_holder_name, account_number_ciphertext, masked_account_number, is_primary, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setString(3, "Techcombank");
                stmt.setString(4, "CTY LUC TRA TINH HOA");
                stmt.setString(5, "cipher_67890");
                stmt.setString(6, "**** **** 6789");
                stmt.setBoolean(7, true); // duplicate primary
                stmt.setString(8, "ACTIVE");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Insert non-primary bank account on SAME branch -> succeeds
            UUID account2 = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branch_bank_accounts (id, branch_id, bank_name, account_holder_name, account_number_ciphertext, masked_account_number, is_primary, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, account2);
                stmt.setObject(2, branchId);
                stmt.setString(3, "Techcombank");
                stmt.setString(4, "CTY LUC TRA TINH HOA");
                stmt.setString(5, "cipher_67890");
                stmt.setString(6, "**** **** 6789");
                stmt.setBoolean(7, false); // non-primary
                stmt.setString(8, "ACTIVE");
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 4. Reject invalid status (check constraint ck_branch_bank_accounts_status)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branch_bank_accounts (id, branch_id, bank_name, account_holder_name, account_number_ciphertext, masked_account_number, is_primary, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setString(3, "ACB");
                stmt.setString(4, "CTY LUC TRA TINH HOA");
                stmt.setString(5, "cipher_acb");
                stmt.setString(6, "**** **** 9999");
                stmt.setBoolean(7, false);
                stmt.setString(8, "SUSPENDED"); // Invalid status
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject orphaned bank account (FK violation)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branch_bank_accounts (id, branch_id, bank_name, account_holder_name, account_number_ciphertext, masked_account_number, is_primary, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, UUID.randomUUID()); // Nonexistent branch
                stmt.setString(3, "BIDV");
                stmt.setString(4, "CTY LUC TRA TINH HOA");
                stmt.setString(5, "cipher_bidv");
                stmt.setString(6, "**** **** 0000");
                stmt.setBoolean(7, false);
                stmt.setString(8, "ACTIVE");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("P0-SEC-01 / V004 Constraint verification: Evidence files integrity")
    void testEvidenceFilesConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            // Insert business entity, branch, and user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_EVIDENCE_TEST");
                stmt.setString(3, "Evidence Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_EVD_01");
                stmt.setString(4, "Evidence Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "evidence_uploader@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "Evidence Uploader");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 1. Insert valid evidence file
            UUID file1 = UUID.randomUUID();
            String storageKey = "branches/" + branchId + "/receipt1.pdf";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.evidence_files (id, business_entity_id, branch_id, storage_object_key, original_filename, content_type, byte_size, checksum_sha256, visibility_class, retention_until, uploaded_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, file1);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setString(4, storageKey);
                stmt.setString(5, "receipt1.pdf");
                stmt.setString(6, "application/pdf");
                stmt.setLong(7, 1024L);
                stmt.setString(8, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                stmt.setString(9, "BRANCH_PRIVATE");
                stmt.setObject(10, java.sql.Date.valueOf("2027-12-31"));
                stmt.setObject(11, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 2. Reject duplicate storage_object_key (UNIQUE constraint)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.evidence_files (id, business_entity_id, branch_id, storage_object_key, original_filename, content_type, byte_size, checksum_sha256, visibility_class, uploaded_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setString(4, storageKey); // duplicate
                stmt.setString(5, "receipt2.pdf");
                stmt.setString(6, "application/pdf");
                stmt.setLong(7, 2048L);
                stmt.setString(8, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                stmt.setString(9, "BRANCH_PRIVATE");
                stmt.setObject(10, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Reject invalid visibility_class (CHECK constraint ck_evidence_files_visibility)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.evidence_files (id, business_entity_id, branch_id, storage_object_key, original_filename, content_type, byte_size, checksum_sha256, visibility_class, uploaded_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setString(4, "branches/" + branchId + "/invalid_vis.pdf");
                stmt.setString(5, "receipt3.pdf");
                stmt.setString(6, "application/pdf");
                stmt.setLong(7, 2048L);
                stmt.setString(8, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                stmt.setString(9, "PUBLIC_FOR_ALL"); // Invalid visibility
                stmt.setObject(10, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject byte_size <= 0 (CHECK constraint byte_size > 0)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.evidence_files (id, business_entity_id, branch_id, storage_object_key, original_filename, content_type, byte_size, checksum_sha256, visibility_class, uploaded_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setString(4, "branches/" + branchId + "/zero_byte.pdf");
                stmt.setString(5, "empty.pdf");
                stmt.setString(6, "application/pdf");
                stmt.setLong(7, 0L); // invalid byte size
                stmt.setString(8, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                stmt.setString(9, "BRANCH_PRIVATE");
                stmt.setObject(10, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject orphaned branch (FK constraint fk_evidence_files_branch)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.evidence_files (id, business_entity_id, branch_id, storage_object_key, original_filename, content_type, byte_size, checksum_sha256, visibility_class, uploaded_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, UUID.randomUUID()); // Nonexistent branch
                stmt.setString(4, "branches/orphan/file.pdf");
                stmt.setString(5, "orphan.pdf");
                stmt.setString(6, "application/pdf");
                stmt.setLong(7, 1024L);
                stmt.setString(8, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                stmt.setString(9, "BRANCH_PRIVATE");
                stmt.setObject(10, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(8)
    @DisplayName("P0-SEC-02 / V005 Constraint verification: Audit events spine integrity & JSONB")
    void testAuditEventsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            // Insert business entity, branch, and user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_AUDIT_TEST");
                stmt.setString(3, "Audit Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_AUDIT_01");
                stmt.setString(4, "Audit Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "audit_actor@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "Audit Actor");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 1. Insert valid audit event with JSONB snapshots
            UUID eventId1 = UUID.randomUUID();
            UUID correlationId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            String oldJson = "{\"status\":\"DRAFT\",\"name\":\"Old Name\"}";
            String newJson = "{\"status\":\"ACTIVE\",\"name\":\"New Name\"}";

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.audit_events (id, branch_id, actor_user_id, correlation_id, target_type, target_id, event_name, action_reason, old_snapshot, new_snapshot) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)")) {
                stmt.setObject(1, eventId1);
                stmt.setObject(2, branchId);
                stmt.setObject(3, userId);
                stmt.setObject(4, correlationId);
                stmt.setString(5, "BRANCH");
                stmt.setObject(6, targetId);
                stmt.setString(7, "BRANCH_ACTIVATED");
                stmt.setString(8, "Onboarding completed");
                stmt.setString(9, oldJson);
                stmt.setString(10, newJson);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Verify select & read JSONB
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, branch_id, actor_user_id, correlation_id, target_type, target_id, event_name, action_reason, old_snapshot::text, new_snapshot::text, occurred_at " +
                    "FROM mtms.audit_events WHERE id = ?")) {
                stmt.setObject(1, eventId1);
                try (var rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getObject("branch_id")).isEqualTo(branchId);
                    assertThat(rs.getObject("actor_user_id")).isEqualTo(userId);
                    assertThat(rs.getObject("correlation_id")).isEqualTo(correlationId);
                    assertThat(rs.getString("target_type")).isEqualTo("BRANCH");
                    assertThat(rs.getObject("target_id")).isEqualTo(targetId);
                    assertThat(rs.getString("event_name")).isEqualTo("BRANCH_ACTIVATED");
                    assertThat(rs.getString("action_reason")).isEqualTo("Onboarding completed");
                    assertThat(rs.getString("old_snapshot")).contains("\"status\": \"DRAFT\"");
                    assertThat(rs.getString("new_snapshot")).contains("\"status\": \"ACTIVE\"");
                    assertThat(rs.getTimestamp("occurred_at")).isNotNull();
                }
            }

            // 2. Insert valid system audit event (NULL branch_id, NULL actor_user_id)
            UUID eventId2 = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.audit_events (id, branch_id, actor_user_id, correlation_id, target_type, target_id, event_name, action_reason, old_snapshot, new_snapshot) " +
                    "VALUES (?, NULL, NULL, NULL, ?, ?, ?, ?, NULL, NULL)")) {
                stmt.setObject(1, eventId2);
                stmt.setString(2, "SYSTEM");
                stmt.setObject(3, UUID.randomUUID());
                stmt.setString(4, "SYSTEM_INIT");
                stmt.setString(5, "Automated bootstrap");
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 3. Reject invalid branch_id (FK constraint violation)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.audit_events (id, branch_id, actor_user_id, target_type, target_id, event_name) " +
                    "VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, UUID.randomUUID()); // nonexistent branch
                stmt.setObject(3, userId);
                stmt.setString(4, "BRANCH");
                stmt.setObject(5, UUID.randomUUID());
                stmt.setString(6, "BRANCH_UPDATED");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject invalid actor_user_id (FK constraint violation)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.audit_events (id, branch_id, actor_user_id, target_type, target_id, event_name) " +
                    "VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, UUID.randomUUID()); // nonexistent user
                stmt.setString(4, "BRANCH");
                stmt.setObject(5, UUID.randomUUID());
                stmt.setString(6, "BRANCH_UPDATED");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject null target_type (NOT NULL constraint)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.audit_events (id, branch_id, actor_user_id, target_type, target_id, event_name) " +
                    "VALUES (?, ?, ?, NULL, ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, userId);
                stmt.setObject(4, UUID.randomUUID());
                stmt.setString(5, "TEST_EVENT");
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("P0-SEC-03 / V006 Constraint verification: Idempotency records table, unique constraints, and indices")
    void testIdempotencyRecordsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID userId = UUID.randomUUID();

            // Insert prerequisite user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "idempotency_test_user");
                stmt.setString(3, "$2a$10$dummyHashToSatisfyConstraint1234567890");
                stmt.setString(4, "Idempotency Test User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 1. Insert valid idempotency record
            UUID recordId = UUID.randomUUID();
            String idempotencyKey = "req-idem-key-001";
            String requestHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.idempotency_records (id, actor_user_id, idempotency_key, request_hash, status, response_status, response_headers, response_body, expires_at) " +
                    "VALUES (?, ?, ?, ?, 'IN_PROGRESS', NULL, NULL, NULL, now() + interval '1 day')")) {
                stmt.setObject(1, recordId);
                stmt.setObject(2, userId);
                stmt.setString(3, idempotencyKey);
                stmt.setString(4, requestHash);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 2. Reject duplicate (actor_user_id, idempotency_key) -> Unique constraint violation
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.idempotency_records (id, actor_user_id, idempotency_key, request_hash, status, expires_at) " +
                    "VALUES (?, ?, ?, ?, 'COMPLETED', now() + interval '1 day')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, userId);
                stmt.setString(3, idempotencyKey);
                stmt.setString(4, requestHash);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Reject invalid actor_user_id (foreign key violation)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.idempotency_records (id, actor_user_id, idempotency_key, request_hash, status, expires_at) " +
                    "VALUES (?, ?, ?, ?, 'IN_PROGRESS', now() + interval '1 day')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, UUID.randomUUID()); // non-existent user
                stmt.setString(3, "another-key");
                stmt.setString(4, requestHash);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject invalid status (check constraint violation)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.idempotency_records (id, actor_user_id, idempotency_key, request_hash, status, expires_at) " +
                    "VALUES (?, ?, ?, ?, 'INVALID_STATUS', now() + interval '1 day')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, userId);
                stmt.setString(3, "invalid-status-key");
                stmt.setString(4, requestHash);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("P0-SEC-04 / V007 Constraint verification: Import batches and rows tables, foreign keys, and unique constraints")
    void testImportBatchesAndRowsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            // Insert prerequisite entity, branch, user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_IMPORT_TEST");
                stmt.setString(3, "Import Test Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_IMPORT_01");
                stmt.setString(4, "Branch Import Test");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "import_test_user");
                stmt.setString(3, "$2a$10$dummyHashToSatisfyConstraint1234567890");
                stmt.setString(4, "Import Test User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 1. Insert valid import_batch
            UUID batchId = UUID.randomUUID();
            String sha256 = "a1b2c3d4e5f60123456789abcdef0123456789abcdef0123456789abcdef0123";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.import_batches (id, branch_id, import_kind, source_filename, source_sha256, status, total_rows, valid_rows, error_rows, preview_summary) " +
                    "VALUES (?, ?, 'OPENING_INVENTORY', 'inventory_20261116.xlsx', ?, 'PREVIEW', 2, 2, 0, '{\"sample\":\"test\"}'::jsonb)")) {
                stmt.setObject(1, batchId);
                stmt.setObject(2, branchId);
                stmt.setString(3, sha256);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 2. Reject duplicate (import_kind, branch_id, source_sha256)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.import_batches (id, branch_id, import_kind, source_filename, source_sha256, status) " +
                    "VALUES (?, ?, 'OPENING_INVENTORY', 'duplicate.xlsx', ?, 'PREVIEW')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setString(3, sha256);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Reject invalid status (check constraint)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.import_batches (id, branch_id, import_kind, source_filename, source_sha256, status) " +
                    "VALUES (?, ?, 'ITEMS_CATALOG', 'other.xlsx', 'b1b2c3d4e5f60123456789abcdef0123456789abcdef0123456789abcdef0123', 'UNKNOWN_STATUS')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Insert valid import_row
            UUID rowId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.import_rows (id, import_batch_id, row_number, raw_data, normalized_data, errors, status) " +
                    "VALUES (?, ?, 1, '{\"item_code\":\"CF-01\"}'::jsonb, '{\"item_code\":\"CF-01\"}'::jsonb, '[]'::jsonb, 'VALID')")) {
                stmt.setObject(1, rowId);
                stmt.setObject(2, batchId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 5. Reject duplicate (import_batch_id, row_number)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.import_rows (id, import_batch_id, row_number, raw_data, status) " +
                    "VALUES (?, ?, 1, '{\"item_code\":\"CF-02\"}'::jsonb, 'VALID')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, batchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Reject invalid row_number <= 0 (check constraint)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.import_rows (id, import_batch_id, row_number, raw_data, status) " +
                    "VALUES (?, ?, 0, '{\"item_code\":\"CF-03\"}'::jsonb, 'VALID')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, batchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("P0-INV-01 / V008 Constraint verification: Units, Suppliers, Catalog Items and Pack Conversions")
    void testInventoryCatalogAndConversionsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();

            // Insert prerequisite entity
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_INV_TEST");
                stmt.setString(3, "Inventory Test Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            // 1. Verify seeded units exist
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM mtms.units WHERE unit_code = 'CAN'")) {
                var rs = stmt.executeQuery();
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }

            // 2. Reject duplicate unit_code
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.units (id, unit_code, unit_name, dimension) VALUES (?, 'CAN', 'Duplicate Can', 'COUNT')")) {
                stmt.setObject(1, UUID.randomUUID());
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Reject invalid unit dimension
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.units (id, unit_code, unit_name, dimension) VALUES (?, 'INVALID_DIM', 'Name', 'UNKNOWN_DIM')")) {
                stmt.setObject(1, UUID.randomUUID());
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Insert valid supplier
            UUID supplierId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.suppliers (id, business_entity_id, supplier_code, name, status) VALUES (?, ?, 'SUP-TEST-01', 'Test Supplier', 'ACTIVE')")) {
                stmt.setObject(1, supplierId);
                stmt.setObject(2, entityId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 5. Reject duplicate supplier_code within same entity
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.suppliers (id, business_entity_id, supplier_code, name, status) VALUES (?, ?, 'SUP-TEST-01', 'Duplicate Supplier', 'ACTIVE')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Get base unit ID for 'CAN'
            UUID canUnitId;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM mtms.units WHERE unit_code = 'CAN'")) {
                var rs = stmt.executeQuery();
                rs.next();
                canUnitId = (UUID) rs.getObject(1);
            }

            // 7. Reject catalog item where expiration_tracking_required=true but lot_tracking_required=false (ck_catalog_items_exp_lot)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.catalog_items (id, business_entity_id, item_code, name, base_unit_id, item_kind, lot_tracking_required, expiration_tracking_required, status) " +
                    "VALUES (?, ?, 'ING-INVALID-01', 'Invalid Tracking Item', ?, 'INGREDIENT', false, true, 'ACTIVE')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, canUnitId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Insert valid catalog item
            UUID itemId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.catalog_items (id, business_entity_id, item_code, name, base_unit_id, item_kind, lot_tracking_required, expiration_tracking_required, status) " +
                    "VALUES (?, ?, 'ING-MILK-OT', 'Ong Tho Milk', ?, 'INGREDIENT', true, true, 'ACTIVE')")) {
                stmt.setObject(1, itemId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, canUnitId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 9. Reject duplicate item_code within same entity
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.catalog_items (id, business_entity_id, item_code, name, base_unit_id, item_kind, lot_tracking_required, expiration_tracking_required, status) " +
                    "VALUES (?, ?, 'ING-MILK-OT', 'Duplicate Milk', ?, 'INGREDIENT', true, true, 'ACTIVE')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, canUnitId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 10. Insert valid pack conversion (1 box = 24 cans)
            UUID conversionId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.supplier_pack_conversions (id, business_entity_id, supplier_id, item_id, pack_name, base_units_per_pack, effective_from) " +
                    "VALUES (?, ?, ?, ?, 'Thung 24 lon', 24.000, '2026-01-01')")) {
                stmt.setObject(1, conversionId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, supplierId);
                stmt.setObject(4, itemId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 11. Reject non-positive base_units_per_pack <= 0
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.supplier_pack_conversions (id, business_entity_id, supplier_id, item_id, pack_name, base_units_per_pack, effective_from) " +
                    "VALUES (?, ?, ?, ?, 'Thung 0 lon', 0.000, '2026-01-01')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, supplierId);
                stmt.setObject(4, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 12. Reject invalid date span effective_to < effective_from
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.supplier_pack_conversions (id, business_entity_id, supplier_id, item_id, pack_name, base_units_per_pack, effective_from, effective_to) " +
                    "VALUES (?, ?, ?, ?, 'Invalid Date Pack', 10.000, '2026-05-01', '2026-04-01')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, supplierId);
                stmt.setObject(4, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 13. Reject duplicate pack conversion (supplier_id, item_id, pack_name, effective_from)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.supplier_pack_conversions (id, business_entity_id, supplier_id, item_id, pack_name, base_units_per_pack, effective_from) " +
                    "VALUES (?, ?, ?, ?, 'Thung 24 lon', 24.000, '2026-01-01')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, supplierId);
                stmt.setObject(4, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(11)
    @DisplayName("P0-INV-02 / V009 Constraint verification: Stock Balances, Stock Lots, Stock Movements, and Opening Inventory Records")
    void testStockBalancesAndMovementsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            // Insert prerequisite entity, branch, user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_STOCK_TEST");
                stmt.setString(3, "Stock Test Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_STOCK_01");
                stmt.setString(4, "Branch Stock Test");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "stock_test_user@mtms.local");
                stmt.setString(3, "$2a$10$dummyHashToSatisfyConstraint1234567890");
                stmt.setString(4, "Stock Test User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // Get base unit ID for 'CAN'
            UUID canUnitId;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM mtms.units WHERE unit_code = 'CAN'")) {
                var rs = stmt.executeQuery();
                rs.next();
                canUnitId = (UUID) rs.getObject(1);
            }

            // Insert catalog item
            UUID itemId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.catalog_items (id, business_entity_id, item_code, name, base_unit_id, item_kind, lot_tracking_required, expiration_tracking_required, status) " +
                    "VALUES (?, ?, 'ING-MILK-01', 'Milk Test', ?, 'INGREDIENT', true, true, 'ACTIVE')")) {
                stmt.setObject(1, itemId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, canUnitId);
                stmt.executeUpdate();
            }

            // 1. Insert valid stock_balance
            UUID balanceId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_balances (id, business_entity_id, branch_id, item_id, quantity, inventory_value, avg_unit_cost) " +
                    "VALUES (?, ?, ?, ?, 50.000, 1200000.00, 24000.000000)")) {
                stmt.setObject(1, balanceId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, itemId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 2. Reject duplicate stock_balance (branch_id, item_id)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_balances (id, business_entity_id, branch_id, item_id, quantity, inventory_value, avg_unit_cost) " +
                    "VALUES (?, ?, ?, ?, 10.000, 240000.00, 24000.000000)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Reject negative quantity on stock_balances
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_balances (id, business_entity_id, branch_id, item_id, quantity, inventory_value, avg_unit_cost) " +
                    "VALUES (?, ?, ?, ?, -5.000, 0, 0)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, UUID.randomUUID());
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Insert valid stock_lot
            UUID lotId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_lots (id, business_entity_id, branch_id, item_id, lot_code, expires_on, quantity) " +
                    "VALUES (?, ?, ?, ?, 'LOT-202611-01', '2027-11-16', 50.000)")) {
                stmt.setObject(1, lotId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, itemId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 5. Reject duplicate stock_lot (branch_id, item_id, lot_code)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_lots (id, business_entity_id, branch_id, item_id, lot_code, expires_on, quantity) " +
                    "VALUES (?, ?, ?, ?, 'LOT-202611-01', '2027-11-16', 20.000)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Insert valid stock_movement
            UUID movementId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_movements (id, business_entity_id, branch_id, item_id, lot_id, business_date, movement_type, signed_quantity, signed_value, booked_avg_cost, source_type, source_id, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, '2026-11-16', 'OPENING_BALANCE', 50.000, 1200000.00, 24000.000000, 'OPENING_INVENTORY_RECORD', ?, ?)")) {
                stmt.setObject(1, movementId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, itemId);
                stmt.setObject(5, lotId);
                stmt.setObject(6, UUID.randomUUID());
                stmt.setObject(7, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 7. Reject stock_movement with signed_quantity = 0
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_movements (id, business_entity_id, branch_id, item_id, business_date, movement_type, signed_quantity, signed_value, booked_avg_cost, source_type, source_id) " +
                    "VALUES (?, ?, ?, ?, '2026-11-16', 'OPENING_BALANCE', 0.000, 0.00, 0.000000, 'OPENING_INVENTORY_RECORD', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, itemId);
                stmt.setObject(5, UUID.randomUUID());
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Insert valid opening_inventory_record
            UUID openingId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_inventory_records (id, business_entity_id, branch_id, opening_date, total_items, total_quantity, total_value, status, approved_by, note) " +
                    "VALUES (?, ?, ?, '2026-11-16', 1, 50.000, 1200000.00, 'COMMITTED', ?, 'Opening note')")) {
                stmt.setObject(1, openingId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 9. Reject second opening_inventory_record on SAME branch -> Enforces TC-024 POST ONCE
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_inventory_records (id, business_entity_id, branch_id, opening_date, total_items, total_quantity, total_value, status, approved_by) " +
                    "VALUES (?, ?, ?, '2026-11-16', 2, 70.000, 4200000.00, 'COMMITTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId); // Duplicate branch_id!
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 10. Reject total_items <= 0 on opening_inventory_records
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_inventory_records (id, business_entity_id, branch_id, opening_date, total_items, total_quantity, total_value, status, approved_by) " +
                    "VALUES (?, ?, ?, '2026-11-16', 0, 50.000, 1200000.00, 'COMMITTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, UUID.randomUUID()); // new branch
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(12)
    @DisplayName("P0-INV-03 / V010 Constraint verification: Purchase Receipts, Lines, and Payables")
    void testPurchaseReceiptsAndPayablesConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID supplierId = UUID.randomUUID();
            UUID unitId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            // 1. Seed Business Entity, Branch, User
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_INV03_TEST");
                stmt.setString(3, "INV03 Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_INV03_01");
                stmt.setString(4, "INV03 Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "inv03_user@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "INV03 User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 2. Seed Supplier, Unit, Item
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.suppliers (id, business_entity_id, supplier_code, name, status) VALUES (?, ?, ?, ?, 'ACTIVE')")) {
                stmt.setObject(1, supplierId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "SUP_INV03_01");
                stmt.setString(4, "Vinamilk Supplier");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.units (id, unit_code, unit_name, dimension) VALUES (?, 'CAN_INV03', 'Lon INV03', 'COUNT')")) {
                stmt.setObject(1, unitId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.catalog_items (id, business_entity_id, item_code, name, base_unit_id, item_kind, lot_tracking_required, expiration_tracking_required, status) " +
                    "VALUES (?, ?, 'ITEM_INV03_01', 'Test Sữa Hộp', ?, 'INGREDIENT', true, true, 'ACTIVE')")) {
                stmt.setObject(1, itemId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, unitId);
                stmt.executeUpdate();
            }

            // 3. Insert valid payable
            UUID payableId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payables (id, business_entity_id, branch_id, counterparty_label, source_type, source_id, incurred_date, due_date, original_amount, paid_amount, status) " +
                    "VALUES (?, ?, ?, 'Vinamilk Supplier', 'PURCHASE_RECEIPT', ?, '2026-11-16', '2026-12-16', 5000000.00, 0.00, 'OPEN')")) {
                stmt.setObject(1, payableId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, UUID.randomUUID());
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 4. Reject payable with paid_amount > original_amount (CHECK constraint ck_payables_paid_le_original)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payables (id, business_entity_id, branch_id, counterparty_label, source_type, incurred_date, original_amount, paid_amount, status) " +
                    "VALUES (?, ?, ?, 'Vinamilk Supplier', 'PURCHASE_RECEIPT', '2026-11-16', 5000000.00, 6000000.00, 'OPEN')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject payable with invalid status
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payables (id, business_entity_id, branch_id, counterparty_label, source_type, incurred_date, original_amount, paid_amount, status) " +
                    "VALUES (?, ?, ?, 'Vinamilk Supplier', 'PURCHASE_RECEIPT', '2026-11-16', 5000000.00, 0.00, 'INVALID_STATUS')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Insert valid purchase_receipt referencing payable
            UUID receiptId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.purchase_receipts (id, business_entity_id, branch_id, receipt_number, supplier_id, received_on, invoice_number, invoice_total, payment_status, payable_id, status, posted_by, posted_at) " +
                    "VALUES (?, ?, ?, 'PNK-202611-0001', ?, '2026-11-16', 'INV-001', 5000000.00, 'UNPAID', ?, 'POSTED', ?, now())")) {
                stmt.setObject(1, receiptId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, supplierId);
                stmt.setObject(5, payableId);
                stmt.setObject(6, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 7. Reject duplicate receipt_number in SAME branch (UNIQUE constraint uq_purchase_receipts_branch_receipt_number)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.purchase_receipts (id, business_entity_id, branch_id, receipt_number, received_on, invoice_total, payment_status, status) " +
                    "VALUES (?, ?, ?, 'PNK-202611-0001', '2026-11-16', 1000000.00, 'UNPAID', 'DRAFT')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Reject invalid receipt status
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.purchase_receipts (id, business_entity_id, branch_id, receipt_number, received_on, invoice_total, payment_status, status) " +
                    "VALUES (?, ?, ?, 'PNK-202611-0002', '2026-11-16', 1000000.00, 'UNPAID', 'CANCELLED')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 9. Insert valid purchase_receipt_line
            UUID lineId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.purchase_receipt_lines (id, receipt_id, line_no, item_id, pack_name, declared_pack_quantity, conversion_snapshot, received_base_quantity, base_unit_cost, line_total, lot_code, expires_on) " +
                    "VALUES (?, ?, 1, ?, 'Thùng 24 lon', 5.000, 24.000, 120.000, 25000.000000, 3000000.00, 'LOT-VNM-01', '2027-11-16')")) {
                stmt.setObject(1, lineId);
                stmt.setObject(2, receiptId);
                stmt.setObject(3, itemId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 10. Reject duplicate line_no in SAME receipt (UNIQUE constraint uq_purchase_receipt_lines_receipt_line_no)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.purchase_receipt_lines (id, receipt_id, line_no, item_id, conversion_snapshot, received_base_quantity, base_unit_cost, line_total) " +
                    "VALUES (?, ?, 1, ?, 1.000, 10.000, 200000.000000, 2000000.00)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, receiptId);
                stmt.setObject(3, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 11. Reject received_base_quantity <= 0 (CHECK constraint ck_purchase_receipt_lines_received_base_quantity)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.purchase_receipt_lines (id, receipt_id, line_no, item_id, conversion_snapshot, received_base_quantity, base_unit_cost, line_total) " +
                    "VALUES (?, ?, 2, ?, 1.000, 0.000, 200000.000000, 0.00)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, receiptId);
                stmt.setObject(3, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 12. Test ON DELETE CASCADE: Deleting receipt deletes its lines
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mtms.purchase_receipts WHERE id = ?")) {
                stmt.setObject(1, receiptId);
                int deleted = stmt.executeUpdate();
                assertThat(deleted).isEqualTo(1);
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT count(*) FROM mtms.purchase_receipt_lines WHERE id = ?")) {
                stmt.setObject(1, lineId);
                try (var rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isZero();
                }
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("P0-INV-04 & P0-INV-05 / V011 Constraint verification: Stock Documents and Document Lines")
    void testStockDocumentsAndLinesConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID unitId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            // 1. Seed Business Entity, Branch, User, Unit, Item
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_INV04_TEST");
                stmt.setString(3, "INV04 Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_INV04_01");
                stmt.setString(4, "INV04 Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "inv04_user@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "INV04 User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.units (id, unit_code, unit_name, dimension) VALUES (?, 'KG_INV04', 'Kilogram INV04', 'MASS')")) {
                stmt.setObject(1, unitId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.catalog_items (id, business_entity_id, item_code, name, base_unit_id, item_kind, lot_tracking_required, expiration_tracking_required, status) " +
                    "VALUES (?, ?, 'ITEM_INV04_01', 'Test Cafe Bean', ?, 'INGREDIENT', false, false, 'ACTIVE')")) {
                stmt.setObject(1, itemId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, unitId);
                stmt.executeUpdate();
            }

            // 2. Insert valid stock_document
            UUID docId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_documents (id, business_entity_id, branch_id, document_number, document_type, document_date, status, reason, total_lines, total_quantity, total_booked_cost, created_by) " +
                    "VALUES (?, ?, ?, 'PXK-202611-0001', 'CONSUMPTION_ISSUE', '2026-11-16', 'DRAFT', 'Xuất dùng quầy bar', 1, 5.000, 550000.00, ?)")) {
                stmt.setObject(1, docId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 3. Reject duplicate document_number in same branch
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_documents (id, business_entity_id, branch_id, document_number, document_type, document_date, status, reason, created_by) " +
                    "VALUES (?, ?, ?, 'PXK-202611-0001', 'CONSUMPTION_ISSUE', '2026-11-16', 'DRAFT', 'Duplicate doc number', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject invalid document_type
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_documents (id, business_entity_id, branch_id, document_number, document_type, document_date, status, created_by) " +
                    "VALUES (?, ?, ?, 'PXK-202611-0002', 'INVALID_TYPE', '2026-11-16', 'DRAFT', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject invalid status
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_documents (id, business_entity_id, branch_id, document_number, document_type, document_date, status, created_by) " +
                    "VALUES (?, ?, ?, 'PXK-202611-0003', 'CONSUMPTION_ISSUE', '2026-11-16', 'UNKNOWN_STATUS', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Insert valid stock_document_lines
            UUID lineId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_document_lines (id, document_id, line_no, item_id, issue_quantity, booked_unit_cost, booked_total, notes) " +
                    "VALUES (?, ?, 1, ?, 5.000, 110000.000000, 550000.00, '5kg cafe')")) {
                stmt.setObject(1, lineId);
                stmt.setObject(2, docId);
                stmt.setObject(3, itemId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 7. Reject duplicate line_no in same document
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_document_lines (id, document_id, line_no, item_id, issue_quantity) " +
                    "VALUES (?, ?, 1, ?, 2.000)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, docId);
                stmt.setObject(3, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Reject issue_quantity <= 0
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.stock_document_lines (id, document_id, line_no, item_id, issue_quantity) " +
                    "VALUES (?, ?, 2, ?, 0.000)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, docId);
                stmt.setObject(3, itemId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 9. Test ON DELETE CASCADE
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mtms.stock_documents WHERE id = ?")) {
                stmt.setObject(1, docId);
                int deleted = stmt.executeUpdate();
                assertThat(deleted).isEqualTo(1);
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT count(*) FROM mtms.stock_document_lines WHERE id = ?")) {
                stmt.setObject(1, lineId);
                try (var rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isZero();
                }
            }
        }
    }

    @Test
    @Order(14)
    @DisplayName("V012: Inventory reporting indexes exist on stock_movements and stock_balances")
    void testInventoryReportingIndexesExist() throws SQLException {
        try (Connection conn = createConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT indexname FROM pg_indexes WHERE schemaname = 'mtms' AND tablename = 'stock_movements' AND indexname = 'idx_stock_movements_cogs_lookup'")) {
                try (var rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("indexname")).isEqualTo("idx_stock_movements_cogs_lookup");
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT indexname FROM pg_indexes WHERE schemaname = 'mtms' AND tablename = 'stock_balances' AND indexname = 'idx_stock_balances_valuation'")) {
                try (var rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("indexname")).isEqualTo("idx_stock_balances_valuation");
                }
            }
        }
    }

    @Test
    @Order(15)
    @DisplayName("V013: Business days and daily revenue constraints verification")
    void testBusinessDaysAndDailyRevenueConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, 'ENT_REV', 'Rev Entity', 'ACTIVE')")) {
                stmt.setObject(1, entityId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, 'BR_REV_01', 'Rev Branch', 'ACTIVE')")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, 'rev_user@mtms.local', '$2a$12$dummy', 'Rev User', 'ACTIVE')")) {
                stmt.setObject(1, userId);
                stmt.executeUpdate();
            }

            // 1. Valid business_days (OPEN)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_days (id, business_entity_id, branch_id, business_date, operation_status, close_reason, recorded_by) " +
                    "VALUES (?, ?, ?, '2026-11-16', 'OPEN', NULL, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 2. Reject duplicate (branch_id, business_date)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_days (id, business_entity_id, branch_id, business_date, operation_status, close_reason, recorded_by) " +
                    "VALUES (?, ?, ?, '2026-11-16', 'OPEN', NULL, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Reject CLOSED without close_reason
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_days (id, business_entity_id, branch_id, business_date, operation_status, close_reason, recorded_by) " +
                    "VALUES (?, ?, ?, '2026-11-17', 'CLOSED', NULL, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Valid business_days (CLOSED with reason)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_days (id, business_entity_id, branch_id, business_date, operation_status, close_reason, recorded_by) " +
                    "VALUES (?, ?, ?, '2026-11-17', 'CLOSED', 'Cúp điện khu vực', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 5. Valid daily_revenue_reports
            UUID reportId1 = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.daily_revenue_reports (id, business_entity_id, branch_id, business_date, revision, is_current, cash_amount, bank_transfer_amount, delivery_app_net_amount, other_adjustment_amount, total_revenue, status, submitted_by) " +
                    "VALUES (?, ?, ?, '2026-11-16', 1, TRUE, 1000.00, 2000.00, 500.00, 0.00, 3500.00, 'SUBMITTED', ?)")) {
                stmt.setObject(1, reportId1);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 6. Reject invalid math total_revenue != sum
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.daily_revenue_reports (id, business_entity_id, branch_id, business_date, revision, is_current, cash_amount, bank_transfer_amount, delivery_app_net_amount, other_adjustment_amount, total_revenue, status, submitted_by) " +
                    "VALUES (?, ?, ?, '2026-11-18', 1, TRUE, 1000.00, 2000.00, 500.00, 0.00, 9999.00, 'SUBMITTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 7. Reject duplicate is_current = TRUE for same branch and date
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.daily_revenue_reports (id, business_entity_id, branch_id, business_date, revision, is_current, cash_amount, bank_transfer_amount, delivery_app_net_amount, other_adjustment_amount, total_revenue, status, submitted_by) " +
                    "VALUES (?, ?, ?, '2026-11-16', 2, TRUE, 1500.00, 2000.00, 500.00, 0.00, 4000.00, 'SUBMITTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Allow second revision when old revision has is_current = FALSE
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE mtms.daily_revenue_reports SET is_current = FALSE, status = 'SUPERSEDED' WHERE id = ?")) {
                stmt.setObject(1, reportId1);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.daily_revenue_reports (id, business_entity_id, branch_id, business_date, revision, is_current, cash_amount, bank_transfer_amount, delivery_app_net_amount, other_adjustment_amount, total_revenue, status, submitted_by) " +
                    "VALUES (?, ?, ?, '2026-11-16', 2, TRUE, 1500.00, 2000.00, 500.00, 0.00, 4000.00, 'SUBMITTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }
        }
    }

    @Test
    @Order(17)
    @DisplayName("P0-FIN-01 / P0-FIN-02 / V014 Constraint verification: Financial categories, accounts, transfers, and cash ledger")
    void testFinancialCategoriesAndCashLedgerConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            // Insert base business entity, branch, and user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_FIN_TEST");
                stmt.setString(3, "Finance Test Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_FIN_01");
                stmt.setString(4, "Finance Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "finance_admin@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "Finance Admin");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 1. Verify 13 seeded system categories
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM mtms.financial_categories WHERE is_system = TRUE")) {
                try (var rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(13);
                }
            }

            // 2. Reject duplicate category_code
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system) " +
                    "VALUES ('REV_CASH', 'Duplicate Revenue', 'REVENUE', 'INFLOW', FALSE)")) {
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Reject invalid category_group
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_categories (category_code, category_name, category_group, flow_direction, is_system) " +
                    "VALUES ('INVALID_GRP', 'Invalid Group Category', 'UNKNOWN_GROUP', 'INFLOW', FALSE)")) {
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Create financial accounts: 1 Cash account and 1 Bank account
            UUID cashAccountId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, account_name, account_kind, current_balance, status) " +
                    "VALUES (?, ?, 'Quỹ tiền mặt chi nhánh', 'CASH', 10000000.00, 'ACTIVE')")) {
                stmt.setObject(1, cashAccountId);
                stmt.setObject(2, branchId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Reject duplicate account_name on same branch
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, account_name, account_kind, current_balance, status) " +
                    "VALUES (?, ?, 'Quỹ tiền mặt chi nhánh', 'CASH', 5000000.00, 'ACTIVE')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // Insert branch bank account for the bank financial account
            UUID branchBankAccountId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branch_bank_accounts (id, branch_id, bank_name, account_holder_name, account_number_ciphertext, masked_account_number, is_primary, status) " +
                    "VALUES (?, ?, 'Vietcombank', 'CTY PHUC LONG', 'cipher_123', '**** **** 1234', TRUE, 'ACTIVE')")) {
                stmt.setObject(1, branchBankAccountId);
                stmt.setObject(2, branchId);
                stmt.executeUpdate();
            }

            UUID bankAccountId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, bank_account_id, account_name, account_kind, current_balance, status) " +
                    "VALUES (?, ?, ?, 'Tài khoản Vietcombank', 'BANK', 20000000.00, 'ACTIVE')")) {
                stmt.setObject(1, bankAccountId);
                stmt.setObject(2, branchId);
                stmt.setObject(3, branchBankAccountId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 5. Internal transfer (TC-010): from Cash to Bank
            UUID transferId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.internal_transfers (id, branch_id, from_account_id, to_account_id, amount, business_date, status, note, created_by) " +
                    "VALUES (?, ?, ?, ?, 5000000.00, '2026-11-17', 'COMPLETED', 'Nộp tiền mặt vào ngân hàng', ?)")) {
                stmt.setObject(1, transferId);
                stmt.setObject(2, branchId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, bankAccountId);
                stmt.setObject(5, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Reject transfer where from_account == to_account
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.internal_transfers (id, branch_id, from_account_id, to_account_id, amount, business_date, status, created_by) " +
                    "VALUES (?, ?, ?, ?, 1000000.00, '2026-11-17', 'COMPLETED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, cashAccountId); // Same account
                stmt.setObject(5, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Cash Ledger Entries: Record 2 legs of internal transfer
            UUID trfCategoryId;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id FROM mtms.financial_categories WHERE category_code = 'TRF_INTERNAL'")) {
                try (var rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    trfCategoryId = (UUID) rs.getObject("id");
                }
            }

            // Source leg (OUTFLOW from Cash)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.cash_ledger_entries (id, account_id, branch_id, business_date, entry_type, amount, signed_amount, running_balance, category_id, source_type, source_id, source_leg, status, created_by) " +
                    "VALUES (?, ?, ?, '2026-11-17', 'OUTFLOW', 5000000.00, -5000000.00, 5000000.00, ?, 'INTERNAL_TRANSFER', ?, 'SOURCE', 'POSTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, cashAccountId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, trfCategoryId);
                stmt.setObject(5, transferId);
                stmt.setObject(6, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Destination leg (INFLOW to Bank)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.cash_ledger_entries (id, account_id, branch_id, business_date, entry_type, amount, signed_amount, running_balance, category_id, source_type, source_id, source_leg, status, created_by) " +
                    "VALUES (?, ?, ?, '2026-11-17', 'INFLOW', 5000000.00, 5000000.00, 25000000.00, ?, 'INTERNAL_TRANSFER', ?, 'DESTINATION', 'POSTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, bankAccountId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, trfCategoryId);
                stmt.setObject(5, transferId);
                stmt.setObject(6, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // TC-011: Reject duplicate posting on same (account_id, source_type, source_id, source_leg)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.cash_ledger_entries (id, account_id, branch_id, business_date, entry_type, amount, signed_amount, running_balance, category_id, source_type, source_id, source_leg, status, created_by) " +
                    "VALUES (?, ?, ?, '2026-11-17', 'OUTFLOW', 5000000.00, -5000000.00, 0.00, ?, 'INTERNAL_TRANSFER', ?, 'SOURCE', 'POSTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, cashAccountId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, trfCategoryId);
                stmt.setObject(5, transferId);
                stmt.setObject(6, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(16)
    @DisplayName("Constraint verification: V015 Payable payments and Receivables constraints")
    void testPayablePaymentsAndReceivablesConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID cashAccountId = UUID.randomUUID();
            UUID payableId = UUID.randomUUID();
            UUID receivableId = UUID.randomUUID();

            // 1. Setup base entity, branch, user, account
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, 'ENT-V015', 'Entity V015', 'ACTIVE')")) {
                stmt.setObject(1, entityId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, 'BR-V015', 'Branch V015', 'ACTIVE')")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, 'user_v015', 'hash', 'User V015', 'ACTIVE')")) {
                stmt.setObject(1, userId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, account_name, account_kind, current_balance, status) VALUES (?, ?, 'Quỹ tiền mặt V015', 'CASH', 10000000.00, 'ACTIVE')")) {
                stmt.setObject(1, cashAccountId);
                stmt.setObject(2, branchId);
                stmt.executeUpdate();
            }

            // 2. Setup payable
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payables (id, business_entity_id, branch_id, counterparty_label, source_type, incurred_date, original_amount, paid_amount, status) " +
                    "VALUES (?, ?, ?, 'NCC V015', 'MANUAL_PAYABLE', '2026-11-18', 5000000.00, 0.00, 'OPEN')")) {
                stmt.setObject(1, payableId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.executeUpdate();
            }

            // 3. Insert valid payable payment
            UUID paymentId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payable_payments (id, payable_id, account_id, amount, paid_on, payment_method, status, posted_by) " +
                    "VALUES (?, ?, ?, 2000000.00, '2026-11-18', 'CASH', 'POSTED', ?)")) {
                stmt.setObject(1, paymentId);
                stmt.setObject(2, payableId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Reject payable payment with non-positive amount
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payable_payments (id, payable_id, account_id, amount, paid_on, payment_method, status, posted_by) " +
                    "VALUES (?, ?, ?, 0.00, '2026-11-18', 'CASH', 'POSTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, payableId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // Reject payable payment with invalid method
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payable_payments (id, payable_id, account_id, amount, paid_on, payment_method, status, posted_by) " +
                    "VALUES (?, ?, ?, 100000.00, '2026-11-18', 'INVALID_METHOD', 'POSTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, payableId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Insert valid receivable
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.receivables (id, business_entity_id, branch_id, counterparty_label, source_type, incurred_date, original_amount, received_amount, status, created_by) " +
                    "VALUES (?, ?, ?, 'Đối tác bồi thường V015', 'MANUAL_RECEIVABLE', '2026-11-18', 3000000.00, 0.00, 'OPEN', ?)")) {
                stmt.setObject(1, receivableId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Reject receivable where received_amount > original_amount
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.receivables (id, business_entity_id, branch_id, counterparty_label, source_type, incurred_date, original_amount, received_amount, status, created_by) " +
                    "VALUES (?, ?, ?, 'Invalid Rec', 'MANUAL_RECEIVABLE', '2026-11-18', 1000000.00, 2000000.00, 'OPEN', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Insert valid receivable receipt
            UUID receiptId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.receivable_receipts (id, receivable_id, account_id, amount, received_on, payment_method, status, posted_by) " +
                    "VALUES (?, ?, ?, 1500000.00, '2026-11-18', 'BANK_TRANSFER', 'POSTED', ?)")) {
                stmt.setObject(1, receiptId);
                stmt.setObject(2, receivableId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Reject receivable receipt with non-positive amount
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.receivable_receipts (id, receivable_id, account_id, amount, received_on, payment_method, status, posted_by) " +
                    "VALUES (?, ?, ?, -500.00, '2026-11-18', 'BANK_TRANSFER', 'POSTED', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, receivableId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(17)
    @DisplayName("Constraint verification: V016 Branch expense configs and Expenses constraints")
    void testExpenseAndApprovalConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID cashAccountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID expenseId = UUID.randomUUID();

            // 1. Setup base entity, branch, user, account, category
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, 'ENT-V016', 'Entity V016', 'ACTIVE')")) {
                stmt.setObject(1, entityId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, 'BR-V016', 'Branch V016', 'ACTIVE')")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, 'user_v016', 'hash', 'User V016', 'ACTIVE')")) {
                stmt.setObject(1, userId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, account_name, account_kind, current_balance, status) VALUES (?, ?, 'Quỹ tiền mặt V016', 'CASH', 10000000.00, 'ACTIVE')")) {
                stmt.setObject(1, cashAccountId);
                stmt.setObject(2, branchId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_categories (id, category_code, category_name, category_group, flow_direction, is_system) " +
                    "VALUES (?, 'OPEX_TEST_V016', 'Chi phí test V016', 'OPERATING_EXPENSE', 'OUTFLOW', FALSE)")) {
                stmt.setObject(1, categoryId);
                stmt.executeUpdate();
            }

            // 2. Insert valid branch expense config
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branch_expense_configs (branch_id, alert_threshold_amount, evidence_required_threshold) " +
                    "VALUES (?, 2500000.00, 600000.00)")) {
                stmt.setObject(1, branchId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 3. Insert valid paid expense
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.expenses (id, branch_id, expense_code, expense_date, category_id, amount, is_paid, payment_account_id, status, reason, created_by) " +
                    "VALUES (?, ?, 'EXP-BR01-202611-001', '2026-11-18', ?, 1200000.00, TRUE, ?, 'DRAFT', 'Tiền điện', ?)")) {
                stmt.setObject(1, expenseId);
                stmt.setObject(2, branchId);
                stmt.setObject(3, categoryId);
                stmt.setObject(4, cashAccountId);
                stmt.setObject(5, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 4. Reject expense with is_paid = TRUE but payment_account_id = NULL
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.expenses (id, branch_id, expense_code, expense_date, category_id, amount, is_paid, payment_account_id, status, reason, created_by) " +
                    "VALUES (?, ?, 'EXP-BR01-202611-002', '2026-11-18', ?, 500000.00, TRUE, NULL, 'DRAFT', 'Thiếu tài khoản', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, categoryId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject duplicate (branch_id, expense_code)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.expenses (id, branch_id, expense_code, expense_date, category_id, amount, is_paid, payment_account_id, status, reason, created_by) " +
                    "VALUES (?, ?, 'EXP-BR01-202611-001', '2026-11-18', ?, 800000.00, TRUE, ?, 'DRAFT', 'Trùng mã', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, categoryId);
                stmt.setObject(4, cashAccountId);
                stmt.setObject(5, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Reject amount <= 0
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.expenses (id, branch_id, expense_code, expense_date, category_id, amount, is_paid, payment_account_id, status, reason, created_by) " +
                    "VALUES (?, ?, 'EXP-BR01-202611-003', '2026-11-18', ?, 0.00, TRUE, ?, 'DRAFT', 'Số tiền bằng 0', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, categoryId);
                stmt.setObject(4, cashAccountId);
                stmt.setObject(5, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 7. Reject invalid status
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.expenses (id, branch_id, expense_code, expense_date, category_id, amount, is_paid, payment_account_id, status, reason, created_by) " +
                    "VALUES (?, ?, 'EXP-BR01-202611-004', '2026-11-18', ?, 200000.00, TRUE, ?, 'INVALID_STATUS', 'Trạng thái sai', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, categoryId);
                stmt.setObject(4, cashAccountId);
                stmt.setObject(5, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(18)
    @DisplayName("Constraint verification: V017 Cash reconciliations constraints and seed categories (TC-012)")
    void testCashReconciliationConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID cashAccountId = UUID.randomUUID();
            UUID reconId = UUID.randomUUID();

            // 1. Verify seed categories exist
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT category_code, flow_direction FROM mtms.financial_categories WHERE category_code IN ('ADJ_SURPLUS', 'ADJ_SHORTAGE')")) {
                var rs = stmt.executeQuery();
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                assertThat(count).isEqualTo(2);
            }

            // 2. Setup entity, branch, user, account
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, 'ENT-V017', 'Entity V017', 'ACTIVE')")) {
                stmt.setObject(1, entityId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, 'BR-V017', 'Branch V017', 'ACTIVE')")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, 'user_v017', 'hash', 'User V017', 'ACTIVE')")) {
                stmt.setObject(1, userId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, account_name, account_kind, current_balance, status) VALUES (?, ?, 'Quỹ tiền mặt V017', 'CASH', 15000000.00, 'ACTIVE')")) {
                stmt.setObject(1, cashAccountId);
                stmt.setObject(2, branchId);
                stmt.executeUpdate();
            }

            // 3. Insert valid cash reconciliation
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.cash_reconciliations (id, branch_id, account_id, reconciliation_date, recorded_balance, observed_balance, variance_amount, disposition, note, created_by) " +
                    "VALUES (?, ?, ?, '2026-11-18', 15000000.00, 14950000.00, -50000.00, 'OPEN', 'Kiểm đếm thiếu 50k', ?)")) {
                stmt.setObject(1, reconId);
                stmt.setObject(2, branchId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 4. Reject mismatch in variance_amount check constraint (observed - recorded != variance)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.cash_reconciliations (id, branch_id, account_id, reconciliation_date, recorded_balance, observed_balance, variance_amount, disposition, created_by) " +
                    "VALUES (?, ?, ?, '2026-11-19', 15000000.00, 14950000.00, 100000.00, 'OPEN', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject duplicate (account_id, reconciliation_date)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.cash_reconciliations (id, branch_id, account_id, reconciliation_date, recorded_balance, observed_balance, variance_amount, disposition, created_by) " +
                    "VALUES (?, ?, ?, '2026-11-18', 15000000.00, 14950000.00, -50000.00, 'OPEN', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Reject invalid disposition
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.cash_reconciliations (id, branch_id, account_id, reconciliation_date, recorded_balance, observed_balance, variance_amount, disposition, created_by) " +
                    "VALUES (?, ?, ?, '2026-11-20', 1000000.00, 1000000.00, 0.00, 'INVALID_DISP', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, cashAccountId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(19)
    @DisplayName("Constraint verification: V018 Tax obligations and payments constraints and seed categories (BR-FIN-04 / TC-012)")
    void testTaxObligationsAndPaymentsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID obligationId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();

            // 1. Verify seed category TAX_PAYMENT exists
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT category_code, flow_direction, is_system FROM mtms.financial_categories WHERE category_code = 'TAX_PAYMENT'")) {
                var rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("flow_direction")).isEqualTo("OUTFLOW");
                assertThat(rs.getBoolean("is_system")).isTrue();
            }

            // 2. Setup entity, branch, user, account
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, 'ENT-V018', 'Entity V018', 'ACTIVE')")) {
                stmt.setObject(1, entityId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, 'BR-V018', 'Branch V018', 'ACTIVE')")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, 'user_v018', 'hash', 'User V018', 'ACTIVE')")) {
                stmt.setObject(1, userId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, account_name, account_kind, current_balance, status) VALUES (?, ?, 'Tài khoản thuế V018', 'BANK', 50000000.00, 'ACTIVE')")) {
                stmt.setObject(1, accountId);
                stmt.setObject(2, branchId);
                stmt.executeUpdate();
            }

            // 3. Insert valid tax obligation
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.entity_tax_obligations (" +
                    "id, business_entity_id, branch_id, obligation_code, obligation_type, tax_authority_name, " +
                    "tax_period_start, tax_period_end, due_date, amount_due, amount_paid, status, note, entered_by) " +
                    "VALUES (?, ?, ?, 'TAX-2026-Q1-VAT', 'VAT', 'Chi cục Thuế Q1', '2026-01-01', '2026-03-31', '2026-04-30', 12500000.00, 0.00, 'OPEN', 'Thuế GTGT Q1', ?)")) {
                stmt.setObject(1, obligationId);
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 4. Reject duplicate (business_entity_id, obligation_code)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.entity_tax_obligations (" +
                    "id, business_entity_id, branch_id, obligation_code, obligation_type, " +
                    "tax_period_start, tax_period_end, amount_due, amount_paid, status, entered_by) " +
                    "VALUES (?, ?, ?, 'TAX-2026-Q1-VAT', 'VAT', '2026-01-01', '2026-03-31', 5000000.00, 0.00, 'OPEN', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject invalid period (tax_period_end < tax_period_start)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.entity_tax_obligations (" +
                    "id, business_entity_id, branch_id, obligation_code, obligation_type, " +
                    "tax_period_start, tax_period_end, amount_due, amount_paid, status, entered_by) " +
                    "VALUES (?, ?, ?, 'TAX-INVALID-PERIOD', 'VAT', '2026-04-01', '2026-01-01', 5000000.00, 0.00, 'OPEN', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Reject invalid amount_paid > amount_due
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.entity_tax_obligations (" +
                    "id, business_entity_id, branch_id, obligation_code, obligation_type, " +
                    "tax_period_start, tax_period_end, amount_due, amount_paid, status, entered_by) " +
                    "VALUES (?, ?, ?, 'TAX-OVERPAID', 'VAT', '2026-01-01', '2026-03-31', 5000000.00, 6000000.00, 'OPEN', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 7. Reject invalid obligation_type
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.entity_tax_obligations (" +
                    "id, business_entity_id, branch_id, obligation_code, obligation_type, " +
                    "tax_period_start, tax_period_end, amount_due, amount_paid, status, entered_by) " +
                    "VALUES (?, ?, ?, 'TAX-INVALID-TYPE', 'ILLEGAL_TAX', '2026-01-01', '2026-03-31', 5000000.00, 0.00, 'OPEN', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, entityId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Insert valid tax payment
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.tax_payments (" +
                    "id, obligation_id, branch_id, payment_date, amount, payment_method, financial_account_id, receipt_number, created_by) " +
                    "VALUES (?, ?, ?, '2026-04-15', 5000000.00, 'BANK_TRANSFER', ?, 'UNC-001', ?)")) {
                stmt.setObject(1, paymentId);
                stmt.setObject(2, obligationId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, accountId);
                stmt.setObject(5, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 9. Reject tax payment with amount <= 0
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.tax_payments (" +
                    "id, obligation_id, branch_id, payment_date, amount, payment_method, financial_account_id, created_by) " +
                    "VALUES (?, ?, ?, '2026-04-15', 0.00, 'BANK_TRANSFER', ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, obligationId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, accountId);
                stmt.setObject(5, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 10. Reject tax payment with invalid payment method
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.tax_payments (" +
                    "id, obligation_id, branch_id, payment_date, amount, payment_method, financial_account_id, created_by) " +
                    "VALUES (?, ?, ?, '2026-04-15', 100000.00, 'CRYPTO', ?, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, obligationId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, accountId);
                stmt.setObject(5, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(22)
    @DisplayName("P0-FIN-08 / V019 Constraint verification: Opening balance batches and account balances integrity")
    void testOpeningBalanceBatchesConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            // 1. Verify OPENING_BALANCE seeded system category
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT category_code, category_name, category_group, flow_direction, is_system " +
                    "FROM mtms.financial_categories WHERE category_code = 'OPENING_BALANCE'")) {
                try (var rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("category_group")).isEqualTo("CAPITAL_FLOW");
                    assertThat(rs.getString("flow_direction")).isEqualTo("INFLOW");
                    assertThat(rs.getBoolean("is_system")).isTrue();
                }
            }

            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID accountId1 = UUID.randomUUID();
            UUID accountId2 = UUID.randomUUID();

            // Insert base entity, branch, and user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_OB_TEST");
                stmt.setString(3, "Opening Balance Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_OB_01");
                stmt.setString(4, "Opening Balance Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "ob_user@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "Opening Balance User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // Insert two financial accounts
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, account_name, account_kind, current_balance, status) " +
                    "VALUES (?, ?, ?, ?, 0.00, 'ACTIVE')")) {
                stmt.setObject(1, accountId1);
                stmt.setObject(2, branchId);
                stmt.setString(3, "Quầy tiền mặt");
                stmt.setString(4, "CASH");
                stmt.executeUpdate();

                stmt.setObject(1, accountId2);
                stmt.setObject(2, branchId);
                stmt.setString(3, "Tài khoản VCB");
                stmt.setString(4, "BANK");
                stmt.executeUpdate();
            }

            // 2. Insert valid opening balance batch
            UUID batchId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_balance_batches (" +
                    "id, branch_id, cutover_date, status, total_cash_amount, total_bank_amount, total_inventory_valuation, " +
                    "total_payable_amount, total_receivable_amount, total_tax_liability_amount, net_working_capital, " +
                    "summary_snapshot, note, created_by) " +
                    "VALUES (?, ?, '2026-01-01', 'DRAFT', 1000000.00, 5000000.00, 2000000.00, 500000.00, 100000.00, 0.00, 7600000.00, " +
                    "'{\"test\":true}'::jsonb, 'Opening test batch', ?)")) {
                stmt.setObject(1, batchId);
                stmt.setObject(2, branchId);
                stmt.setObject(3, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 3. Reject duplicate opening batch for same branch (uq_opening_batches_branch)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_balance_batches (" +
                    "id, branch_id, cutover_date, status, created_by) " +
                    "VALUES (?, ?, '2026-01-01', 'DRAFT', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                stmt.setObject(3, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject invalid status (ck_opening_batches_status)
            UUID otherBranch = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, otherBranch);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_OB_02");
                stmt.setString(4, "Opening Balance Branch 2");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_balance_batches (" +
                    "id, branch_id, cutover_date, status, created_by) " +
                    "VALUES (?, ?, '2026-01-01', 'INVALID_STATUS', ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, otherBranch);
                stmt.setObject(3, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject negative amount (ck_opening_batches_cash)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_balance_batches (" +
                    "id, branch_id, cutover_date, status, total_cash_amount, created_by) " +
                    "VALUES (?, ?, '2026-01-01', 'DRAFT', -100.00, ?)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, otherBranch);
                stmt.setObject(3, userId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Insert valid opening account balance
            UUID accountBalanceId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_account_balances (id, batch_id, account_id, opening_balance) " +
                    "VALUES (?, ?, ?, 1000000.00)")) {
                stmt.setObject(1, accountBalanceId);
                stmt.setObject(2, batchId);
                stmt.setObject(3, accountId1);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 7. Reject duplicate account for same batch (uq_opening_account_batch)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_account_balances (id, batch_id, account_id, opening_balance) " +
                    "VALUES (?, ?, ?, 2000000.00)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, batchId);
                stmt.setObject(3, accountId1);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Reject negative opening balance (ck_opening_account_balance)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.opening_account_balances (id, batch_id, account_id, opening_balance) " +
                    "VALUES (?, ?, ?, -500.00)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, batchId);
                stmt.setObject(3, accountId2);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(23)
    @DisplayName("P0-HR-01 & P0-HR-02 / V020 Constraint verification: Employees, employee assignments, and salary policies integrity")
    void testEmployeesAssignmentsAndSalaryPoliciesConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();

            // 1. Setup prerequisite entity, branch, and user
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_HR_TEST");
                stmt.setString(3, "HR Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_HR_01");
                stmt.setString(4, "HR Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "hr_user@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "HR Testing User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            // 2. Insert valid employee
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employees (id, user_id, staff_code, full_name, position_name, phone_number, email, hired_on, status, notes) " +
                    "VALUES (?, ?, 'EMP-TEST-001', 'Nguyễn Văn Test', 'Barista', '0901234567', 'test@mtms.vn', '2026-01-01', 'ACTIVE', 'Test employee')")) {
                stmt.setObject(1, employeeId);
                stmt.setObject(2, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 3. Reject duplicate staff_code
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employees (id, staff_code, full_name, position_name, hired_on) " +
                    "VALUES (?, 'EMP-TEST-001', 'Duplicate Staff Code', 'Barista', '2026-01-01')")) {
                stmt.setObject(1, UUID.randomUUID());
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject left_on < hired_on
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employees (id, staff_code, full_name, position_name, hired_on, left_on) " +
                    "VALUES (?, 'EMP-TEST-002', 'Invalid Dates', 'Barista', '2026-05-01', '2026-04-30')")) {
                stmt.setObject(1, UUID.randomUUID());
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject invalid status
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employees (id, staff_code, full_name, position_name, hired_on, status) " +
                    "VALUES (?, 'EMP-TEST-003', 'Invalid Status', 'Barista', '2026-05-01', 'UNKNOWN_STATUS')")) {
                stmt.setObject(1, UUID.randomUUID());
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Insert valid employee assignment
            UUID assignmentId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employee_assignments (id, employee_id, branch_id, is_primary, effective_from) " +
                    "VALUES (?, ?, ?, TRUE, '2026-01-01')")) {
                stmt.setObject(1, assignmentId);
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 7. Reject assignment with effective_to < effective_from
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employee_assignments (id, employee_id, branch_id, is_primary, effective_from, effective_to) " +
                    "VALUES (?, ?, ?, FALSE, '2026-02-01', '2026-01-15')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Insert valid salary policy
            UUID policyId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.salary_policies (id, employee_id, branch_id, rate_type, rate_amount, standard_workdays, paid_minutes_rounding_block, effective_from, approved_by) " +
                    "VALUES (?, ?, ?, 'HOURLY', 25000.00, 26.00, 15, '2026-01-01', ?)")) {
                stmt.setObject(1, policyId);
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 9. Reject negative rate_amount
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.salary_policies (id, employee_id, branch_id, rate_type, rate_amount, effective_from) " +
                    "VALUES (?, ?, ?, 'HOURLY', -1000.00, '2026-01-01')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 10. Reject standard_workdays <= 0
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.salary_policies (id, employee_id, branch_id, rate_type, rate_amount, standard_workdays, effective_from) " +
                    "VALUES (?, ?, ?, 'MONTHLY', 10000000.00, 0.00, '2026-01-01')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 11. Reject invalid paid_minutes_rounding_block (not in 1, 5, 10, 15, 30, 60)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.salary_policies (id, employee_id, branch_id, rate_type, rate_amount, paid_minutes_rounding_block, effective_from) " +
                    "VALUES (?, ?, ?, 'HOURLY', 20000.00, 25, '2026-01-01')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 12. Reject salary policy with effective_to < effective_from
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.salary_policies (id, employee_id, branch_id, rate_type, rate_amount, effective_from, effective_to) " +
                    "VALUES (?, ?, ?, 'HOURLY', 22000.00, '2026-06-01', '2026-05-31')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(25)
    @DisplayName("P0-HR-03 & P0-HR-04 / V021 Constraint verification: Timesheet entries integrity")
    void testTimesheetEntriesConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();

            // 1. Setup prerequisite entity, branch, user, and employee
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_TS_TEST");
                stmt.setString(3, "Timesheet Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_TS_01");
                stmt.setString(4, "Timesheet Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "ts_user@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "Timesheet Testing User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employees (id, user_id, staff_code, full_name, position_name, hired_on, status) " +
                    "VALUES (?, ?, 'EMP-TS-001', 'Timesheet Staff', 'Barista', '2026-01-01', 'ACTIVE')")) {
                stmt.setObject(1, employeeId);
                stmt.setObject(2, userId);
                stmt.executeUpdate();
            }

            // 2. Insert valid timesheet entry
            UUID entryId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.timesheet_entries (id, employee_id, branch_id, work_date, raw_minutes, approved_minutes, payable_workdays, source_type, status, note, approved_by, approved_at, approval_note) " +
                    "VALUES (?, ?, ?, '2026-11-19', 480, 480, 1.00, 'MANUAL', 'APPROVED', 'Morning shift', ?, now(), 'Approved by manager')")) {
                stmt.setObject(1, entryId);
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 3. Reject duplicate (employee_id, branch_id, work_date)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.timesheet_entries (id, employee_id, branch_id, work_date, approved_minutes, payable_workdays) " +
                    "VALUES (?, ?, ?, '2026-11-19', 480, 1.00)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject negative raw_minutes (< 0)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.timesheet_entries (id, employee_id, branch_id, work_date, raw_minutes) " +
                    "VALUES (?, ?, ?, '2026-11-20', -10)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject negative approved_minutes (< 0)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.timesheet_entries (id, employee_id, branch_id, work_date, approved_minutes) " +
                    "VALUES (?, ?, ?, '2026-11-20', -60)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Reject negative payable_workdays (< 0)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.timesheet_entries (id, employee_id, branch_id, work_date, payable_workdays) " +
                    "VALUES (?, ?, ?, '2026-11-20', -0.50)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 7. Reject invalid status
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.timesheet_entries (id, employee_id, branch_id, work_date, status) " +
                    "VALUES (?, ?, ?, '2026-11-20', 'INVALID_STATUS')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Reject invalid source_type
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.timesheet_entries (id, employee_id, branch_id, work_date, source_type) " +
                    "VALUES (?, ?, ?, '2026-11-20', 'INVALID_SOURCE')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, employeeId);
                stmt.setObject(3, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    @Order(26)
    @DisplayName("P0-HR-05 & P0-HR-06 / V022 Constraint verification: Payroll periods, lines, and adjustments integrity")
    void testPayrollPeriodsLinesAndAdjustmentsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();

            // 1. Setup prerequisite entity, branch, user, and employee
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, entity_code, registered_name, status) VALUES (?, ?, ?, ?)")) {
                stmt.setObject(1, entityId);
                stmt.setString(2, "ENT_PAY_TEST");
                stmt.setString(3, "Payroll Testing Entity");
                stmt.setString(4, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, branch_code, name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.setString(3, "BR_PAY_01");
                stmt.setString(4, "Payroll Branch 1");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, login_identifier, password_hash, display_name, status) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setObject(1, userId);
                stmt.setString(2, "pay_owner@mtms.local");
                stmt.setString(3, "$2a$12$dummyhash");
                stmt.setString(4, "Payroll Owner User");
                stmt.setString(5, "ACTIVE");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employees (id, user_id, staff_code, full_name, position_name, hired_on, status) " +
                    "VALUES (?, ?, 'EMP-PAY-001', 'Payroll Staff', 'Barista', '2026-01-01', 'ACTIVE')")) {
                stmt.setObject(1, employeeId);
                stmt.setObject(2, userId);
                stmt.executeUpdate();
            }

            // 2. Reject month_start not starting on day 1 (e.g. 2026-11-15)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_periods (id, branch_id, month_start, revision, is_current, status) " +
                    "VALUES (?, ?, '2026-11-15', 1, true, 'DRAFT')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 3. Insert valid payroll period
            UUID periodId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_periods (id, branch_id, month_start, revision, is_current, status, total_gross_salary, total_base_salary, total_adjustments, employee_count) " +
                    "VALUES (?, ?, '2026-11-01', 1, true, 'DRAFT', 2750000.00, 2350000.00, 400000.00, 1)")) {
                stmt.setObject(1, periodId);
                stmt.setObject(2, branchId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 4. Reject duplicate unique (branch_id, month_start, revision)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_periods (id, branch_id, month_start, revision, is_current, status) " +
                    "VALUES (?, ?, '2026-11-01', 1, false, 'DRAFT')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject duplicate current period on partial index (uq_payroll_period_current)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_periods (id, branch_id, month_start, revision, is_current, status) " +
                    "VALUES (?, ?, '2026-11-01', 2, true, 'DRAFT')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Insert valid payroll line
            UUID lineId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_lines (id, payroll_period_id, employee_id, expense_branch_id, rate_type, approved_minutes, payable_workdays, base_salary, adjustments_total, gross_salary, paid_amount) " +
                    "VALUES (?, ?, ?, ?, 'HOURLY', 6000, 0.00, 2350000.00, 400000.00, 2750000.00, 0.00)")) {
                stmt.setObject(1, lineId);
                stmt.setObject(2, periodId);
                stmt.setObject(3, employeeId);
                stmt.setObject(4, branchId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 7. Reject duplicate employee in same period
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_lines (id, payroll_period_id, employee_id, expense_branch_id, rate_type, approved_minutes, payable_workdays, base_salary, adjustments_total, gross_salary) " +
                    "VALUES (?, ?, ?, ?, 'HOURLY', 3000, 0.00, 1000000.00, 0.00, 1000000.00)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, periodId);
                stmt.setObject(3, employeeId);
                stmt.setObject(4, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 8. Reject paid_amount > gross_salary
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_lines (id, payroll_period_id, employee_id, expense_branch_id, rate_type, approved_minutes, payable_workdays, base_salary, adjustments_total, gross_salary, paid_amount) " +
                    "VALUES (?, ?, ?, ?, 'HOURLY', 6000, 0.00, 2000000.00, 0.00, 2000000.00, 2500000.00)")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, periodId);
                stmt.setObject(3, UUID.randomUUID());
                stmt.setObject(4, branchId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 9. Insert valid adjustment
            UUID adjId = UUID.randomUUID();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_adjustments (id, payroll_line_id, adjustment_code, signed_amount, explanation) " +
                    "VALUES (?, ?, 'BONUS', 500000.00, 'Morning shift bonus')")) {
                stmt.setObject(1, adjId);
                stmt.setObject(2, lineId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 10. Reject signed_amount = 0 in adjustments
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_adjustments (id, payroll_line_id, adjustment_code, signed_amount, explanation) " +
                    "VALUES (?, ?, 'ZERO', 0.00, 'Zero adjustment invalid')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, lineId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 11. Verify CASCADE delete: deleting payroll_period removes lines and adjustments
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mtms.payroll_periods WHERE id = ?")) {
                stmt.setObject(1, periodId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // Verify line and adjustment are cascaded
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM mtms.payroll_lines WHERE id = ?")) {
                stmt.setObject(1, lineId);
                var rs = stmt.executeQuery();
                rs.next();
                assertThat(rs.getLong(1)).isZero();
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM mtms.payroll_adjustments WHERE id = ?")) {
                stmt.setObject(1, adjId);
                var rs = stmt.executeQuery();
                rs.next();
                assertThat(rs.getLong(1)).isZero();
            }
        }
    }

    @Test
    @Order(25)
    @DisplayName("V023: mtms.payroll_payments table constraints, foreign keys, and CASCADE delete")
    void testV023PayrollPaymentsConstraints() throws SQLException {
        try (Connection conn = createConnection()) {
            UUID entityId = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID employeeId = UUID.randomUUID();
            UUID periodId = UUID.randomUUID();
            UUID lineId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();

            // 1. Setup prerequisite entities
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.business_entities (id, code, name, tax_code) VALUES (?, 'ENT_V23', 'Entity V23', '0100000023')")) {
                stmt.setObject(1, entityId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.branches (id, business_entity_id, code, name) VALUES (?, ?, 'BR_V23', 'Branch V23')")) {
                stmt.setObject(1, branchId);
                stmt.setObject(2, entityId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.users (id, username, password_hash, full_name) VALUES (?, 'user_v23', 'hash', 'User V23')")) {
                stmt.setObject(1, userId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.financial_accounts (id, branch_id, account_name, account_kind, current_balance) " +
                    "VALUES (?, ?, 'Cash V23', 'CASH', 10000000.00)")) {
                stmt.setObject(1, accountId);
                stmt.setObject(2, branchId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.employees (id, staff_code, full_name, position_name, hired_on) " +
                    "VALUES (?, 'EMP_V23', 'Staff V23', 'Barista', '2026-01-01')")) {
                stmt.setObject(1, employeeId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_periods (id, branch_id, month_start, revision, is_current, status, total_gross_salary) " +
                    "VALUES (?, ?, '2026-11-01', 1, true, 'APPROVED', 5000000.00)")) {
                stmt.setObject(1, periodId);
                stmt.setObject(2, branchId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_lines (id, payroll_period_id, employee_id, expense_branch_id, rate_type, gross_salary, paid_amount) " +
                    "VALUES (?, ?, ?, ?, 'HOURLY', 5000000.00, 2000000.00)")) {
                stmt.setObject(1, lineId);
                stmt.setObject(2, periodId);
                stmt.setObject(3, employeeId);
                stmt.setObject(4, branchId);
                stmt.executeUpdate();
            }

            // 2. Insert valid payroll payment
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_payments (id, payroll_line_id, account_id, amount, paid_on, payment_method, status, external_reference, note, confirmed_by) " +
                    "VALUES (?, ?, ?, 2000000.00, '2026-12-05', 'BANK_TRANSFER', 'POSTED', 'REF-001', 'Salary payment 1', ?)")) {
                stmt.setObject(1, paymentId);
                stmt.setObject(2, lineId);
                stmt.setObject(3, accountId);
                stmt.setObject(4, userId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            // 3. Reject amount <= 0
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_payments (id, payroll_line_id, account_id, amount, paid_on) " +
                    "VALUES (?, ?, ?, 0.00, '2026-12-05')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, lineId);
                stmt.setObject(3, accountId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 4. Reject invalid payment_method
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_payments (id, payroll_line_id, account_id, amount, paid_on, payment_method) " +
                    "VALUES (?, ?, ?, 100000.00, '2026-12-05', 'CRYPTO')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, lineId);
                stmt.setObject(3, accountId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 5. Reject invalid status
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mtms.payroll_payments (id, payroll_line_id, account_id, amount, paid_on, status) " +
                    "VALUES (?, ?, ?, 100000.00, '2026-12-05', 'CANCELLED')")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, lineId);
                stmt.setObject(3, accountId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            }

            // 6. Verify CASCADE delete: deleting payroll_lines removes payroll_payments
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mtms.payroll_lines WHERE id = ?")) {
                stmt.setObject(1, lineId);
                int rows = stmt.executeUpdate();
                assertThat(rows).isEqualTo(1);
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM mtms.payroll_payments WHERE id = ?")) {
                stmt.setObject(1, paymentId);
                var rs = stmt.executeQuery();
                rs.next();
                assertThat(rs.getLong(1)).isZero();
            }
        }
    }
}
