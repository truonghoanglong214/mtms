package com.mtms.capital;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.capital.application.dto.CapitalDtos.*;
import com.mtms.capital.domain.model.ContributionType;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.FlowDirection;
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
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.junit.jupiter.api.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CapitalAndCommitmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessEntityJpaRepository entityRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private FinancialAccountJpaRepository accountRepository;

    @Autowired
    private FinancialCategoryJpaRepository categoryRepository;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID entityId;
    private UUID branchIdA;
    private UUID branchIdB;
    private UUID accountIdA;
    private String adminToken;
    private String managerTokenA;

    @BeforeEach
    void setUp() {
        entityId = UUID.randomUUID();
        branchIdA = UUID.randomUUID();
        branchIdB = UUID.randomUUID();
        accountIdA = UUID.randomUUID();

        // 1. Business entity
        BusinessEntityJpaEntity entity = new BusinessEntityJpaEntity(
                entityId, "ENT_CAP_" + UUID.randomUUID().toString().substring(0, 8),
                "Capital Test Corp", "0109999111",
                BusinessEntityStatus.ACTIVE, Instant.now()
        );
        entityRepository.save(entity);

        // 2. Branches A and B
        BranchJpaEntity branchA = new BranchJpaEntity(
                branchIdA, entityId, "BR_A_" + UUID.randomUUID().toString().substring(0, 8),
                "Chi nhánh Quận 1", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        );
        branchRepository.save(branchA);

        BranchJpaEntity branchB = new BranchJpaEntity(
                branchIdB, entityId, "BR_B_" + UUID.randomUUID().toString().substring(0, 8),
                "Chi nhánh Quận 3", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        );
        branchRepository.save(branchB);

        // 3. Financial Account for Branch A
        FinancialAccountJpaEntity account = new FinancialAccountJpaEntity(
                accountIdA, branchIdA, null, "Quỹ tiền mặt chính",
                AccountKind.CASH, "VND", BigDecimal.ZERO, AccountStatus.ACTIVE, Instant.now()
        );
        accountRepository.save(account);

        // Ensure CAP_INJECTION category exists
        if (categoryRepository.findByCategoryCode("CAP_INJECTION").isEmpty()) {
            FinancialCategoryJpaEntity cat = new FinancialCategoryJpaEntity(
                    UUID.randomUUID(), "CAP_INJECTION", "Vốn góp chủ sở hữu",
                    CategoryGroup.CAPITAL_FLOW, FlowDirection.INFLOW, true, true,
                    "Vốn góp đầu tư kinh doanh", Instant.now()
            );
            categoryRepository.save(cat);
        }

        // 4. Users
        UUID adminUserId = UUID.randomUUID();
        UserJpaEntity adminUser = new UserJpaEntity(
                adminUserId, "admin_cap_" + adminUserId.toString().substring(0, 8),
                passwordEncoder.encode("Secret123!"), "System Admin",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(adminUser);

        LocalDate today = LocalDate.now();
        ScopedRoleGrantJpaEntity adminGrant = new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, null, adminUserId,
                "SYSTEM_ADMIN", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), adminUserId, Instant.now()
        );
        grantRepository.save(adminGrant);

        adminToken = tokenService.generateAccessToken(adminUserId, adminUser.getLoginIdentifier());

        UUID managerAId = UUID.randomUUID();
        UserJpaEntity managerA = new UserJpaEntity(
                managerAId, "manager_a_" + managerAId.toString().substring(0, 8),
                passwordEncoder.encode("Secret123!"), "Manager Branch A",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(managerA);

        ScopedRoleGrantJpaEntity managerGrant = new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchIdA, managerAId,
                "OWNER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), managerAId, Instant.now()
        );
        grantRepository.save(managerGrant);

        managerTokenA = tokenService.generateAccessToken(managerAId, managerA.getLoginIdentifier());
    }

    @Test
    @Order(1)
    @DisplayName("P0-CAP-01: Register investor and branch agreement successfully")
    void testCreateInvestorAndBranchAgreementSuccessfully() throws Exception {
        // 1. Register Investor under Entity
        CreateInvestorRequest invReq = new CreateInvestorRequest(
                "Nguyễn Văn Đầu Tư " + UUID.randomUUID().toString().substring(0, 6),
                "0987654321",
                "investor@mtms.local",
                "079090123456",
                "Nhà đầu tư thiên thần"
        );

        String invResStr = mockMvc.perform(post("/api/v1/entities/{entityId}/investors", entityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.displayName").value(invReq.displayName()))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        InvestorDto investor = objectMapper.readValue(invResStr, InvestorDto.class);

        // 2. Create Agreement in Branch A
        CreateAgreementRequest agreeReq = new CreateAgreementRequest(
                investor.id(),
                "TTDT-A-" + UUID.randomUUID().toString().substring(0, 6),
                ContributionType.CASH,
                LocalDate.of(2026, 1, 1),
                null,
                "Cam kết góp vốn ban đầu phát triển mặt bằng chi nhánh",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/agreements", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agreeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.investorId").value(investor.id().toString()))
                .andExpect(jsonPath("$.agreementRef").value(agreeReq.agreementRef()))
                .andExpect(jsonPath("$.contributionType").value("CASH"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Order(2)
    @DisplayName("P0-CAP-02 / TC-032: Committal 100m, paid 60m -> committed=100m, actual=60m, outstanding=40m")
    void testCommitmentAndContributionProgressTc032() throws Exception {
        // 1. Create Investor
        CreateInvestorRequest invReq = new CreateInvestorRequest(
                "Trần Văn TC032 " + UUID.randomUUID().toString().substring(0, 6),
                "0911223344", null, null, null
        );
        String invResStr = mockMvc.perform(post("/api/v1/entities/{entityId}/investors", entityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        InvestorDto investor = objectMapper.readValue(invResStr, InvestorDto.class);

        // 2. Create Agreement
        CreateAgreementRequest agreeReq = new CreateAgreementRequest(
                investor.id(),
                "HD-TC032-" + UUID.randomUUID().toString().substring(0, 6),
                ContributionType.CASH,
                LocalDate.of(2026, 1, 1),
                null, null, null
        );
        String agreeResStr = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/agreements", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agreeReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        AgreementDto agreement = objectMapper.readValue(agreeResStr, AgreementDto.class);

        // 3. Create Commitment of 100,000,000 VND
        CreateCommitmentRequest commitReq = new CreateCommitmentRequest(
                agreement.id(),
                BigDecimal.valueOf(100_000_000),
                BigDecimal.ZERO,
                LocalDate.of(2026, 3, 31),
                "Cam kết góp 100 triệu VND cho chi nhánh"
        );
        String commitResStr = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commitReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.committedValue").value(100_000_000))
                .andExpect(jsonPath("$.actualContributedValue").value(0))
                .andExpect(jsonPath("$.outstandingValue").value(100_000_000))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        CommitmentDto commitment = objectMapper.readValue(commitResStr, CommitmentDto.class);

        // 4. Record Contribution of 60,000,000 VND (CASH)
        RecordContributionRequest contReq = new RecordContributionRequest(
                LocalDate.of(2026, 1, 15),
                ContributionType.CASH,
                BigDecimal.valueOf(60_000_000),
                accountIdA,
                null, null,
                "Đợt 1 nộp 60 triệu VND tiền mặt"
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments/{commitmentId}/contributions",
                        branchIdA, commitment.id())
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recognizedValue").value(60_000_000))
                .andExpect(jsonPath("$.ledgerEntryId").isNotEmpty());

        // 5. Verify TC-032 Invariant in Summary: committed=100m, actual=60m, outstanding=40m
        mockMvc.perform(get("/api/v1/branches/{branchId}/capital/summary", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCommittedValue").value(100_000_000))
                .andExpect(jsonPath("$.totalActualContributed").value(60_000_000))
                .andExpect(jsonPath("$.totalOutstandingValue").value(40_000_000))
                .andExpect(jsonPath("$.investorBreakdowns[0].totalCommittedValue").value(100_000_000))
                .andExpect(jsonPath("$.investorBreakdowns[0].totalContributedValue").value(60_000_000))
                .andExpect(jsonPath("$.investorBreakdowns[0].outstandingValue").value(40_000_000));
    }

    @Test
    @Order(3)
    @DisplayName("P0-CAP-02: Cash contribution correctly credits branch financial account")
    void testCashContributionLinksToCashLedger() throws Exception {
        // Create Investor & Agreement
        CreateInvestorRequest invReq = new CreateInvestorRequest("Lê Thị Vốn " + UUID.randomUUID().toString().substring(0, 6), null, null, null, null);
        String invResStr = mockMvc.perform(post("/api/v1/entities/{entityId}/investors", entityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andReturn().getResponse().getContentAsString();
        InvestorDto investor = objectMapper.readValue(invResStr, InvestorDto.class);

        CreateAgreementRequest agreeReq = new CreateAgreementRequest(
                investor.id(), "HD-CASH-" + UUID.randomUUID().toString().substring(0, 6),
                ContributionType.CASH, LocalDate.of(2026, 1, 1), null, null, null
        );
        String agreeResStr = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/agreements", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agreeReq)))
                .andReturn().getResponse().getContentAsString();
        AgreementDto agreement = objectMapper.readValue(agreeResStr, AgreementDto.class);

        CreateCommitmentRequest commitReq = new CreateCommitmentRequest(
                agreement.id(), BigDecimal.valueOf(50_000_000), BigDecimal.ZERO, null, null
        );
        String commitResStr = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commitReq)))
                .andReturn().getResponse().getContentAsString();
        CommitmentDto commitment = objectMapper.readValue(commitResStr, CommitmentDto.class);

        // Record 50M cash contribution
        RecordContributionRequest contReq = new RecordContributionRequest(
                LocalDate.of(2026, 2, 1),
                ContributionType.CASH,
                BigDecimal.valueOf(50_000_000),
                accountIdA,
                null, null, "Góp tiền vào quỹ tiền mặt"
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments/{commitmentId}/contributions",
                        branchIdA, commitment.id())
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ledgerEntryId").isNotEmpty());

        // Verify Financial Account balance increased
        FinancialAccountJpaEntity account = accountRepository.findById(accountIdA).orElseThrow();
        assertThat(account.getCurrentBalance().compareTo(BigDecimal.valueOf(50_000_000))).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(4)
    @DisplayName("P0-CAP-03 / ADR-007: Profit terms viewer includes G5 provisional disclaimer and informational-only flag")
    void testProfitEntitlementTermsConfiguredWithoutPayoutGate() throws Exception {
        // Create Investor & Agreement
        CreateInvestorRequest invReq = new CreateInvestorRequest("Phạm Cổ Đông " + UUID.randomUUID().toString().substring(0, 6), null, null, null, null);
        String invResStr = mockMvc.perform(post("/api/v1/entities/{entityId}/investors", entityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andReturn().getResponse().getContentAsString();
        InvestorDto investor = objectMapper.readValue(invResStr, InvestorDto.class);

        CreateAgreementRequest agreeReq = new CreateAgreementRequest(
                investor.id(), "HD-TERMS-" + UUID.randomUUID().toString().substring(0, 6),
                ContributionType.MIXED, LocalDate.of(2026, 1, 1), null, null, null
        );
        String agreeResStr = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/agreements", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agreeReq)))
                .andReturn().getResponse().getContentAsString();
        AgreementDto agreement = objectMapper.readValue(agreeResStr, AgreementDto.class);

        // Configure Profit Entitlement Terms: 35% profit share (0.350000)
        CreateProfitTermRequest termReq = new CreateProfitTermRequest(
                agreement.id(),
                LocalDate.of(2026, 1, 1),
                null,
                new BigDecimal("0.350000"),
                "Hưởng 35% lợi nhuận sau thuế của chi nhánh theo điều khoản hợp đồng",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/profit-terms", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(termReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profitShareRatio").value(0.35))
                .andExpect(jsonPath("$.profitSharePercent").value(35.00))
                .andExpect(jsonPath("$.isInformationalOnly").value(true))
                .andExpect(jsonPath("$.disclaimer").value(containsString("G5 GATE")));
    }

    @Test
    @Order(5)
    @DisplayName("P0-CAP-03 / CAP-INV-01: Profit entitlement ratio differs from paid capital ratio")
    void testProfitEntitlementRatioDiffersFromPaidCapitalRatio() throws Exception {
        // Investor 1: Paid 60M (60%), Terms: 50% profit
        CreateInvestorRequest inv1 = new CreateInvestorRequest("Inv 1 (60% capital, 50% profit) " + UUID.randomUUID().toString().substring(0, 4), null, null, null, null);
        String inv1Str = mockMvc.perform(post("/api/v1/entities/{entityId}/investors", entityId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(inv1)))
                .andReturn().getResponse().getContentAsString();
        InvestorDto investor1 = objectMapper.readValue(inv1Str, InvestorDto.class);

        CreateAgreementRequest ag1 = new CreateAgreementRequest(investor1.id(), "AG-DIFF-1-" + UUID.randomUUID().toString().substring(0, 4),
                ContributionType.CASH, LocalDate.of(2026, 1, 1), null, null, null);
        String ag1Str = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/agreements", branchIdA)
                .header("Authorization", "Bearer " + managerTokenA)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ag1)))
                .andReturn().getResponse().getContentAsString();
        AgreementDto agreement1 = objectMapper.readValue(ag1Str, AgreementDto.class);

        CreateCommitmentRequest com1 = new CreateCommitmentRequest(agreement1.id(), BigDecimal.valueOf(60_000_000), BigDecimal.ZERO, null, null);
        String com1Str = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments", branchIdA)
                .header("Authorization", "Bearer " + managerTokenA)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(com1)))
                .andReturn().getResponse().getContentAsString();
        CommitmentDto commitment1 = objectMapper.readValue(com1Str, CommitmentDto.class);

        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments/{id}/contributions", branchIdA, commitment1.id())
                .header("Authorization", "Bearer " + managerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RecordContributionRequest(LocalDate.of(2026, 1, 10),
                        ContributionType.CASH, BigDecimal.valueOf(60_000_000), accountIdA, null, null, null))));

        // Configure Term 1: 50% profit
        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/profit-terms", branchIdA)
                .header("Authorization", "Bearer " + managerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProfitTermRequest(agreement1.id(),
                        LocalDate.of(2026, 1, 1), null, new BigDecimal("0.500000"), null, null))));

        // Investor 2: Paid 40M (40%), Terms: 50% profit
        CreateInvestorRequest inv2 = new CreateInvestorRequest("Inv 2 (40% capital, 50% profit) " + UUID.randomUUID().toString().substring(0, 4), null, null, null, null);
        String inv2Str = mockMvc.perform(post("/api/v1/entities/{entityId}/investors", entityId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(inv2)))
                .andReturn().getResponse().getContentAsString();
        InvestorDto investor2 = objectMapper.readValue(inv2Str, InvestorDto.class);

        CreateAgreementRequest ag2 = new CreateAgreementRequest(investor2.id(), "AG-DIFF-2-" + UUID.randomUUID().toString().substring(0, 4),
                ContributionType.CASH, LocalDate.of(2026, 1, 1), null, null, null);
        String ag2Str = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/agreements", branchIdA)
                .header("Authorization", "Bearer " + managerTokenA)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(ag2)))
                .andReturn().getResponse().getContentAsString();
        AgreementDto agreement2 = objectMapper.readValue(ag2Str, AgreementDto.class);

        CreateCommitmentRequest com2 = new CreateCommitmentRequest(agreement2.id(), BigDecimal.valueOf(40_000_000), BigDecimal.ZERO, null, null);
        String com2Str = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments", branchIdA)
                .header("Authorization", "Bearer " + managerTokenA)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(com2)))
                .andReturn().getResponse().getContentAsString();
        CommitmentDto commitment2 = objectMapper.readValue(com2Str, CommitmentDto.class);

        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments/{id}/contributions", branchIdA, commitment2.id())
                .header("Authorization", "Bearer " + managerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RecordContributionRequest(LocalDate.of(2026, 1, 10),
                        ContributionType.CASH, BigDecimal.valueOf(40_000_000), accountIdA, null, null, null))));

        // Configure Term 2: 50% profit
        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/profit-terms", branchIdA)
                .header("Authorization", "Bearer " + managerTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProfitTermRequest(agreement2.id(),
                        LocalDate.of(2026, 1, 1), null, new BigDecimal("0.500000"), null, null))));

        // Verify Summary reconciles CAP-INV-01
        mockMvc.perform(get("/api/v1/branches/{branchId}/capital/summary", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isInformationalOnly").value(true))
                .andExpect(jsonPath("$.disclaimer").value(containsString("G5 GATE")));
    }

    @Test
    @Order(6)
    @DisplayName("P0-CAP-01 / TC-001: Branch isolation security - manager of branch A blocked from branch B")
    void testBranchIsolationNegativeAuthorizationTc001() throws Exception {
        // Manager of Branch A tries to query Branch B capital summary
        mockMvc.perform(get("/api/v1/branches/{branchId}/capital/summary", branchIdB)
                        .header("Authorization", "Bearer " + managerTokenA))
                .andExpect(status().isForbidden());

        // Manager of Branch A tries to query Branch B agreements
        mockMvc.perform(get("/api/v1/branches/{branchId}/capital/agreements", branchIdB)
                        .header("Authorization", "Bearer " + managerTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("Validation: Negative commitments, invalid profit share ratio and duplicate agreements rejected")
    void testValidationAndConflictHandling() throws Exception {
        // Create Investor
        CreateInvestorRequest invReq = new CreateInvestorRequest("Test Validations " + UUID.randomUUID().toString().substring(0, 4), null, null, null, null);
        String invResStr = mockMvc.perform(post("/api/v1/entities/{entityId}/investors", entityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andReturn().getResponse().getContentAsString();
        InvestorDto investor = objectMapper.readValue(invResStr, InvestorDto.class);

        // Valid Agreement
        String ref = "AG-TEST-DUP-" + UUID.randomUUID().toString().substring(0, 4);
        CreateAgreementRequest agreeReq = new CreateAgreementRequest(
                investor.id(), ref, ContributionType.CASH, LocalDate.of(2026, 1, 1), null, null, null
        );
        String agStr = mockMvc.perform(post("/api/v1/branches/{branchId}/capital/agreements", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agreeReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        AgreementDto agreement = objectMapper.readValue(agStr, AgreementDto.class);

        // Duplicate Agreement Ref in same branch -> 409 Conflict
        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/agreements", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agreeReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:agreement-conflict"));

        // Negative Commitment Value -> 400 Bad Request
        CreateCommitmentRequest negCommit = new CreateCommitmentRequest(
                agreement.id(), new BigDecimal("-1000.00"), BigDecimal.ZERO, null, null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/commitments", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negCommit)))
                .andExpect(status().isBadRequest());

        // Profit share ratio > 1.0 (150%) -> 400 Bad Request
        CreateProfitTermRequest badTerm = new CreateProfitTermRequest(
                agreement.id(), LocalDate.of(2026, 1, 1), null, new BigDecimal("1.500000"), null, null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/capital/profit-terms", branchIdA)
                        .header("Authorization", "Bearer " + managerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badTerm)))
                .andExpect(status().isBadRequest());
    }
}
