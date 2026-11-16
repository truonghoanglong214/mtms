package com.mtms.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.application.dto.GrantRoleCommand;
import com.mtms.identity.domain.RoleKey;
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
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Integration test suite verifying:
 * 1. TC-011 Concurrency & deduplication: duplicate request returns cached response with Idempotent-Replayed: true.
 * 2. Payload mismatch rejection (HTTP 422).
 * 3. In-progress concurrent conflict rejection (HTTP 409).
 * 4. Optimistic locking failure translation to RFC 7807 (HTTP 409 urn:mtms:error:version-conflict).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IdempotencyIntegrationTest.ConcurrencyTestConfig.class)
class IdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private IdempotencyService idempotencyService;

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

    private UUID adminUserId;
    private String adminToken;
    private UUID targetBranchId;
    private UUID targetEntityId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        idempotencyRecordRepository.deleteAll();
        bankAccountRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Admin User
        adminUserId = UUID.randomUUID();
        UserJpaEntity adminUser = new UserJpaEntity(
                adminUserId,
                "admin_idem",
                passwordEncoder.encode("SecretAdmin123!"),
                "Admin Idempotency",
                UserStatus.ACTIVE,
                Instant.now()
        );
        userRepository.save(adminUser);

        // 2. Create Target User
        targetUserId = UUID.randomUUID();
        UserJpaEntity targetUser = new UserJpaEntity(
                targetUserId,
                "target_idem",
                passwordEncoder.encode("SecretTarget123!"),
                "Target User",
                UserStatus.ACTIVE,
                Instant.now()
        );
        userRepository.save(targetUser);

        // 3. Create Entity & Branch
        targetEntityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                targetEntityId,
                "ENT_IDEM",
                "Idempotency Entity",
                "0109998888",
                BusinessEntityStatus.ACTIVE,
                Instant.now()
        ));

        targetBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                targetBranchId,
                targetEntityId,
                "BR_IDEM_01",
                "Idempotency Branch",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        ));

        // 4. Grant Admin SYSTEM_ADMIN role
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                targetEntityId,
                null,
                adminUserId,
                "SYSTEM_ADMIN",
                "*",
                "ALL",
                "ALL",
                LocalDate.now().minusDays(1),
                null,
                adminUserId,
                Instant.now()
        ));

        // Generate Admin JWT token
        adminToken = jwtTokenService.generateAccessToken(adminUserId, adminUser.getLoginIdentifier());
    }

    @Test
    @DisplayName("TC-011 Concurrency & Deduplication: Two identical requests with same Idempotency-Key create entity once and return cached response on replay")
    void tc011_deduplicationWithSameIdempotencyKey() throws Exception {
        String idempotencyKey = "key-tc011-" + UUID.randomUUID();

        GrantRoleCommand command = new GrantRoleCommand(
                targetEntityId,
                targetBranchId,
                targetUserId,
                RoleKey.MANAGER,
                "READ,WRITE",
                "FINANCE",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(6)
        );
        String requestJson = objectMapper.writeValueAsString(command);

        // --- FIRST REQUEST ---
        MvcResult firstResult = mockMvc.perform(post("/api/v1/auth/grants")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleKey").value("MANAGER"))
                .andReturn();

        String firstResponseBody = firstResult.getResponse().getContentAsString();
        assertThat(firstResult.getResponse().getHeader("Idempotent-Replayed")).isNull();

        // Verify database has exactly 2 grants (1 sysadmin grant from setup + 1 new grant)
        assertThat(grantRepository.count()).isEqualTo(2);

        // Verify idempotency record in DB is COMPLETED
        IdempotencyRecordJpaEntity record = idempotencyRecordRepository
                .findByActorUserIdAndIdempotencyKey(adminUserId, idempotencyKey)
                .orElseThrow();
        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(record.getResponseStatus()).isEqualTo(201);

        // --- SECOND REQUEST (DUPLICATE REPLAY) ---
        MvcResult secondResult = mockMvc.perform(post("/api/v1/auth/grants")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn();

        String secondResponseBody = secondResult.getResponse().getContentAsString();
        assertThat(secondResponseBody).isEqualTo(firstResponseBody);

        // Verify NO duplicate grant created in DB
        assertThat(grantRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-011 Deduplication on BranchBankController: duplicate addBankAccount creates 1 account only and replays response")
    void tc011_branchBankAccountDeduplication() throws Exception {
        String idempotencyKey = "key-bank-" + UUID.randomUUID();

        CreateBankAccountCommand command = new CreateBankAccountCommand(
                "Vietcombank",
                "Cong Ty Luc Tra Tinh Hoa",
                "0123456789012",
                true
        );
        String requestJson = objectMapper.writeValueAsString(command);

        // 1. Initial request
        mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", targetBranchId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bankName").value("Vietcombank"));

        assertThat(bankAccountRepository.count()).isEqualTo(1);

        // 2. Duplicate replay
        mockMvc.perform(post("/api/v1/branches/{branchId}/bank-accounts", targetBranchId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.data.bankName").value("Vietcombank"));

        // Still only 1 bank account in database
        assertThat(bankAccountRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Payload Mismatch: Reusing same Idempotency-Key with different payload rejects with HTTP 422")
    void payloadMismatch_rejectedWith422() throws Exception {
        String idempotencyKey = "key-mismatch-" + UUID.randomUUID();

        GrantRoleCommand command1 = new GrantRoleCommand(
                targetEntityId,
                targetBranchId,
                targetUserId,
                RoleKey.EMPLOYEE,
                "READ",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(1)
        );

        // 1. Initial request succeeds
        mockMvc.perform(post("/api/v1/auth/grants")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command1)))
                .andExpect(status().isCreated());

        // 2. Different payload with SAME idempotency key
        GrantRoleCommand command2 = new GrantRoleCommand(
                targetEntityId,
                targetBranchId,
                targetUserId,
                RoleKey.BRANCH_OWNER, // CHANGED ROLE
                "ALL",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(12)
        );

        mockMvc.perform(post("/api/v1/auth/grants")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command2)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:idempotency-payload-mismatch"))
                .andExpect(jsonPath("$.title").value("Idempotency Payload Mismatch"))
                .andExpect(jsonPath("$.detail", containsString("Payload mismatch for idempotency key")));
    }

    @Test
    @DisplayName("In-Progress Concurrent Conflict: Second request arriving while first is IN_PROGRESS rejects with HTTP 409")
    void inProgressConflict_rejectedWith409() throws Exception {
        String idempotencyKey = "key-in-progress-" + UUID.randomUUID();

        // Simulate request 1 having acquired the key and still IN_PROGRESS
        String dummyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        idempotencyService.tryAcquire(adminUserId, idempotencyKey, dummyHash, Duration.ofMinutes(10));

        GrantRoleCommand command = new GrantRoleCommand(
                targetEntityId,
                targetBranchId,
                targetUserId,
                RoleKey.MANAGER,
                "ALL",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(3)
        );

        mockMvc.perform(post("/api/v1/auth/grants")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:idempotency-in-progress"))
                .andExpect(jsonPath("$.title").value("Idempotency In Progress"))
                .andExpect(jsonPath("$.detail", containsString("currently in progress")));
    }

    @Test
    @DisplayName("Optimistic Locking Conflict: Spring OptimisticLockingFailureException translates to RFC 7807 HTTP 409 urn:mtms:error:version-conflict")
    void optimisticLocking_springException_translatesTo409() throws Exception {
        mockMvc.perform(post("/api/v1/test/concurrency/optimistic-lock-spring")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:version-conflict"))
                .andExpect(jsonPath("$.title").value("Version Conflict"))
                .andExpect(jsonPath("$.detail").value("Dữ liệu đã được cập nhật bởi một giao dịch khác. Vui lòng tải lại và thử lại."));
    }

    @Test
    @DisplayName("Optimistic Locking Conflict: JPA OptimisticLockException translates to RFC 7807 HTTP 409 urn:mtms:error:version-conflict")
    void optimisticLocking_jpaException_translatesTo409() throws Exception {
        mockMvc.perform(post("/api/v1/test/concurrency/optimistic-lock-jpa")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:version-conflict"))
                .andExpect(jsonPath("$.title").value("Version Conflict"))
                .andExpect(jsonPath("$.detail").value("Dữ liệu đã được cập nhật bởi một giao dịch khác. Vui lòng tải lại và thử lại."));
    }

    @Test
    @DisplayName("Key length validation: Idempotency-Key exceeding 160 characters returns 400 Bad Request")
    void invalidKeyLength_returns400() throws Exception {
        String longKey = "a".repeat(161);

        GrantRoleCommand command = new GrantRoleCommand(
                targetEntityId,
                targetBranchId,
                targetUserId,
                RoleKey.MANAGER,
                "ALL",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(3)
        );

        mockMvc.perform(post("/api/v1/auth/grants")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", longKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:invalid-idempotency-key"));
    }

    @Test
    @DisplayName("Expired IN_PROGRESS key allows acquisition takeover")
    void expiredInProgress_allowsTakeover() {
        String key = "key-expired-" + UUID.randomUUID();
        String initialHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        // Save expired in-progress record
        IdempotencyRecordJpaEntity expiredEntity = new IdempotencyRecordJpaEntity(
                UUID.randomUUID(),
                adminUserId,
                key,
                initialHash,
                IdempotencyStatus.IN_PROGRESS,
                null,
                null,
                null,
                Instant.now().minusSeconds(60), // expired 1 minute ago
                Instant.now().minusSeconds(120)
        );
        idempotencyRecordRepository.save(expiredEntity);

        // Try acquire with new hash
        String newHash = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
        AcquireResult result = idempotencyService.tryAcquire(adminUserId, key, newHash, Duration.ofMinutes(30));

        assertThat(result.isAcquired()).isTrue();
        assertThat(result.record().getRequestHash()).isEqualTo(newHash);
        assertThat(result.record().isInProgress()).isTrue();
    }

    @TestConfiguration
    static class ConcurrencyTestConfig {
        @RestController
        @RequestMapping("/api/v1/test/concurrency")
        static class ConcurrencyTestController {

            @PostMapping("/optimistic-lock-spring")
            public void triggerSpringOptimisticLock() {
                throw new OptimisticLockingFailureException("Row updated or deleted by another transaction");
            }

            @PostMapping("/optimistic-lock-jpa")
            public void triggerJpaOptimisticLock() {
                throw new OptimisticLockException("Row updated or deleted by another transaction");
            }
        }
    }
}
