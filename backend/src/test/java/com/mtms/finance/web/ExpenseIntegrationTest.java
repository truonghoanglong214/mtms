package com.mtms.finance.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.finance.application.dto.CreateExpenseCommand;
import com.mtms.finance.application.dto.UpdateExpenseConfigCommand;
import com.mtms.finance.application.dto.VoidExpenseCommand;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.ExpenseStatus;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.LedgerStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.BranchExpenseConfigJpaRepository;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.ExpenseJpaEntity;
import com.mtms.finance.infrastructure.persistence.ExpenseJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.domain.PayableStatus;
import com.mtms.inventory.infrastructure.persistence.PayableJpaEntity;
import com.mtms.inventory.infrastructure.persistence.PayableJpaRepository;
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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-FIN-03 / P0-FIN-04 / BR-FIN-07 / TC-011
 * Integration test covering operating expense entry, approval lifecycle, alert threshold,
 * immediate payment vs on-credit liabilities, voiding & reversal, category restrictions, and branch isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpenseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseJpaRepository expenseRepository;

    @Autowired
    private BranchExpenseConfigJpaRepository configRepository;

    @Autowired
    private CashLedgerEntryJpaRepository ledgerEntryRepository;

    @Autowired
    private PayableJpaRepository payableRepository;

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

    private FinancialCategoryJpaEntity opexSupplies;
    private FinancialCategoryJpaEntity opexRent;
    private FinancialCategoryJpaEntity opexMaintenance;
    private FinancialCategoryJpaEntity capInjection;
    private FinancialCategoryJpaEntity revCash;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        configRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        payableRepository.deleteAll();
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
                entityId, "ENT_EXP_INT", "Expense Integration Testing Co", "0109999222", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR01", "Chi nhánh Expense 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, entityId, "BR02", "Chi nhánh Khác 2", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Users & Grants
        manager = createUser("manager_expense", "Quản Lý Chi Phí");
        unauthorizedUser = createUser("unauth_expense", "Người Không Có Quyền");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, manager.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), manager.getId(), Instant.now()
        ));

        tokenManager = jwtTokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());
        tokenUnauthorized = jwtTokenService.generateAccessToken(unauthorizedUser.getId(), unauthorizedUser.getLoginIdentifier());

        // 3. Seed System Categories
        seedCategories();

        opexSupplies = categoryRepository.findByCategoryCode("OPEX_SUPPLIES").orElseThrow();
        opexRent = categoryRepository.findByCategoryCode("OPEX_RENT").orElseThrow();
        opexMaintenance = categoryRepository.findByCategoryCode("OPEX_MAINTENANCE").orElseThrow();
        capInjection = categoryRepository.findByCategoryCode("CAP_INJECTION").orElseThrow();
        revCash = categoryRepository.findByCategoryCode("REV_CASH").orElseThrow();

        // 4. Create Initial Accounts: Cash with 10M, Bank with 2M
        cashAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, null, "Quỹ tiền mặt chi nhánh",
                AccountKind.CASH, "VND", new BigDecimal("10000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));

        BranchBankAccountJpaEntity branchBank = branchBankRepository.save(new BranchBankAccountJpaEntity(
                UUID.randomUUID(), branchId, "Vietcombank", "CTY EXPENSE", "cipher_acc", "**** **** 8888", true, BankAccountStatus.ACTIVE, Instant.now()
        ));

        bankAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, branchBank.getId(), "Tài khoản Vietcombank",
                AccountKind.BANK, "VND", new BigDecimal("2000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));
    }

    private void seedCategories() {
        categoryRepository.saveAll(List.of(
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REV_CASH", "Doanh thu bán hàng - Tiền mặt", CategoryGroup.REVENUE, FlowDirection.INFLOW, true, true, "Tiền mặt thu từ khách hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "CAP_INJECTION", "Vốn góp chủ sở hữu", CategoryGroup.CAPITAL_FLOW, FlowDirection.INFLOW, true, true, "Vốn góp", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_RENT", "Chi phí thuê mặt bằng", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Tiền thuê mặt bằng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_UTILITIES", "Chi phí điện nước", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Điện nước", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_SUPPLIES", "Chi phí vật tư bao bì", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Ly, bao bì", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "OPEX_MAINTENANCE", "Chi phí bảo trì", CategoryGroup.OPERATING_EXPENSE, FlowDirection.OUTFLOW, true, true, "Sửa chữa", Instant.now())
        ));
    }

    private UserJpaEntity createUser(String login, String name) {
        return userRepository.save(new UserJpaEntity(
                UUID.randomUUID(), login, passwordEncoder.encode("TestPass123!"), name, UserStatus.ACTIVE, Instant.now()
        ));
    }

    @Test
    @Order(1)
    @DisplayName("Kiểm thử 1: Chi phí thanh toán ngay (1M tiền mặt, OPEX_SUPPLIES) -> Quỹ tiền mặt giảm 1M, sổ quỹ ghi OUTFLOW MANUAL_EXPENSE, review_flag = false")
    void test01_StandardPaidExpenseImmediately() throws Exception {
        CreateExpenseCommand command = new CreateExpenseCommand(
                LocalDate.of(2026, 11, 18),
                opexSupplies.getId(),
                new BigDecimal("1000000.00"),
                true,
                cashAccount.getId(),
                null,
                "Mua ly và ống hút nhựa",
                true
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.amount", is(1000000.00)))
                .andExpect(jsonPath("$.reviewFlag", is(false)))
                .andExpect(jsonPath("$.isPaid", is(true)))
                .andExpect(jsonPath("$.paymentAccountName", is("Quỹ tiền mặt chi nhánh")));

        // Verify cash account balance was deducted
        FinancialAccountJpaEntity updatedCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(updatedCash.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("9000000.00")); // 10M - 1M

        // Verify cash ledger entry was posted
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository
                .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(branchId, SourceType.MANUAL_EXPENSE,
                        expenseRepository.findAll().get(0).getId());
        assertThat(entries).hasSize(1);
        CashLedgerEntryJpaEntity entry = entries.get(0);
        assertThat(entry.getEntryType()).isEqualTo(EntryType.OUTFLOW);
        assertThat(entry.getAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(entry.getSignedAmount()).isEqualByComparingTo(new BigDecimal("-1000000.00"));
        assertThat(entry.getRunningBalance()).isEqualByComparingTo(new BigDecimal("9000000.00"));
        assertThat(entry.getStatus()).isEqualTo(LedgerStatus.POSTED);
    }

    @Test
    @Order(2)
    @DisplayName("Kiểm thử 2: Cảnh báo chi phí lớn (3.5M >= 2M threshold) -> review_flag = true")
    void test02_AbnormalThresholdAlert() throws Exception {
        CreateExpenseCommand command = new CreateExpenseCommand(
                LocalDate.of(2026, 11, 18),
                opexRent.getId(),
                new BigDecimal("3500000.00"),
                true,
                cashAccount.getId(),
                null,
                "Tiền thuê mặt bằng đợt 1",
                false // Save as draft first
        );

        String responseJson = mockMvc.perform(post("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.amount", is(3500000.00)))
                .andExpect(jsonPath("$.reviewFlag", is(true)))
                .andReturn().getResponse().getContentAsString();

        UUID expenseId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());

        // Now confirm it
        mockMvc.perform(post("/api/v1/branches/{branchId}/expenses/{id}/confirm", branchId, expenseId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.reviewFlag", is(true)));

        // Cash balance should be reduced: 10M - 3.5M = 6.5M
        FinancialAccountJpaEntity updatedCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(updatedCash.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("6500000.00"));
    }

    @Test
    @Order(3)
    @DisplayName("Kiểm thử 3: Chi phí ghi nợ (is_paid = false) -> Quỹ tiền mặt không đổi, mtms.payables được tạo")
    void test03_UnpaidExpenseOnCredit() throws Exception {
        BigDecimal cashBefore = cashAccount.getCurrentBalance();

        CreateExpenseCommand command = new CreateExpenseCommand(
                LocalDate.of(2026, 11, 18),
                opexMaintenance.getId(),
                new BigDecimal("5000000.00"),
                false,
                null,
                null,
                "Bảo dưỡng máy pha cafe trả chậm NCC",
                true
        );

        String responseJson = mockMvc.perform(post("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.isPaid", is(false)))
                .andExpect(jsonPath("$.payableId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        UUID payableId = UUID.fromString(objectMapper.readTree(responseJson).get("payableId").asText());

        // Verify cash account balance is completely untouched
        FinancialAccountJpaEntity cashAfter = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(cashAfter.getCurrentBalance()).isEqualByComparingTo(cashBefore);

        // Verify payable record was created in mtms.payables
        PayableJpaEntity payable = payableRepository.findById(payableId).orElseThrow();
        assertThat(payable.getBranchId()).isEqualTo(branchId);
        assertThat(payable.getBusinessEntityId()).isEqualTo(entityId);
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
        assertThat(payable.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(payable.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(payable.getSourceType()).isEqualTo("MANUAL_PAYABLE");
    }

    @Test
    @Order(4)
    @DisplayName("Kiểm thử 4: Hủy phiếu chi đã thanh toán -> Hoàn tiền lại quỹ, bút toán đảo REVERSED, status = VOIDED")
    void test04_VoidPaidExpenseRestoresFundsAndReversesLedger() throws Exception {
        // Create and confirm paid expense 1M
        CreateExpenseCommand command = new CreateExpenseCommand(
                LocalDate.of(2026, 11, 18),
                opexSupplies.getId(),
                new BigDecimal("1000000.00"),
                true,
                cashAccount.getId(),
                null,
                "Mua hóa chất tẩy rửa",
                true
        );

        String responseJson = mockMvc.perform(post("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andReturn().getResponse().getContentAsString();

        UUID expenseId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());

        // Balance was 10M - 1M = 9M
        FinancialAccountJpaEntity intermediateCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(intermediateCash.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("9000000.00"));

        // Void the expense
        VoidExpenseCommand voidCmd = new VoidExpenseCommand("Nhập nhầm hóa đơn chi");
        mockMvc.perform(post("/api/v1/branches/{branchId}/expenses/{id}/void", branchId, expenseId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("VOIDED")))
                .andExpect(jsonPath("$.voidReason", is("Nhập nhầm hóa đơn chi")));

        // Funds must be restored to 10M
        FinancialAccountJpaEntity restoredCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(restoredCash.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("10000000.00"));

        // Ledger entry must be REVERSED
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository
                .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(branchId, SourceType.MANUAL_EXPENSE, expenseId);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getStatus()).isEqualTo(LedgerStatus.REVERSED);
    }

    @Test
    @Order(5)
    @DisplayName("Kiểm thử 5: Danh mục không hợp lệ (CAPITAL_FLOW / REVENUE) -> 400 Bad Request urn:mtms:error:invalid-expense-category")
    void test05_CategoryRestriction() throws Exception {
        // Try with CAPITAL_FLOW
        CreateExpenseCommand cmdCapital = new CreateExpenseCommand(
                LocalDate.of(2026, 11, 18),
                capInjection.getId(),
                new BigDecimal("500000.00"),
                true,
                cashAccount.getId(),
                null,
                "Chi vốn góp",
                false
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmdCapital)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-expense-category")));

        // Try with REVENUE
        CreateExpenseCommand cmdRevenue = new CreateExpenseCommand(
                LocalDate.of(2026, 11, 18),
                revCash.getId(),
                new BigDecimal("200000.00"),
                true,
                cashAccount.getId(),
                null,
                "Chi doanh thu",
                false
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmdRevenue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-expense-category")));
    }

    @Test
    @Order(6)
    @DisplayName("Kiểm thử 6: Chi vượt số dư quỹ tiền mặt -> 400 Bad Request urn:mtms:error:insufficient-account-balance")
    void test06_InsufficientCashBalanceRejected() throws Exception {
        // Cash has 10M, try to spend 50M immediately
        CreateExpenseCommand command = new CreateExpenseCommand(
                LocalDate.of(2026, 11, 18),
                opexSupplies.getId(),
                new BigDecimal("50000000.00"),
                true,
                cashAccount.getId(),
                null,
                "Chi vật tư vượt quá tiền quỹ",
                true
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:insufficient-account-balance")));
    }

    @Test
    @Order(7)
    @DisplayName("Kiểm thử 7: Branch isolation -> 403 Forbidden")
    void test07_BranchIsolation() throws Exception {
        // Manager of branchId attempts to view expenses of otherBranchId
        mockMvc.perform(get("/api/v1/branches/{branchId}/expenses", otherBranchId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isForbidden());

        // Unauthorized user attempts to access branchId
        mockMvc.perform(get("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenUnauthorized))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("Kiểm thử 8: Cấu hình hạn mức chi phí (Config) & kiểm tra thay đổi ngưỡng cảnh báo")
    void test08_BranchExpenseConfigUpdate() throws Exception {
        // Default config: 2,000,000 alert threshold
        mockMvc.perform(get("/api/v1/branches/{branchId}/expenses/config", branchId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertThresholdAmount", is(2000000.00)))
                .andExpect(jsonPath("$.evidenceRequiredThreshold", is(500000.00)));

        // Update threshold to 4,000,000
        UpdateExpenseConfigCommand updateCmd = new UpdateExpenseConfigCommand(
                new BigDecimal("4000000.00"),
                new BigDecimal("800000.00")
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/expenses/config", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertThresholdAmount", is(4000000.00)))
                .andExpect(jsonPath("$.evidenceRequiredThreshold", is(800000.00)));

        // Now create expense of 3,000,000: with new threshold of 4M, reviewFlag must be FALSE!
        CreateExpenseCommand command = new CreateExpenseCommand(
                LocalDate.of(2026, 11, 18),
                opexSupplies.getId(),
                new BigDecimal("3000000.00"),
                true,
                cashAccount.getId(),
                null,
                "Mua nguyên vật liệu đợt 2",
                false
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/expenses", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewFlag", is(false)));
    }
}
