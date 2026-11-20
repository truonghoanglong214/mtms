package com.mtms.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.finance.application.dto.CreateOpeningBatchCommand;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.OpeningBatchStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.OpeningAccountBalanceJpaEntity;
import com.mtms.finance.infrastructure.persistence.OpeningAccountBalanceJpaRepository;
import com.mtms.finance.infrastructure.persistence.OpeningBalanceBatchJpaEntity;
import com.mtms.finance.infrastructure.persistence.OpeningBalanceBatchJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.organization.domain.BankAccountStatus;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchBankAccountJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchBankAccountJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-FIN-08 / BR-FIN-08 / GAP-01 / ADR-009 / TC-017
 * Integration test covering opening balances cutover lifecycle:
 * - Query status before and after setup
 * - Manager draft capture of opening cash & bank balances
 * - Working capital reconciliation
 * - Owner post cutover batch with atomic cash ledger posting and account balance recalculation
 * - Immutability & double posting rejection
 * - Branch scoped security & isolation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpeningBalanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OpeningBalanceBatchJpaRepository batchRepository;

    @Autowired
    private OpeningAccountBalanceJpaRepository openingAccountBalanceRepository;

    @Autowired
    private CashLedgerEntryJpaRepository ledgerEntryRepository;

    @Autowired
    private FinancialAccountJpaRepository financialAccountRepository;

    @Autowired
    private FinancialCategoryJpaRepository categoryRepository;

    @Autowired
    private BranchBankAccountJpaRepository branchBankRepository;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID otherEntityId;
    private UUID branchId;
    private UUID otherBranchId;
    private UserJpaEntity ownerUser;
    private UserJpaEntity managerUser;
    private UserJpaEntity otherBranchOwner;
    private String tokenOwner;
    private String tokenManager;
    private String tokenOtherOwner;

    private FinancialAccountJpaEntity cashAccount;
    private FinancialAccountJpaEntity bankAccount;

    private UUID savedBatchId;

    @BeforeAll
    void setUp() {
        openingAccountBalanceRepository.deleteAll();
        batchRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        financialAccountRepository.deleteAll();
        categoryRepository.deleteAll();
        branchBankRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Organization & Branches
        entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_OB_01", "Opening Balance Corp", "0109999888", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR-OB-01", "Chi nhánh Khai Trương 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        otherEntityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                otherEntityId, "ENT_OB_02", "Other Entity Corp", "0109999777", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, otherEntityId, "BR-OB-02", "Chi nhánh Khác", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Users & Grants
        ownerUser = createUser("owner_ob", "Chủ Cửa Hàng");
        managerUser = createUser("manager_ob", "Quản Lý Cửa Hàng");
        otherBranchOwner = createUser("other_owner_ob", "Chủ Chi Nhánh Khác");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, ownerUser.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), ownerUser.getId(), Instant.now()
        ));

        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, managerUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), ownerUser.getId(), Instant.now()
        ));

        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), otherEntityId, otherBranchId, otherBranchOwner.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), otherBranchOwner.getId(), Instant.now()
        ));

        tokenOwner = jwtTokenService.generateAccessToken(ownerUser.getId(), ownerUser.getLoginIdentifier());
        tokenManager = jwtTokenService.generateAccessToken(managerUser.getId(), managerUser.getLoginIdentifier());
        tokenOtherOwner = jwtTokenService.generateAccessToken(otherBranchOwner.getId(), otherBranchOwner.getLoginIdentifier());

        // 3. Seed System Category for Opening Balance
        categoryRepository.save(new FinancialCategoryJpaEntity(
                UUID.randomUUID(),
                "OPENING_BALANCE",
                "Số dư tiền đầu kỳ",
                CategoryGroup.CAPITAL_FLOW,
                FlowDirection.INFLOW,
                true,
                true,
                "Số dư tiền mặt và ngân hàng đầu kỳ (BR-FIN-08)",
                Instant.now()
        ));

        // 4. Financial Accounts for branchId: Cash and Bank accounts with 0 initial balance
        cashAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, null, "Quỹ tiền mặt quầy",
                AccountKind.CASH, "VND", BigDecimal.ZERO, AccountStatus.ACTIVE, Instant.now()
        ));

        BranchBankAccountJpaEntity branchBank = branchBankRepository.save(new BranchBankAccountJpaEntity(
                UUID.randomUUID(), branchId, "Vietcombank", "CTY OB 01", "cipher_ob", "**** **** 1122", true, BankAccountStatus.ACTIVE, Instant.now()
        ));

        bankAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, branchBank.getId(), "Tài khoản Vietcombank",
                AccountKind.BANK, "VND", BigDecimal.ZERO, AccountStatus.ACTIVE, Instant.now()
        ));
    }

    private UserJpaEntity createUser(String username, String displayName) {
        return userRepository.save(new UserJpaEntity(
                UUID.randomUUID(),
                username + "@mtms.local",
                passwordEncoder.encode("Pass@1234"),
                displayName,
                UserStatus.ACTIVE,
                Instant.now()
        ));
    }

    @Test
    @Order(1)
    @DisplayName("GET /status initially returns isPosted=false and null activeBatch")
    void testGetInitialStatus() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/opening-balances/status", branchId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPosted", is(false)))
                .andExpect(jsonPath("$.activeBatch").value(nullValue()));
    }

    @Test
    @Order(2)
    @DisplayName("POST / creates draft opening balance batch with account balances")
    void testCreateDraftOpeningBatch() throws Exception {
        CreateOpeningBatchCommand command = new CreateOpeningBatchCommand(
                LocalDate.of(2026, 1, 1),
                "Biên bản chốt số dư chuyển giao hệ thống",
                null,
                List.of(
                        new CreateOpeningBatchCommand.AccountBalanceItem(cashAccount.getId(), new BigDecimal("15000000.00")),
                        new CreateOpeningBatchCommand.AccountBalanceItem(bankAccount.getId(), new BigDecimal("85000000.00"))
                )
        );

        String responseBody = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/opening-balances", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.branchId", is(branchId.toString())))
                .andExpect(jsonPath("$.cutoverDate", is("2026-01-01")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.totalCashAmount", is(15000000.00)))
                .andExpect(jsonPath("$.totalBankAmount", is(85000000.00)))
                .andExpect(jsonPath("$.netWorkingCapital", is(100000000.00)))
                .andExpect(jsonPath("$.accountBalances", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        savedBatchId = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());
        assertThat(savedBatchId).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("GET /status now returns isPosted=false and activeBatch with DRAFT status")
    void testGetStatusWithDraft() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/opening-balances/status", branchId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPosted", is(false)))
                .andExpect(jsonPath("$.cutoverDate", is("2026-01-01")))
                .andExpect(jsonPath("$.activeBatch.id", is(savedBatchId.toString())))
                .andExpect(jsonPath("$.activeBatch.status", is("DRAFT")))
                .andExpect(jsonPath("$.activeBatch.totalCashAmount", is(15000000.00)))
                .andExpect(jsonPath("$.activeBatch.totalBankAmount", is(85000000.00)));
    }

    @Test
    @Order(4)
    @DisplayName("GET /{id} returns detail of opening balance batch")
    void testGetBatchDetail() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/opening-balances/{id}", branchId, savedBatchId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedBatchId.toString())))
                .andExpect(jsonPath("$.cutoverDate", is("2026-01-01")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.accountBalances", hasSize(2)));
    }

    @Test
    @Order(5)
    @DisplayName("POST /{id}/post by non-owner (MANAGER) is rejected with 403 Forbidden")
    void testPostBatchForbiddenForManager() throws Exception {
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/opening-balances/{id}/post", branchId, savedBatchId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("POST /{id}/post by OWNER successfully posts cutover batch and creates ledger inflows")
    void testPostBatchSuccessByOwner() throws Exception {
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/opening-balances/{id}/post", branchId, savedBatchId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedBatchId.toString())))
                .andExpect(jsonPath("$.status", is("POSTED")))
                .andExpect(jsonPath("$.postedById", is(ownerUser.getId().toString())))
                .andExpect(jsonPath("$.postedAt", notNullValue()));

        // 1. Verify batch status in DB
        OpeningBalanceBatchJpaEntity batch = batchRepository.findById(savedBatchId).orElseThrow();
        assertThat(batch.getStatus()).isEqualTo(OpeningBatchStatus.POSTED);
        assertThat(batch.getPostedBy()).isEqualTo(ownerUser.getId());
        assertThat(batch.getPostedAt()).isNotNull();

        // 2. Verify financial accounts balances were updated
        FinancialAccountJpaEntity updatedCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(updatedCash.getCurrentBalance()).isEqualByComparingTo("15000000.00");

        FinancialAccountJpaEntity updatedBank = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedBank.getCurrentBalance()).isEqualByComparingTo("85000000.00");

        // 3. Verify cash ledger entries created
        List<CashLedgerEntryJpaEntity> ledgerEntries = ledgerEntryRepository.findAll().stream()
                .filter(e -> savedBatchId.equals(e.getSourceId()))
                .toList();
        assertThat(ledgerEntries).hasSize(2);

        for (CashLedgerEntryJpaEntity entry : ledgerEntries) {
            assertThat(entry.getSourceType()).isEqualTo(SourceType.CAPITAL_FLOW);
            assertThat(entry.getSourceLeg()).isEqualTo("OPENING_BALANCE");
            assertThat(entry.getEntryType()).isEqualTo(EntryType.INFLOW);
            assertThat(entry.getBusinessDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        }

        // 4. Verify opening account balance records have linked ledger entry IDs
        List<OpeningAccountBalanceJpaEntity> accountBalances = openingAccountBalanceRepository.findByBatchId(savedBatchId);
        assertThat(accountBalances).hasSize(2);
        for (OpeningAccountBalanceJpaEntity item : accountBalances) {
            assertThat(item.getCashLedgerEntryId()).isNotNull();
        }
    }

    @Test
    @Order(7)
    @DisplayName("GET /status after posting returns isPosted=true and activeBatch status=POSTED")
    void testGetStatusAfterPosting() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/opening-balances/status", branchId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPosted", is(true)))
                .andExpect(jsonPath("$.cutoverDate", is("2026-01-01")))
                .andExpect(jsonPath("$.activeBatch.status", is("POSTED")));
    }

    @Test
    @Order(8)
    @DisplayName("Re-posting an already posted batch is rejected with 400 Bad Request")
    void testRepostingAlreadyPostedRejected() throws Exception {
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/opening-balances/{id}/post", branchId, savedBatchId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:opening-balance-already-posted")));
    }

    @Test
    @Order(9)
    @DisplayName("Editing or saving draft when batch is already posted is rejected with 400 Bad Request")
    void testEditingPostedBatchRejected() throws Exception {
        CreateOpeningBatchCommand command = new CreateOpeningBatchCommand(
                LocalDate.of(2026, 1, 2),
                "Cập nhật số dư mới",
                null,
                List.of(new CreateOpeningBatchCommand.AccountBalanceItem(cashAccount.getId(), new BigDecimal("20000000.00")))
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/opening-balances", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:opening-balance-already-posted")));
    }

    @Test
    @Order(10)
    @DisplayName("Branch isolation: user from another branch cannot access opening balances")
    void testBranchIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/opening-balances/{id}", branchId, savedBatchId)
                        .header("Authorization", "Bearer " + tokenOtherOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("Invalid cutover date (null) is rejected with 400 Bad Request")
    void testInvalidCutoverDateRejected() throws Exception {
        String invalidJson = """
                {
                    "cutoverDate": null,
                    "note": "Missing date"
                }
                """;

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/opening-balances", otherBranchId)
                        .header("Authorization", "Bearer " + tokenOtherOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
