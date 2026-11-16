package com.mtms.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.audit.application.AuditLogService;
import com.mtms.audit.application.dto.AuditEventDto;
import com.mtms.audit.application.dto.RecordAuditEventCommand;
import com.mtms.audit.infrastructure.persistence.AuditEventJpaRepository;
import com.mtms.identity.application.ScopedPermissionService;
import com.mtms.identity.application.UserService;
import com.mtms.identity.application.dto.GrantRoleCommand;
import com.mtms.identity.application.dto.ScopedRoleGrantDto;
import com.mtms.identity.application.dto.UpdateUserStatusCommand;
import com.mtms.identity.domain.RoleKey;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.organization.application.BranchBankService;
import com.mtms.organization.application.dto.BranchBankAccountDto;
import com.mtms.organization.application.dto.CreateBankAccountCommand;
import com.mtms.organization.domain.BranchBankAccountRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-SEC-02 / BR-SEC-03 / TC-041
 * Comprehensive integration tests for immutable audit events, branch isolation security,
 * JSON snapshot accuracy, and sensitive operation audit triggers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditEventJpaRepository auditEventJpaRepository;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private BranchBankAccountRepository bankAccountRepository;

    @Autowired
    private BranchBankService branchBankService;

    @Autowired
    private UserService userService;

    @Autowired
    private ScopedPermissionService scopedPermissionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchAId;
    private UUID branchBId;

    private UserJpaEntity userOwnerA;
    private UserJpaEntity userOwnerB;
    private UserJpaEntity userSystemAdmin;

    private String tokenOwnerA;
    private String tokenOwnerB;
    private String tokenSystemAdmin;

    @BeforeEach
    void setUp() {
        auditEventJpaRepository.deleteAll();
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
        userOwnerA = createUser("owner_a", "Branch A Owner");
        userOwnerB = createUser("owner_b", "Branch B Owner");
        userSystemAdmin = createUser("sys_admin", "Global System Admin");

        LocalDate today = LocalDate.now();

        // 4. Role Grants
        // User A -> BRANCH_OWNER on Branch A
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

        // User B -> BRANCH_OWNER on Branch B
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

        // Admin -> SYSTEM_ADMIN unscoped
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                null,
                userSystemAdmin.getId(),
                "SYSTEM_ADMIN",
                "*",
                "ALL",
                "ALL",
                today.minusDays(1),
                null,
                userSystemAdmin.getId(),
                Instant.now()
        ));

        tokenOwnerA = jwtTokenService.generateAccessToken(userOwnerA.getId(), userOwnerA.getLoginIdentifier());
        tokenOwnerB = jwtTokenService.generateAccessToken(userOwnerB.getId(), userOwnerB.getLoginIdentifier());
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
    @DisplayName("Record and query audit event successfully via service")
    void testRecordAndQueryAuditEventSuccess() {
        UUID targetId = UUID.randomUUID();
        RecordAuditEventCommand cmd = new RecordAuditEventCommand(
                branchAId,
                userOwnerA.getId(),
                UUID.randomUUID(),
                "TEST_TARGET",
                targetId,
                "TEST_CREATED",
                "Initial creation test",
                null,
                "{\"status\":\"CREATED\"}"
        );

        AuditEventDto recorded = auditLogService.recordEvent(cmd);

        assertThat(recorded).isNotNull();
        assertThat(recorded.id()).isNotNull();
        assertThat(recorded.branchId()).isEqualTo(branchAId);
        assertThat(recorded.actorUserId()).isEqualTo(userOwnerA.getId());
        assertThat(recorded.targetType()).isEqualTo("TEST_TARGET");
        assertThat(recorded.targetId()).isEqualTo(targetId);
        assertThat(recorded.eventName()).isEqualTo("TEST_CREATED");
        assertThat(recorded.actionReason()).isEqualTo("Initial creation test");
        assertThat(recorded.newSnapshot()).isEqualTo("{\"status\":\"CREATED\"}");
        assertThat(recorded.occurredAt()).isNotNull();

        // Query by branch
        Page<AuditEventDto> branchEvents = auditLogService.listEventsByBranch(branchAId, PageRequest.of(0, 10));
        assertThat(branchEvents.getContent()).hasSize(1);
        assertThat(branchEvents.getContent().get(0).id()).isEqualTo(recorded.id());

        // Query by target
        List<AuditEventDto> targetEvents = auditLogService.findByTarget("TEST_TARGET", targetId);
        assertThat(targetEvents).hasSize(1);
        assertThat(targetEvents.get(0).id()).isEqualTo(recorded.id());
    }

    @Test
    @DisplayName("Verify JSON snapshot structure and accuracy are preserved")
    void testJsonSnapshotPreservation() {
        UUID targetId = UUID.randomUUID();
        String oldJson = "{\"status\":\"DRAFT\",\"amount\":1500000.50,\"tags\":[\"t1\",\"t2\"]}";
        String newJson = "{\"status\":\"ACTIVE\",\"amount\":2000000.00,\"tags\":[\"t1\",\"t2\",\"t3\"]}";

        RecordAuditEventCommand cmd = new RecordAuditEventCommand(
                branchAId,
                userOwnerA.getId(),
                UUID.randomUUID(),
                "COMPLEX_TARGET",
                targetId,
                "UPDATED",
                "JSON snapshot test",
                oldJson,
                newJson
        );

        AuditEventDto recorded = auditLogService.recordEvent(cmd);

        assertThat(recorded.oldSnapshot()).isEqualTo(oldJson);
        assertThat(recorded.newSnapshot()).isEqualTo(newJson);

        List<AuditEventDto> list = auditLogService.findByTarget("COMPLEX_TARGET", targetId);
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).oldSnapshot()).isEqualTo(oldJson);
        assertThat(list.get(0).newSnapshot()).isEqualTo(newJson);
    }

    @Test
    @DisplayName("Branch isolation: Branch B owner is forbidden (403) from accessing Branch A audit events")
    void testBranchIsolationForbidden() throws Exception {
        // Record an audit event in Branch A
        auditLogService.recordEvent(new RecordAuditEventCommand(
                branchAId,
                userOwnerA.getId(),
                UUID.randomUUID(),
                "BRANCH",
                branchAId,
                "BRANCH_AUDIT_LOG",
                "Sensitive branch event",
                null,
                "{\"action\":\"view\"}"
        ));

        // User B attempts to access Branch A's audit events -> 403 Forbidden
        mockMvc.perform(get("/api/v1/branches/" + branchAId + "/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOwnerB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Branch authorized: Branch A owner can view Branch A audit events")
    void testBranchAuthorizedAccess() throws Exception {
        // Record an audit event in Branch A
        auditLogService.recordEvent(new RecordAuditEventCommand(
                branchAId,
                userOwnerA.getId(),
                UUID.randomUUID(),
                "BRANCH",
                branchAId,
                "BRANCH_AUDIT_LOG",
                "Authorized view test",
                null,
                "{\"action\":\"view\"}"
        ));

        // User A accesses Branch A's audit events -> 200 OK
        mockMvc.perform(get("/api/v1/branches/" + branchAId + "/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].eventName").value("BRANCH_AUDIT_LOG"))
                .andExpect(jsonPath("$.data.content[0].branchId").value(branchAId.toString()));
    }

    @Test
    @DisplayName("System Admin can query global audit events")
    void testSystemAdminGlobalAuditQuery() throws Exception {
        auditLogService.recordEvent(new RecordAuditEventCommand(
                branchAId,
                userOwnerA.getId(),
                UUID.randomUUID(),
                "BRANCH",
                branchAId,
                "GLOBAL_VIEW_TEST",
                "Global view test",
                null,
                null
        ));

        mockMvc.perform(get("/api/v1/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSystemAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Non-System-Admin user is forbidden (403) from querying global audit events")
    void testNonAdminGlobalAuditForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sensitive operation audit: BranchBankService addBankAccount and activateBranch trigger audit events")
    void testSensitiveBranchBankAuditEvents() {
        // 1. Add bank account
        CreateBankAccountCommand bankCmd = new CreateBankAccountCommand(
                "Vietcombank",
                "CONG TY LUC TRA",
                "0123456789",
                true
        );
        BranchBankAccountDto accountDto = branchBankService.addBankAccount(branchAId, bankCmd);

        List<AuditEventDto> bankAudits = auditLogService.findByTarget("BRANCH_BANK_ACCOUNT", accountDto.id());
        assertThat(bankAudits).isNotEmpty();
        AuditEventDto bankEvent = bankAudits.get(0);
        assertThat(bankEvent.eventName()).isEqualTo("BANK_ACCOUNT_ADDED");
        assertThat(bankEvent.branchId()).isEqualTo(branchAId);
        assertThat(bankEvent.newSnapshot()).contains("Vietcombank");

        // 2. Activate branch
        UUID draftBranchId = UUID.randomUUID();
        BranchJpaEntity draftBranch = new BranchJpaEntity(
                draftBranchId,
                entityId,
                "LTT_HCM_03",
                "Luc Tra Tinh Hoa - Thu Duc",
                "Asia/Ho_Chi_Minh",
                BranchStatus.DRAFT,
                Instant.now()
        );
        branchRepository.save(draftBranch);

        // Add account to satisfy readiness gate
        branchBankService.addBankAccount(draftBranchId, bankCmd);

        // Activate branch
        branchBankService.activateBranch(draftBranchId);

        List<AuditEventDto> branchAudits = auditLogService.findByTarget("BRANCH", draftBranchId);
        assertThat(branchAudits).isNotEmpty();
        AuditEventDto actEvent = branchAudits.stream()
                .filter(e -> "BRANCH_ACTIVATED".equals(e.eventName()))
                .findFirst()
                .orElseThrow();
        assertThat(actEvent.oldSnapshot()).contains("DRAFT");
        assertThat(actEvent.newSnapshot()).contains("ACTIVE");
    }

    @Test
    @DisplayName("Sensitive operation audit: UserService updateUserStatus triggers audit event")
    void testSensitiveUserStatusAudit() {
        UserJpaEntity targetUser = createUser("test_user_status", "Test Status User");

        userService.updateUserStatus(targetUser.getId(), new UpdateUserStatusCommand(UserStatus.SUSPENDED));

        List<AuditEventDto> audits = auditLogService.findByTarget("USER", targetUser.getId());
        assertThat(audits).isNotEmpty();
        AuditEventDto event = audits.get(0);
        assertThat(event.eventName()).isEqualTo("USER_STATUS_CHANGED");
        assertThat(event.oldSnapshot()).contains("ACTIVE");
        assertThat(event.newSnapshot()).contains("SUSPENDED");
    }

    @Test
    @DisplayName("Sensitive operation audit: ScopedPermissionService grantRole and revokeGrant trigger audit events")
    void testSensitiveRoleGrantAudit() {
        GrantRoleCommand grantCmd = new GrantRoleCommand(
                entityId,
                branchAId,
                userOwnerA.getId(),
                RoleKey.MANAGER,
                "READ",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(3)
        );

        ScopedRoleGrantDto granted = scopedPermissionService.grantRole(grantCmd, userSystemAdmin.getId());

        List<AuditEventDto> grantAudits = auditLogService.findByTarget("SCOPED_ROLE_GRANT", granted.id());
        assertThat(grantAudits).isNotEmpty();
        AuditEventDto grantEvent = grantAudits.stream()
                .filter(e -> "ROLE_GRANTED".equals(e.eventName()))
                .findFirst()
                .orElseThrow();
        assertThat(grantEvent.newSnapshot()).contains("MANAGER");

        // Revoke role
        scopedPermissionService.revokeGrant(granted.id());

        List<AuditEventDto> revokeAudits = auditLogService.findByTarget("SCOPED_ROLE_GRANT", granted.id());
        AuditEventDto revokeEvent = revokeAudits.stream()
                .filter(e -> "ROLE_REVOKED".equals(e.eventName()))
                .findFirst()
                .orElseThrow();
        assertThat(revokeEvent.oldSnapshot()).contains("MANAGER");
    }
}
