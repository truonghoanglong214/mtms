package com.mtms.finance.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.finance.application.dto.CreateManualPayableCommand;
import com.mtms.finance.application.dto.CreateReceivableCommand;
import com.mtms.finance.application.dto.RecordReceiptCommand;
import com.mtms.finance.application.dto.SettlePayableCommand;
import com.mtms.finance.domain.AccountKind;
import com.mtms.finance.domain.AccountStatus;
import com.mtms.finance.domain.CategoryGroup;
import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.FlowDirection;
import com.mtms.finance.domain.PaymentMethod;
import com.mtms.finance.domain.ReceivableStatus;
import com.mtms.finance.domain.SourceType;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaEntity;
import com.mtms.finance.infrastructure.persistence.CashLedgerEntryJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialAccountJpaRepository;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaEntity;
import com.mtms.finance.infrastructure.persistence.FinancialCategoryJpaRepository;
import com.mtms.finance.infrastructure.persistence.PayablePaymentJpaEntity;
import com.mtms.finance.infrastructure.persistence.PayablePaymentJpaRepository;
import com.mtms.finance.infrastructure.persistence.ReceivableJpaEntity;
import com.mtms.finance.infrastructure.persistence.ReceivableJpaRepository;
import com.mtms.finance.infrastructure.persistence.ReceivableReceiptJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.domain.PayableStatus;
import com.mtms.inventory.domain.PaymentStatus;
import com.mtms.inventory.domain.ReceiptStatus;
import com.mtms.inventory.infrastructure.persistence.PayableJpaEntity;
import com.mtms.inventory.infrastructure.persistence.PayableJpaRepository;
import com.mtms.inventory.infrastructure.persistence.PurchaseReceiptJpaEntity;
import com.mtms.inventory.infrastructure.persistence.PurchaseReceiptJpaRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-FIN-05 / BR-FIN-02 / TC-009
 * Integration test proving payable/receivable ledgers, partial and full debt settlements,
 * zero double OPEX, cash subledger integration, and RFC 7807 error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayableSettlementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PayablePaymentJpaRepository paymentRepository;

    @Autowired
    private PayableJpaRepository payableRepository;

    @Autowired
    private PurchaseReceiptJpaRepository purchaseReceiptRepository;

    @Autowired
    private ReceivableReceiptJpaRepository receivableReceiptRepository;

    @Autowired
    private ReceivableJpaRepository receivableRepository;

    @Autowired
    private CashLedgerEntryJpaRepository ledgerEntryRepository;

    @Autowired
    private FinancialAccountJpaRepository financialAccountRepository;

    @Autowired
    private FinancialCategoryJpaRepository categoryRepository;

    @Autowired
    private BranchBankAccountJpaRepository branchBankRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchId;
    private UUID otherBranchId;
    private UserJpaEntity manager;
    private UserJpaEntity unauthorizedUser;
    private String tokenManager;
    private String tokenUnauthorized;

    private FinancialAccountJpaEntity cashAccount;
    private FinancialAccountJpaEntity bankAccount;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        receivableReceiptRepository.deleteAll();
        receivableRepository.deleteAll();
        purchaseReceiptRepository.deleteAll();
        payableRepository.deleteAll();
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
                entityId, "ENT_PAY_INT", "Payable Integration Testing Co", "0109999888", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR_PAY_01", "Chi nhánh Công Nợ 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        otherBranchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                otherBranchId, entityId, "BR_PAY_02", "Chi nhánh Cách Ly 2", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Users & Grants
        manager = createUser("manager_payable", "Quản Lý Công Nợ");
        unauthorizedUser = createUser("unauth_payable", "Người Ngoài Chi Nhánh");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, manager.getId(),
                "BRANCH_OWNER", "ALL", "ALL", "ALL",
                today.minusDays(10), today.plusDays(30), manager.getId(), Instant.now()
        ));

        tokenManager = jwtTokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());
        tokenUnauthorized = jwtTokenService.generateAccessToken(unauthorizedUser.getId(), unauthorizedUser.getLoginIdentifier());

        // 3. System Categories
        seedSystemCategories();

        // 4. Financial Accounts: Cash with 10M, Bank with 20M
        cashAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, null, "Quỹ tiền mặt chi nhánh",
                AccountKind.CASH, "VND", new BigDecimal("10000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));

        BranchBankAccountJpaEntity branchBank = branchBankRepository.save(new BranchBankAccountJpaEntity(
                UUID.randomUUID(), branchId, "Vietcombank", "CTY CONG NO", "cipher_acc_pay", "**** **** 9999", true, BankAccountStatus.ACTIVE, Instant.now()
        ));

        bankAccount = financialAccountRepository.save(new FinancialAccountJpaEntity(
                UUID.randomUUID(), branchId, branchBank.getId(), "Tài khoản Vietcombank",
                AccountKind.BANK, "VND", new BigDecimal("20000000.00"), AccountStatus.ACTIVE, Instant.now()
        ));
    }

    private void seedSystemCategories() {
        categoryRepository.saveAll(List.of(
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REV_CASH", "Doanh thu bán hàng - Tiền mặt", CategoryGroup.REVENUE, FlowDirection.INFLOW, true, true, "Tiền mặt thu từ khách hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REV_BANK", "Doanh thu bán hàng - Chuyển khoản", CategoryGroup.REVENUE, FlowDirection.INFLOW, true, true, "Tiền chuyển khoản ngân hàng", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "TRF_INTERNAL", "Chuyển quỹ nội bộ", CategoryGroup.INTERNAL_TRANSFER, FlowDirection.BOTH, true, true, "Luân chuyển nội bộ", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "DEBT_SUPPLIER", "Chi trả nợ nhà cung cấp mua hàng", CategoryGroup.DEBT_SETTLEMENT, FlowDirection.OUTFLOW, true, true, "Thanh toán tiền mua nguyên vật liệu đã ghi nợ (TC-009)", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "PAY_SALARY", "Chi trả lương nhân viên", CategoryGroup.PAYROLL, FlowDirection.OUTFLOW, true, true, "Thanh toán tiền lương thực tế cho nhân sự", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "CAP_INJECTION", "Vốn góp chủ sở hữu", CategoryGroup.CAPITAL_FLOW, FlowDirection.INFLOW, true, true, "Vốn góp", Instant.now()),
                new FinancialCategoryJpaEntity(UUID.randomUUID(), "REC_OTHER", "Thu hồi công nợ phải thu khác", CategoryGroup.OTHER, FlowDirection.INFLOW, true, true, "Thu hồi công nợ phải thu khác", Instant.now())
        ));
    }

    private UserJpaEntity createUser(String username, String fullName) {
        return userRepository.save(new UserJpaEntity(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode("SecretPass123!"),
                fullName,
                UserStatus.ACTIVE,
                Instant.now()
        ));
    }

    @Test
    @Order(1)
    @DisplayName("TC-009: Partial settlement (2M Cash) + Full settlement (3M Bank), Zero double OPEX")
    void testTC009_PartialAndFullDebtSettlement_NoDoubleOpex() throws Exception {
        // 1. Setup Purchase Receipt with 5,000,000 VND UNPAID and linked Payable
        UUID receiptId = UUID.randomUUID();
        UUID payableId = UUID.randomUUID();

        PayableJpaEntity payable = payableRepository.save(new PayableJpaEntity(
                payableId,
                entityId,
                branchId,
                "Công ty TNHH Sữa Tươi Dalat",
                "PURCHASE_RECEIPT",
                receiptId,
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(25),
                new BigDecimal("5000000.00"),
                BigDecimal.ZERO,
                PayableStatus.OPEN,
                Instant.now()
        ));

        PurchaseReceiptJpaEntity receipt = purchaseReceiptRepository.save(new PurchaseReceiptJpaEntity(
                receiptId,
                entityId,
                branchId,
                "PNK-202611-001",
                null,
                LocalDate.now().minusDays(5),
                "INV-DALAT-001",
                new BigDecimal("5000000.00"),
                PaymentStatus.UNPAID,
                payableId,
                ReceiptStatus.POSTED,
                "Nhập sữa tươi thanh trùng",
                manager.getId(),
                Instant.now(),
                Instant.now()
        ));

        // 2. Installment 1: Pay 2,000,000 VND from Cash account
        SettlePayableCommand command1 = new SettlePayableCommand(
                cashAccount.getId(),
                new BigDecimal("2000000.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/payables/{id}/payments", branchId, payableId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .header("Idempotency-Key", "TC009-INSTALLMENT-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payableId", is(payableId.toString())))
                .andExpect(jsonPath("$.amount", is(2000000.00)))
                .andExpect(jsonPath("$.remainingDebt", is(3000000.00)))
                .andExpect(jsonPath("$.payableStatus", is("OPEN")))
                .andExpect(jsonPath("$.receiptPaymentStatus", is("PARTIALLY_PAID")));

        // Verify account balance: Cash dropped from 10M to 8M
        FinancialAccountJpaEntity updatedCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(updatedCash.getCurrentBalance()).isEqualByComparingTo("8000000.00");

        // Verify purchase receipt paymentStatus: PARTIALLY_PAID
        PurchaseReceiptJpaEntity updatedReceipt = purchaseReceiptRepository.findById(receiptId).orElseThrow();
        assertThat(updatedReceipt.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

        // Verify cash ledger entry: OUTFLOW under DEBT_SUPPLIER
        List<CashLedgerEntryJpaEntity> entries = ledgerEntryRepository.findAll();
        assertThat(entries).hasSize(1);
        CashLedgerEntryJpaEntity entry1 = entries.get(0);
        assertThat(entry1.getEntryType()).isEqualTo(EntryType.OUTFLOW);
        assertThat(entry1.getAmount()).isEqualByComparingTo("2000000.00");
        assertThat(entry1.getSourceType()).isEqualTo(SourceType.SUPPLIER_PAYMENT);

        // Verify Category Group is DEBT_SETTLEMENT, NOT OPERATING_EXPENSE (No double OPEX / TC-009)
        FinancialCategoryJpaEntity cat = categoryRepository.findById(entry1.getCategoryId()).orElseThrow();
        assertThat(cat.getCategoryCode()).isEqualTo("DEBT_SUPPLIER");
        assertThat(cat.getCategoryGroup()).isEqualTo(CategoryGroup.DEBT_SETTLEMENT);

        // 3. Installment 2: Pay remaining 3,000,000 VND from Bank account
        SettlePayableCommand command2 = new SettlePayableCommand(
                bankAccount.getId(),
                new BigDecimal("3000000.00"),
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "FT261118001",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/payables/{id}/payments", branchId, payableId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .header("Idempotency-Key", "TC009-INSTALLMENT-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payableId", is(payableId.toString())))
                .andExpect(jsonPath("$.amount", is(3000000.00)))
                .andExpect(jsonPath("$.remainingDebt", is(0.00)))
                .andExpect(jsonPath("$.payableStatus", is("SETTLED")))
                .andExpect(jsonPath("$.receiptPaymentStatus", is("PAID")));

        // Verify account balance: Bank dropped from 20M to 17M
        FinancialAccountJpaEntity updatedBank = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedBank.getCurrentBalance()).isEqualByComparingTo("17000000.00");

        // Verify payable status: SETTLED with paidAmount = 5,000,000
        PayableJpaEntity settledPayable = payableRepository.findById(payableId).orElseThrow();
        assertThat(settledPayable.getStatus()).isEqualTo(PayableStatus.SETTLED);
        assertThat(settledPayable.getPaidAmount()).isEqualByComparingTo("5000000.00");

        // Verify purchase receipt: PAID
        updatedReceipt = purchaseReceiptRepository.findById(receiptId).orElseThrow();
        assertThat(updatedReceipt.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        // Verify payment history: 2 installments recorded
        List<PayablePaymentJpaEntity> payments = paymentRepository.findByPayableIdOrderByPaidOnAscCreatedAtAsc(payableId);
        assertThat(payments).hasSize(2);
        assertThat(payments.get(0).getAmount()).isEqualByComparingTo("2000000.00");
        assertThat(payments.get(1).getAmount()).isEqualByComparingTo("3000000.00");

        // Verify detail endpoint
        mockMvc.perform(get("/api/v1/branches/{branchId}/payables/{id}", branchId, payableId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(payableId.toString())))
                .andExpect(jsonPath("$.status", is("SETTLED")))
                .andExpect(jsonPath("$.remainingDebt", is(0.00)))
                .andExpect(jsonPath("$.payments", hasSize(2)));
    }

    @Test
    @Order(2)
    @DisplayName("Overpayment rejection: Payment amount exceeding remaining debt returns 400 Bad Request")
    void testOverpayment_RejectedWith400() throws Exception {
        UUID payableId = UUID.randomUUID();
        payableRepository.save(new PayableJpaEntity(
                payableId,
                entityId,
                branchId,
                "NCC Trà Thái",
                "MANUAL_PAYABLE",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(15),
                new BigDecimal("2000000.00"),
                BigDecimal.ZERO,
                PayableStatus.OPEN,
                Instant.now()
        ));

        // Attempt to pay 2,500,000 VND (exceeds 2,000,000 remaining)
        SettlePayableCommand overpaymentCommand = new SettlePayableCommand(
                cashAccount.getId(),
                new BigDecimal("2500000.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/payables/{id}/payments", branchId, payableId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overpaymentCommand)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:payment-exceeds-remaining-debt")));

        // Assert debt is untouched
        PayableJpaEntity payable = payableRepository.findById(payableId).orElseThrow();
        assertThat(payable.getPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
    }

    @Test
    @Order(3)
    @DisplayName("Settled payable rejection: Payment against an already SETTLED debt returns 400 Bad Request")
    void testSettledPayable_RejectedWith400() throws Exception {
        UUID payableId = UUID.randomUUID();
        payableRepository.save(new PayableJpaEntity(
                payableId,
                entityId,
                branchId,
                "NCC Bao Bì",
                "MANUAL_PAYABLE",
                null,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(10),
                new BigDecimal("1000000.00"),
                new BigDecimal("1000000.00"),
                PayableStatus.SETTLED,
                Instant.now()
        ));

        SettlePayableCommand command = new SettlePayableCommand(
                cashAccount.getId(),
                new BigDecimal("500000.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/payables/{id}/payments", branchId, payableId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:payable-already-settled")));
    }

    @Test
    @Order(4)
    @DisplayName("Insufficient balance rejection: Paying more than account balance returns 400 Bad Request")
    void testInsufficientAccountBalance_RejectedWith400() throws Exception {
        UUID payableId = UUID.randomUUID();
        payableRepository.save(new PayableJpaEntity(
                payableId,
                entityId,
                branchId,
                "NCC Máy Pha Cafe",
                "MANUAL_PAYABLE",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                new BigDecimal("50000000.00"),
                BigDecimal.ZERO,
                PayableStatus.OPEN,
                Instant.now()
        ));

        // Attempt to pay 15,000,000 VND from Cash account (which only has 10,000,000 VND)
        SettlePayableCommand command = new SettlePayableCommand(
                cashAccount.getId(),
                new BigDecimal("15000000.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/payables/{id}/payments", branchId, payableId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:insufficient-account-balance")));

        // Balance remains 10M
        FinancialAccountJpaEntity cash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(cash.getCurrentBalance()).isEqualByComparingTo("10000000.00");
    }

    @Test
    @Order(5)
    @DisplayName("Branch isolation: Unauthorized user receives 403 Forbidden")
    void testBranchIsolation_ForbiddenWith403() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/payables", branchId)
                        .header("Authorization", "Bearer " + tokenUnauthorized))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/branches/{branchId}/payables", otherBranchId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("Manual payable creation, query list and detail inspection")
    void testManualPayableCreationAndDetail() throws Exception {
        CreateManualPayableCommand command = new CreateManualPayableCommand(
                "Công ty Điện Lạnh Sài Gòn",
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                new BigDecimal("12000000.00"),
                "Sửa chữa hệ thống điều hòa quầy pha chế"
        );

        String responseBody = mockMvc.perform(post("/api/v1/branches/{branchId}/payables", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.counterpartyLabel", is("Công ty Điện Lạnh Sài Gòn")))
                .andExpect(jsonPath("$.originalAmount").value(12000000.00))
                .andExpect(jsonPath("$.paidAmount").value(0))
                .andExpect(jsonPath("$.remainingDebt").value(12000000.00))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andReturn().getResponse().getContentAsString();

        String payableId = objectMapper.readTree(responseBody).get("id").asText();

        // Query list
        mockMvc.perform(get("/api/v1/branches/{branchId}/payables", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(payableId)));
    }

    @Test
    @Order(7)
    @DisplayName("Receivable lifecycle: Creation, collection to cash/bank, over-collection rejection")
    void testReceivableLifecycleAndReceiptCollection() throws Exception {
        // 1. Create manual receivable for 4,000,000 VND
        CreateReceivableCommand createCmd = new CreateReceivableCommand(
                "Đơn vị bồi thường vỡ kính",
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                new BigDecimal("4000000.00"),
                "Bồi thường sự cố kính cường lực",
                "MANUAL_RECEIVABLE",
                null
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/receivables", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.counterpartyLabel", is("Đơn vị bồi thường vỡ kính")))
                .andExpect(jsonPath("$.originalAmount").value(4000000.00))
                .andExpect(jsonPath("$.receivedAmount").value(0))
                .andExpect(jsonPath("$.remainingAmount").value(4000000.00))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andReturn().getResponse().getContentAsString();

        String receivableId = objectMapper.readTree(createRes).get("id").asText();

        // 2. Collect receipt 1: 1,500,000 VND into Cash account
        RecordReceiptCommand receiptCmd1 = new RecordReceiptCommand(
                cashAccount.getId(),
                new BigDecimal("1500000.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/receivables/{id}/receipts", branchId, receivableId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptCmd1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(1500000.00)));

        // Verify Cash account balance increased by 1.5M (10M -> 11.5M)
        FinancialAccountJpaEntity updatedCash = financialAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertThat(updatedCash.getCurrentBalance()).isEqualByComparingTo("11500000.00");

        // 3. Collect receipt 2: remaining 2,500,000 VND into Bank account
        RecordReceiptCommand receiptCmd2 = new RecordReceiptCommand(
                bankAccount.getId(),
                new BigDecimal("2500000.00"),
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "FT_REC_2026",
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/receivables/{id}/receipts", branchId, receivableId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptCmd2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(2500000.00)));

        // Verify Bank account balance increased by 2.5M (20M -> 22.5M)
        FinancialAccountJpaEntity updatedBank = financialAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertThat(updatedBank.getCurrentBalance()).isEqualByComparingTo("22500000.00");

        // Verify receivable is SETTLED
        ReceivableJpaEntity settledRec = receivableRepository.findById(UUID.fromString(receivableId)).orElseThrow();
        assertThat(settledRec.getStatus()).isEqualTo(ReceivableStatus.SETTLED);
        assertThat(settledRec.getReceivedAmount()).isEqualByComparingTo("4000000.00");

        // 4. Over-collection attempt on settled receivable: 400 Bad Request
        RecordReceiptCommand overRecCmd = new RecordReceiptCommand(
                cashAccount.getId(),
                new BigDecimal("500000.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/receivables/{id}/receipts", branchId, receivableId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overRecCmd)))
                .andExpect(status().isBadRequest());
    }
}
