package com.mtms.hr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.hr.application.dto.PayrollDtos.CreateAdjustmentCommand;
import com.mtms.hr.application.dto.PayrollDtos.GeneratePayrollCommand;
import com.mtms.hr.application.dto.PayrollDtos.PayrollPeriodDto;
import com.mtms.hr.domain.AdjustmentCode;
import com.mtms.hr.domain.EmployeeStatus;
import com.mtms.hr.domain.PayrollPeriodStatus;
import com.mtms.hr.domain.RateType;
import com.mtms.hr.domain.TimesheetSourceType;
import com.mtms.hr.domain.TimesheetStatus;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollAdjustmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollLineJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaRepository;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaEntity;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaRepository;
import com.mtms.hr.infrastructure.persistence.TimesheetEntryJpaEntity;
import com.mtms.hr.infrastructure.persistence.TimesheetEntryJpaRepository;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-HR-05 & P0-HR-06 Comprehensive Integration Test.
 * Authoritative: docs/api/contracts/P0-HR-05-06.md
 * Verifies:
 * - TC-013: Midmonth wage time-slicing (50h@22k + 50h@25k = 2,350,000 VND).
 * - TC-014: Monthly wage proration by standard workdays (7M * 13/26 = 3,500,000 VND).
 * - TC-015: Primary branch transfer on month-end allocation.
 * - Manual adjustments (bonus/penalty) updating gross salary.
 * - TC-017 / HR-INV-02: Manager approval denied (403), Owner approval accepted (200).
 * - Approved payroll period locking against recalculate or modifications (409 Conflict).
 * - Branch isolation (403 Forbidden).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayrollEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PayrollPeriodJpaRepository periodRepository;

    @Autowired
    private PayrollLineJpaRepository lineRepository;

    @Autowired
    private PayrollAdjustmentJpaRepository adjustmentRepository;

    @Autowired
    private TimesheetEntryJpaRepository timesheetRepository;

    @Autowired
    private SalaryPolicyJpaRepository salaryPolicyRepository;

    @Autowired
    private EmployeeAssignmentJpaRepository assignmentRepository;

    @Autowired
    private EmployeeJpaRepository employeeRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branch1Id;
    private UUID branch2Id;
    private UUID otherBranchId;

    private UserJpaEntity branchManagerUser;
    private UserJpaEntity branchOwnerUser;
    private UserJpaEntity otherBranchManagerUser;

    private String tokenManager;
    private String tokenOwner;
    private String tokenOther;

    @BeforeEach
    void setUp() {
        adjustmentRepository.deleteAll();
        lineRepository.deleteAll();
        periodRepository.deleteAll();
        timesheetRepository.deleteAll();
        salaryPolicyRepository.deleteAll();
        assignmentRepository.deleteAll();
        employeeRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Business Entities
        entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_HR_TEST", "MTMS Corp", "0109999888",
                BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        UUID otherEntityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                otherEntityId, "ENT_OTHER", "Other Corp", "0109999999",
                BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        // 2. Branches
        branch1Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branch1Id, entityId, "BR-HR-01", "MTMS Chi nhánh 1", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        branch2Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branch2Id, entityId, "BR-HR-02", "MTMS Chi nhánh 2", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, otherEntityId, "BR-OTHER", "Chi nhánh Khác", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        // 3. Users
        branchManagerUser = new UserJpaEntity(
                UUID.randomUUID(), "mgr_b1", "$2a$12$dummyhash", "Quản Lý CN1",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(branchManagerUser);

        branchOwnerUser = new UserJpaEntity(
                UUID.randomUUID(), "owner_b1", "$2a$12$dummyhash", "Chủ Quán CN1",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(branchOwnerUser);

        otherBranchManagerUser = new UserJpaEntity(
                UUID.randomUUID(), "mgr_other", "$2a$12$dummyhash", "Quản Lý Khác",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(otherBranchManagerUser);

        // 4. Role Grants
        LocalDate today = LocalDate.now();
        // Manager role for branch 1 and branch 2
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branch1Id, branchManagerUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), branchOwnerUser.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branch2Id, branchManagerUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), branchOwnerUser.getId(), Instant.now()
        ));

        // Branch Owner role for branch 1 and branch 2
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branch1Id, branchOwnerUser.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), branchOwnerUser.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branch2Id, branchOwnerUser.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), branchOwnerUser.getId(), Instant.now()
        ));

        // Other manager role for other branch
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), otherEntityId, otherBranchId, otherBranchManagerUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), otherBranchManagerUser.getId(), Instant.now()
        ));

        tokenManager = jwtTokenService.generateAccessToken(branchManagerUser.getId(), branchManagerUser.getLoginIdentifier());
        tokenOwner = jwtTokenService.generateAccessToken(branchOwnerUser.getId(), branchOwnerUser.getLoginIdentifier());
        tokenOther = jwtTokenService.generateAccessToken(otherBranchManagerUser.getId(), otherBranchManagerUser.getLoginIdentifier());
    }

    @Test
    @Order(1)
    @DisplayName("TC-013: Hourly employee with midmonth wage hike (50h@22k + 50h@25k = 2,350,000 VND)")
    void testHourlyWageMidmonthSlices_TC013() throws Exception {
        // 1. Create employee
        UUID empId = UUID.randomUUID();
        employeeRepository.save(new EmployeeJpaEntity(
                empId, null, "EMP-H-001", "Nguyễn Văn Giờ", "Barista",
                "0901234567", "gio@mtms.vn", LocalDate.of(2026, 11, 1), null,
                EmployeeStatus.ACTIVE, "Barista part-time", Instant.now(), Instant.now()
        ));

        // Primary assignment at branch 1 for the whole month
        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), empId, branch1Id, true,
                LocalDate.of(2026, 11, 1), null, "Primary CN1", Instant.now()
        ));

        // Salary policy 1: 22,000 VND from 2026-11-01 to 2026-11-15
        salaryPolicyRepository.save(new SalaryPolicyJpaEntity(
                UUID.randomUUID(), empId, branch1Id, RateType.HOURLY,
                BigDecimal.valueOf(22000.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15),
                "Mức lương thử việc 22k", branchOwnerUser.getId(), Instant.now()
        ));

        // Salary policy 2: 25,000 VND from 2026-11-16 onwards
        salaryPolicyRepository.save(new SalaryPolicyJpaEntity(
                UUID.randomUUID(), empId, branch1Id, RateType.HOURLY,
                BigDecimal.valueOf(25000.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 16), null,
                "Mức lương chính thức 25k", branchOwnerUser.getId(), Instant.now().plusSeconds(1)
        ));

        // Approved timesheets: 50h before Nov 16 (3000 minutes)
        // 5 shifts of 600 min = 3000 min
        for (int day = 1; day <= 5; day++) {
            timesheetRepository.save(new TimesheetEntryJpaEntity(
                    UUID.randomUUID(), empId, branch1Id, LocalDate.of(2026, 11, day),
                    600, 600, BigDecimal.ONE, TimesheetSourceType.MANUAL,
                    TimesheetStatus.APPROVED, "Ca 10h", branchManagerUser.getId(), Instant.now(),
                    "Approved", Instant.now(), Instant.now()
            ));
        }

        // Approved timesheets: 50h from Nov 16 (3000 minutes)
        // 5 shifts of 600 min = 3000 min
        for (int day = 16; day <= 20; day++) {
            timesheetRepository.save(new TimesheetEntryJpaEntity(
                    UUID.randomUUID(), empId, branch1Id, LocalDate.of(2026, 11, day),
                    600, 600, BigDecimal.ONE, TimesheetSourceType.MANUAL,
                    TimesheetStatus.APPROVED, "Ca 10h", branchManagerUser.getId(), Instant.now(),
                    "Approved", Instant.now(), Instant.now()
            ));
        }

        // 2. Generate payroll for 2026-11-01
        GeneratePayrollCommand cmd = new GeneratePayrollCommand(LocalDate.of(2026, 11, 1));
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.totalBaseSalary", is(2350000.00)))
                .andExpect(jsonPath("$.totalGrossSalary", is(2350000.00)))
                .andExpect(jsonPath("$.employeeCount", is(1)))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].staffCode", is("EMP-H-001")))
                .andExpect(jsonPath("$.lines[0].rateType", is("HOURLY")))
                .andExpect(jsonPath("$.lines[0].approvedMinutes", is(6000)))
                .andExpect(jsonPath("$.lines[0].baseSalary", is(2350000.00)))
                .andExpect(jsonPath("$.lines[0].grossSalary", is(2350000.00)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.slices", hasSize(2)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.slices[0].rate", is(22000.00)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.slices[0].hours", is(50.0)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.slices[0].amount", is(1100000.00)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.slices[1].rate", is(25000.00)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.slices[1].hours", is(50.0)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.slices[1].amount", is(1250000.00)));
    }

    @Test
    @Order(2)
    @DisplayName("TC-014: Monthly wage proration by standard workdays (7,000,000 VND * 13 / 26 = 3,500,000 VND)")
    void testMonthlyWageProration_TC014() throws Exception {
        UUID empId = UUID.randomUUID();
        employeeRepository.save(new EmployeeJpaEntity(
                empId, null, "EMP-M-001", "Trần Văn Tháng", "Cửa hàng phó",
                "0907777888", "thang@mtms.vn", LocalDate.of(2026, 11, 1), null,
                EmployeeStatus.ACTIVE, "Lương tháng", Instant.now(), Instant.now()
        ));

        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), empId, branch1Id, true,
                LocalDate.of(2026, 11, 1), null, "Primary CN1", Instant.now()
        ));

        salaryPolicyRepository.save(new SalaryPolicyJpaEntity(
                UUID.randomUUID(), empId, branch1Id, RateType.MONTHLY,
                BigDecimal.valueOf(7000000.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 1), null,
                "Lương tháng 7 triệu công chuẩn 26", branchOwnerUser.getId(), Instant.now()
        ));

        // 13 days approved payable_workdays (1.00 each)
        for (int day = 1; day <= 13; day++) {
            timesheetRepository.save(new TimesheetEntryJpaEntity(
                    UUID.randomUUID(), empId, branch1Id, LocalDate.of(2026, 11, day),
                    480, 480, BigDecimal.ONE, TimesheetSourceType.MANUAL,
                    TimesheetStatus.APPROVED, "1 công chuẩn", branchManagerUser.getId(), Instant.now(),
                    "Approved", Instant.now(), Instant.now()
            ));
        }

        // Generate payroll
        GeneratePayrollCommand cmd = new GeneratePayrollCommand(LocalDate.of(2026, 11, 1));
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.totalBaseSalary", is(3500000.00)))
                .andExpect(jsonPath("$.totalGrossSalary", is(3500000.00)))
                .andExpect(jsonPath("$.lines[0].staffCode", is("EMP-M-001")))
                .andExpect(jsonPath("$.lines[0].rateType", is("MONTHLY")))
                .andExpect(jsonPath("$.lines[0].payableWorkdays", is(13.0)))
                .andExpect(jsonPath("$.lines[0].baseSalary", is(3500000.00)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.standardWorkdays", is(26.00)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.payableWorkdays", is(13.00)))
                .andExpect(jsonPath("$.lines[0].calculationSnapshot.amount", is(3500000.00)));
    }

    @Test
    @Order(3)
    @DisplayName("TC-015: Primary branch transfer on Nov 21 allocated fully to Branch 2 on month-end")
    void testPrimaryBranchTransferMonthEndAllocation_TC015() throws Exception {
        UUID empId = UUID.randomUUID();
        employeeRepository.save(new EmployeeJpaEntity(
                empId, null, "EMP-T-001", "Lê Văn Chuyển", "Pha chế",
                "0909999111", "chuyen@mtms.vn", LocalDate.of(2026, 11, 1), null,
                EmployeeStatus.ACTIVE, "Chuyển chi nhánh", Instant.now(), Instant.now()
        ));

        // 1. Initial primary assignment at Branch 1: Nov 1 to Nov 20
        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), empId, branch1Id, true,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20), "Primary CN1", Instant.now()
        ));

        // 2. Transferred primary assignment to Branch 2: Nov 21 onwards (active on Nov 30)
        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), empId, branch2Id, true,
                LocalDate.of(2026, 11, 21), null, "Primary CN2", Instant.now()
        ));

        // Salary policy
        salaryPolicyRepository.save(new SalaryPolicyJpaEntity(
                UUID.randomUUID(), empId, branch2Id, RateType.HOURLY,
                BigDecimal.valueOf(25000.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 1), null,
                "Lương 25k/h", branchOwnerUser.getId(), Instant.now()
        ));

        // Timesheets during month (some at CN1, some at CN2)
        timesheetRepository.save(new TimesheetEntryJpaEntity(
                UUID.randomUUID(), empId, branch1Id, LocalDate.of(2026, 11, 10),
                300, 300, BigDecimal.valueOf(0.5), TimesheetSourceType.MANUAL,
                TimesheetStatus.APPROVED, "Làm tại CN1", branchManagerUser.getId(), Instant.now(),
                "Approved", Instant.now(), Instant.now()
        ));
        timesheetRepository.save(new TimesheetEntryJpaEntity(
                UUID.randomUUID(), empId, branch2Id, LocalDate.of(2026, 11, 25),
                300, 300, BigDecimal.valueOf(0.5), TimesheetSourceType.MANUAL,
                TimesheetStatus.APPROVED, "Làm tại CN2", branchManagerUser.getId(), Instant.now(),
                "Approved", Instant.now(), Instant.now()
        ));

        // Generate payroll for Branch 1 (CN1): should NOT contain EMP-T-001 because on Nov 30 primary is CN2
        GeneratePayrollCommand cmd = new GeneratePayrollCommand(LocalDate.of(2026, 11, 1));
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(0)))
                .andExpect(jsonPath("$.lines", hasSize(0)));

        // Generate payroll for Branch 2 (CN2): MUST contain EMP-T-001 bearing full November salary
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch2Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount", is(1)))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].staffCode", is("EMP-T-001")))
                .andExpect(jsonPath("$.lines[0].approvedMinutes", is(600)))
                .andExpect(jsonPath("$.lines[0].baseSalary", is(250000.00)))
                .andExpect(jsonPath("$.lines[0].grossSalary", is(250000.00)));
    }

    @Test
    @Order(4)
    @DisplayName("Manual adjustments: add bonus (+500k), penalty (-100k), and delete adjustment updates gross")
    void testManualAdjustmentsAddAndDelete() throws Exception {
        // Setup employee with 2,000,000 base salary
        UUID empId = UUID.randomUUID();
        employeeRepository.save(new EmployeeJpaEntity(
                empId, null, "EMP-ADJ-001", "Phạm Văn Thưởng Phạt", "Thu ngân",
                "0903333444", "adj@mtms.vn", LocalDate.of(2026, 11, 1), null,
                EmployeeStatus.ACTIVE, "Thu ngân", Instant.now(), Instant.now()
        ));

        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), empId, branch1Id, true,
                LocalDate.of(2026, 11, 1), null, "Primary CN1", Instant.now()
        ));

        salaryPolicyRepository.save(new SalaryPolicyJpaEntity(
                UUID.randomUUID(), empId, branch1Id, RateType.MONTHLY,
                BigDecimal.valueOf(4000000.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 1), null, "Lương tháng", branchOwnerUser.getId(), Instant.now()
        ));

        timesheetRepository.save(new TimesheetEntryJpaEntity(
                UUID.randomUUID(), empId, branch1Id, LocalDate.of(2026, 11, 1),
                480, 480, BigDecimal.valueOf(13.00), TimesheetSourceType.MANUAL,
                TimesheetStatus.APPROVED, "13 công", branchManagerUser.getId(), Instant.now(),
                "Approved", Instant.now(), Instant.now()
        ));

        // Generate draft payroll: base = 2,000,000
        GeneratePayrollCommand genCmd = new GeneratePayrollCommand(LocalDate.of(2026, 11, 1));
        MvcResult genResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBaseSalary", is(2000000.00)))
                .andExpect(jsonPath("$.totalGrossSalary", is(2000000.00)))
                .andReturn();

        PayrollPeriodDto periodDto = objectMapper.readValue(
                genResult.getResponse().getContentAsString(), PayrollPeriodDto.class
        );
        UUID lineId = periodDto.lines().get(0).id();

        // 1. Add BONUS +500,000
        CreateAdjustmentCommand bonusCmd = new CreateAdjustmentCommand(
                AdjustmentCode.BONUS, BigDecimal.valueOf(500000.00), "Thưởng doanh số chi nhánh"
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/adjustments", branch1Id, lineId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bonusCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustmentCode", is("BONUS")))
                .andExpect(jsonPath("$.signedAmount", is(500000.00)));

        // 2. Add PENALTY -100,000
        CreateAdjustmentCommand penaltyCmd = new CreateAdjustmentCommand(
                AdjustmentCode.PENALTY, BigDecimal.valueOf(-100000.00), "Phạt đi muộn 2 lần"
        );
        MvcResult penaltyResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/adjustments", branch1Id, lineId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(penaltyCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustmentCode", is("PENALTY")))
                .andExpect(jsonPath("$.signedAmount", is(-100000.00)))
                .andReturn();

        UUID penaltyAdjId = objectMapper.readTree(penaltyResult.getResponse().getContentAsString()).get("id").traverse(objectMapper).readValueAs(UUID.class);

        // Verify period and line gross: 2,000,000 + 500,000 - 100,000 = 2,400,000
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/payroll-periods/current", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("month", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAdjustments", is(400000.00)))
                .andExpect(jsonPath("$.totalGrossSalary", is(2400000.00)))
                .andExpect(jsonPath("$.lines[0].adjustmentsTotal", is(400000.00)))
                .andExpect(jsonPath("$.lines[0].grossSalary", is(2400000.00)))
                .andExpect(jsonPath("$.lines[0].adjustments", hasSize(2)));

        // 3. Delete PENALTY adjustment (-100,000 removed -> total adjustments = +500,000, gross = 2,500,000)
        mockMvc.perform(delete("/api/v1/branches/{branchId}/hr/payroll-adjustments/{adjId}", branch1Id, penaltyAdjId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/payroll-periods/current", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("month", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAdjustments", is(500000.00)))
                .andExpect(jsonPath("$.totalGrossSalary", is(2500000.00)))
                .andExpect(jsonPath("$.lines[0].adjustments", hasSize(1)));
    }

    @Test
    @Order(5)
    @DisplayName("TC-017 / HR-INV-02: Manager approval is rejected (403 Forbidden); Owner approval succeeds (200 OK)")
    void testManagerApprovalBlockedAndOwnerApprovalSucceeds_TC017() throws Exception {
        UUID empId = UUID.randomUUID();
        employeeRepository.save(new EmployeeJpaEntity(
                empId, null, "EMP-APP-001", "Vũ Duyệt Lương", "Pha chế",
                "0904444555", "duyet@mtms.vn", LocalDate.of(2026, 11, 1), null,
                EmployeeStatus.ACTIVE, "Pha chế", Instant.now(), Instant.now()
        ));

        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), empId, branch1Id, true,
                LocalDate.of(2026, 11, 1), null, "Primary CN1", Instant.now()
        ));

        salaryPolicyRepository.save(new SalaryPolicyJpaEntity(
                UUID.randomUUID(), empId, branch1Id, RateType.MONTHLY,
                BigDecimal.valueOf(5000000.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 1), null, "Lương tháng", branchOwnerUser.getId(), Instant.now()
        ));

        timesheetRepository.save(new TimesheetEntryJpaEntity(
                UUID.randomUUID(), empId, branch1Id, LocalDate.of(2026, 11, 1),
                480, 480, BigDecimal.valueOf(26.00), TimesheetSourceType.MANUAL,
                TimesheetStatus.APPROVED, "Đủ tháng", branchManagerUser.getId(), Instant.now(),
                "Approved", Instant.now(), Instant.now()
        ));

        // Create draft period
        MvcResult genResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GeneratePayrollCommand(LocalDate.of(2026, 11, 1)))))
                .andExpect(status().isOk())
                .andReturn();

        PayrollPeriodDto periodDto = objectMapper.readValue(genResult.getResponse().getContentAsString(), PayrollPeriodDto.class);
        UUID periodId = periodDto.id();

        // 1. Manager attempts to approve -> 403 Forbidden (SelfApprovalDeniedException)
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/{id}/approve", branch1Id, periodId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:self-approval-denied")));

        // 2. Owner approves -> 200 OK
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/{id}/approve", branch1Id, periodId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approvedBy", is(branchOwnerUser.getId().toString())))
                .andExpect(jsonPath("$.approvedAt").isNotEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("Approved payroll period is locked permanently: recalculation or modifications return 409 Conflict")
    void testApprovedPayrollPeriodIsLocked_409Conflict() throws Exception {
        UUID empId = UUID.randomUUID();
        employeeRepository.save(new EmployeeJpaEntity(
                empId, null, "EMP-LOCK-001", "Hoàng Bất Biến", "Pha chế",
                "0905555666", "lock@mtms.vn", LocalDate.of(2026, 11, 1), null,
                EmployeeStatus.ACTIVE, "Pha chế", Instant.now(), Instant.now()
        ));

        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), empId, branch1Id, true,
                LocalDate.of(2026, 11, 1), null, "Primary CN1", Instant.now()
        ));

        salaryPolicyRepository.save(new SalaryPolicyJpaEntity(
                UUID.randomUUID(), empId, branch1Id, RateType.MONTHLY,
                BigDecimal.valueOf(6000000.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 1), null, "Lương tháng", branchOwnerUser.getId(), Instant.now()
        ));

        timesheetRepository.save(new TimesheetEntryJpaEntity(
                UUID.randomUUID(), empId, branch1Id, LocalDate.of(2026, 11, 1),
                480, 480, BigDecimal.valueOf(26.00), TimesheetSourceType.MANUAL,
                TimesheetStatus.APPROVED, "Đủ tháng", branchManagerUser.getId(), Instant.now(),
                "Approved", Instant.now(), Instant.now()
        ));

        // Generate and approve
        MvcResult genResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch1Id)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GeneratePayrollCommand(LocalDate.of(2026, 11, 1)))))
                .andExpect(status().isOk())
                .andReturn();

        PayrollPeriodDto periodDto = objectMapper.readValue(genResult.getResponse().getContentAsString(), PayrollPeriodDto.class);
        UUID periodId = periodDto.id();
        UUID lineId = periodDto.lines().get(0).id();

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/{id}/approve", branch1Id, periodId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // 1. Attempt recalculate -> 409 Conflict
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch1Id)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GeneratePayrollCommand(LocalDate.of(2026, 11, 1)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:payroll-already-approved")));

        // 2. Attempt add adjustment -> 409 Conflict
        CreateAdjustmentCommand cmd = new CreateAdjustmentCommand(
                AdjustmentCode.BONUS, BigDecimal.valueOf(100000.00), "Thêm sau duyệt"
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/adjustments", branch1Id, lineId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:payroll-already-approved")));

        // 3. Attempt approve already approved period -> 409 Conflict
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/{id}/approve", branch1Id, periodId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:payroll-already-approved")));
    }

    @Test
    @Order(7)
    @DisplayName("Branch scoping isolation: Unauthorized manager receives 403 Forbidden")
    void testBranchScopingIsolation_403Forbidden() throws Exception {
        // User from other branch attempts to access Branch 1 payroll
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/payroll-periods", branch1Id)
                        .header("Authorization", "Bearer " + tokenOther))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));

        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/payroll-periods/current", branch1Id)
                        .header("Authorization", "Bearer " + tokenOther))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/generate", branch1Id)
                        .header("Authorization", "Bearer " + tokenOther)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GeneratePayrollCommand(LocalDate.of(2026, 11, 1)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));
    }
}
