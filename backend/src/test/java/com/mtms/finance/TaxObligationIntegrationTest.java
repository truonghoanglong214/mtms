package com.mtms.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.finance.application.dto.CreateTaxObligationCommand;
import com.mtms.finance.application.dto.PayTaxObligationCommand;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.PaymentMethod;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.domain.TaxObligationStatus;
import com.mtms.finance.domain.TaxObligationType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.EntityTaxObligationJpaEntity;
import com.mtms.finance.infrastructure.persistence.EntityTaxObligationJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.TaxPaymentJpaEntity;
import com.mtms.finance.infrastructure.persistence.TaxPaymentJpaRepository;
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
 * P0-FIN-07 / BR-FIN-04 / TC-012 / FE-P0-15
 * Integration test covering manual tax obligations lifecycle:
 * creation, partial & full payment settlement with cash ledger OUTFLOW,
 * remaining liability validation, overpayment rejection, cancellation safety,
 * entity uniqueness, and branch scoping isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaxObligationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityTaxObligationJpaRepository obligationRepository;

    @Autowired
    private TaxPaymentJpaRepository paymentRepository;

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
    private UUID otherEntityId;
    private UUID branchId;
    private UUID otherBranchId;
    private UserJpaEntity ownerUser;
    private UserJpaEntity otherBranchOwner;
    private String tokenOwner;
    private String tokenOtherOwner;

    private FinancialAccountJpaEntity bankAccount;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        obligationRepository.deleteAll();
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
                entityId, "ENT_TAX_01", "Tax Obligation Entity Corp", "0109999444", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR-TAX-01", "Chi nhánh Thuế 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // Other entity and branch for isolation testing
        otherEntityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                otherEntityId, "ENT_TAX_02", "Other Entity Corp", "0109999555", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, otherEntityId, "BR-TAX-02", "Chi nhánh Thuế 2", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Users & Grants
        ownerUser = createUser("owner_tax", "Chủ Cửa Hàng Thuế");
        otherBranchOwner = createUser("other_owner_tax", "Chủ Chi Nhánh Khác");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, ownerUser.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), ownerUser.getId(), Instant.now()
        ));

        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), otherEntityId, otherBranchId, otherBranchOwner.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), otherBranchOwner.getId(), Instant.now()
        ));

        tokenOwner = jwtTokenService.generateAccessToken(ownerUser.getId(), ownerUser.getLoginIdentifier());
        tokenOtherOwner = jwtTokenService.generateAccessToken(otherBranchOwner.getId(), otherBranchOwner.getLoginIdentifier());

        // 3. Seed System Category for Tax Payment
        seedCategories();

        // 4. Financial Account: Bank account with 50,000,000 VND initial balance
        BranchBankAccountJpaEntity branchBank = branchBankRepository.save(new BranchBankAccountJpaEntity(
                UUID.randomUUID(), branchId, "Vietcombank", "CTY TAX 01", "cipher_tax", "**** **** 5678", true, BankAccountStatus.ACTIVE, Instant.now()
        ));

        bankAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, branchBank.getId(), "Tài khoản VCB Chi Nhánh",
                AccountKind.BANK, "VND", new BigDecimal("50000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));
    }

    private void seedCategories() {
        categoryRepository.saveAll(List.of(
                new FinancialCategoryJpaEntity(
                        UUID.randomUUID(),
                        "TAX_PAYMENT",
                        "Chi nộp thuế và nghĩa vụ nhà nước",
                        CategoryGroup.OTHER,
                        FlowDirection.OUTFLOW,
                        true,
                        true,
                        "Chi nộp thuế môn bài, thuế GTGT, thuế TNDN, thuế TNCN (BR-FIN-04)",
                        Instant.now()
                )
        ));
    }

    private UserJpaEntity createUser(String login, String name) {
        return userRepository.save(new UserJpaEntity(
                UUID.randomUUID(), login, passwordEncoder.encode("TestPass123!"), name, UserStatus.ACTIVE, Instant.now()
        ));
    }

    @Test
    @Order(1)
    @DisplayName("TC-012: Create tax obligation -> OPEN status, remainingLiability equals amountDue")
    void testCreateTaxObligation() throws Exception {
        CreateTaxObligationCommand command = new CreateTaxObligationCommand(
                "TAX-2026-Q1-VAT",
                TaxObligationType.VAT,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("12500000.00"),
                true,
                "Thuế GTGT tạm tính Q1/2026",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.obligationCode", is("TAX-2026-Q1-VAT")))
                .andExpect(jsonPath("$.obligationType", is("VAT")))
                .andExpect(jsonPath("$.amountDue", is(12500000.00)))
                .andExpect(jsonPath("$.amountPaid").value(0))
                .andExpect(jsonPath("$.remainingLiability", is(12500000.00)))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.branchId", is(branchId.toString())));

        List<EntityTaxObligationJpaEntity> obligations = obligationRepository.findAll();
        assertThat(obligations).hasSize(1);
        assertThat(obligations.getFirst().getObligationCode()).isEqualTo("TAX-2026-Q1-VAT");
    }

    @Test
    @Order(2)
    @DisplayName("TC-012: Partial payment -> status PARTIALLY_PAID, posts OUTFLOW cash ledger entry, reduces bank balance")
    void testPartialPaymentLifecycle() throws Exception {
        // 1. Create obligation (12,500,000 VND)
        CreateTaxObligationCommand createCmd = new CreateTaxObligationCommand(
                "TAX-2026-Q1-VAT-PARTIAL",
                TaxObligationType.VAT,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("12500000.00"),
                true,
                "Thuế GTGT Q1",
                null
        );

        String createResponse = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String obligationIdStr = objectMapper.readTree(createResponse).get("id").asText();
        UUID obligationId = UUID.fromString(obligationIdStr);

        // 2. Pay 5,000,000 VND from bankAccount (initial 50M -> should become 45M)
        PayTaxObligationCommand payCmd = new PayTaxObligationCommand(
                LocalDate.of(2026, 4, 15),
                new BigDecimal("5000000.00"),
                PaymentMethod.BANK_TRANSFER,
                bankAccount.getId(),
                "UNC-20260415-001",
                "Nộp đợt 1 thuế GTGT Q1",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay", branchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(obligationIdStr)))
                .andExpect(jsonPath("$.amountDue", is(12500000.00)))
                .andExpect(jsonPath("$.amountPaid", is(5000000.00)))
                .andExpect(jsonPath("$.remainingLiability", is(7500000.00)))
                .andExpect(jsonPath("$.status", is("PARTIALLY_PAID")))
                .andExpect(jsonPath("$.payments", hasSize(1)))
                .andExpect(jsonPath("$.payments[0].amount", is(5000000.00)))
                .andExpect(jsonPath("$.payments[0].receiptNumber", is("UNC-20260415-001")));

        // 3. Verify bank balance reduced by 5,000,000 VND
        FinancialAccountJpaEntity updatedAccount = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("45000000.00"));

        // 4. Verify Cash Ledger entry
        List<CashLedgerEntryJpaEntity> ledgerEntries = ledgerEntryRepository.findAll();
        assertThat(ledgerEntries).hasSize(1);
        CashLedgerEntryJpaEntity entry = ledgerEntries.getFirst();
        assertThat(entry.getEntryType()).isEqualTo(EntryType.OUTFLOW);
        assertThat(entry.getSourceType()).isEqualTo(SourceType.TAX_PAYMENT);
        assertThat(entry.getAmount()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(entry.getSignedAmount()).isEqualByComparingTo(new BigDecimal("-5000000.00"));
        assertThat(entry.getRunningBalance()).isEqualByComparingTo(new BigDecimal("45000000.00"));
    }

    @Test
    @Order(3)
    @DisplayName("TC-012: Full settlement -> second installment pays remaining liability -> status PAID")
    void testFullSettlementLifecycle() throws Exception {
        // 1. Create obligation (6,000,000 VND)
        CreateTaxObligationCommand createCmd = new CreateTaxObligationCommand(
                "TAX-2026-MON-BAI",
                TaxObligationType.MON_BAI,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 1, 30),
                new BigDecimal("6000000.00"),
                true,
                "Lệ phí môn bài năm 2026",
                null
        );

        String createResponse = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID obligationId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        // 2. Pay 4,000,000 VND (partial)
        PayTaxObligationCommand pay1 = new PayTaxObligationCommand(
                LocalDate.of(2026, 1, 15),
                new BigDecimal("4000000.00"),
                PaymentMethod.BANK_TRANSFER,
                bankAccount.getId(),
                "UNC-MONBAI-01",
                "Nộp đợt 1",
                null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay", branchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pay1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PARTIALLY_PAID")))
                .andExpect(jsonPath("$.remainingLiability", is(2000000.00)));

        // 3. Pay remaining 2,000,000 VND (full settlement)
        PayTaxObligationCommand pay2 = new PayTaxObligationCommand(
                LocalDate.of(2026, 1, 20),
                new BigDecimal("2000000.00"),
                PaymentMethod.BANK_TRANSFER,
                bankAccount.getId(),
                "UNC-MONBAI-02",
                "Nộp tất toán đợt 2",
                null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay", branchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pay2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.amountPaid", is(6000000.00)))
                .andExpect(jsonPath("$.remainingLiability").value(0))
                .andExpect(jsonPath("$.payments", hasSize(2)));

        // 4. Verify account balance reduced by total 6M (50M -> 44M)
        FinancialAccountJpaEntity updatedAccount = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("44000000.00"));
    }

    @Test
    @Order(4)
    @DisplayName("TC-012: Overpayment rejected with 400 (tax-payment-exceeds-remaining-liability)")
    void testOverpaymentRejected() throws Exception {
        // Create obligation with 5,000,000 VND
        CreateTaxObligationCommand createCmd = new CreateTaxObligationCommand(
                "TAX-2026-OVERPAY",
                TaxObligationType.CIT,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("5000000.00"),
                true,
                "Thuế TNDN tạm nộp",
                null
        );

        String createResponse = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID obligationId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        // Attempt to pay 5,000,001 VND (> remaining 5M)
        PayTaxObligationCommand overpayCmd = new PayTaxObligationCommand(
                LocalDate.of(2026, 4, 15),
                new BigDecimal("5000000.01"),
                PaymentMethod.BANK_TRANSFER,
                bankAccount.getId(),
                "UNC-FAIL",
                "Nộp dư",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay", branchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overpayCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:tax-payment-exceeds-remaining-liability")));
    }

    @Test
    @Order(5)
    @DisplayName("TC-012: Paying already paid obligation rejected with 400 (tax-obligation-already-paid)")
    void testPayingSettledObligationRejected() throws Exception {
        CreateTaxObligationCommand createCmd = new CreateTaxObligationCommand(
                "TAX-2026-PAID-TEST",
                TaxObligationType.OTHER,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("1000000.00"),
                true,
                "Thuế khác",
                null
        );

        String createResponse = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID obligationId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        // Settle completely
        PayTaxObligationCommand settleCmd = new PayTaxObligationCommand(
                LocalDate.of(2026, 4, 10),
                new BigDecimal("1000000.00"),
                PaymentMethod.BANK_TRANSFER,
                bankAccount.getId(),
                "UNC-100",
                "Tất toán",
                null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay", branchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settleCmd)))
                .andExpect(status().isOk());

        // Attempt another payment on settled obligation
        PayTaxObligationCommand extraPayCmd = new PayTaxObligationCommand(
                LocalDate.of(2026, 4, 11),
                new BigDecimal("50000.00"),
                PaymentMethod.BANK_TRANSFER,
                bankAccount.getId(),
                "UNC-EXTRA",
                "Nộp thêm",
                null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay", branchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraPayCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:tax-obligation-already-paid")));
    }

    @Test
    @Order(6)
    @DisplayName("TC-012: Cancel allowed when unpaid; rejected when partially paid with 400 (cannot-cancel-partially-paid-tax)")
    void testCancelLifecycle() throws Exception {
        // 1. Create obligation to cancel
        CreateTaxObligationCommand createCmd = new CreateTaxObligationCommand(
                "TAX-2026-TO-CANCEL",
                TaxObligationType.PIT,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("3000000.00"),
                true,
                "Thuế TNCN",
                null
        );

        String res1 = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID obligationId = UUID.fromString(objectMapper.readTree(res1).get("id").asText());

        // Cancel unpaid obligation -> success
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/cancel", branchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Attempt to pay cancelled obligation -> rejected with 400
        PayTaxObligationCommand payCmd = new PayTaxObligationCommand(
                LocalDate.of(2026, 4, 15),
                new BigDecimal("1000000.00"),
                PaymentMethod.BANK_TRANSFER,
                bankAccount.getId(),
                "UNC-CANCELLED",
                "Cố nộp cho nghĩa vụ đã hủy",
                null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay", branchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payCmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:tax-obligation-cancelled")));

        // 2. Create another obligation, partially pay it, then attempt cancel -> rejected with 400
        CreateTaxObligationCommand createCmd2 = new CreateTaxObligationCommand(
                "TAX-2026-PARTIAL-CANCEL",
                TaxObligationType.PIT,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("4000000.00"),
                true,
                "Thuế TNCN",
                null
        );

        String res2 = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID obligationId2 = UUID.fromString(objectMapper.readTree(res2).get("id").asText());

        // Partially pay 1M
        PayTaxObligationCommand payPartCmd = new PayTaxObligationCommand(
                LocalDate.of(2026, 4, 15),
                new BigDecimal("1000000.00"),
                PaymentMethod.BANK_TRANSFER,
                bankAccount.getId(),
                "UNC-PART",
                "Nộp đợt 1",
                null
        );
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay", branchId, obligationId2)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payPartCmd)))
                .andExpect(status().isOk());

        // Cancel partially paid obligation -> rejected with 400
        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations/{id}/cancel", branchId, obligationId2)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:cannot-cancel-partially-paid-tax")));
    }

    @Test
    @Order(7)
    @DisplayName("TC-012: Duplicate obligation code in same entity rejected with 409 (tax-obligation-already-exists)")
    void testDuplicateObligationCodeRejected() throws Exception {
        CreateTaxObligationCommand cmd1 = new CreateTaxObligationCommand(
                "TAX-2026-DUPLICATE",
                TaxObligationType.VAT,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("10000000.00"),
                true,
                "Thuế đợt 1",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd1)))
                .andExpect(status().isCreated());

        // Duplicate insert with same code in same business entity
        CreateTaxObligationCommand cmd2 = new CreateTaxObligationCommand(
                "TAX-2026-DUPLICATE",
                TaxObligationType.VAT,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("8000000.00"),
                true,
                "Thuế đợt 2 (mã trùng)",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:tax-obligation-already-exists")));
    }

    @Test
    @Order(8)
    @DisplayName("TC-012: Branch & Entity isolation -> other entity branch cannot access or pay obligation")
    void testBranchEntityIsolation() throws Exception {
        CreateTaxObligationCommand cmd = new CreateTaxObligationCommand(
                "TAX-2026-ISOLATION",
                TaxObligationType.VAT,
                "Chi cục Thuế Quận 1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                new BigDecimal("5000000.00"),
                true,
                "Thuế bảo mật",
                null
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/finance/tax-obligations", branchId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID obligationId = UUID.fromString(objectMapper.readTree(createRes).get("id").asText());

        // Other branch user from different entity attempts GET -> 404 Not Found
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/tax-obligations/{id}", otherBranchId, obligationId)
                        .header("Authorization", "Bearer " + tokenOtherOwner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:tax-obligation-not-found")));

        // Other branch user list obligations -> should not see the other branch's obligation
        mockMvc.perform(get("/api/v1/branches/{branchId}/finance/tax-obligations", otherBranchId)
                        .header("Authorization", "Bearer " + tokenOtherOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
