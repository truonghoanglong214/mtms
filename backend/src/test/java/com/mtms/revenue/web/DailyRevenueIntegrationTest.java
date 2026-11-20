package com.mtms.revenue.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockDocumentJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockLotJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import com.mtms.revenue.application.dto.ConfirmRevenueCommand;
import com.mtms.revenue.application.dto.CreateDailyRevenueCommand;
import com.mtms.revenue.application.dto.SetBusinessDayCommand;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DailyRevenueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DailyRevenueReportJpaRepository dailyRevenueReportRepository;

    @Autowired
    private BusinessDayJpaRepository businessDayRepository;

    @Autowired
    private StockDocumentJpaRepository stockDocumentRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private StockLotJpaRepository stockLotRepository;

    @Autowired
    private StockBalanceJpaRepository stockBalanceRepository;

    @Autowired
    private CatalogItemJpaRepository catalogItemRepository;

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
    private UUID branchId;
    private UserJpaEntity manager;
    private UserJpaEntity employee;
    private UserJpaEntity unauthorizedUser;

    private String tokenManager;
    private String tokenEmployee;
    private String tokenUnauthorized;

    @BeforeEach
    void setUp() {
        dailyRevenueReportRepository.deleteAll();
        businessDayRepository.deleteAll();
        stockDocumentRepository.deleteAll();
        stockMovementRepository.deleteAll();
        stockLotRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        catalogItemRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Business entity and branch
        entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_REV_TEST", "Revenue Testing Co", "0109999888", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR_REV_01", "Chi nhánh Doanh Thu 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Users
        manager = createUser("manager_revenue", "Quản Lý Cửa Hàng");
        employee = createUser("barista_revenue", "Nhân Viên Thu Ngân");
        unauthorizedUser = createUser("unauthorized_revenue", "Khách Ngoài Lề");

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

        // 4. JWT Tokens
        tokenManager = jwtTokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());
        tokenEmployee = jwtTokenService.generateAccessToken(employee.getId(), employee.getLoginIdentifier());
        tokenUnauthorized = jwtTokenService.generateAccessToken(unauthorizedUser.getId(), unauthorizedUser.getLoginIdentifier());
    }

    private UserJpaEntity createUser(String login, String name) {
        UserJpaEntity user = new UserJpaEntity(
                UUID.randomUUID(),
                login,
                passwordEncoder.encode("TestPass123!"),
                name,
                UserStatus.ACTIVE,
                Instant.now()
        );
        return userRepository.save(user);
    }

    @Test
    @Order(1)
    @DisplayName("TC-004: Open business day with no report -> missing-dates returns the date")
    void testTc004MissingDatesReturnsOpenBusinessDayWithoutReport() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 16);

        // Explicitly set the day as OPEN
        SetBusinessDayCommand openCmd = new SetBusinessDayCommand(OperationStatus.OPEN, null);
        mockMvc.perform(put("/api/v1/branches/{branchId}/business-days/{date}", branchId, testDate)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationStatus", is("OPEN")));

        // Query missing dates on daily-revenue endpoint
        mockMvc.perform(get("/api/v1/branches/{branchId}/daily-revenue/missing-dates", branchId)
                        .param("startDate", testDate.toString())
                        .param("endDate", testDate.toString())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is(testDate.toString())));

        // Also query business-days missing-dates endpoint
        mockMvc.perform(get("/api/v1/branches/{branchId}/business-days/missing-dates", branchId)
                        .param("startDate", testDate.toString())
                        .param("endDate", testDate.toString())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is(testDate.toString())));
    }

    @Test
    @Order(2)
    @DisplayName("TC-005: Setting day to CLOSED with reason -> missing-dates does NOT return that date (exempt)")
    void testTc005ClosedDayExemptFromMissingDates() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 16);

        // Set day to CLOSED with a valid reason
        SetBusinessDayCommand closeCmd = new SetBusinessDayCommand(
                OperationStatus.CLOSED, "Bảo trì nâng cấp hệ thống điện nước quầy bar"
        );
        mockMvc.perform(put("/api/v1/branches/{branchId}/business-days/{date}", branchId, testDate)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationStatus", is("CLOSED")))
                .andExpect(jsonPath("$.closeReason", is("Bảo trì nâng cấp hệ thống điện nước quầy bar")));

        // Query missing dates: must be empty because CLOSED day is exempt
        mockMvc.perform(get("/api/v1/branches/{branchId}/daily-revenue/missing-dates", branchId)
                        .param("startDate", testDate.toString())
                        .param("endDate", testDate.toString())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(3)
    @DisplayName("TC-006: Submit and confirm daily report twice -> idempotent, no duplicate active report or double posting")
    void testTc006SubmitAndConfirmReportWithIdempotentRetry() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 16);

        // 1. Submit daily revenue report
        CreateDailyRevenueCommand createCmd = new CreateDailyRevenueCommand(
                testDate,
                new BigDecimal("3500000.00"),
                new BigDecimal("4200000.00"),
                new BigDecimal("2100000.00"),
                BigDecimal.ZERO,
                new BigDecimal("9800000.00"),
                null,
                "Doanh thu ca ngày thứ Hai",
                true // submit immediately
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue", branchId)
                        .header("Authorization", "Bearer " + tokenEmployee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.totalRevenue", is("9800000.00")))
                .andExpect(jsonPath("$.revision", is(1)))
                .andExpect(jsonPath("$.isCurrent", is(true)))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        UUID reportId = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());

        // 2. Confirm report as Manager
        ConfirmRevenueCommand confirmCmd = new ConfirmRevenueCommand(
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, reportId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.confirmedByName", is("Quản Lý Cửa Hàng")));

        // 3. Retry confirm second time (idempotent retry)
        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, reportId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.id", is(reportId.toString())));

        // Verify only 1 active report exists in DB
        List<DailyRevenueReportJpaEntity> reportsInDb = dailyRevenueReportRepository
                .findByBranchIdAndBusinessDateOrderByRevisionDesc(branchId, testDate);
        assertThat(reportsInDb).hasSize(1);
        assertThat(reportsInDb.get(0).getStatus()).isEqualTo(RevenueReportStatus.CONFIRMED);
        assertThat(reportsInDb.get(0).isCurrent()).isTrue();

        // 4. Missing dates query for this date must now be empty
        mockMvc.perform(get("/api/v1/branches/{branchId}/daily-revenue/missing-dates", branchId)
                        .param("startDate", testDate.toString())
                        .param("endDate", testDate.toString())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 5. Attempting to create duplicate report on same date -> 409 Conflict
        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue", branchId)
                        .header("Authorization", "Bearer " + tokenEmployee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:duplicate-daily-report")));
    }

    @Test
    @Order(4)
    @DisplayName("Validation: Discrepancy between totalRevenue and sum of components returns 400 Bad Request")
    void testValidationMathDiscrepancy() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 17);

        // 1000 + 2000 + 500 = 3500 != 9999
        CreateDailyRevenueCommand badMathCmd = new CreateDailyRevenueCommand(
                testDate,
                new BigDecimal("1000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                new BigDecimal("9999.00"),
                null,
                "Sai tổng tiền",
                false
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badMathCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-revenue-math")))
                .andExpect(jsonPath("$.title", is("Invalid Revenue Math")));
    }

    @Test
    @Order(5)
    @DisplayName("Validation: Setting day to CLOSED without closeReason returns 400 Bad Request")
    void testValidationMissingCloseReason() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 18);

        SetBusinessDayCommand missingReasonCmd = new SetBusinessDayCommand(OperationStatus.CLOSED, "   ");

        mockMvc.perform(put("/api/v1/branches/{branchId}/business-days/{date}", branchId, testDate)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingReasonCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:missing-close-reason")))
                .andExpect(jsonPath("$.title", is("Missing Close Reason")));
    }

    @Test
    @Order(6)
    @DisplayName("Security: Access without branch grant returns 403 Forbidden")
    void testSecurityUnauthorizedAccess() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 19);

        // Unauthorized user attempts to read business days
        mockMvc.perform(get("/api/v1/branches/{branchId}/business-days", branchId)
                        .header("Authorization", "Bearer " + tokenUnauthorized))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));

        // Unauthorized user attempts to create report
        CreateDailyRevenueCommand cmd = new CreateDailyRevenueCommand(
                testDate,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                null,
                "Test",
                false
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue", branchId)
                        .header("Authorization", "Bearer " + tokenUnauthorized)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));
    }

    @Test
    @Order(7)
    @DisplayName("Revision Management: Owner adjustment to confirmed report creates new revision and supersedes old")
    void testAdjustConfirmedReportCreatesNewRevision() throws Exception {
        LocalDate testDate = LocalDate.of(2026, 11, 19);

        // 1. Create and confirm report revision 1
        CreateDailyRevenueCommand initialCmd = new CreateDailyRevenueCommand(
                testDate,
                new BigDecimal("1000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                new BigDecimal("3500.00"),
                null,
                "Initial report",
                true
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialCmd)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID reportId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, reportId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk());

        // 2. Owner adjusts numbers (adds 500k cash)
        ConfirmRevenueCommand adjustCmd = new ConfirmRevenueCommand(
                "Kiểm đếm tiền két thực tế dư 500.000đ",
                new BigDecimal("1500.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                new BigDecimal("4000.00"),
                null,
                "Updated note"
        );

        MvcResult adjustResult = mockMvc.perform(post("/api/v1/branches/{branchId}/daily-revenue/{id}/confirm", branchId, reportId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision", is(2)))
                .andExpect(jsonPath("$.isCurrent", is(true)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.cashAmount", is("1500.00")))
                .andExpect(jsonPath("$.totalRevenue", is("4000.00")))
                .andExpect(jsonPath("$.adjustmentReason", is("Kiểm đếm tiền két thực tế dư 500.000đ")))
                .andReturn();

        UUID newReportId = UUID.fromString(objectMapper.readTree(adjustResult.getResponse().getContentAsString()).get("id").asText());
        assertThat(newReportId).isNotEqualTo(reportId);

        // 3. Verify in repository: old revision is SUPERSEDED, not current
        DailyRevenueReportJpaEntity oldReport = dailyRevenueReportRepository.findById(reportId).orElseThrow();
        assertThat(oldReport.getStatus()).isEqualTo(RevenueReportStatus.SUPERSEDED);
        assertThat(oldReport.isCurrent()).isFalse();

        // New revision is CONFIRMED, current
        DailyRevenueReportJpaEntity newReport = dailyRevenueReportRepository.findById(newReportId).orElseThrow();
        assertThat(newReport.getStatus()).isEqualTo(RevenueReportStatus.CONFIRMED);
        assertThat(newReport.isCurrent()).isTrue();
        assertThat(newReport.getRevision()).isEqualTo(2);
    }
}
