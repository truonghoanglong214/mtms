package com.mtms.hr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.hr.application.dto.HrDtos.CreateAssignmentCommand;
import com.mtms.hr.application.dto.HrDtos.CreateEmployeeCommand;
import com.mtms.hr.application.dto.HrDtos.CreateSalaryPolicyCommand;
import com.mtms.hr.application.dto.HrDtos.CreatedEmployeeResponse;
import com.mtms.hr.application.dto.HrDtos.InitialSalaryPolicyCommand;
import com.mtms.hr.application.dto.HrDtos.UpdateEmployeeCommand;
import com.mtms.hr.domain.EmployeeStatus;
import com.mtms.hr.domain.RateType;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaEntity;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaRepository;
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

/**
 * P0-HR-01 & P0-HR-02 / BR-HR-01 / BR-HR-03 / TC-013 / TC-015
 * Integration test verifying workforce employee directory, branch assignment tracking (TC-015),
 * and effective wage/salary policy time-slicing (TC-013).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmployeeAndSalaryPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeJpaRepository employeeRepository;

    @Autowired
    private EmployeeAssignmentJpaRepository assignmentRepository;

    @Autowired
    private SalaryPolicyJpaRepository salaryPolicyRepository;

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
    private UserJpaEntity branchManager;
    private UserJpaEntity otherManager;
    private String tokenBranch1;
    private String tokenOtherBranch;

    @BeforeEach
    void setUp() {
        salaryPolicyRepository.deleteAll();
        assignmentRepository.deleteAll();
        employeeRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Entities
        entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_HR_TEST", "MTMS Workforce Corp", "0109999888",
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
                branch1Id, entityId, "BR-HR-01", "MTMS Quận 1", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        branch2Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branch2Id, entityId, "BR-HR-02", "MTMS Quận 3", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, otherEntityId, "BR-OTHER", "Chi nhánh Khác", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        // 3. Users & Grants
        branchManager = new UserJpaEntity(
                UUID.randomUUID(), "mgr_q1", "$2a$12$dummyhash", "Manager Q1",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(branchManager);

        otherManager = new UserJpaEntity(
                UUID.randomUUID(), "mgr_other", "$2a$12$dummyhash", "Manager Other",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(otherManager);

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branch1Id, branchManager.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), branchManager.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branch2Id, branchManager.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), branchManager.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), otherEntityId, otherBranchId, otherManager.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), otherManager.getId(), Instant.now()
        ));

        tokenBranch1 = jwtTokenService.generateAccessToken(branchManager.getId(), branchManager.getLoginIdentifier());
        tokenOtherBranch = jwtTokenService.generateAccessToken(otherManager.getId(), otherManager.getLoginIdentifier());
    }

    @Test
    @Order(1)
    @DisplayName("Create employee: auto-creates primary assignment and initial salary policy")
    void testCreateEmployeeWithDefaultAssignmentAndSalaryPolicy() throws Exception {
        InitialSalaryPolicyCommand initialPolicy = new InitialSalaryPolicyCommand(
                RateType.HOURLY,
                BigDecimal.valueOf(22000.00),
                BigDecimal.valueOf(26.00),
                15,
                LocalDate.of(2026, 11, 1),
                "Lương thử việc ban đầu"
        );

        CreateEmployeeCommand command = new CreateEmployeeCommand(
                "EMP-001",
                "Nguyễn Văn A",
                "Pha chế (Barista)",
                "0987654321",
                "nva@milktea.vn",
                LocalDate.of(2026, 11, 1),
                null,
                "Nhân viên part-time",
                initialPolicy
        );

        MvcResult result = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.staffCode", is("EMP-001")))
                .andExpect(jsonPath("$.fullName", is("Nguyễn Văn A")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn();

        CreatedEmployeeResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CreatedEmployeeResponse.class
        );

        // Verify Employee in DB
        EmployeeJpaEntity employee = employeeRepository.findById(response.id()).orElseThrow();
        assertThat(employee.getStaffCode()).isEqualTo("EMP-001");
        assertThat(employee.getHiredOn()).isEqualTo(LocalDate.of(2026, 11, 1));

        // Verify default primary assignment
        List<EmployeeAssignmentJpaEntity> assignments = assignmentRepository.findByEmployeeIdAndBranchId(response.id(), branch1Id);
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).isPrimary()).isTrue();
        assertThat(assignments.get(0).getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(assignments.get(0).getEffectiveTo()).isNull();

        // Verify initial salary policy
        List<SalaryPolicyJpaEntity> policies = salaryPolicyRepository.findEffectivePolicies(
                response.id(), branch1Id, LocalDate.of(2026, 11, 1)
        );
        assertThat(policies).hasSize(1);
        assertThat(policies.get(0).getRateType()).isEqualTo(RateType.HOURLY);
        assertThat(policies.get(0).getRateAmount()).isEqualByComparingTo("22000.00");
    }

    @Test
    @Order(2)
    @DisplayName("Reject duplicate staff_code with 409 Conflict")
    void testStaffCodeConflictRejection() throws Exception {
        CreateEmployeeCommand command1 = new CreateEmployeeCommand(
                "EMP-001", "Nguyễn Văn A", "Barista", null, null,
                LocalDate.of(2026, 11, 1), null, null, null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command1)))
                .andExpect(status().isCreated());

        CreateEmployeeCommand duplicate = new CreateEmployeeCommand(
                "EMP-001", "Trùng Mã Nhân Viên", "Thu ngân", null, null,
                LocalDate.of(2026, 11, 1), null, null, null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:staff-code-conflict")));
    }

    @Test
    @Order(3)
    @DisplayName("Reject invalid dates when leftOn is before hiredOn with 400 Bad Request")
    void testRejectInvalidDatesLeftOnBeforeHiredOn() throws Exception {
        CreateEmployeeCommand command = new CreateEmployeeCommand(
                "EMP-BAD-DATE", "Lê Văn Sai", "Barista", null, null,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 31), null, null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-employee-dates")));
    }

    @Test
    @Order(4)
    @DisplayName("TC-015: Transfer primary branch on date D, auto-closes previous primary assignment")
    void testTC015_PrimaryBranchTransfer() throws Exception {
        // 1. Create employee in Branch 1 from 2026-11-01
        CreateEmployeeCommand createCmd = new CreateEmployeeCommand(
                "EMP-TC015", "Vũ Hoàng Minh", "Barista", "0911223344", null,
                LocalDate.of(2026, 11, 1), null, "Thử việc tại Q1", null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn();

        CreatedEmployeeResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CreatedEmployeeResponse.class
        );
        UUID empId = created.id();

        // 2. Transfer primary branch to Branch 2 starting 2026-11-21
        CreateAssignmentCommand assignCmd = new CreateAssignmentCommand(
                branch2Id,
                true,
                LocalDate.of(2026, 11, 21),
                null,
                "Điều chuyển chi nhánh chính sang MTMS Quận 3 từ 21/11"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees/{id}/assignments", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.branchId", is(branch2Id.toString())))
                .andExpect(jsonPath("$.isPrimary", is(true)))
                .andExpect(jsonPath("$.effectiveFrom", is("2026-11-21")))
                .andExpect(jsonPath("$.effectiveTo").doesNotExist());

        // 3. Verify TC-015 invariants in DB
        List<EmployeeAssignmentJpaEntity> allAssignments =
                assignmentRepository.findByEmployeeIdOrderByEffectiveFromAscCreatedAtAsc(empId);
        assertThat(allAssignments).hasSize(2);

        EmployeeAssignmentJpaEntity prev = allAssignments.get(0);
        assertThat(prev.getBranchId()).isEqualTo(branch1Id);
        assertThat(prev.isPrimary()).isTrue();
        assertThat(prev.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 11, 1));
        // Must be closed on 2026-11-20 (D - 1 day)
        assertThat(prev.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 11, 20));

        EmployeeAssignmentJpaEntity current = allAssignments.get(1);
        assertThat(current.getBranchId()).isEqualTo(branch2Id);
        assertThat(current.isPrimary()).isTrue();
        assertThat(current.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(current.getEffectiveTo()).isNull();

        // 4. Verify primary branch queries before and after transfer date
        // On 2026-11-15: Branch 1 is primary
        List<EmployeeAssignmentJpaEntity> primaryOn15th =
                assignmentRepository.findActivePrimaryAssignmentsOnDate(empId, LocalDate.of(2026, 11, 15));
        assertThat(primaryOn15th).hasSize(1);
        assertThat(primaryOn15th.get(0).getBranchId()).isEqualTo(branch1Id);

        // On 2026-11-25: Branch 2 is primary
        List<EmployeeAssignmentJpaEntity> primaryOn25th =
                assignmentRepository.findActivePrimaryAssignmentsOnDate(empId, LocalDate.of(2026, 11, 25));
        assertThat(primaryOn25th).hasSize(1);
        assertThat(primaryOn25th.get(0).getBranchId()).isEqualTo(branch2Id);

        // 5. Verify employee detail endpoint returns full assignments history
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/employees/{id}", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments", hasSize(2)))
                .andExpect(jsonPath("$.assignments[0].effectiveTo", is("2026-11-20")))
                .andExpect(jsonPath("$.assignments[1].effectiveFrom", is("2026-11-21")));
    }

    @Test
    @Order(5)
    @DisplayName("TC-013: Midmonth wage slices, effective policy lookup preserves full history")
    void testTC013_MidmonthWageSlicesHourlyPolicy() throws Exception {
        // 1. Create employee with HOURLY 22k from 2026-11-01
        CreateEmployeeCommand createCmd = new CreateEmployeeCommand(
                "EMP-TC013", "Đặng Thuỳ Chi", "Pha chế", "0977889900", null,
                LocalDate.of(2026, 11, 1), null, null,
                new InitialSalaryPolicyCommand(
                        RateType.HOURLY, BigDecimal.valueOf(22000.00), BigDecimal.valueOf(26.00), 15,
                        LocalDate.of(2026, 11, 1), "Lương thử việc ban đầu"
                )
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn();

        CreatedEmployeeResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CreatedEmployeeResponse.class
        );
        UUID empId = created.id();

        // 2. Raise wage to 25k effective from 2026-11-16
        CreateSalaryPolicyCommand newPolicyCmd = new CreateSalaryPolicyCommand(
                RateType.HOURLY,
                BigDecimal.valueOf(25000.00),
                BigDecimal.valueOf(26.00),
                15,
                LocalDate.of(2026, 11, 16),
                null,
                "Tăng lương chính thức sau thử việc"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees/{id}/salary-policies", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPolicyCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rateType", is("HOURLY")))
                .andExpect(jsonPath("$.rateAmount", is(25000.00)))
                .andExpect(jsonPath("$.effectiveFrom", is("2026-11-16")));

        // 3. Verify effective wage on 2026-11-10 is 22k
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/employees/{id}/effective-salary-policy", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .param("date", "2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateType", is("HOURLY")))
                .andExpect(jsonPath("$.rateAmount", is(22000.00)))
                .andExpect(jsonPath("$.effectiveFrom", is("2026-11-01")))
                .andExpect(jsonPath("$.effectiveTo", is("2026-11-15")));

        // 4. Verify effective wage on 2026-11-17 is 25k
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/employees/{id}/effective-salary-policy", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .param("date", "2026-11-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateType", is("HOURLY")))
                .andExpect(jsonPath("$.rateAmount", is(25000.00)))
                .andExpect(jsonPath("$.effectiveFrom", is("2026-11-16")))
                .andExpect(jsonPath("$.effectiveTo").doesNotExist());

        // 5. Verify full salary policy history on employee detail (no overwrite, no data loss)
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/employees/{id}", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salaryPolicies", hasSize(2)))
                .andExpect(jsonPath("$.salaryPolicies[0].rateAmount", is(22000.00)))
                .andExpect(jsonPath("$.salaryPolicies[0].effectiveTo", is("2026-11-15")))
                .andExpect(jsonPath("$.salaryPolicies[1].rateAmount", is(25000.00)))
                .andExpect(jsonPath("$.salaryPolicies[1].effectiveFrom", is("2026-11-16")));
    }

    @Test
    @Order(6)
    @DisplayName("Configure MONTHLY salary policy with standard workdays and rounding block")
    void testMonthlySalaryPolicyConfiguration() throws Exception {
        CreateEmployeeCommand createCmd = new CreateEmployeeCommand(
                "EMP-MGR-01", "Phạm Văn Quản Lý", "Cửa hàng trưởng", "0909090909", null,
                LocalDate.of(2026, 11, 1), null, null, null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn();

        CreatedEmployeeResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CreatedEmployeeResponse.class
        );
        UUID empId = created.id();

        CreateSalaryPolicyCommand monthlyPolicy = new CreateSalaryPolicyCommand(
                RateType.MONTHLY,
                BigDecimal.valueOf(15000000.00),
                BigDecimal.valueOf(24.00),
                30,
                LocalDate.of(2026, 11, 1),
                null,
                "Lương quản lý cố định 24 ngày công"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees/{id}/salary-policies", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(monthlyPolicy)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rateType", is("MONTHLY")))
                .andExpect(jsonPath("$.rateAmount", is(15000000.00)))
                .andExpect(jsonPath("$.standardWorkdays", is(24.00)))
                .andExpect(jsonPath("$.paidMinutesRoundingBlock", is(30)));
    }

    @Test
    @Order(7)
    @DisplayName("Reject invalid salary policies (negative amount, non-positive standard workdays, invalid rounding block)")
    void testRejectInvalidSalaryPolicies() throws Exception {
        CreateEmployeeCommand createCmd = new CreateEmployeeCommand(
                "EMP-TEST-POL", "Test Policy Validation", "Barista", null, null,
                LocalDate.of(2026, 11, 1), null, null, null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID empId = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CreatedEmployeeResponse.class
        ).id();

        // 1. Negative rateAmount
        CreateSalaryPolicyCommand negRate = new CreateSalaryPolicyCommand(
                RateType.HOURLY, BigDecimal.valueOf(-500.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 1), null, null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees/{id}/salary-policies", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negRate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-salary-policy")));

        // 2. standardWorkdays <= 0
        CreateSalaryPolicyCommand zeroWorkdays = new CreateSalaryPolicyCommand(
                RateType.MONTHLY, BigDecimal.valueOf(10000000.00), BigDecimal.valueOf(0.00), 15,
                LocalDate.of(2026, 11, 1), null, null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees/{id}/salary-policies", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroWorkdays)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-salary-policy")));

        // 3. Invalid rounding block (e.g. 25 minutes)
        CreateSalaryPolicyCommand badRounding = new CreateSalaryPolicyCommand(
                RateType.HOURLY, BigDecimal.valueOf(25000.00), BigDecimal.valueOf(26.00), 25,
                LocalDate.of(2026, 11, 1), null, null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees/{id}/salary-policies", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRounding)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-salary-policy")));

        // 4. effectiveTo before effectiveFrom
        CreateSalaryPolicyCommand badDates = new CreateSalaryPolicyCommand(
                RateType.HOURLY, BigDecimal.valueOf(25000.00), BigDecimal.valueOf(26.00), 15,
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10), null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees/{id}/salary-policies", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDates)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-salary-policy")));
    }

    @Test
    @Order(8)
    @DisplayName("Branch scoping isolation: Unauthorized branch manager receives 403 Forbidden")
    void testBranchScopingIsolation() throws Exception {
        // User from other branch attempts to list employees of branch1Id
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenOtherBranch))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));

        // User from other branch attempts to create employee in branch1Id
        CreateEmployeeCommand cmd = new CreateEmployeeCommand(
                "EMP-FORBIDDEN", "Forbidden User", "Barista", null, null,
                LocalDate.of(2026, 11, 1), null, null, null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenOtherBranch)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));
    }

    @Test
    @Order(9)
    @DisplayName("Employee directory: pagination and search filters")
    void testEmployeeDirectoryListingAndSearch() throws Exception {
        // Create 2 employees
        CreateEmployeeCommand emp1 = new CreateEmployeeCommand(
                "EMP-SRCH-01", "Trần Mai Anh", "Barista", null, null,
                LocalDate.of(2026, 11, 1), null, null, null
        );
        CreateEmployeeCommand emp2 = new CreateEmployeeCommand(
                "EMP-SRCH-02", "Lê Văn Hùng", "Thu ngân", null, null,
                LocalDate.of(2026, 11, 1), null, null, null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                .header("Authorization", "Bearer " + tokenBranch1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emp1)));

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                .header("Authorization", "Bearer " + tokenBranch1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emp2)));

        // List all
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));

        // Search by name "Mai Anh"
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .param("search", "Mai Anh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].staffCode", is("EMP-SRCH-01")));

        // Search by staff code "SRCH-02"
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .param("search", "SRCH-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].staffCode", is("EMP-SRCH-02")));
    }

    @Test
    @Order(10)
    @DisplayName("Update employee profile and validate date constraints")
    void testUpdateEmployeeProfile() throws Exception {
        CreateEmployeeCommand createCmd = new CreateEmployeeCommand(
                "EMP-UPD-01", "Hoàng Văn Nam", "Thu ngân", "0912000111", null,
                LocalDate.of(2026, 11, 1), null, "Ban đầu", null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/employees", branch1Id)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID empId = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CreatedEmployeeResponse.class
        ).id();

        // Valid update
        UpdateEmployeeCommand updateCmd = new UpdateEmployeeCommand(
                "Hoàng Văn Nam",
                "Trưởng ca thu ngân",
                "0912000222",
                "namhv@milktea.vn",
                EmployeeStatus.ACTIVE,
                null,
                "Đã thăng chức"
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/hr/employees/{id}", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionName", is("Trưởng ca thu ngân")))
                .andExpect(jsonPath("$.phoneNumber", is("0912000222")))
                .andExpect(jsonPath("$.notes", is("Đã thăng chức")));

        // Invalid update: leftOn < hiredOn
        UpdateEmployeeCommand invalidDate = new UpdateEmployeeCommand(
                "Hoàng Văn Nam", "Trưởng ca thu ngân", null, null, null,
                LocalDate.of(2026, 10, 15), null
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/hr/employees/{id}", branch1Id, empId)
                        .header("Authorization", "Bearer " + tokenBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-employee-dates")));
    }
}
