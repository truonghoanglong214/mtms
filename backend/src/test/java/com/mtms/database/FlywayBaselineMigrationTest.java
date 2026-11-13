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
    @DisplayName("Clean bootstrap: Flyway applies exactly one migration (V001) in mtms schema")
    void testCleanBootstrap() {
        Flyway flyway = createFlyway();
        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(1);
        assertThat(result.targetSchemaVersion).isIn("1", "001");
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
}
