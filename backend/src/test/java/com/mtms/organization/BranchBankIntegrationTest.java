package com.mtms.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.organization.application.dto.CreateBankAccountCommand;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchBankAccountJpaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-ORG-07 / BR-ORG-03 / TC-003
 * Integration tests for branch bank account onboarding, operational readiness gate,
 * and branch-scoped authorization controls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BranchBankIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private BranchBankAccountJpaRepository bankAccountRepository;

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

    private UserJpaEntity userOwnerA;
    private UserJpaEntity userOwnerB;
    private UserJpaEntity userManagerA;
    private UserJpaEntity userSystemAdmin;

    private String tokenOwnerA;
    private String tokenOwnerB;
    private String tokenManagerA;
    private String tokenSystemAdmin;

    @BeforeEach
    void setUp() {
        bankAccountRepository.deleteAll();
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

        // 2. Create Branch A in DRAFT status, Branch B in ACTIVE status
        branchAId = UUID.randomUUID();
        BranchJpaEntity branchA = new BranchJpaEntity(
                branchAId,
                entityId,
                "LTT_HCM_01",
                "Luc Tra Tinh Hoa - Quan 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.DRAFT,
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
        userOwnerA = createUser("owner_a", "Branch A Owner");
        userOwnerB = createUser("owner_b", "Branch B Owner");
        userManagerA = createUser("manager_a", "Branch A Manager");
        userSystemAdmin = createUser("sys_admin", "Global System Admin");

        LocalDate today = LocalDate.now();

        // 4. Role Grants
        // Owner A -> BRANCH_OWNER on Branch A
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchAId,
                userOwnerA.getId(),
                "BRANCH_OWNER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // Owner B -> BRANCH_OWNER on Branch B
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchBId,
                userOwnerB.getId(),
                "BRANCH_OWNER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // Manager A -> MANAGER on Branch A
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchAId,
                userManagerA.getId(),
                "MANAGER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // System Admin -> unscoped SYSTEM_ADMIN
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
                null,
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // Generate tokens
        tokenOwnerA = jwtTokenService.generateAccessToken(userOwnerA.getId(), userOwnerA.getLoginIdentifier());
        tokenOwnerB = jwtTokenService.generateAccessToken(userOwnerB.getId(), userOwnerB.getLoginIdentifier());
        tokenManagerA = jwtTokenService.generateAccessToken(userManagerA.getId(), userManagerA.getLoginIdentifier());
        tokenSystemAdmin = jwtTokenService.generateAccessToken(userSystemAdmin.getId(), userSystemAdmin.getLoginIdentifier());
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
    @DisplayName("TC-003: Branch without bank account fails readiness and activation is blocked (HTTP 422)")
    void testReadinessAndActivationBlockedWithoutBankAccount() throws Exception {
        // 1. Check readiness -> readyForActivation must be false
        mockMvc.perform(get("/api/v1/branches/{branchId}/readiness", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readyForActivation").value(false))
                .andExpect(jsonPath("$.data.missingRequirements.length()").value(1));

        // 2. Attempt activation -> must be blocked with HTTP 422 Unprocessable Entity
        mockMvc.perform(patch("/api/v1/branches/{branchId}/activate", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:branch-not-ready"))
                .andExpect(jsonPath("$.title").value("Branch Not Ready For Activation"))
                .andExpect(jsonPath("$.missingRequirements.length()").value(1));

        // 3. Confirm branch status remains DRAFT in database
        BranchJpaEntity branch = branchRepository.findById(branchAId).orElseThrow();
        assertThat(branch.getStatus()).isEqualTo(BranchStatus.DRAFT);
    }

    @Test
    @DisplayName("TC-003: Adding bank account fulfills readiness and enables branch activation")
    void testOnboardingBankAccountAndActivationWorkflow() throws Exception {
        // 1. Add bank account to Branch A
        CreateBankAccountCommand cmd = new CreateBankAccountCommand(
                "Vietcombank",
                "CTY CP LUC TRA TINH HOA - CN Q1",
                "0071001234567",
                true
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bankName").value("Vietcombank"))
                .andExpect(jsonPath("$.data.accountHolderName").value("CTY CP LUC TRA TINH HOA - CN Q1"))
                .andExpect(jsonPath("$.data.maskedAccountNumber").value("**** **** 4567"))
                .andExpect(jsonPath("$.data.isPrimary").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        // 2. Check readiness -> must be true
        mockMvc.perform(get("/api/v1/branches/{branchId}/readiness", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readyForActivation").value(true))
                .andExpect(jsonPath("$.data.missingRequirements").isEmpty());

        // 3. Activate branch -> must succeed (HTTP 200)
        mockMvc.perform(patch("/api/v1/branches/{branchId}/activate", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 4. Verify branch status in database is now ACTIVE
        BranchJpaEntity branch = branchRepository.findById(branchAId).orElseThrow();
        assertThat(branch.getStatus()).isEqualTo(BranchStatus.ACTIVE);
    }

    @Test
    @DisplayName("Primary toggle: Switching primary bank account updates flags correctly")
    void testPrimaryAccountToggle() throws Exception {
        // 1. Add first account (becomes primary)
        CreateBankAccountCommand cmd1 = new CreateBankAccountCommand(
                "Vietcombank",
                "CTY CP LUC TRA TINH HOA",
                "1111222233334444",
                true
        );
        String res1 = mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String acc1Id = objectMapper.readTree(res1).path("data").path("id").asText();

        // 2. Add second account with isPrimary = false
        CreateBankAccountCommand cmd2 = new CreateBankAccountCommand(
                "Techcombank",
                "CTY CP LUC TRA TINH HOA",
                "5555666677778888",
                false
        );
        String res2 = mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPrimary").value(false))
                .andReturn().getResponse().getContentAsString();

        String acc2Id = objectMapper.readTree(res2).path("data").path("id").asText();

        // 3. Set second account as primary
        mockMvc.perform(patch("/api/v1/branches/{branchId}/bank-accounts/{accountId}/primary", branchAId, acc2Id)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPrimary").value(true));

        // 4. List accounts and verify account 1 is no longer primary and account 2 is primary
        mockMvc.perform(get("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        var acc1Entity = bankAccountRepository.findById(UUID.fromString(acc1Id)).orElseThrow();
        var acc2Entity = bankAccountRepository.findById(UUID.fromString(acc2Id)).orElseThrow();
        assertThat(acc1Entity.isPrimary()).isFalse();
        assertThat(acc2Entity.isPrimary()).isTrue();
    }

    @Test
    @DisplayName("Security Isolation: User from Branch B cannot access or modify Branch A bank accounts")
    void testCrossBranchForbidden() throws Exception {
        CreateBankAccountCommand cmd = new CreateBankAccountCommand(
                "ACB",
                "Intruder Name",
                "9876543210",
                true
        );

        // Branch B Owner attempting to add bank account to Branch A -> 403 Forbidden
        mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));

        // Branch B Owner attempting to view Branch A bank accounts -> 403 Forbidden
        mockMvc.perform(get("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));

        // Branch B Owner attempting to activate Branch A -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/branches/{branchId}/activate", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }

    @Test
    @DisplayName("Role RBAC: MANAGER can view readiness and bank accounts, but cannot onboard or activate")
    void testManagerRolePermissions() throws Exception {
        // First onboard an account as Owner
        CreateBankAccountCommand cmd = new CreateBankAccountCommand(
                "BIDV",
                "Luc Tra Tinh Hoa",
                "1234567890123",
                true
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenOwnerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated());

        // Manager CAN read bank accounts (GET)
        mockMvc.perform(get("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // Manager CAN read readiness (GET)
        mockMvc.perform(get("/api/v1/branches/{branchId}/readiness", branchAId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readyForActivation").value(true));

        // Manager CANNOT add bank account (POST) -> 403 Forbidden
        CreateBankAccountCommand mgrCmd = new CreateBankAccountCommand(
                "MBBank",
                "Luc Tra Tinh Hoa",
                "999988887777",
                false
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mgrCmd)))
                .andExpect(status().isForbidden());

        // Manager CANNOT activate branch (PATCH) -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/branches/{branchId}/activate", branchAId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Access: SYSTEM_ADMIN can onboard bank accounts and activate any branch")
    void testSystemAdminFullAccess() throws Exception {
        CreateBankAccountCommand cmd = new CreateBankAccountCommand(
                "VietinBank",
                "Luc Tra Tinh Hoa Central",
                "11223344556677",
                true
        );

        // System Admin can add bank account
        mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", branchAId)
                        .header("Authorization", "Bearer " + tokenSystemAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bankName").value("VietinBank"));

        // System Admin can activate branch
        mockMvc.perform(patch("/api/v1/branches/{branchId}/activate", branchAId)
                        .header("Authorization", "Bearer " + tokenSystemAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/bank-accounts", branchAId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:unauthorized"));
    }
}
