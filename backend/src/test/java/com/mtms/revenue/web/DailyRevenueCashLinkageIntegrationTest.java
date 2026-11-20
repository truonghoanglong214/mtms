package com.mtms.revenue.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.LedgerStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
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
import com.mtms.organization.domain.BankAccountStatus;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchBankAccountJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchBankAccountJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import com.mtms.revenue.application.dto.ConfirmRevenueCommand;
import com.mtms.revenue.application.dto.CreateDailyRevenueCommand;
import com.mtms.revenue.domain.RevenueReportStatus;
import com.mtms.revenue.infrastructure.persistence.DailyRevenueReportJpaEntity;
import com.mtms.revenue.infrastructure.persistence.DailyRevenueReportJpaRepository;
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
import org.springframework.test.web.servlet.MvcResult;

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

/**
 * P0-REV-05 / BR-REV-04 / TC-007 / TC-006 / TC-011
 * Integration test suite for Revenue Cash Linkage:
 * - TC-007: Confirmation of cash and bank transfer creates linked cash/bank ledger entries while
 *   delivery app net revenue NEVER touches bank accounts or cash drawers.
 * - TC-006 / TC-011: Idempotent confirmation retries prevent duplicate subledger postings.
 * - Revision adjustment: Old revision entries are marked REVERSED and account balance is properly adjusted.
 * - Branch isolation: 403 Forbidden for unauthorized access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DailyRevenueCashLinkageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DailyRevenueReportJpaRepository dailyRevenueReportRepository;

    @Autowired
    private FinancialAccountJpaRepository financialAccountRepository;

    @Autowired
    private FinancialCategoryJpaRepository categoryRepository;

    @Autowired
    private CashLedgerEntryJpaRepository ledgerEntryRepository;

    @Autowired
    private BranchBankAccountJpaRepository branchBankRepository;

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
    private UUID branchId;
    private UUID otherBranchId;
    private UserJpaEntity manager;
    private UserJpaEntity employee;
    private UserJpaEntity unauthorizedUser;

    private String tokenManager;
    private String tokenEmployee;
    private String tokenUnauthorized;

    private FinancialAccountJpaEntity cashAccount;
    private FinancialAccountJpaEntity bankAccount;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        financialAccountRepository.deleteAll();
        categoryRepository.deleteAll();
        branchBankRepository.deleteAll();
        dailyRevenueReportRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Business entity and branches
        entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_LINK_TEST", "Revenue Linkage Testing Entity", "0109999777", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR_LINK_01", "Chi nhánh Doanh Thu Linkage 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, entityId, "BR_LINK_02", "Chi nhánh Khác 2", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Users
        manager = createUser("manager_linkage", "Quản Lý Cửa Hàng");
        employee = createUser("barista_linkage", "Nhân Viên Thu Ngân");
        unauthorizedUser = createUser("unauthorized_linkage", "Người Không Có Quyền");

        // 3. Role grants
        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, manager.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(5), today.plusDays(30), manager.getId(), Instant.now()
        ));

        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, employee.getId(),
                "EMPLOYEE", "ALL", "ALL", "ALL",
                today.minusDays(5), today.plusDays(30), manager.getId(), Instant.now()
        ));

        tokenManager = jwtTokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());
        tokenEmployee = jwtTokenService.generateAccessToken(employee.getId(), employee.getLoginIdentifier());
        tokenUnauthorized = jwtTokenService.generateAccessToken(unauthorizedUser.getId(), unauthorizedUser.getLoginIdentifier());

        // 4. Seed system categories
        categoryRepository.saveAll(List.of(
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REV_CASH", "Doanh thu bán hàng - Tiền mặt", CategoryGroup.REVENUE, FlowDirection.INFLOW, true, true, "Tiền mặt thu từ khách hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REV_BANK", "Doanh thu bán hàng - Chuyển khoản", CategoryGroup.REVENUE, FlowDirection.INFLOW, true, true, "Tiền chuyển khoản ngân hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "TRF_INTERNAL", "Chuyển quỹ nội bộ", CategoryGroup.INTERNAL_TRANSFER, FlowDirection.BOTH, true, true, "Chuyển quỹ nội bộ", Instant.now())
        ));

        // 5. Seed initial branch bank and accounts: Cash with 1,000,000 VND, Bank with 2,000,000 VND
        cashAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, null, "Quỹ tiền mặt chi nhánh",
                AccountKind.CASH, "VND", new BigDecimal("1000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));

        BranchBankAccountJpaEntity branchBank = branchBankRepository.save(new BranchBankAccountJpaEntity(
                UUID.randomUUID(), branchId, "Vietcombank", "CTY TEST", "cipher_acc", "**** 9999", true, BankAccountStatus.ACTIVE, Instant.now()
        ));

        bankAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, branchBank.getId(), "Tài khoản Vietcombank (**** 9999)",
                AccountKind.BANK, "VND", new BigDecimal("2000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));
    }

    private UserJpaEntity createUser(String login, String name) {
        return userRepository.save(new UserJpaEntity(
                UUID.randomUUID(), login, passwordEncoder.encode("TestPass123!"), name, UserStatus.ACTIVE, Instant.now()
        ));
    }

    @Test
    @Order(1)
    @DisplayName("TC-007 Invariant: Confirm report with Cash 5M, Bank 3M, App Net 2M -> Cash +5M, Bank +3M, App Net NEVER in bank balance")
    void testTc007RevenueCashAndBankLinkageWithAppNetSeparation() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 17);

        // 1. Submit report with Cash 5M, Bank 3M, Delivery App Net 2M (Total 10M)
        CreateDailyRevenueCommand createCmd = new CreateDailyRevenueCommand(
                testDate,
                new BigDecimal("5000000.00"),
                new BigDecimal("3000000.00"),
                new BigDecimal("2000000.00"),
                BigDecimal.ZERO,
                new BigDecimal("10000000.00"),
                null,
                "Doanh thu tổng hợp ca 1 và ca 2",
                true // submit immediately
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue", branchId)
                        .header("Authorization", "Bearer " + tokenEmployee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.cashAmount", is("5000000.00")))
                .andExpect(jsonPath("$.bankTransferAmount", is("3000000.00")))
                .andExpect(jsonPath("$.deliveryAppNetAmount", is("2000000.00")))
                .andExpect(jsonPath("$.totalRevenue", is("10000000.00")))
                .andReturn();

        UUID reportId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        // Verify that BEFORE confirmation, balances are unchanged
        assertThat(financialAccountRepository.findById(cashAccount.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("1000000.00");
        assertThat(financialAccountRepository.findById(bankAccount.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo("2000000.00");
        assertThat(ledgerEntryRepository.findAll()).isEmpty();

        // 2. Confirm report as Manager
        ConfirmRevenueCommand confirmCmd = new ConfirmRevenueCommand(
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, reportId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // 3. Verify TC-007 Invariants:
        // Quỹ tiền mặt tăng đúng 5,000,000 (1M -> 6M)
        FinancialAccountJpaEntity updatedCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(updatedCash.getCurrentBalance()).isEqualByComparingTo("6000000.00");

        // Quỹ ngân hàng tăng đúng 3,000,000 (2M -> 5M)
        FinancialAccountJpaEntity updatedBank = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedBank.getCurrentBalance()).isEqualByComparingTo("5000000.00");

        // BẤT BIẾN TC-007: Tiền ngân hàng KHÔNG tăng thêm 2,000,000 của Delivery App Net!
        assertThat(updatedBank.getCurrentBalance()).isNotEqualByComparingTo("7000000.00");

        // Đúng 2 bút toán sổ quỹ được tạo (CASH_LEG và BANK_LEG), không có bút toán nào cho App Net
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository.findByBranchIdOrderByPostedAtDesc(branchId);
        assertThat(entries).hasSize(2);

        CashLedgerEntryJpaEntity cashEntry = entries.stream()
                .filter(e -> "CASH_LEG".equals(e.getSourceLeg()))
                .findFirst()
                .orElseThrow();
        assertThat(cashEntry.getEntryType()).isEqualTo(EntryType.INFLOW);
        assertThat(cashEntry.getAmount()).isEqualByComparingTo("5000000.00");
        assertThat(cashEntry.getRunningBalance()).isEqualByComparingTo("6000000.00");
        assertThat(cashEntry.getSourceType()).isEqualTo(SourceType.DAILY_REVENUE);
        assertThat(cashEntry.getSourceId()).isEqualTo(reportId);
        assertThat(cashEntry.getStatus()).isEqualTo(LedgerStatus.POSTED);
        assertThat(cashEntry.getMemo()).isEqualTo("Doanh thu tiền mặt ngày 2026-11-17");

        CashLedgerEntryJpaEntity bankEntry = entries.stream()
                .filter(e -> "BANK_LEG".equals(e.getSourceLeg()))
                .findFirst()
                .orElseThrow();
        assertThat(bankEntry.getEntryType()).isEqualTo(EntryType.INFLOW);
        assertThat(bankEntry.getAmount()).isEqualByComparingTo("3000000.00");
        assertThat(bankEntry.getRunningBalance()).isEqualByComparingTo("5000000.00");
        assertThat(bankEntry.getSourceType()).isEqualTo(SourceType.DAILY_REVENUE);
        assertThat(bankEntry.getSourceId()).isEqualTo(reportId);
        assertThat(bankEntry.getStatus()).isEqualTo(LedgerStatus.POSTED);
        assertThat(bankEntry.getMemo()).isEqualTo("Doanh thu chuyển khoản ngày 2026-11-17");

        // 4. Test REST Endpoint: GET /api/v1/branches/{branchId}/daily-revenue/{id}/ledger-entries
        mockMvc.perform(get("/api/v1/branches/{branchId}/daily-revenue/{id}/ledger-entries", branchId, reportId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sourceLeg", is("CASH_LEG")))
                .andExpect(jsonPath("$[0].amount", is(5000000.00)))
                .andExpect(jsonPath("$[0].categoryCode", is("REV_CASH")))
                .andExpect(jsonPath("$[0].status", is("POSTED")))
                .andExpect(jsonPath("$[1].sourceLeg", is("BANK_LEG")))
                .andExpect(jsonPath("$[1].amount", is(3000000.00)))
                .andExpect(jsonPath("$[1].categoryCode", is("REV_BANK")))
                .andExpect(jsonPath("$[1].status", is("POSTED")));
    }

    @Test
    @Order(2)
    @DisplayName("TC-006 / TC-011: Idempotency - Confirming already confirmed report does not duplicate ledger entries")
    void testTc006AndTc011IdempotentConfirmation() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 18);

        // 1. Create and confirm report with Cash 4M, Bank 2M
        DailyRevenueReportJpaEntity report = dailyRevenueReportRepository.save(new DailyRevenueReportJpaEntity(
                UUID.randomUUID(), entityId, branchId, testDate, 1, true,
                new BigDecimal("4000000.00"), new BigDecimal("2000000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("6000000.00"), RevenueReportStatus.SUBMITTED,
                employee.getId(), Instant.now(), null, null, null, null, "Test ca sáng", Instant.now(), Instant.now()
        ));

        ConfirmRevenueCommand confirmCmd = new ConfirmRevenueCommand(
                null, null, null, null, null, null, null, null
        );

        // First confirmation
        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, report.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        BigDecimal cashBalanceAfterFirst = financialAccountRepository.findById(cashAccount.getId()).orElseThrow().getCurrentBalance();
        BigDecimal bankBalanceAfterFirst = financialAccountRepository.findById(bankAccount.getId()).orElseThrow().getCurrentBalance();
        int ledgerCountAfterFirst = ledgerEntryRepository.findAll().size();

        // 2. Second confirmation retry (TC-006 / TC-011 Idempotency)
        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, report.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Verify balances did NOT double
        BigDecimal cashBalanceAfterSecond = financialAccountRepository.findById(cashAccount.getId()).orElseThrow().getCurrentBalance();
        BigDecimal bankBalanceAfterSecond = financialAccountRepository.findById(bankAccount.getId()).orElseThrow().getCurrentBalance();
        int ledgerCountAfterSecond = ledgerEntryRepository.findAll().size();

        assertThat(cashBalanceAfterSecond).isEqualByComparingTo(cashBalanceAfterFirst);
        assertThat(bankBalanceAfterSecond).isEqualByComparingTo(bankBalanceAfterFirst);
        assertThat(ledgerCountAfterSecond).isEqualTo(ledgerCountAfterFirst);
    }

    @Test
    @Order(3)
    @DisplayName("Revision Adjustment: Adjust report from Cash 5M to Cash 6M -> Old reversed, new posted, net cash +1M")
    void testRevisionAdjustmentReversalAndBalanceAdjustment() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 19);

        // Initial cash balance is 1,000,000
        BigDecimal initialCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow().getCurrentBalance();

        // 1. Submit and confirm original report with Cash 5M, Bank 0
        DailyRevenueReportJpaEntity originalReport = dailyRevenueReportRepository.save(new DailyRevenueReportJpaEntity(
                UUID.randomUUID(), entityId, branchId, testDate, 1, true,
                new BigDecimal("5000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("5000000.00"), RevenueReportStatus.SUBMITTED,
                employee.getId(), Instant.now(), null, null, null, null, "Báo cáo gốc", Instant.now(), Instant.now()
        ));

        ConfirmRevenueCommand firstConfirm = new ConfirmRevenueCommand(
                null, null, null, null, null, null, null, null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, originalReport.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstConfirm)))
                .andExpect(status().isOk());

        // Balance after first confirmation: initial + 5M
        assertThat(financialAccountRepository.findById(cashAccount.getId()).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo(initialCash.add(new BigDecimal("5000000.00")));

        // Verify original ledger entry is POSTED
        List<CashLedgerEntryJpaEntity> origEntries = ledgerEntryRepository
                .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(branchId, SourceType.DAILY_REVENUE, originalReport.getId());
        assertThat(origEntries).hasSize(1);
        assertThat(origEntries.get(0).getStatus()).isEqualTo(LedgerStatus.POSTED);

        // 2. Adjust report from Cash 5M to Cash 6M (Revision 2)
        ConfirmRevenueCommand adjustCmd = new ConfirmRevenueCommand(
                "Kiểm đếm lại két thực tế dư 1,000,000 VND",
                new BigDecimal("6000000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("6000000.00"),
                null,
                "Điều chỉnh ca"
        );

        MvcResult adjustResult = mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, originalReport.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision", is(2)))
                .andExpect(jsonPath("$.isCurrent", is(true)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.cashAmount", is("6000000.00")))
                .andExpect(jsonPath("$.totalRevenue", is("6000000.00")))
                .andReturn();

        UUID newReportId = UUID.fromString(objectMapper.readTree(adjustResult.getResponse().getContentAsString()).get("id").asText());
        assertThat(newReportId).isNotEqualTo(originalReport.getId());

        // 3. Verify net cash balance increased by exactly 1,000,000 (net increase from 5M to 6M)
        BigDecimal finalCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow().getCurrentBalance();
        assertThat(finalCash).isEqualByComparingTo(initialCash.add(new BigDecimal("6000000.00")));

        // 4. Verify old entry was REVERSED
        List<CashLedgerEntryJpaEntity> reversedEntries = ledgerEntryRepository
                .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(branchId, SourceType.DAILY_REVENUE, originalReport.getId());
        assertThat(reversedEntries).hasSize(1);
        assertThat(reversedEntries.get(0).getStatus()).isEqualTo(LedgerStatus.REVERSED);

        // 5. Verify new entry was POSTED with 6M
        List<CashLedgerEntryJpaEntity> newEntries = ledgerEntryRepository
                .findByBranchIdAndSourceTypeAndSourceIdOrderByPostedAtAsc(branchId, SourceType.DAILY_REVENUE, newReportId);
        assertThat(newEntries).hasSize(1);
        assertThat(newEntries.get(0).getStatus()).isEqualTo(LedgerStatus.POSTED);
        assertThat(newEntries.get(0).getAmount()).isEqualByComparingTo("6000000.00");

        // 6. Verify GET endpoint for old report returns entry with status REVERSED
        mockMvc.perform(get("/api/v1/branches/{branchId}/daily-revenue/{id}/ledger-entries", branchId, originalReport.getId())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("REVERSED")))
                .andExpect(jsonPath("$[0].amount", is(5000000.00)));

        // Verify GET endpoint for new report returns entry with status POSTED
        mockMvc.perform(get("/api/v1/branches/{branchId}/daily-revenue/{id}/ledger-entries", branchId, newReportId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("POSTED")))
                .andExpect(jsonPath("$[0].amount", is(6000000.00)));
    }

    @Test
    @Order(4)
    @DisplayName("Branch Isolation: Unauthorized user receives 403 Forbidden on ledger entries endpoint")
    void testBranchIsolationReturns403Forbidden() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 20);

        DailyRevenueReportJpaEntity report = dailyRevenueReportRepository.save(new DailyRevenueReportJpaEntity(
                UUID.randomUUID(), entityId, branchId, testDate, 1, true,
                new BigDecimal("2000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("2000000.00"), RevenueReportStatus.CONFIRMED,
                manager.getId(), Instant.now(), manager.getId(), Instant.now(), null, null, "Test isolation", Instant.now(), Instant.now()
        ));

        // 1. Unauthorized user attempting to access branch 1 -> 403 Forbidden
        mockMvc.perform(get("/api/v1/branches/{branchId}/daily-revenue/{id}/ledger-entries", branchId, report.getId())
                        .header("Authorization", "Bearer " + tokenUnauthorized))
                .andExpect(status().isForbidden());

        // 2. Manager of branch 1 attempting to access other branch -> 403 Forbidden
        mockMvc.perform(get("/api/v1/branches/{branchId}/daily-revenue/{id}/ledger-entries", otherBranchId, report.getId())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isForbidden());
    }
}
