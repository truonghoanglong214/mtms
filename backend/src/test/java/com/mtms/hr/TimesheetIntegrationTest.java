package com.mtms.hr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.hr.application.dto.TimesheetDtos.ApproveTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.BulkApproveCommand;
import com.mtms.hr.application.dto.TimesheetDtos.BulkTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.BulkTimesheetItemCommand;
import com.mtms.hr.application.dto.TimesheetDtos.CreateTimesheetCommand;
import com.mtms.hr.application.dto.TimesheetDtos.RejectTimesheetCommand;
import com.mtms.hr.domain.EmployeeStatus;
import com.mtms.hr.domain.TimesheetStatus;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
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
 * P0-HR-03 & P0-HR-04 / BR-HR-02 / TC-013 / TC-017
 * Comprehensive Integration Test verifying Timesheet Ingestion, Review, Monthly Summary,
 * and Invariant TC-017 Prevention of Manager Self-Approval.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimesheetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TimesheetEntryJpaRepository timesheetRepository;

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
    private UUID otherBranchId;

    private UserJpaEntity branchManagerUser;
    private UserJpaEntity branchOwnerUser;
    private UserJpaEntity otherManagerUser;

    private EmployeeJpaEntity managerEmployee;
    private EmployeeJpaEntity baristaEmployee;
    private EmployeeJpaEntity cashierEmployee;

    private String tokenManager;
    private String tokenOwner;
    private String tokenOther;

    @BeforeEach
    void setUp() {
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

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, otherEntityId, "BR-OTHER", "Chi nhánh Khác", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        // 3. Users
        branchManagerUser = new UserJpaEntity(
                UUID.randomUUID(), "mgr_q1", "$2a$12$dummyhash", "Trần Quản Lý Q1",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(branchManagerUser);

        branchOwnerUser = new UserJpaEntity(
                UUID.randomUUID(), "owner_q1", "$2a$12$dummyhash", "Lê Chủ Quán Q1",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(branchOwnerUser);

        otherManagerUser = new UserJpaEntity(
                UUID.randomUUID(), "mgr_other", "$2a$12$dummyhash", "Quản Lý Khác",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(otherManagerUser);

        // 4. Role Grants
        LocalDate today = LocalDate.now();
        // Manager grant
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branch1Id, branchManagerUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), branchOwnerUser.getId(), Instant.now()
        ));
        // Owner grant
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branch1Id, branchOwnerUser.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), branchOwnerUser.getId(), Instant.now()
        ));
        // Other manager grant
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), otherEntityId, otherBranchId, otherManagerUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), otherManagerUser.getId(), Instant.now()
        ));

        // 5. Employees
        // Manager employee (linked to branchManagerUser.getId())
        managerEmployee = new EmployeeJpaEntity(
                UUID.randomUUID(), branchManagerUser.getId(), "EMP-MGR", "Trần Quản Lý", "Cửa hàng trưởng",
                "0901111222", "mgr@mtms.vn", LocalDate.of(2026, 1, 1), null,
                EmployeeStatus.ACTIVE, "Quản lý chi nhánh", Instant.now(), Instant.now()
        );
        employeeRepository.save(managerEmployee);
        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), managerEmployee.getId(), branch1Id, true,
                LocalDate.of(2026, 1, 1), null, "Primary", Instant.now()
        ));

        // Barista employee (normal employee, no user_id)
        baristaEmployee = new EmployeeJpaEntity(
                UUID.randomUUID(), null, "EMP-001", "Nguyễn Văn Barista", "Pha chế",
                "0902222333", "barista@mtms.vn", LocalDate.of(2026, 2, 1), null,
                EmployeeStatus.ACTIVE, "Barista chính", Instant.now(), Instant.now()
        );
        employeeRepository.save(baristaEmployee);
        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), baristaEmployee.getId(), branch1Id, true,
                LocalDate.of(2026, 2, 1), null, "Primary", Instant.now()
        ));

        // Cashier employee (normal employee)
        cashierEmployee = new EmployeeJpaEntity(
                UUID.randomUUID(), null, "EMP-002", "Lê Thị Thu Ngân", "Thu ngân",
                "0903333444", "cashier@mtms.vn", LocalDate.of(2026, 3, 1), null,
                EmployeeStatus.ACTIVE, "Thu ngân ca sáng", Instant.now(), Instant.now()
        );
        employeeRepository.save(cashierEmployee);
        assignmentRepository.save(new EmployeeAssignmentJpaEntity(
                UUID.randomUUID(), cashierEmployee.getId(), branch1Id, true,
                LocalDate.of(2026, 3, 1), null, "Primary", Instant.now()
        ));

        // 6. JWT Tokens
        tokenManager = jwtTokenService.generateAccessToken(branchManagerUser.getId(), branchManagerUser.getLoginIdentifier());
        tokenOwner = jwtTokenService.generateAccessToken(branchOwnerUser.getId(), branchOwnerUser.getLoginIdentifier());
        tokenOther = jwtTokenService.generateAccessToken(otherManagerUser.getId(), otherManagerUser.getLoginIdentifier());
    }

    @Test
    @Order(1)
    @DisplayName("Single timesheet: create DRAFT and update when SUBMITTED")
    void testCreateAndModifyTimesheetDraft() throws Exception {
        LocalDate workDate = LocalDate.of(2026, 11, 19);

        // 1. Create initial DRAFT timesheet
        CreateTimesheetCommand createCmd = new CreateTimesheetCommand(
                baristaEmployee.getId(),
                workDate,
                480,
                480,
                BigDecimal.valueOf(1.00),
                TimesheetStatus.DRAFT,
                "Ca sáng 8 tiếng"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.employeeId", is(baristaEmployee.getId().toString())))
                .andExpect(jsonPath("$.staffCode", is("EMP-001")))
                .andExpect(jsonPath("$.employeeName", is("Nguyễn Văn Barista")))
                .andExpect(jsonPath("$.workDate", is("2026-11-19")))
                .andExpect(jsonPath("$.approvedMinutes", is(480)))
                .andExpect(jsonPath("$.payableWorkdays", is(1.00)))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andReturn();

        // 2. Update to SUBMITTED with overtime
        CreateTimesheetCommand updateCmd = new CreateTimesheetCommand(
                baristaEmployee.getId(),
                workDate,
                510,
                510,
                BigDecimal.valueOf(1.05),
                TimesheetStatus.SUBMITTED,
                "Ca sáng + 30 phút tăng ca"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedMinutes", is(510)))
                .andExpect(jsonPath("$.payableWorkdays", is(1.05)))
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.note", is("Ca sáng + 30 phút tăng ca")));

        // Verify only one entry exists in database
        List<TimesheetEntryJpaEntity> entries = timesheetRepository.findByBranchIdAndWorkDateBetween(branch1Id, workDate, workDate);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getApprovedMinutes()).isEqualTo(510);
        assertThat(entries.get(0).getStatus()).isEqualTo(TimesheetStatus.SUBMITTED);
    }

    @Test
    @Order(2)
    @DisplayName("Bulk create timesheet entries for multiple employees on a work date")
    void testBulkCreateTimesheets() throws Exception {
        LocalDate workDate = LocalDate.of(2026, 11, 20);

        BulkTimesheetCommand bulkCmd = new BulkTimesheetCommand(
                workDate,
                List.of(
                        new BulkTimesheetItemCommand(
                                baristaEmployee.getId(),
                                480,
                                480,
                                BigDecimal.valueOf(1.00),
                                TimesheetStatus.SUBMITTED,
                                "Barista ca chuẩn"
                        ),
                        new BulkTimesheetItemCommand(
                                cashierEmployee.getId(),
                                240,
                                240,
                                BigDecimal.valueOf(0.50),
                                TimesheetStatus.SUBMITTED,
                                "Thu ngân ca gãy 4 tiếng"
                        )
                )
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets/bulk", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].workDate", is("2026-11-20")))
                .andExpect(jsonPath("$[1].workDate", is("2026-11-20")));

        List<TimesheetEntryJpaEntity> entries = timesheetRepository.findByBranchIdAndWorkDateBetween(branch1Id, workDate, workDate);
        assertThat(entries).hasSize(2);
    }

    @Test
    @Order(3)
    @DisplayName("Manager approves subordinate (barista) timesheet -> 200 OK")
    void testManagerApprovesSubordinateTimesheet() throws Exception {
        LocalDate workDate = LocalDate.of(2026, 11, 21);

        TimesheetEntryJpaEntity entry = new TimesheetEntryJpaEntity(
                UUID.randomUUID(), baristaEmployee.getId(), branch1Id, workDate,
                480, 480, BigDecimal.valueOf(1.00),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.SUBMITTED, "Ca chuẩn",
                null, null, null, Instant.now(), Instant.now()
        );
        timesheetRepository.save(entry);

        ApproveTimesheetCommand approveCmd = new ApproveTimesheetCommand("Xác nhận đúng giờ làm thực tế");

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets/{id}/approve", branch1Id, entry.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approvedBy", is(branchManagerUser.getId().toString())))
                .andExpect(jsonPath("$.approverName", is("Trần Quản Lý Q1")))
                .andExpect(jsonPath("$.approvalNote", is("Xác nhận đúng giờ làm thực tế")));

        TimesheetEntryJpaEntity updated = timesheetRepository.findById(entry.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(TimesheetStatus.APPROVED);
        assertThat(updated.getApprovedBy()).isEqualTo(branchManagerUser.getId());
        assertThat(updated.getApprovedAt()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("TC-017 Core Invariant: Manager attempts to self-approve own timesheet -> 403 Forbidden urn:mtms:error:self-approval-denied")
    void testManagerSelfApprovalDenied() throws Exception {
        LocalDate workDate = LocalDate.of(2026, 11, 22);

        // Timesheet entry for managerEmployee (which has user_id = branchManagerUser.getId())
        TimesheetEntryJpaEntity managerEntry = new TimesheetEntryJpaEntity(
                UUID.randomUUID(), managerEmployee.getId(), branch1Id, workDate,
                480, 480, BigDecimal.valueOf(1.00),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.SUBMITTED, "Ca quản lý",
                null, null, null, Instant.now(), Instant.now()
        );
        timesheetRepository.save(managerEntry);

        ApproveTimesheetCommand approveCmd = new ApproveTimesheetCommand("Tự duyệt công của mình");

        // Manager calls approve API on their own timesheet
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets/{id}/approve", branch1Id, managerEntry.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveCmd)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:self-approval-denied")))
                .andExpect(jsonPath("$.title", is("Self Approval Denied")));

        // Verify invariant: status remains SUBMITTED in database
        TimesheetEntryJpaEntity notApproved = timesheetRepository.findById(managerEntry.getId()).orElseThrow();
        assertThat(notApproved.getStatus()).isEqualTo(TimesheetStatus.SUBMITTED);
        assertThat(notApproved.getApprovedBy()).isNull();
        assertThat(notApproved.getApprovedAt()).isNull();
    }

    @Test
    @Order(5)
    @DisplayName("TC-017 Bulk approve: Manager attempts bulk approve including own entry -> 403 Forbidden urn:mtms:error:self-approval-denied")
    void testManagerBulkSelfApprovalDenied() throws Exception {
        LocalDate workDate = LocalDate.of(2026, 11, 23);

        TimesheetEntryJpaEntity baristaEntry = new TimesheetEntryJpaEntity(
                UUID.randomUUID(), baristaEmployee.getId(), branch1Id, workDate,
                480, 480, BigDecimal.valueOf(1.00),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.SUBMITTED, "Ca barista",
                null, null, null, Instant.now(), Instant.now()
        );
        timesheetRepository.save(baristaEntry);

        TimesheetEntryJpaEntity managerEntry = new TimesheetEntryJpaEntity(
                UUID.randomUUID(), managerEmployee.getId(), branch1Id, workDate,
                480, 480, BigDecimal.valueOf(1.00),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.SUBMITTED, "Ca quản lý",
                null, null, null, Instant.now(), Instant.now()
        );
        timesheetRepository.save(managerEntry);

        BulkApproveCommand bulkCmd = new BulkApproveCommand(
                List.of(baristaEntry.getId(), managerEntry.getId()),
                "Duyệt công hàng loạt ca hôm qua"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets/bulk-approve", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkCmd)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:self-approval-denied")));
    }

    @Test
    @Order(6)
    @DisplayName("TC-017 Branch Owner approves Manager timesheet -> 200 OK")
    void testBranchOwnerApprovesManagerTimesheet() throws Exception {
        LocalDate workDate = LocalDate.of(2026, 11, 22);

        TimesheetEntryJpaEntity managerEntry = new TimesheetEntryJpaEntity(
                UUID.randomUUID(), managerEmployee.getId(), branch1Id, workDate,
                480, 480, BigDecimal.valueOf(1.00),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.SUBMITTED, "Ca quản lý",
                null, null, null, Instant.now(), Instant.now()
        );
        timesheetRepository.save(managerEntry);

        ApproveTimesheetCommand approveCmd = new ApproveTimesheetCommand("Chủ quán duyệt công cho Cửa hàng trưởng");

        // Owner calls approve on manager's timesheet
        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets/{id}/approve", branch1Id, managerEntry.getId())
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approvedBy", is(branchOwnerUser.getId().toString())))
                .andExpect(jsonPath("$.approverName", is("Lê Chủ Quán Q1")))
                .andExpect(jsonPath("$.approvalNote", is("Chủ quán duyệt công cho Cửa hàng trưởng")));

        TimesheetEntryJpaEntity approved = timesheetRepository.findById(managerEntry.getId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(TimesheetStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo(branchOwnerUser.getId());
    }

    @Test
    @Order(7)
    @DisplayName("Reject timesheet entry with reason -> 200 OK with REJECTED status")
    void testRejectTimesheet() throws Exception {
        LocalDate workDate = LocalDate.of(2026, 11, 24);

        TimesheetEntryJpaEntity entry = new TimesheetEntryJpaEntity(
                UUID.randomUUID(), cashierEmployee.getId(), branch1Id, workDate,
                240, 240, BigDecimal.valueOf(0.50),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.SUBMITTED, "Ca gãy",
                null, null, null, Instant.now(), Instant.now()
        );
        timesheetRepository.save(entry);

        RejectTimesheetCommand rejectCmd = new RejectTimesheetCommand("Sai ca làm việc, chưa thấy check-in thực tế");

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets/{id}/reject", branch1Id, entry.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.approvalNote", is("Sai ca làm việc, chưa thấy check-in thực tế")));

        TimesheetEntryJpaEntity rejected = timesheetRepository.findById(entry.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(TimesheetStatus.REJECTED);
    }

    @Test
    @Order(8)
    @DisplayName("Prevent modifying already APPROVED timesheet -> 409 Conflict urn:mtms:error:timesheet-already-approved")
    void testPreventModifyApprovedTimesheet() throws Exception {
        LocalDate workDate = LocalDate.of(2026, 11, 21);

        TimesheetEntryJpaEntity approvedEntry = new TimesheetEntryJpaEntity(
                UUID.randomUUID(), baristaEmployee.getId(), branch1Id, workDate,
                480, 480, BigDecimal.valueOf(1.00),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.APPROVED, "Ca chuẩn đã duyệt",
                branchManagerUser.getId(), Instant.now(), "Đã duyệt", Instant.now(), Instant.now()
        );
        timesheetRepository.save(approvedEntry);

        CreateTimesheetCommand modifyCmd = new CreateTimesheetCommand(
                baristaEmployee.getId(),
                workDate,
                600,
                600,
                BigDecimal.valueOf(1.25),
                TimesheetStatus.DRAFT,
                "Cố gắng sửa bản ghi đã duyệt"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modifyCmd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:timesheet-already-approved")))
                .andExpect(jsonPath("$.title", is("Timesheet Already Approved")));
    }

    @Test
    @Order(9)
    @DisplayName("Reject invalid negative minutes or workdays -> 400 Bad Request urn:mtms:error:invalid-timesheet-data")
    void testInvalidTimesheetDataNegativeMinutes() throws Exception {
        CreateTimesheetCommand invalidCmd = new CreateTimesheetCommand(
                baristaEmployee.getId(),
                LocalDate.of(2026, 11, 28),
                -10,
                -60,
                BigDecimal.valueOf(-1.00),
                TimesheetStatus.DRAFT,
                "Số phút âm"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/timesheets", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-timesheet-data")))
                .andExpect(jsonPath("$.title", is("Invalid Timesheet Data")));
    }

    @Test
    @Order(10)
    @DisplayName("Monthly timesheet summary computes correct approved hours and day counts")
    void testMonthlyTimesheetSummary() throws Exception {
        LocalDate workDate1 = LocalDate.of(2026, 11, 10);
        LocalDate workDate2 = LocalDate.of(2026, 11, 11);

        timesheetRepository.save(new TimesheetEntryJpaEntity(
                UUID.randomUUID(), baristaEmployee.getId(), branch1Id, workDate1,
                480, 480, BigDecimal.valueOf(1.00),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.APPROVED, "Ca 1",
                branchManagerUser.getId(), Instant.now(), "Duyệt", Instant.now(), Instant.now()
        ));

        timesheetRepository.save(new TimesheetEntryJpaEntity(
                UUID.randomUUID(), baristaEmployee.getId(), branch1Id, workDate2,
                240, 240, BigDecimal.valueOf(0.50),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.SUBMITTED, "Ca 2",
                null, null, null, Instant.now(), Instant.now()
        ));
        timesheetRepository.save(new TimesheetEntryJpaEntity(
                UUID.randomUUID(), cashierEmployee.getId(), branch1Id, workDate1,
                240, 240, BigDecimal.valueOf(0.50),
                com.mtms.hr.domain.TimesheetSourceType.MANUAL,
                TimesheetStatus.REJECTED, "Ca 3 bị từ chối",
                null, null, "Từ chối", Instant.now(), Instant.now()
        ));

        // Query November 2026 summary
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/timesheets/monthly-summary", branch1Id)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("year", "2026")
                        .param("month", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                // Verify barista employee exists in summary with correct aggregates
                .andExpect(jsonPath("$[?(@.staffCode == 'EMP-001')].totalApprovedMinutes").value(480))
                .andExpect(jsonPath("$[?(@.staffCode == 'EMP-001')].totalApprovedHours").value(8.00))
                .andExpect(jsonPath("$[?(@.staffCode == 'EMP-001')].totalPayableWorkdays").value(1.00))
                .andExpect(jsonPath("$[?(@.staffCode == 'EMP-001')].approvedDaysCount").value(1))
                .andExpect(jsonPath("$[?(@.staffCode == 'EMP-001')].pendingDaysCount").value(1))
                .andExpect(jsonPath("$[?(@.staffCode == 'EMP-002')].rejectedDaysCount").value(1));
    }

    @Test
    @Order(11)
    @DisplayName("Branch isolation: manager without grant on branch is forbidden -> 403 Forbidden")
    void testBranchIsolationForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/timesheets", branch1Id)
                        .header("Authorization", "Bearer " + tokenOther))
                .andExpect(status().isForbidden());
    }
}
