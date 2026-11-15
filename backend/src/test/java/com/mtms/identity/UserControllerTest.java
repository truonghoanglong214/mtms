package com.mtms.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.application.dto.CreateUserCommand;
import com.mtms.identity.application.dto.UpdateUserStatusCommand;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-ORG-06 / BR-ORG-01 / TC-001
 * Integration test for administrative User Management API.
 * Verifies that only SYSTEM_ADMIN can list, create, and modify user statuses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UserJpaEntity adminUser;
    private UserJpaEntity regularUser;
    private String adminToken;
    private String regularToken;

    @BeforeEach
    void setUp() {
        grantRepository.deleteAll();
        userRepository.deleteAll();
        businessEntityRepository.deleteAll();

        // 1. Create Business Entity
        UUID entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId,
                "LTT_VN",
                "Cong ty Luc Tra Tinh Hoa",
                "0109999999",
                BusinessEntityStatus.ACTIVE,
                Instant.now()
        ));

        // 2. Create Users
        adminUser = userRepository.save(new UserJpaEntity(
                UUID.randomUUID(),
                "admin_super",
                passwordEncoder.encode("AdminPass123!"),
                "Super Administrator",
                UserStatus.ACTIVE,
                Instant.now()
        ));

        regularUser = userRepository.save(new UserJpaEntity(
                UUID.randomUUID(),
                "staff_member",
                passwordEncoder.encode("StaffPass123!"),
                "Normal Staff Member",
                UserStatus.ACTIVE,
                Instant.now()
        ));

        // 3. Grant SYSTEM_ADMIN to adminUser
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                null,
                adminUser.getId(),
                "SYSTEM_ADMIN",
                "*",
                "ALL",
                "ALL",
                LocalDate.now().minusDays(1),
                null,
                adminUser.getId(),
                Instant.now()
        ));

        adminToken = jwtTokenService.generateAccessToken(adminUser.getId(), adminUser.getLoginIdentifier());
        regularToken = jwtTokenService.generateAccessToken(regularUser.getId(), regularUser.getLoginIdentifier());
    }

    @Test
    @DisplayName("Unauthenticated request to GET /api/v1/users returns 401 Unauthorized")
    void testUnauthenticatedGetUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:unauthorized"));
    }

    @Test
    @DisplayName("TC-001 (Security Isolation): Non-admin user accessing GET /api/v1/users returns 403 Forbidden")
    void testNonAdminCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + regularToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }

    @Test
    @DisplayName("TC-001 (Security Isolation): Non-admin user creating a user returns 403 Forbidden")
    void testNonAdminCannotCreateUser() throws Exception {
        CreateUserCommand command = new CreateUserCommand(
                "new_employee",
                "Password123!",
                "New Employee"
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + regularToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }

    @Test
    @DisplayName("SYSTEM_ADMIN successfully retrieves all users")
    void testSystemAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("SYSTEM_ADMIN creates a new user successfully (201 Created)")
    void testSystemAdminCreatesUser() throws Exception {
        CreateUserCommand command = new CreateUserCommand(
                "branch_manager_01",
                "StrongPassword123!",
                "Quan Ly Chi Nhanh 1"
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginIdentifier").value("branch_manager_01"))
                .andExpect(jsonPath("$.data.displayName").value("Quan Ly Chi Nhanh 1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Creating user with duplicate login identifier returns 409 Conflict")
    void testDuplicateLoginIdentifierReturnsConflict() throws Exception {
        CreateUserCommand command = new CreateUserCommand(
                "staff_member", // duplicate with regularUser
                "StrongPassword123!",
                "Another Staff Member"
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:resource-conflict"));
    }

    @Test
    @DisplayName("Creating user with invalid format returns 400 Bad Request")
    void testInvalidUserCommandReturnsBadRequest() throws Exception {
        CreateUserCommand command = new CreateUserCommand(
                "ab", // Too short (min 3)
                "short", // Too short (min 8)
                "" // Blank display name
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:validation-error"))
                .andExpect(jsonPath("$.fieldErrors.loginIdentifier").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.displayName").exists());
    }

    @Test
    @DisplayName("SYSTEM_ADMIN updates user status to SUSPENDED successfully")
    void testUpdateUserStatus() throws Exception {
        UpdateUserStatusCommand command = new UpdateUserStatusCommand(UserStatus.SUSPENDED);

        mockMvc.perform(patch("/api/v1/users/{id}/status", regularUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }
}
