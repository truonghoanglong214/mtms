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
    @DisplayName("Clean bootstrap: Flyway applies migrations (V001, V002, V003, V004, V005, V006) in mtms schema")
    void testCleanBootstrap() {
        Flyway flyway = createFlyway();
        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(6);
        assertThat(result.targetSchemaVersion).isIn("6", "006");
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
}
