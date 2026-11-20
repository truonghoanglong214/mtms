package com.mtms.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.finance.application.dto.AdjustReconciliationCommand;
import com.mtms.finance.application.dto.CreateReconciliationCommand;
import com.mtms.finance.application.dto.ExplainDiscrepancyCommand;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.ReconciliationDisposition;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.CashReconciliationJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashReconciliationJpaRepository;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-FIN-06 / BR-FIN-06 / TC-012 / FE-P0-14
 * Integration test covering cash and bank account reconciliation lifecycle:
 * snapshot creation, discrepancy tracking (variance = observed - recorded),
 * explanation notes, authorized adjusting journal entries (INFLOW/OUTFLOW in cash ledger),
 * auto-closing on zero variance, and branch isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CashReconciliationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CashReconciliationJpaRepository reconciliationRepository;

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
    private UUID branchId;
    private UUID otherBranchId;
    private UserJpaEntity ownerUser;
    private UserJpaEntity managerUser;
    private UserJpaEntity employeeUser;
    private String tokenOwner;
    private String tokenManager;
    private String tokenEmployee;

    private FinancialAccountJpaEntity cashAccount;
    private FinancialAccountJpaEntity bankAccount;

    @BeforeEach
    void setUp() {
        reconciliationRepository.deleteAll();
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
                entityId, "ENT_RECON", "Reconciliation Test Corp", "0109999333", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR-RECON-01", "Chi nhánh Đối Soát 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, entityId, "BR-RECON-02", "Chi nhánh Đối Soát 2", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Users & Grants
        ownerUser = createUser("owner_recon", "Chủ Chi Nhánh");
        managerUser = createUser("manager_recon", "Quản Lý Cửa Hàng");
        employeeUser = createUser("employee_recon", "Nhân Viên Thu Ngân");

        LocalDate today = LocalDate.now();
        // Owner grant
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, ownerUser.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), ownerUser.getId(), Instant.now()
        ));
        // Manager grant
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, managerUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), ownerUser.getId(), Instant.now()
        ));
        // Employee grant
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, employeeUser.getId(),
                "EMPLOYEE", "READ", "READ", "READ",
                today.minusDays(10), today.plusDays(30), ownerUser.getId(), Instant.now()
        ));

        tokenOwner = jwtTokenService.generateAccessToken(ownerUser.getId(), ownerUser.getLoginIdentifier());
        tokenManager = jwtTokenService.generateAccessToken(managerUser.getId(), managerUser.getLoginIdentifier());
        tokenEmployee = jwtTokenService.generateAccessToken(employeeUser.getId(), employeeUser.getLoginIdentifier());

        // 3. Seed System Categories
        seedCategories();

        // 4. Accounts: Cash (15M) and Bank (50M)
        cashAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, null, "Quỹ tiền mặt quầy",
                AccountKind.CASH, "VND", new BigDecimal("15000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));

        BranchBankAccountJpaEntity branchBank = branchBankRepository.save(new BranchBankAccountJpaEntity(
                UUID.randomUUID(), branchId, "Vietcombank", "CTY RECON", "cipher_recon", "**** **** 1234", true, BankAccountStatus.ACTIVE, Instant.now()
        ));

        bankAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, branchBank.getId(), "Tài khoản VCB Chi Nhánh",
                AccountKind.BANK, "VND", new BigDecimal("50000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));
    }

    private void seedCategories() {
        categoryRepository.saveAll(List.of(
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "ADJ_SURPLUS", "Điều chỉnh thừa quỹ kiểm kê/đối soát", CategoryGroup.OTHER, FlowDirection.INFLOW, true, true, "Hạch toán chênh lệch thừa tiền mặt/ngân hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "ADJ_SHORTAGE", "Điều chỉnh thiếu quỹ kiểm kê/đối soát", CategoryGroup.OTHER, FlowDirection.OUTFLOW, true, true, "Hạch toán chênh lệch thiếu tiền mặt/ngân hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REV_CASH", "Doanh thu bán hàng - Tiền mặt", CategoryGroup.REVENUE, FlowDirection.INFLOW, true, true, "Tiền mặt thu", Instant.now())
        ));
    }

    private UserJpaEntity createUser(String login, String name) {
        return userRepository.save(new UserJpaEntity(
                UUID.randomUUID(), login, passwordEncoder.encode("TestPass123!"), name, UserStatus.ACTIVE, Instant.now()
        ));
    }

    @Test
    @Order(1)
    @DisplayName("TC-012: Cash count shortage -> OPEN discrepancy -> explain -> authorized adjustment posts OUTFLOW and updates balance")
    void testReconciliationShortageLifecycle_TC012() throws Exception {
        LocalDate reconDate = LocalDate.now();

        // 1. Create reconciliation snapshot with shortage: recorded 15M, observed 14.95M -> variance -50,000
        CreateReconciliationCommand createCmd = new CreateReconciliationCommand(
                cashAccount.getId(),
                reconDate,
                new BigDecimal("14950000.00"),
                "Kiểm đếm cuối ca chiều thiếu 50k",
                null
        );

        String createResJson = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.branchId", is(branchId.toString())))
                .andExpect(jsonPath("$.accountId", is(cashAccount.getId().toString())))
                .andExpect(jsonPath("$.accountName", is("Quỹ tiền mặt quầy")))
                .andExpect(jsonPath("$.recordedBalance", is(15000000.00)))
                .andExpect(jsonPath("$.observedBalance", is(14950000.00)))
                .andExpect(jsonPath("$.varianceAmount", is(-50000.00)))
                .andExpect(jsonPath("$.disposition", is("OPEN")))
                .andExpect(jsonPath("$.createdByName", is("Quản Lý Cửa Hàng")))
                .andReturn().getResponse().getContentAsString();

        UUID reconId = UUID.fromString(objectMapper.readTree(createResJson).get("id").asText());

        // 2. Explain discrepancy
        ExplainDiscrepancyCommand explainCmd = new ExplainDiscrepancyCommand(
                "Nghi ngờ thối nhầm tiền cho khách hàng bàn 4",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations/{id}/explain", branchId, reconId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(explainCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reconId.toString())))
                .andExpect(jsonPath("$.disposition", is("EXPLAINED")))
                .andExpect(jsonPath("$.note", is("Nghi ngờ thối nhầm tiền cho khách hàng bàn 4")));

        // 3. Authorized adjustment by OWNER -> posts OUTFLOW of 50,000 with category ADJ_SHORTAGE
        AdjustReconciliationCommand adjustCmd = new AdjustReconciliationCommand(
                "Chấp thuận bù thiếu quỹ tiền mặt ca chiều"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations/{id}/adjust", branchId, reconId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reconId.toString())))
                .andExpect(jsonPath("$.disposition", is("ADJUSTED")))
                .andExpect(jsonPath("$.adjustmentLedgerEntryId", notNullValue()))
                .andExpect(jsonPath("$.confirmedByName", is("Chủ Chi Nhánh")))
                .andExpect(jsonPath("$.confirmedAt", notNullValue()));

        // 4. Verify Account balance synchronized to observed count (14,950,000.00)
        FinancialAccountJpaEntity updatedAccount = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getCurrentBalance()).isEqualByComparingTo("14950000.00");

        // 5. Verify Cash ledger entry created with exact amount, type, source, and category
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository.findAll();
        assertThat(entries).hasSize(1);
        CashLedgerEntryJpaEntity entry = entries.get(0);
        assertThat(entry.getAccountId()).isEqualTo(cashAccount.getId());
        assertThat(entry.getEntryType()).isEqualTo(EntryType.OUTFLOW);
        assertThat(entry.getAmount()).isEqualByComparingTo("50000.00");
        assertThat(entry.getSourceType()).isEqualTo(SourceType.MANUAL_ADJUSTMENT);
        assertThat(entry.getSourceId()).isEqualTo(reconId);
        assertThat(entry.getSourceLeg()).isEqualTo("RECON_ADJUSTMENT");
    }

    @Test
    @Order(2)
    @DisplayName("TC-012: Bank surplus -> OPEN discrepancy -> authorized adjustment posts INFLOW and increases balance")
    void testReconciliationSurplusLifecycle_TC012() throws Exception {
        LocalDate reconDate = LocalDate.now();

        // 1. Bank recorded 50M, bank statement observed 50.2M -> variance +200,000 (Surplus)
        CreateReconciliationCommand createCmd = new CreateReconciliationCommand(
                bankAccount.getId(),
                reconDate,
                new BigDecimal("50200000.00"),
                "Sao kê ngân hàng cuối ngày",
                null
        );

        String createResJson = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.varianceAmount", is(200000.00)))
                .andExpect(jsonPath("$.disposition", is("OPEN")))
                .andReturn().getResponse().getContentAsString();

        UUID reconId = UUID.fromString(objectMapper.readTree(createResJson).get("id").asText());

        // 2. Adjust surplus by OWNER -> posts INFLOW of 200,000 with category ADJ_SURPLUS
        AdjustReconciliationCommand adjustCmd = new AdjustReconciliationCommand("Hạch toán tiền khách chuyển khoản thêm");

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations/{id}/adjust", branchId, reconId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disposition", is("ADJUSTED")))
                .andExpect(jsonPath("$.adjustmentLedgerEntryId", notNullValue()));

        // 3. Verify Account balance synchronized to 50,200,000.00
        FinancialAccountJpaEntity updatedAccount = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getCurrentBalance()).isEqualByComparingTo("50200000.00");

        // 4. Verify Ledger Entry
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository.findAll();
        assertThat(entries).hasSize(1);
        CashLedgerEntryJpaEntity entry = entries.get(0);
        assertThat(entry.getAccountId()).isEqualTo(bankAccount.getId());
        assertThat(entry.getEntryType()).isEqualTo(EntryType.INFLOW);
        assertThat(entry.getAmount()).isEqualByComparingTo("200000.00");
    }

    @Test
    @Order(3)
    @DisplayName("Zero variance auto-closes snapshot (disposition = CLOSED)")
    void testZeroVarianceAutoCloses() throws Exception {
        LocalDate reconDate = LocalDate.now();

        CreateReconciliationCommand createCmd = new CreateReconciliationCommand(
                cashAccount.getId(),
                reconDate,
                new BigDecimal("15000000.00"), // Identical to recorded balance
                "Kiểm đếm khớp 100%",
                null
        );

        String resJson = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.varianceAmount", is(0.00)))
                .andExpect(jsonPath("$.disposition", is("CLOSED")))
                .andExpect(jsonPath("$.confirmedByName", is("Quản Lý Cửa Hàng")))
                .andExpect(jsonPath("$.confirmedAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        UUID reconId = UUID.fromString(objectMapper.readTree(resJson).get("id").asText());

        // Attempting to adjust closed reconciliation should be rejected with 400 Bad Request
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations/{id}/adjust", branchId, reconId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustReconciliationCommand("Thử điều chỉnh"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:reconciliation-already-adjusted")));
    }

    @Test
    @Order(4)
    @DisplayName("Duplicate reconciliation for same account and date rejected with 409 Conflict")
    void testDuplicateReconciliationRejected_Conflict409() throws Exception {
        LocalDate reconDate = LocalDate.now();

        CreateReconciliationCommand cmd1 = new CreateReconciliationCommand(
                cashAccount.getId(), reconDate, new BigDecimal("15000000.00"), "Lần 1", null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd1)))
                .andExpect(status().isCreated());

        CreateReconciliationCommand cmd2 = new CreateReconciliationCommand(
                cashAccount.getId(), reconDate, new BigDecimal("14900000.00"), "Lần 2 trùng ngày", null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:reconciliation-already-exists")));
    }

    @Test
    @Order(5)
    @DisplayName("Attempting to re-adjust already adjusted reconciliation rejected with 400 Bad Request")
    void testReAdjustingRejected() throws Exception {
        LocalDate reconDate = LocalDate.now();

        CreateReconciliationCommand cmd = new CreateReconciliationCommand(
                cashAccount.getId(), reconDate, new BigDecimal("14900000.00"), "Thiếu 100k", null
        );

        String res = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID reconId = UUID.fromString(objectMapper.readTree(res).get("id").asText());

        // First adjustment: Success
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations/{id}/adjust", branchId, reconId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustReconciliationCommand("Bù thiếu"))))
                .andExpect(status().isOk());

        // Second adjustment: Fails with 400
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations/{id}/adjust", branchId, reconId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustReconciliationCommand("Bù thiếu lần 2"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:reconciliation-already-adjusted")));
    }

    @Test
    @Order(6)
    @DisplayName("Unauthorized adjustment by non-OWNER is forbidden (403)")
    void testUnauthorizedAdjustmentForbidden() throws Exception {
        LocalDate reconDate = LocalDate.now();

        CreateReconciliationCommand cmd = new CreateReconciliationCommand(
                cashAccount.getId(), reconDate, new BigDecimal("14800000.00"), "Thiếu 200k", null
        );

        String res = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID reconId = UUID.fromString(objectMapper.readTree(res).get("id").asText());

        // Employee attempts to adjust -> 403 Forbidden
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations/{id}/adjust", branchId, reconId)
                        .header("Authorization", "Bearer " + tokenEmployee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustReconciliationCommand("Nhân viên tự chỉnh"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("Branch isolation: querying reconciliation from foreign branch returns 404 Not Found")
    void testBranchIsolation() throws Exception {
        LocalDate reconDate = LocalDate.now();

        CreateReconciliationCommand cmd = new CreateReconciliationCommand(
                cashAccount.getId(), reconDate, new BigDecimal("14500000.00"), "Thiếu 500k", null
        );

        String res = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID reconId = UUID.fromString(objectMapper.readTree(res).get("id").asText());

        // Give owner access to other branch as well
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, otherBranchId, ownerUser.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), ownerUser.getId(), Instant.now()
        ));

        // Querying reconId from otherBranchId returns 404
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/reconciliations/{id}", otherBranchId, reconId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:reconciliation-not-found")));
    }

    @Test
    @Order(8)
    @DisplayName("List reconciliations supports filtering and pagination")
    void testListReconciliationsWithFilters() throws Exception {
        LocalDate d1 = LocalDate.now().minusDays(2);
        LocalDate d2 = LocalDate.now().minusDays(1);

        // Record 1: Cash account, d1, balanced (CLOSED)
        reconciliationRepository.save(new CashReconciliationJpaEntity(
                UUID.randomUUID(), branchId, cashAccount.getId(), d1,
                new BigDecimal("15000000.00"), new BigDecimal("15000000.00"), BigDecimal.ZERO,
                ReconciliationDisposition.CLOSED, "Khớp", null, null, ownerUser.getId(), ownerUser.getId(), Instant.now(), Instant.now()
        ));

        // Record 2: Bank account, d2, shortage (OPEN)
        reconciliationRepository.save(new CashReconciliationJpaEntity(
                UUID.randomUUID(), branchId, bankAccount.getId(), d2,
                new BigDecimal("50000000.00"), new BigDecimal("49000000.00"), new BigDecimal("-1000000.00"),
                ReconciliationDisposition.OPEN, "Lệch bank", null, null, ownerUser.getId(), null, null, Instant.now()
        ));

        // 1. Filter by accountId = bankAccount
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .param("accountId", bankAccount.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].accountId", is(bankAccount.getId().toString())));

        // 2. Filter by disposition = CLOSED
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .param("disposition", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].disposition", is("CLOSED")));

        // 3. No filter -> returns both
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/reconciliations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
}
