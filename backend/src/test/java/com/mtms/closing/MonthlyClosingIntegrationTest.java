package com.mtms.closing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.closing.application.dto.ClosingDtos.ClosePeriodCommand;
import com.mtms.closing.infrastructure.persistence.SpringDataCloseComponentRepository;
import com.mtms.closing.infrastructure.persistence.SpringDataMonthlyCloseRepository;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.ExpenseStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.ExpenseJpaEntity;
import com.mtms.finance.infrastructure.persistence.ExpenseJpaRepository;
import com.mtms.hr.domain.PayrollPeriodStatus;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.domain.StockMovementType;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import com.mtms.revenue.domain.OperationStatus;
import com.mtms.revenue.domain.RevenueReportStatus;
import com.mtms.revenue.infrastructure.persistence.BusinessDayJpaEntity;
import com.mtms.revenue.infrastructure.persistence.BusinessDayJpaRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MonthlyClosingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataMonthlyCloseRepository monthlyCloseRepository;

    @Autowired
    private SpringDataCloseComponentRepository closeComponentRepository;

    @Autowired
    private DailyRevenueReportJpaRepository dailyRevenueReportRepository;

    @Autowired
    private BusinessDayJpaRepository businessDayRepository;

    @Autowired
    private PayrollPeriodJpaRepository payrollPeriodRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository catalogItemRepository;

    @Autowired
    private com.mtms.inventory.infrastructure.persistence.UnitJpaRepository unitRepository;

    @Autowired
    private ExpenseJpaRepository expenseRepository;

    @Autowired
    private CashLedgerEntryJpaRepository cashLedgerEntryRepository;

    @Autowired
    private BusinessEntityJpaRepository entityRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID entityId;
    private UUID branchIdA;
    private UUID branchIdB;
    private UUID ownerUserId;
    private String ownerToken;
    private String otherToken;

    private final LocalDate monthStart = LocalDate.of(2026, 11, 1);

    private DailyRevenueReportJpaEntity createReport(LocalDate date, BigDecimal cash, BigDecimal bank, BigDecimal app) {
        BigDecimal total = cash.add(bank).add(app);
        return new DailyRevenueReportJpaEntity(
                UUID.randomUUID(), entityId, branchIdA, date, 1, true,
                cash, bank, app, BigDecimal.ZERO, total, RevenueReportStatus.CONFIRMED,
                ownerUserId, Instant.now(), ownerUserId, Instant.now(), null, null, "Báo cáo ngày",
                Instant.now(), Instant.now()
        );
    }

    @BeforeEach
    void setUp() {
        closeComponentRepository.deleteAll();
        monthlyCloseRepository.deleteAll();
        dailyRevenueReportRepository.deleteAll();
        businessDayRepository.deleteAll();
        payrollPeriodRepository.deleteAll();
        stockMovementRepository.deleteAll();
        catalogItemRepository.deleteAll();
        expenseRepository.deleteAll();
        cashLedgerEntryRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        entityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Business Entity
        entityId = UUID.randomUUID();
        entityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_CLS_" + UUID.randomUUID().toString().substring(0, 8),
                "Closing Test Entity", "0109999111",
                BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        // 2. Create Branch A
        branchIdA = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchIdA, entityId, "BR_CLS_A_" + UUID.randomUUID().toString().substring(0, 8),
                "Chi nhánh Quận 1", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        // 3. Create Branch B
        branchIdB = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchIdB, entityId, "BR_CLS_B_" + UUID.randomUUID().toString().substring(0, 8),
                "Chi nhánh Quận 3", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        // 4. Create Owner user for Branch A
        UserJpaEntity owner = new UserJpaEntity(
                UUID.randomUUID(), "cls_owner_" + UUID.randomUUID().toString().substring(0, 8),
                passwordEncoder.encode("Password@123"), "Chủ Quán Chi Nhánh A",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(owner);
        ownerUserId = owner.getId();

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchIdA, owner.getId(),
                "OWNER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), owner.getId(), Instant.now()
        ));
        ownerToken = tokenService.generateAccessToken(owner.getId(), owner.getLoginIdentifier());

        // 5. Create Other user granted only to Branch B
        UserJpaEntity otherUser = new UserJpaEntity(
                UUID.randomUUID(), "other_user_" + UUID.randomUUID().toString().substring(0, 8),
                passwordEncoder.encode("Password@123"), "Other User",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(otherUser);

        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchIdB, otherUser.getId(),
                "OWNER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), otherUser.getId(), Instant.now()
        ));
        otherToken = tokenService.generateAccessToken(otherUser.getId(), otherUser.getLoginIdentifier());
    }

    @Test
    @Order(1)
    @DisplayName("P0-CLS-01 / TC-040: Prerequisites check returns missing revenue and draft payroll blocking close")
    void testPrerequisitesCheckFailsWhenIncomplete() throws Exception {
        // Setup only 1 confirmed revenue report for a 30-day month (November 2026)
        dailyRevenueReportRepository.save(createReport(
                monthStart,
                new BigDecimal("5000000.00"),
                new BigDecimal("3000000.00"),
                new BigDecimal("2000000.00")
        ));

        // Payroll period in DRAFT
        payrollPeriodRepository.save(new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchIdA, monthStart, 1, true,
                PayrollPeriodStatus.DRAFT, new BigDecimal("35000000.00"), new BigDecimal("30000000.00"),
                new BigDecimal("5000000.00"), 5, "Bảng lương nháp", null, null, Instant.now(), Instant.now()
        ));

        mockMvc.perform(get("/api/v1/branches/{branchId}/closing/prerequisites", branchIdA)
                        .param("month", "2026-11-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canClose").value(false))
                .andExpect(jsonPath("$.revenueCheck.passed").value(false))
                .andExpect(jsonPath("$.revenueCheck.reportedDays").value(1))
                .andExpect(jsonPath("$.revenueCheck.totalDaysInMonth").value(30))
                .andExpect(jsonPath("$.payrollCheck.passed").value(false))
                .andExpect(jsonPath("$.payrollCheck.status").value("DRAFT"))
                .andExpect(jsonPath("$.blockReason", containsString("chưa nộp báo cáo doanh thu")))
                .andExpect(jsonPath("$.blockReason", containsString("chưa được phê duyệt")));
    }

    @Test
    @Order(2)
    @DisplayName("P0-CLS-01 / TC-040: Close request is rejected with 400 Bad Request when prerequisites not met")
    void testClosePeriodFailsWhenPrerequisitesNotMet() throws Exception {
        ClosePeriodCommand command = new ClosePeriodCommand(monthStart, "Cố gắng chốt sổ");

        mockMvc.perform(post("/api/v1/branches/{branchId}/closing/close", branchIdA)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:closing-prerequisite-failed"))
                .andExpect(jsonPath("$.title").value("Closing Prerequisite Failed"));
    }

    @Test
    @Order(3)
    @DisplayName("P0-CLS-02 / TC-016 & TC-051: Managerial P&L query returns accrual payroll and TC-051 depreciation disclaimer")
    void testManagerialPnLQuery() throws Exception {
        // Seed 1 confirmed report: Revenue = 15,000,000 (Cash 5M, Bank 7M, App Net 3M)
        dailyRevenueReportRepository.save(createReport(
                monthStart,
                new BigDecimal("5000000.00"),
                new BigDecimal("7000000.00"),
                new BigDecimal("3000000.00")
        ));

        // Seed CatalogItem and Unit for COGS
        var unit = unitRepository.findAll().stream().findFirst().orElseGet(() ->
                unitRepository.save(new com.mtms.inventory.infrastructure.persistence.UnitJpaEntity(
                        UUID.randomUUID(), "KG", "Kilogram", com.mtms.inventory.domain.UnitDimension.MASS, Instant.now()
                ))
        );
        UUID itemId = UUID.randomUUID();
        catalogItemRepository.save(new com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity(
                itemId, entityId, "ITEM-CF-001", "Cà phê hạt Robusta",
                unit.getId(), com.mtms.inventory.domain.ItemKind.INGREDIENT,
                false, false, com.mtms.inventory.domain.ItemStatus.ACTIVE,
                Instant.now(), Instant.now()
        ));

        // Seed COGS consumption movement: 4,000,000
        stockMovementRepository.save(new StockMovementJpaEntity(
                UUID.randomUUID(), entityId, branchIdA, itemId, null, monthStart,
                StockMovementType.MANUAL_ISSUE, new BigDecimal("-20.000"), new BigDecimal("-4000000.00"),
                new BigDecimal("200000.000000"), "STOCK_DOC", UUID.randomUUID(), "SD-001",
                ownerUserId, Instant.now()
        ));

        // Seed Payroll: 5,000,000
        payrollPeriodRepository.save(new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchIdA, monthStart, 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("5000000.00"), new BigDecimal("5000000.00"),
                BigDecimal.ZERO, 2, "Lương tháng 11", ownerUserId, Instant.now(), Instant.now(), Instant.now()
        ));

        // Seed OPEX expense: 1,500,000
        expenseRepository.save(new ExpenseJpaEntity(
                UUID.randomUUID(), branchIdA, "EXP-001", monthStart, UUID.randomUUID(),
                new BigDecimal("1500000.00"), true, null, null, null,
                ExpenseStatus.CONFIRMED, false, "Mua vật dụng pha chế",
                ownerUserId, ownerUserId, Instant.now(), null, null, null, Instant.now()
        ));

        // Seed Cash Ledger entry: Cash Inflow 5,000,000
        cashLedgerEntryRepository.save(new CashLedgerEntryJpaEntity(
                UUID.randomUUID(), UUID.randomUUID(), branchIdA, monthStart,
                EntryType.INFLOW, new BigDecimal("5000000.00"), new BigDecimal("5000000.00"),
                new BigDecimal("5000000.00"), UUID.randomUUID(), SourceType.DAILY_REVENUE,
                UUID.randomUUID(), "MAIN", com.mtms.finance.domain.LedgerStatus.POSTED,
                null, "Thu tiền mặt ngày 1", null, ownerUserId, Instant.now()
        ));

        mockMvc.perform(get("/api/v1/branches/{branchId}/closing/pnl", branchIdA)
                        .param("month", "2026-11-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isProvisional").value(true))
                .andExpect(jsonPath("$.depreciationDisclaimer", containsString("TC-051 / ADR-023")))
                .andExpect(jsonPath("$.netRevenue.total").value(15000000.00))
                .andExpect(jsonPath("$.netRevenue.cashRevenue").value(5000000.00))
                .andExpect(jsonPath("$.netRevenue.bankRevenue").value(7000000.00))
                .andExpect(jsonPath("$.netRevenue.appNetRevenue").value(3000000.00))
                .andExpect(jsonPath("$.costOfGoodsSold.total").value(4000000.00))
                .andExpect(jsonPath("$.grossProfit").value(11000000.00))
                .andExpect(jsonPath("$.operatingExpenses.payrollAccrual").value(5000000.00))
                .andExpect(jsonPath("$.operatingExpenses.otherOpex").value(1500000.00))
                .andExpect(jsonPath("$.operatingExpenses.totalOpex").value(6500000.00))
                .andExpect(jsonPath("$.managerialOperatingProfit").value(4500000.00))
                .andExpect(jsonPath("$.cashFlowComparison.cashInflow").value(5000000.00))
                .andExpect(jsonPath("$.cashFlowComparison.netCashFlow").value(5000000.00))
                .andExpect(jsonPath("$.cashFlowComparison.accrualCashVariance").value(-500000.00));
    }

    @Test
    @Order(4)
    @DisplayName("P0-CLS-03 / TC-041: Owner closes month when prerequisites pass, creating immutable JSONB snapshot and components")
    void testOwnerClosesMonthSuccessfully() throws Exception {
        // Setup all 30 days as confirmed (or closed)
        LocalDate curr = monthStart;
        for (int i = 0; i < 30; i++) {
            dailyRevenueReportRepository.save(createReport(
                    curr,
                    new BigDecimal("1000000.00"),
                    new BigDecimal("1000000.00"),
                    new BigDecimal("500000.00")
            ));
            curr = curr.plusDays(1);
        }

        // Setup approved payroll
        payrollPeriodRepository.save(new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchIdA, monthStart, 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("25000000.00"), new BigDecimal("25000000.00"),
                BigDecimal.ZERO, 4, "Lương tháng 11 đã duyệt", ownerUserId, Instant.now(), Instant.now(), Instant.now()
        ));

        ClosePeriodCommand command = new ClosePeriodCommand(monthStart, "Chốt sổ tháng 11/2026 chính thức");

        mockMvc.perform(post("/api/v1/branches/{branchId}/closing/close", branchIdA)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.isProvisional").value(true))
                .andExpect(jsonPath("$.closedBy").value(ownerUserId.toString()))
                .andExpect(jsonPath("$.components", hasSize(12)));

        // Verify status endpoint
        mockMvc.perform(get("/api/v1/branches/{branchId}/closing/status", branchIdA)
                        .param("month", "2026-11-01")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.isClosed").value(true));

        // Verify history endpoint
        mockMvc.perform(get("/api/v1/branches/{branchId}/closing/history", branchIdA)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("CLOSED"));
    }

    @Test
    @Order(5)
    @DisplayName("P0-CLS-03 / TC-041: Duplicate close on already closed month returns 409 Conflict")
    void testDuplicateCloseReturnsConflict() throws Exception {
        // Setup all 30 days
        LocalDate curr = monthStart;
        for (int i = 0; i < 30; i++) {
            dailyRevenueReportRepository.save(createReport(
                    curr,
                    new BigDecimal("1000000.00"),
                    new BigDecimal("1000000.00"),
                    BigDecimal.ZERO
            ));
            curr = curr.plusDays(1);
        }

        payrollPeriodRepository.save(new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchIdA, monthStart, 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("20000000.00"), new BigDecimal("20000000.00"),
                BigDecimal.ZERO, 3, "Lương", ownerUserId, Instant.now(), Instant.now(), Instant.now()
        ));

        ClosePeriodCommand command = new ClosePeriodCommand(monthStart, "Chốt lần 1");
        mockMvc.perform(post("/api/v1/branches/{branchId}/closing/close", branchIdA)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk());

        // Second attempt on same month must return 409 Conflict
        mockMvc.perform(post("/api/v1/branches/{branchId}/closing/close", branchIdA)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:month-already-closed"))
                .andExpect(jsonPath("$.title").value("Month Already Closed"));
    }

    @Test
    @Order(6)
    @DisplayName("TC-001: Branch isolation - User from Branch B cannot access Branch A closing endpoints")
    void testBranchIsolationSecurity() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/closing/prerequisites", branchIdA)
                        .param("month", "2026-11-01")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}
