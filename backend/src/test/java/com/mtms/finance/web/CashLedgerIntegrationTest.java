package com.mtms.finance.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.decimal.MoneyUtils;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.finance.application.dto.CreateFinancialAccountCommand;
import com.mtms.finance.application.dto.CreateFinancialCategoryCommand;
import com.mtms.finance.application.dto.InternalTransferCommand;
import com.mtms.finance.application.dto.PostLedgerEntryCommand;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.InternalTransferJpaRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CashLedgerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CashLedgerEntryJpaRepository ledgerEntryRepository;

    @Autowired
    private InternalTransferJpaRepository internalTransferRepository;

    @Autowired
    private FinancialAccountJpaRepository financialAccountRepository;

    @Autowired
    private FinancialCategoryJpaRepository categoryRepository;

    @Autowired
    private BranchBankAccountJpaRepository branchBankRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchId;
    private UUID otherBranchId;
    private UserJpaEntity manager;
    private UserJpaEntity unauthorizedUser;
    private String tokenManager;
    private String tokenUnauthorized;

    private FinancialAccountJpaEntity cashAccount;
    private FinancialAccountJpaEntity bankAccount;

    private FinancialCategoryJpaEntity trfCategory;
    private FinancialCategoryJpaEntity capCategory;
    private FinancialCategoryJpaEntity opexCategory;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        internalTransferRepository.deleteAll();
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
                entityId, "ENT_FIN_INT", "Finance Integration Testing Co", "0109999111", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR_FIN_INT_01", "Chi nhánh Tài Chính 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, entityId, "BR_FIN_INT_02", "Chi nhánh Khác 2", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Users & Grants
        manager = createUser("manager_finance", "Quản Lý Tài Chính");
        unauthorizedUser = createUser("unauth_finance", "Người Không Có Quyền");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, manager.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), manager.getId(), Instant.now()
        ));

        tokenManager = jwtTokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());
        tokenUnauthorized = jwtTokenService.generateAccessToken(unauthorizedUser.getId(), unauthorizedUser.getLoginIdentifier());

        // 3. Seed System Categories
        seedSystemCategories();

        trfCategory = categoryRepository.findByCategoryCode("TRF_INTERNAL").orElseThrow();
        capCategory = categoryRepository.findByCategoryCode("CAP_INJECTION").orElseThrow();
        opexCategory = categoryRepository.findByCategoryCode("OPEX_UTILITIES").orElseThrow();

        // 4. Create Initial Accounts: Cash with 10M, Bank with 2M
        cashAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, null, "Quỹ tiền mặt chi nhánh",
                AccountKind.CASH, "VND", new BigDecimal("10000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));

        BranchBankAccountJpaEntity branchBank = branchBankRepository.save(new BranchBankAccountJpaEntity(
                UUID.randomUUID(), branchId, "Vietcombank", "CTY TRA HOA", "cipher_acc", "**** **** 8888", true, BankAccountStatus.ACTIVE, Instant.now()
        ));

        bankAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, branchBank.getId(), "Tài khoản Vietcombank",
                AccountKind.BANK, "VND", new BigDecimal("2000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));
    }

    private void seedSystemCategories() {
        categoryRepository.saveAll(List.of(
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REV_CASH", "Doanh thu bán hàng - Tiền mặt", CategoryGroup.REVENUE, FlowDirection.INFLOW, true, true, "Tiền mặt thu từ khách hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REV_BANK", "Doanh thu bán hàng - Chuyển khoản", CategoryGroup.REVENUE, FlowDirection.INFLOW, true, true, "Tiền chuyển khoản ngân hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "TRF_INTERNAL", "Chuyển quỹ nội bộ (Tiền mặt <-> Ngân hàng)", CategoryGroup.INTERNAL_TRANSFER, FlowDirection.BOTH, true, true, "Luân chuyển giữa quỹ tiền mặt và tài khoản ngân hàng chi nhánh (TC-010)", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "DEBT_SUPPLIER", "Chi trả nợ nhà cung cấp mua hàng", CategoryGroup.DEBT_SETTLEMENT, FlowDirection.OUTFLOW, true, true, "Thanh toán tiền mua nguyên vật liệu đã ghi nợ (TC-009)", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "PAY_SALARY", "Chi trả lương nhân viên", CategoryGroup.PAYROLL, FlowDirection.OUTFLOW, true, true, "Thanh toán tiền lương thực tế cho nhân sự", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "CAP_INJECTION", "Vốn góp chủ sở hữu / Nhà đầu tư", CategoryGroup.CAPITAL_FLOW, FlowDirection.INFLOW, true, true, "Tiền vốn góp kinh doanh ban đầu hoặc bổ sung", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "CAP_WITHDRAWAL", "Rút vốn chủ sở hữu", CategoryGroup.CAPITAL_FLOW, FlowDirection.OUTFLOW, true, true, "Rút vốn khỏi hoạt động kinh doanh", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_RENT", "Chi phí thuê mặt bằng", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Tiền thuê địa điểm kinh doanh chi nhánh", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_UTILITIES", "Chi phí điện, nước, internet", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Hóa đơn dịch vụ tiện ích quán", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_SUPPLIES", "Chi phí vật tư, bao bì tiêu hao", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Ly, ống hút, túi, giấy in nhiệt...", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_MARKETING", "Chi phí marketing & quảng cáo", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "In ấn tờ rơi, chạy ads, bảng hiệu...", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_MAINTENANCE", "Chi phí bảo trì, sửa chữa máy móc", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Bảo dưỡng máy pha, máy dập nắp...", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_OTHER", "Chi phí vận hành khác", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Chi phí vận hành lặt vặt khác", Instant.now())
        ));
    }

    private UserJpaEntity createUser(String login, String name) {
        return userRepository.save(new UserJpaEntity(
                UUID.randomUUID(), login, passwordEncoder.encode("TestPass123!"), name, UserStatus.ACTIVE, Instant.now()
        ));
    }

    @Test
    @Order(1)
    @DisplayName("TC-010: Chuyển 5M từ Tiền mặt sang Ngân hàng -> Tiền mặt giảm 5M, ngân hàng tăng 5M, tổng số dư tiền chi nhánh bảo toàn")
    void testTc010InternalTransferPreservesTotalBalance() throws Exception {
        BigDecimal initialTotal = cashAccount.getCurrentBalance().add(bankAccount.getCurrentBalance()); // 12,000,000.00
        BigDecimal transferAmount = new BigDecimal("5000000.00");

        InternalTransferCommand cmd = new InternalTransferCommand(
                cashAccount.getId(),
                bankAccount.getId(),
                transferAmount,
                LocalDate.of(2026, 11, 17),
                "Nộp tiền mặt doanh thu vào tài khoản ngân hàng",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/transfers", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.amount", is(5000000.00)))
                .andExpect(jsonPath("$.fromAccountId", is(cashAccount.getId().toString())))
                .andExpect(jsonPath("$.toAccountId", is(bankAccount.getId().toString())));

        // Verify account balances in database
        FinancialAccountJpaEntity updatedCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        FinancialAccountJpaEntity updatedBank = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();

        assertThat(updatedCash.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("5000000.00")); // 10M - 5M
        assertThat(updatedBank.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("7000000.00")); // 2M + 5M

        // Total branch cash is preserved!
        BigDecimal newTotal = updatedCash.getCurrentBalance().add(updatedBank.getCurrentBalance());
        assertThat(newTotal).isEqualByComparingTo(initialTotal);

        // Verify two immutable ledger entries were created
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository.findByBranchIdOrderByPostedAtDesc(branchId);
        assertThat(entries).hasSize(2);

        CashLedgerEntryJpaEntity sourceLeg = entries.stream()
                .filter(e -> "SOURCE".equals(e.getSourceLeg())).findFirst().orElseThrow();
        CashLedgerEntryJpaEntity destLeg = entries.stream()
                .filter(e -> "DESTINATION".equals(e.getSourceLeg())).findFirst().orElseThrow();

        assertThat(sourceLeg.getAccountId()).isEqualTo(cashAccount.getId());
        assertThat(sourceLeg.getEntryType()).isEqualTo(EntryType.OUTFLOW);
        assertThat(sourceLeg.getSignedAmount()).isEqualByComparingTo(new BigDecimal("-5000000.00"));
        assertThat(sourceLeg.getRunningBalance()).isEqualByComparingTo(new BigDecimal("5000000.00"));

        assertThat(destLeg.getAccountId()).isEqualTo(bankAccount.getId());
        assertThat(destLeg.getEntryType()).isEqualTo(EntryType.INFLOW);
        assertThat(destLeg.getSignedAmount()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(destLeg.getRunningBalance()).isEqualByComparingTo(new BigDecimal("7000000.00"));
    }

    @Test
    @Order(2)
    @DisplayName("TC-011: Hai lần post cùng account_id, source_type, source_id, source_leg -> Đúng 1 entry được tạo, số dư không bị trừ 2 lần")
    void testTc011DoublePostingPrevention() throws Exception {
        UUID fixedSourceId = UUID.randomUUID();
        BigDecimal postAmount = new BigDecimal("1000000.00");

        PostLedgerEntryCommand cmd = new PostLedgerEntryCommand(
                cashAccount.getId(),
                opexCategory.getId(),
                EntryType.OUTFLOW,
                postAmount,
                LocalDate.of(2026, 11, 17),
                "Thanh toán hóa đơn điện lực EVN",
                null,
                SourceType.MANUAL_EXPENSE,
                fixedSourceId,
                "MAIN"
        );

        // 1st request -> Succeeds (201 Created)
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/entries", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(1000000.00)))
                .andExpect(jsonPath("$.runningBalance", is(9000000.00)));

        FinancialAccountJpaEntity balanceAfterFirst = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(balanceAfterFirst.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("9000000.00"));

        // 2nd request with EXACT SAME (account_id, source_type, source_id, source_leg) -> Rejected (409 Conflict)
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/entries", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:duplicate-ledger-posting")));

        // Balance is NOT deducted twice!
        FinancialAccountJpaEntity balanceAfterSecond = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(balanceAfterSecond.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("9000000.00"));

        // Exactly one ledger entry exists in DB
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository.findAll().stream()
                .filter(e -> fixedSourceId.equals(e.getSourceId()))
                .toList();
        assertThat(entries).hasSize(1);
    }

    @Test
    @Order(3)
    @DisplayName("Capital Flow Separation: Góp vốn CAPITAL_FLOW tăng tiền nhưng gắn đúng nhóm vốn, không lẫn vào doanh thu")
    void testCapitalFlowSeparation() throws Exception {
        BigDecimal capitalAmount = new BigDecimal("20000000.00");

        PostLedgerEntryCommand cmd = new PostLedgerEntryCommand(
                bankAccount.getId(),
                capCategory.getId(),
                EntryType.INFLOW,
                capitalAmount,
                LocalDate.of(2026, 11, 17),
                "Chủ đầu tư rót thêm vốn mở rộng quán",
                null,
                SourceType.CAPITAL_FLOW,
                null,
                "MAIN"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/entries", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryGroup", is("CAPITAL_FLOW")))
                .andExpect(jsonPath("$.sourceType", is("CAPITAL_FLOW")))
                .andExpect(jsonPath("$.amount", is(20000000.00)))
                .andExpect(jsonPath("$.runningBalance", is(22000000.00)));

        FinancialAccountJpaEntity updatedBank = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedBank.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("22000000.00"));
    }

    @Test
    @Order(4)
    @DisplayName("Overdraft Validation: Chi tiền vượt quá số dư -> 400 Bad Request")
    void testOverdraftValidationFailsWith400() throws Exception {
        BigDecimal excessiveAmount = new BigDecimal("999000000.00"); // 999M > 10M cash

        PostLedgerEntryCommand cmd = new PostLedgerEntryCommand(
                cashAccount.getId(),
                opexCategory.getId(),
                EntryType.OUTFLOW,
                excessiveAmount,
                LocalDate.of(2026, 11, 17),
                "Chi vượt quá hạn mức quỹ",
                null,
                SourceType.MANUAL_EXPENSE,
                null,
                "MAIN"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/entries", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:insufficient-account-balance")));

        // Balance remains unchanged
        FinancialAccountJpaEntity checkCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(checkCash.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("10000000.00"));
    }

    @Test
    @Order(5)
    @DisplayName("Branch Isolation: Chi nhánh khác truy cập -> 403 Forbidden")
    void testBranchIsolationReturns403() throws Exception {
        // User has no role grant on otherBranchId
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/accounts", otherBranchId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isForbidden());

        // Unauthorized user trying to access branchId
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/accounts", branchId)
                        .header("Authorization", "Bearer " + tokenUnauthorized))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/ledger", branchId)
                        .header("Authorization", "Bearer " + tokenUnauthorized))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("Transfer Invariant: fromAccountId == toAccountId -> 400 Bad Request")
    void testTransferSameAccountFails() throws Exception {
        InternalTransferCommand cmd = new InternalTransferCommand(
                cashAccount.getId(),
                cashAccount.getId(), // Same account!
                new BigDecimal("500000.00"),
                LocalDate.of(2026, 11, 17),
                "Chuyển vào chính mình",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/transfers", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-transfer-accounts")));
    }

    @Test
    @Order(7)
    @DisplayName("Categories Query & Custom Creation: Tạo danh mục con OPERATING_EXPENSE thành công")
    void testCategoriesQueryAndCreation() throws Exception {
        // 1. Get categories
        mockMvc.perform(get("/api/v1/finance/categories")
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("group", "OPERATING_EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));

        // 2. Create custom category
        CreateFinancialCategoryCommand createCmd = new CreateFinancialCategoryCommand(
                "OPEX_ICE_CUBE",
                "Chi phí mua đá viên bổ sung",
                "Mua đá bên ngoài khi máy đá quá tải"
        );

        mockMvc.perform(post("/api/v1/finance/categories")
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryCode", is("OPEX_ICE_CUBE")))
                .andExpect(jsonPath("$.categoryGroup", is("OPERATING_EXPENSE")))
                .andExpect(jsonPath("$.flowDirection", is("OUTFLOW")))
                .andExpect(jsonPath("$.isSystem", is(false)));
    }

    @Test
    @Order(8)
    @DisplayName("Ledger Pagination & Filtering: Tra cứu sổ cái theo chi nhánh và tài khoản")
    void testLedgerPaginationAndFiltering() throws Exception {
        // Post 2 entries
        PostLedgerEntryCommand cmd1 = new PostLedgerEntryCommand(
                cashAccount.getId(), opexCategory.getId(), EntryType.OUTFLOW,
                new BigDecimal("200000.00"), LocalDate.of(2026, 11, 15), "Tiền điện", null, null, null, null
        );
        PostLedgerEntryCommand cmd2 = new PostLedgerEntryCommand(
                cashAccount.getId(), opexCategory.getId(), EntryType.OUTFLOW,
                new BigDecimal("300000.00"), LocalDate.of(2026, 11, 17), "Tiền nước", null, null, null, null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/entries", branchId)
                .header("Authorization", "Bearer " + tokenManager)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd1))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/entries", branchId)
                .header("Authorization", "Bearer " + tokenManager)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd2))).andExpect(status().isCreated());

        // Query ledger
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/ledger", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("accountId", cashAccount.getId().toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }
}
