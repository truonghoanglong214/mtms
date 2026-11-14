package com.mtms.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.application.dto.GrantRoleCommand;
import com.mtms.identity.domain.RoleKey;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-ORG-05 / BR-ORG-04 / TC-001
 * Negative and Positive Isolation Integration Test for branch-scoped permissions.
 * Verifies that a user granted access to Branch A is strictly forbidden (HTTP 403) from Branch B,
 * with RFC 7807 Problem Detail and zero data leak.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BranchAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchAId;
    private UUID branchBId;

    private UserJpaEntity userBranchA;
    private UserJpaEntity userBranchB;
    private UserJpaEntity userExpired;
    private UserJpaEntity userSystemAdmin;

    @BeforeEach
    void setUp() {
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Business Entity
        entityId = UUID.randomUUID();
        BusinessEntityJpaEntity entity = new BusinessEntityJpaEntity(
                entityId,
                "LTT_VN",
                "Cong ty CP Luc Tra Tinh Hoa",
                "0109999999",
                BusinessEntityStatus.ACTIVE,
                Instant.now()
        );
        businessEntityRepository.save(entity);

        // 2. Create Branch A and Branch B
        branchAId = UUID.randomUUID();
        BranchJpaEntity branchA = new BranchJpaEntity(
                branchAId,
                entityId,
                "LTT_HCM_01",
                "Luc Tra Tinh Hoa - Quan 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );
        branchRepository.save(branchA);

        branchBId = UUID.randomUUID();
        BranchJpaEntity branchB = new BranchJpaEntity(
                branchBId,
                entityId,
                "LTT_HCM_02",
                "Luc Tra Tinh Hoa - Binh Thanh",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );
        branchRepository.save(branchB);

        // 3. Create Users
        userBranchA = createUser("owner_a", "Branch A Owner");
        userBranchB = createUser("owner_b", "Branch B Owner");
        userExpired = createUser("owner_expired", "Expired Owner");
        userSystemAdmin = createUser("sys_admin", "Global System Admin");

        LocalDate today = LocalDate.now();

        // 4. Create Role Grants
        // User A: Granted BRANCH_OWNER on Branch A (valid)
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchAId,
                userBranchA.getId(),
                "BRANCH_OWNER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // User B: Granted BRANCH_OWNER on Branch B (valid)
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchBId,
                userBranchB.getId(),
                "BRANCH_OWNER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // User Expired: Granted BRANCH_OWNER on Branch A, but expired yesterday
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchAId,
                userExpired.getId(),
                "BRANCH_OWNER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(30),
                today.minusDays(1), // EXPIRED
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // User Admin: Granted SYSTEM_ADMIN unscoped (valid)
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                null,
                userSystemAdmin.getId(),
                "SYSTEM_ADMIN",
                "*",
                "ALL",
                "ALL",
                today.minusDays(10),
                null, // no expiry
                userSystemAdmin.getId(),
                Instant.now()
        ));
    }

    private UserJpaEntity createUser(String username, String displayName) {
        UserJpaEntity user = new UserJpaEntity(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode("StrongP@ss123"),
                displayName,
                UserStatus.ACTIVE,
                Instant.now()
        );
        return userRepository.save(user);
    }

    @Test
    @DisplayName("TC-001 (Unauthenticated): Request to branch endpoint returns 401 Unauthorized")
    void testUnauthenticatedBranchAccess() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/entities/{entityId}/branches/{branchId}", entityId, branchAId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:unauthorized"));
    }

    @Test
    @DisplayName("TC-001 (Positive): User with grant for Branch A successfully accesses Branch A")
    void testBranchOwnerAccessesOwnBranch() throws Exception {
        String token = jwtTokenService.generateAccessToken(userBranchA.getId(), userBranchA.getLoginIdentifier());

        mockMvc.perform(get("/api/v1/organizations/entities/{entityId}/branches/{branchId}", entityId, branchAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchCode").value("LTT_HCM_01"))
                .andExpect(jsonPath("$.data.name").value("Luc Tra Tinh Hoa - Quan 1"));
    }

    @Test
    @DisplayName("TC-001 (Negative Isolation): User with grant for Branch A requesting Branch B returns 403 Forbidden")
    void testBranchOwnerForbiddenFromAccessingBranchB() throws Exception {
        String token = jwtTokenService.generateAccessToken(userBranchA.getId(), userBranchA.getLoginIdentifier());

        mockMvc.perform(get("/api/v1/organizations/entities/{entityId}/branches/{branchId}", entityId, branchBId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access to the requested resource is forbidden"));
    }

    @Test
    @DisplayName("TC-001 (Temporal Expiry): User with expired grant for Branch A returns 403 Forbidden")
    void testExpiredGrantForbiddenFromAccessingBranch() throws Exception {
        String token = jwtTokenService.generateAccessToken(userExpired.getId(), userExpired.getLoginIdentifier());

        mockMvc.perform(get("/api/v1/organizations/entities/{entityId}/branches/{branchId}", entityId, branchAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    @DisplayName("TC-001 (Admin Bypass): SYSTEM_ADMIN can access any branch across all entities")
    void testSystemAdminCanAccessAnyBranch() throws Exception {
        String token = jwtTokenService.generateAccessToken(userSystemAdmin.getId(), userSystemAdmin.getLoginIdentifier());

        mockMvc.perform(get("/api/v1/organizations/entities/{entityId}/branches/{branchId}", entityId, branchBId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchCode").value("LTT_HCM_02"));
    }

    @Test
    @DisplayName("Non-admin user cannot grant roles via POST /api/v1/auth/grants")
    void testNonAdminCannotGrantRoles() throws Exception {
        String token = jwtTokenService.generateAccessToken(userBranchA.getId(), userBranchA.getLoginIdentifier());

        GrantRoleCommand command = new GrantRoleCommand(
                entityId,
                branchBId,
                userBranchA.getId(),
                RoleKey.MANAGER,
                "ALL",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(3)
        );

        mockMvc.perform(post("/api/v1/auth/grants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }

    @Test
    @DisplayName("SYSTEM_ADMIN can grant and revoke roles successfully")
    void testSystemAdminGrantAndRevokeWorkflow() throws Exception {
        String token = jwtTokenService.generateAccessToken(userSystemAdmin.getId(), userSystemAdmin.getLoginIdentifier());

        GrantRoleCommand command = new GrantRoleCommand(
                entityId,
                branchBId,
                userBranchA.getId(),
                RoleKey.FINANCE,
                "READ",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(3)
        );

        // 1. Grant role
        String responseContent = mockMvc.perform(post("/api/v1/auth/grants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleKey").value("FINANCE"))
                .andReturn().getResponse().getContentAsString();

        String grantIdStr = objectMapper.readTree(responseContent).path("data").path("id").asText();
        UUID grantId = UUID.fromString(grantIdStr);

        // 2. Fetch user grants
        mockMvc.perform(get("/api/v1/auth/grants/user/{userId}", userBranchA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2)); // original Branch A grant + new FINANCE grant

        // 3. Revoke grant
        mockMvc.perform(delete("/api/v1/auth/grants/{grantId}", grantId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
