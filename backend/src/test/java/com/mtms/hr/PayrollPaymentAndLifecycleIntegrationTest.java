package com.mtms.hr;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.mtms.hr.application.dto.PayrollPaymentDtos.BulkPaySalaryCommand;
import com.mtms.hr.application.dto.PayrollPaymentDtos.BulkPaymentItemCommand;
import com.mtms.hr.application.dto.PayrollPaymentDtos.PaySalaryCommand;
import com.mtms.hr.domain.EmployeeStatus;
import com.mtms.hr.domain.PaymentMethod;
import com.mtms.hr.domain.PayrollPeriodStatus;
import com.mtms.hr.domain.RateType;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeAssignmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaEntity;
import com.mtms.hr.infrastructure.persistence.EmployeeJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollAdjustmentJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollLineJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollLineJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollPaymentJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollPaymentJpaRepository;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaEntity;
import com.mtms.hr.infrastructure.persistence.PayrollPeriodJpaRepository;
import com.mtms.hr.infrastructure.persistence.SalaryPolicyJpaRepository;
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
 * P0-HR-07, P0-HR-08 & P0-HR-09 Comprehensive Integration Test.
 * Authoritative: docs/api/contracts/P0-HR-07-09.md
 * Verifies:
 * - Partial payment and full settlement on payroll lines.
 * - Guard against payment exceeding remaining unpaid gross salary (400 Bad Request).
 * - Guard against payment on unapproved DRAFT payroll period (400 Bad Request).
 * - TC-016: Accrual vs Cash Flow invariant (date of cash outflow is paid_on, P&L is service month).
 * - Account balance debit and insufficient funds protection (400 Bad Request).
 * - Bulk payment for multi-employee period settlements.
 * - TC-017 Privacy Guard: employees can only retrieve their own payslip statement.
 * - Branch isolation (403 Forbidden).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayrollPaymentAndLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PayrollPaymentJpaRepository paymentRepository;

    @Autowired
    private CashLedgerEntryJpaRepository ledgerEntryRepository;

    @Autowired
    private PayrollAdjustmentJpaRepository adjustmentRepository;

    @Autowired
    private PayrollLineJpaRepository lineRepository;

    @Autowired
    private PayrollPeriodJpaRepository periodRepository;

    @Autowired
    private FinancialAccountJpaRepository accountRepository;

    @Autowired
    private FinancialCategoryJpaRepository categoryRepository;

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
    private UUID branchId;
    private UUID otherBranchId;

    private UserJpaEntity ownerUser;
    private UserJpaEntity managerUser;
    private UserJpaEntity employeeAUser;
    private UserJpaEntity employeeBUser;
    private UserJpaEntity otherBranchUser;

    private String tokenOwner;
    private String tokenManager;
    private String tokenEmployeeA;
    private String tokenEmployeeB;
    private String tokenOtherBranch;

    private FinancialAccountJpaEntity bankAccount;
    private FinancialCategoryJpaEntity paySalaryCategory;

    private EmployeeJpaEntity employeeA;
    private EmployeeJpaEntity employeeB;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        adjustmentRepository.deleteAll();
        lineRepository.deleteAll();
        periodRepository.deleteAll();
        accountRepository.deleteAll();
        timesheetRepository.deleteAll();
        salaryPolicyRepository.deleteAll();
        assignmentRepository.deleteAll();
        employeeRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure system category PAY_SALARY exists
        paySalaryCategory = categoryRepository.findByCategoryCode("PAY_SALARY").orElseGet(() ->
                categoryRepository.save(new FinancialCategoryJpaEntity(
                        UUID.randomUUID(),
                        "PAY_SALARY",
                        "Chi trả lương nhân viên",
                        CategoryGroup.PAYROLL,
                        FlowDirection.OUTFLOW,
                        true,
                        true,
                        "Thanh toán tiền lương thực tế cho nhân sự",
                        Instant.now()
                ))
        );

        // 1. Organization & Branches
        entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_HR_PAY", "MTMS Pay Corp", "0109999555",
                BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR-PAY-01", "MTMS Chi nhánh Quận 1", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, entityId, "BR-PAY-02", "MTMS Chi nhánh Bình Thạnh", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Financial Account
        bankAccount = new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, null, "Vietcombank CN Q1",
                AccountKind.BANK, "VND", new BigDecimal("20000000.00"),
                AccountStatus.ACTIVE, Instant.now()
        );
        accountRepository.save(bankAccount);

        // 3. Users
        ownerUser = new UserJpaEntity(
                UUID.randomUUID(), "owner_q1", "$2a$12$dummyhash", "Chủ Quán Q1",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(ownerUser);

        managerUser = new UserJpaEntity(
                UUID.randomUUID(), "mgr_q1", "$2a$12$dummyhash", "Quản Lý Q1",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(managerUser);

        employeeAUser = new UserJpaEntity(
                UUID.randomUUID(), "emp_a", "$2a$12$dummyhash", "Nguyễn Văn A",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(employeeAUser);

        employeeBUser = new UserJpaEntity(
                UUID.randomUUID(), "emp_b", "$2a$12$dummyhash", "Trần Thị B",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(employeeBUser);

        otherBranchUser = new UserJpaEntity(
                UUID.randomUUID(), "mgr_other", "$2a$12$dummyhash", "Quản Lý BT",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(otherBranchUser);

        // 4. Role Grants
        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, ownerUser.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), ownerUser.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, managerUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), ownerUser.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, employeeAUser.getId(),
                "EMPLOYEE", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), ownerUser.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, employeeBUser.getId(),
                "EMPLOYEE", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), ownerUser.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, otherBranchId, otherBranchUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), ownerUser.getId(), Instant.now()
        ));

        // 5. Employees
        employeeA = new EmployeeJpaEntity(
                UUID.randomUUID(), employeeAUser.getId(), "EMP-001", "Nguyễn Văn A", "Barista",
                "0901111111", "nva@mtms.vn", LocalDate.of(2026, 1, 1), null,
                EmployeeStatus.ACTIVE, "Barista chính", Instant.now(), Instant.now()
        );
        employeeRepository.save(employeeA);

        employeeB = new EmployeeJpaEntity(
                UUID.randomUUID(), employeeBUser.getId(), "EMP-002", "Trần Thị B", "Thu Ngân",
                "0902222222", "ttb@mtms.vn", LocalDate.of(2026, 1, 1), null,
                EmployeeStatus.ACTIVE, "Thu ngân chính", Instant.now(), Instant.now()
        );
        employeeRepository.save(employeeB);

        // Tokens
        tokenOwner = jwtTokenService.generateAccessToken(ownerUser.getId(), ownerUser.getLoginIdentifier());
        tokenManager = jwtTokenService.generateAccessToken(managerUser.getId(), managerUser.getLoginIdentifier());
        tokenEmployeeA = jwtTokenService.generateAccessToken(employeeAUser.getId(), employeeAUser.getLoginIdentifier());
        tokenEmployeeB = jwtTokenService.generateAccessToken(employeeBUser.getId(), employeeBUser.getLoginIdentifier());
        tokenOtherBranch = jwtTokenService.generateAccessToken(otherBranchUser.getId(), otherBranchUser.getLoginIdentifier());
    }

    @Test
    @Order(1)
    @DisplayName("Partial Payment and Full Settlement: Pay 3M then 2M on 5M gross salary")
    void testPartialPaymentAndFullSettlement() throws Exception {
        // Setup: Approved period and line with gross 5,000,000
        PayrollPeriodJpaEntity period = new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchId, LocalDate.of(2026, 11, 1), 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("5000000.00"), new BigDecimal("5000000.00"),
                BigDecimal.ZERO, 1, "Kỳ lương tháng 11", ownerUser.getId(), Instant.now(),
                Instant.now(), Instant.now()
        );
        periodRepository.save(period);

        PayrollLineJpaEntity line = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeA.getId(), branchId,
                RateType.HOURLY, 6000, BigDecimal.ZERO, new BigDecimal("5000000.00"),
                BigDecimal.ZERO, new BigDecimal("5000000.00"), BigDecimal.ZERO,
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(line);

        // 1. Partial payment: 3,000,000 VND
        PaySalaryCommand payCmd1 = new PaySalaryCommand(
                bankAccount.getId(),
                new BigDecimal("3000000.00"),
                LocalDate.of(2026, 12, 5),
                PaymentMethod.BANK_TRANSFER,
                "UNC-001",
                null,
                "Chi trả đợt 1"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/pay", branchId, line.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payCmd1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(3000000.00)))
                .andExpect(jsonPath("$.paidAmountAfter", is(3000000.00)))
                .andExpect(jsonPath("$.remainingSalary", is(2000000.00)))
                .andExpect(jsonPath("$.status", is("POSTED")));

        // Verify account balance: 20M - 3M = 17M
        FinancialAccountJpaEntity updatedAcc = accountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedAcc.getCurrentBalance()).isEqualByComparingTo("17000000.00");

        // 2. Full settlement: remaining 2,000,000 VND
        PaySalaryCommand payCmd2 = new PaySalaryCommand(
                bankAccount.getId(),
                new BigDecimal("2000000.00"),
                LocalDate.of(2026, 12, 10),
                PaymentMethod.BANK_TRANSFER,
                "UNC-002",
                null,
                "Tất toán đợt 2"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/pay", branchId, line.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payCmd2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(2000000.00)))
                .andExpect(jsonPath("$.paidAmountAfter", is(5000000.00)))
                .andExpect(jsonPath("$.remainingSalary", is(0.00)))
                .andExpect(jsonPath("$.status", is("POSTED")));

        // Verify account balance: 17M - 2M = 15M
        updatedAcc = accountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedAcc.getCurrentBalance()).isEqualByComparingTo("15000000.00");

        // Verify payment history
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/payments", branchId, line.getId())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].amount", is(3000000.00)))
                .andExpect(jsonPath("$[1].amount", is(2000000.00)));

        // Verify full payslip
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/payslip", branchId, line.getId())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSalary", is(5000000.00)))
                .andExpect(jsonPath("$.paidAmount", is(5000000.00)))
                .andExpect(jsonPath("$.remainingSalary", is(0.00)))
                .andExpect(jsonPath("$.payments", hasSize(2)));
    }

    @Test
    @Order(2)
    @DisplayName("Reject overpayment exceeding remaining unpaid salary (400 Bad Request)")
    void testRejectOverpayment() throws Exception {
        PayrollPeriodJpaEntity period = new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchId, LocalDate.of(2026, 11, 1), 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("5000000.00"), new BigDecimal("5000000.00"),
                BigDecimal.ZERO, 1, "Period test overpayment", ownerUser.getId(), Instant.now(),
                Instant.now(), Instant.now()
        );
        periodRepository.save(period);

        PayrollLineJpaEntity line = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeA.getId(), branchId,
                RateType.HOURLY, 6000, BigDecimal.ZERO, new BigDecimal("5000000.00"),
                BigDecimal.ZERO, new BigDecimal("5000000.00"), new BigDecimal("3000000.00"),
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(line);

        // Remaining salary is 2,000,000. Attempt to pay 2,000,001
        PaySalaryCommand overpayCmd = new PaySalaryCommand(
                bankAccount.getId(),
                new BigDecimal("2000001.00"),
                LocalDate.of(2026, 12, 5),
                PaymentMethod.BANK_TRANSFER,
                "UNC-OVER",
                null,
                "Chi vượt quá nợ"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/pay", branchId, line.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overpayCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:payroll-payment-exceeds-remaining")));

        // Account balance untouched
        FinancialAccountJpaEntity updatedAcc = accountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedAcc.getCurrentBalance()).isEqualByComparingTo("20000000.00");
    }

    @Test
    @Order(3)
    @DisplayName("Reject payment when payroll period is still in DRAFT status (400 Bad Request)")
    void testRejectPaymentWhenPayrollNotApproved() throws Exception {
        PayrollPeriodJpaEntity period = new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchId, LocalDate.of(2026, 11, 1), 1, true,
                PayrollPeriodStatus.DRAFT, new BigDecimal("4000000.00"), new BigDecimal("4000000.00"),
                BigDecimal.ZERO, 1, "Draft period", null, null,
                Instant.now(), Instant.now()
        );
        periodRepository.save(period);

        PayrollLineJpaEntity line = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeA.getId(), branchId,
                RateType.HOURLY, 4800, BigDecimal.ZERO, new BigDecimal("4000000.00"),
                BigDecimal.ZERO, new BigDecimal("4000000.00"), BigDecimal.ZERO,
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(line);

        PaySalaryCommand payCmd = new PaySalaryCommand(
                bankAccount.getId(),
                new BigDecimal("1000000.00"),
                LocalDate.of(2026, 12, 5),
                PaymentMethod.BANK_TRANSFER,
                "UNC-DRAFT",
                null,
                "Chi trên bảng lương chưa duyệt"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/pay", branchId, line.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:payroll-not-approved")));
    }

    @Test
    @Order(4)
    @DisplayName("TC-016: Accrual vs Cash Flow Invariant - Outflow occurs on payment date (2026-12-05) without retroactive month modification")
    void testTC016_AccrualVsCashFlowInvariant() throws Exception {
        // November payroll period approved on 2026-12-01
        PayrollPeriodJpaEntity period = new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchId, LocalDate.of(2026, 11, 1), 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("4500000.00"), new BigDecimal("4500000.00"),
                BigDecimal.ZERO, 1, "Kỳ lương tháng 11 duyệt 01/12", ownerUser.getId(), Instant.now(),
                Instant.now(), Instant.now()
        );
        periodRepository.save(period);

        PayrollLineJpaEntity line = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeA.getId(), branchId,
                RateType.HOURLY, 5400, BigDecimal.ZERO, new BigDecimal("4500000.00"),
                BigDecimal.ZERO, new BigDecimal("4500000.00"), BigDecimal.ZERO,
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(line);

        LocalDate cashPaymentDate = LocalDate.of(2026, 12, 5);
        BigDecimal paymentAmount = new BigDecimal("4500000.00");

        PaySalaryCommand payCmd = new PaySalaryCommand(
                bankAccount.getId(),
                paymentAmount,
                cashPaymentDate,
                PaymentMethod.BANK_TRANSFER,
                "UNC-TC016-001",
                null,
                "Thanh toán lương tháng 11 vào ngày 05/12/2026"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/pay", branchId, line.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(4500000.00)))
                .andExpect(jsonPath("$.paidOn", is("2026-12-05")))
                .andExpect(jsonPath("$.remainingSalary", is(0.00)));

        // Verify Cash Ledger Entry:
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository.findAll();
        assertThat(entries).hasSize(1);
        CashLedgerEntryJpaEntity entry = entries.get(0);

        // Invariant TC-016: Entry must be dated exactly on the cash payment date, NOT November
        assertThat(entry.getBusinessDate()).isEqualTo(cashPaymentDate);
        assertThat(entry.getEntryType()).isEqualTo(EntryType.OUTFLOW);
        assertThat(entry.getAmount()).isEqualByComparingTo(paymentAmount);
        assertThat(entry.getSignedAmount()).isEqualByComparingTo(paymentAmount.negate());
        assertThat(entry.getCategoryId()).isEqualTo(paySalaryCategory.getId());
        assertThat(entry.getSourceType()).isEqualTo(SourceType.PAYROLL_PAYMENT);
        assertThat(entry.getSourceLeg()).isEqualTo("PAYMENT");
        assertThat(entry.getRunningBalance()).isEqualByComparingTo("15500000.00");

        // Financial account current balance updated correctly
        FinancialAccountJpaEntity updatedAcc = accountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedAcc.getCurrentBalance()).isEqualByComparingTo("15500000.00");
    }

    @Test
    @Order(5)
    @DisplayName("Reject salary payment when financial account has insufficient funds (400 Bad Request)")
    void testRejectInsufficientAccountBalance() throws Exception {
        // Create an account with small balance: 500,000 VND
        FinancialAccountJpaEntity lowBalanceAcc = new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, null, "Quỹ tiền mặt nhỏ",
                AccountKind.CASH, "VND", new BigDecimal("500000.00"),
                AccountStatus.ACTIVE, Instant.now()
        );
        accountRepository.save(lowBalanceAcc);

        PayrollPeriodJpaEntity period = new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchId, LocalDate.of(2026, 11, 1), 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("2000000.00"), new BigDecimal("2000000.00"),
                BigDecimal.ZERO, 1, "Period test insufficient", ownerUser.getId(), Instant.now(),
                Instant.now(), Instant.now()
        );
        periodRepository.save(period);

        PayrollLineJpaEntity line = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeA.getId(), branchId,
                RateType.HOURLY, 2400, BigDecimal.ZERO, new BigDecimal("2000000.00"),
                BigDecimal.ZERO, new BigDecimal("2000000.00"), BigDecimal.ZERO,
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(line);

        // Attempt to pay 1,000,000 VND from account having only 500,000 VND
        PaySalaryCommand payCmd = new PaySalaryCommand(
                lowBalanceAcc.getId(),
                new BigDecimal("1000000.00"),
                LocalDate.of(2026, 12, 5),
                PaymentMethod.CASH,
                null,
                null,
                "Chi quá quỹ"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/pay", branchId, line.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:insufficient-funds")));

        // Account balance untouched
        FinancialAccountJpaEntity currentLowAcc = accountRepository.findById(lowBalanceAcc.getId()).orElseThrow();
        assertThat(currentLowAcc.getCurrentBalance()).isEqualByComparingTo("500000.00");
    }

    @Test
    @Order(6)
    @DisplayName("Bulk Pay Salary: settle multiple employees in one request")
    void testBulkPaySalary() throws Exception {
        PayrollPeriodJpaEntity period = new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchId, LocalDate.of(2026, 11, 1), 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("5000000.00"), new BigDecimal("5000000.00"),
                BigDecimal.ZERO, 2, "Bulk pay period", ownerUser.getId(), Instant.now(),
                Instant.now(), Instant.now()
        );
        periodRepository.save(period);

        PayrollLineJpaEntity lineA = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeA.getId(), branchId,
                RateType.HOURLY, 2400, BigDecimal.ZERO, new BigDecimal("2000000.00"),
                BigDecimal.ZERO, new BigDecimal("2000000.00"), BigDecimal.ZERO,
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(lineA);

        PayrollLineJpaEntity lineB = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeB.getId(), branchId,
                RateType.HOURLY, 3600, BigDecimal.ZERO, new BigDecimal("3000000.00"),
                BigDecimal.ZERO, new BigDecimal("3000000.00"), BigDecimal.ZERO,
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(lineB);

        BulkPaySalaryCommand bulkCmd = new BulkPaySalaryCommand(
                bankAccount.getId(),
                LocalDate.of(2026, 12, 5),
                PaymentMethod.BANK_TRANSFER,
                List.of(
                        new BulkPaymentItemCommand(lineA.getId(), new BigDecimal("2000000.00"), "UNC-BULK-A"),
                        new BulkPaymentItemCommand(lineB.getId(), new BigDecimal("3000000.00"), "UNC-BULK-B")
                ),
                "Chi trả lương hàng loạt tháng 11"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/hr/payroll-periods/{periodId}/bulk-pay", branchId, period.getId())
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaidAmount", is(5000000.00)))
                .andExpect(jsonPath("$.paidCount", is(2)))
                .andExpect(jsonPath("$.paymentIds", hasSize(2)));

        // Check account balance: 20M - 5M = 15M
        FinancialAccountJpaEntity updatedAcc = accountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedAcc.getCurrentBalance()).isEqualByComparingTo("15000000.00");

        // Check lines updated
        PayrollLineJpaEntity reloadedA = lineRepository.findById(lineA.getId()).orElseThrow();
        assertThat(reloadedA.getPaidAmount()).isEqualByComparingTo("2000000.00");

        PayrollLineJpaEntity reloadedB = lineRepository.findById(lineB.getId()).orElseThrow();
        assertThat(reloadedB.getPaidAmount()).isEqualByComparingTo("3000000.00");
    }

    @Test
    @Order(7)
    @DisplayName("TC-017: Privacy Guard - Employee can only view own payslips; viewing others is forbidden (403)")
    void testEmployeePersonalPayslipPrivacyGuard_TC017() throws Exception {
        PayrollPeriodJpaEntity period = new PayrollPeriodJpaEntity(
                UUID.randomUUID(), branchId, LocalDate.of(2026, 11, 1), 1, true,
                PayrollPeriodStatus.APPROVED, new BigDecimal("6000000.00"), new BigDecimal("6000000.00"),
                BigDecimal.ZERO, 2, "Kỳ lương có bảo mật", ownerUser.getId(), Instant.now(),
                Instant.now(), Instant.now()
        );
        periodRepository.save(period);

        PayrollLineJpaEntity lineA = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeA.getId(), branchId,
                RateType.HOURLY, 3000, BigDecimal.ZERO, new BigDecimal("2500000.00"),
                BigDecimal.ZERO, new BigDecimal("2500000.00"), BigDecimal.ZERO,
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(lineA);

        PayrollLineJpaEntity lineB = new PayrollLineJpaEntity(
                UUID.randomUUID(), period.getId(), employeeB.getId(), branchId,
                RateType.HOURLY, 4000, BigDecimal.ZERO, new BigDecimal("3500000.00"),
                BigDecimal.ZERO, new BigDecimal("3500000.00"), BigDecimal.ZERO,
                "{}", "{}", Instant.now(), Instant.now()
        );
        lineRepository.save(lineB);

        // 1. Employee A calls /my-payslips -> sees exactly 1 payslip (their own)
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/my-payslips", branchId)
                        .header("Authorization", "Bearer " + tokenEmployeeA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId", is(employeeA.getId().toString())))
                .andExpect(jsonPath("$[0].staffCode", is("EMP-001")))
                .andExpect(jsonPath("$[0].grossSalary", is(2500000.00)));

        // 2. Employee A tries to view Employee B's line payslip -> 403 Forbidden (TC-017)
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/payslip", branchId, lineB.getId())
                        .header("Authorization", "Bearer " + tokenEmployeeA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));

        // 3. Manager can view Employee B's line payslip -> 200 OK
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/payslip", branchId, lineB.getId())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId", is(employeeB.getId().toString())))
                .andExpect(jsonPath("$.grossSalary", is(3500000.00)));
    }

    @Test
    @Order(8)
    @DisplayName("Branch Isolation: Users from other branches receive 403 Forbidden")
    void testBranchIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/hr/my-payslips", branchId)
                        .header("Authorization", "Bearer " + tokenOtherBranch))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:forbidden")));
    }
}
