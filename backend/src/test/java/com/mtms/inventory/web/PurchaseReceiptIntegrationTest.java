package com.mtms.inventory.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.application.dto.CreatePurchaseReceiptCommand;
import com.mtms.inventory.application.dto.CreatePurchaseReceiptLineCommand;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.PaymentStatus;
import com.mtms.inventory.domain.ReceiptStatus;
import com.mtms.inventory.domain.StockMovementType;
import com.mtms.inventory.domain.SupplierStatus;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.PayableJpaEntity;
import com.mtms.inventory.infrastructure.persistence.PayableJpaRepository;
import com.mtms.inventory.infrastructure.persistence.PurchaseReceiptJpaEntity;
import com.mtms.inventory.infrastructure.persistence.PurchaseReceiptJpaRepository;
import com.mtms.inventory.infrastructure.persistence.PurchaseReceiptLineJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockLotJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockLotJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
import com.mtms.inventory.infrastructure.persistence.SupplierJpaEntity;
import com.mtms.inventory.infrastructure.persistence.SupplierJpaRepository;
import com.mtms.inventory.infrastructure.persistence.UnitJpaEntity;
import com.mtms.inventory.infrastructure.persistence.UnitJpaRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PurchaseReceiptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private UnitJpaRepository unitRepository;

    @Autowired
    private CatalogItemJpaRepository catalogItemRepository;

    @Autowired
    private SupplierJpaRepository supplierRepository;

    @Autowired
    private StockBalanceJpaRepository stockBalanceRepository;

    @Autowired
    private StockLotJpaRepository stockLotRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private PurchaseReceiptJpaRepository purchaseReceiptRepository;

    @Autowired
    private PayableJpaRepository payableRepository;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityAId;
    private UUID branchA1Id;
    private UUID branchB1Id;

    private UserJpaEntity managerA;
    private UserJpaEntity managerB;
    private UserJpaEntity userNoAccess;

    private String tokenManagerA;
    private String tokenManagerB;
    private String tokenNoAccess;

    private SupplierJpaEntity supplierVinamilk;
    private CatalogItemJpaEntity itemMilk;
    private CatalogItemJpaEntity itemCoffee;

    @BeforeEach
    void setUp() {
        purchaseReceiptRepository.deleteAll();
        payableRepository.deleteAll();
        stockMovementRepository.deleteAll();
        stockLotRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        catalogItemRepository.deleteAll();
        supplierRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Entities
        entityAId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityAId, "ENT_A", "Entity Alpha Co.", "0109999001", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        UUID entityBId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityBId, "ENT_B", "Entity Beta Co.", "0109999002", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        // 2. Branches
        branchA1Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchA1Id, entityAId, "BR_A1", "Chi nhánh Quận 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        branchB1Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchB1Id, entityBId, "BR_B1", "Chi nhánh Bình Thạnh", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 3. Units
        UnitJpaEntity canUnit = unitRepository.findByUnitCode("CAN").orElseGet(() ->
                unitRepository.save(new UnitJpaEntity(UUID.randomUUID(), "CAN", "Lon", UnitDimension.COUNT, Instant.now()))
        );
        UnitJpaEntity kgUnit = unitRepository.findByUnitCode("KG").orElseGet(() ->
                unitRepository.save(new UnitJpaEntity(UUID.randomUUID(), "KG", "Kilogram", UnitDimension.MASS, Instant.now()))
        );

        // 4. Supplier
        supplierVinamilk = supplierRepository.save(new SupplierJpaEntity(
                UUID.randomUUID(), entityAId, "SUP-VNM", "Công ty Cổ phần Sữa Việt Nam (Vinamilk)",
                "Nhà cung cấp sữa", SupplierStatus.ACTIVE, Instant.now()
        ));

        // 5. Items
        itemMilk = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityAId, "ING-MILK-OT-380G", "Sữa đặc có đường Ông Thọ đỏ lon 380g",
                canUnit.getId(), ItemKind.INGREDIENT, true, true,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        itemMilk.setBaseUnit(canUnit);
        itemMilk = catalogItemRepository.save(itemMilk);

        itemCoffee = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityAId, "ING-COFFEE-01", "Cà phê hạt Robusta",
                kgUnit.getId(), ItemKind.INGREDIENT, false, false,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        itemCoffee.setBaseUnit(kgUnit);
        itemCoffee = catalogItemRepository.save(itemCoffee);

        // 6. Users & Roles
        managerA = createUser("manager_a", "Nguyễn Quản Lý A");
        managerB = createUser("manager_b", "Trần Quản Lý B");
        userNoAccess = createUser("no_access_user", "Người Không Có Quyền");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityAId, branchA1Id, managerA.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), managerA.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityBId, branchB1Id, managerB.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), managerB.getId(), Instant.now()
        ));

        tokenManagerA = jwtTokenService.generateAccessToken(managerA.getId(), managerA.getLoginIdentifier());
        tokenManagerB = jwtTokenService.generateAccessToken(managerB.getId(), managerB.getLoginIdentifier());
        tokenNoAccess = jwtTokenService.generateAccessToken(userNoAccess.getId(), userNoAccess.getLoginIdentifier());
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
    @DisplayName("TC-008 Verification: Unpaid 5m goods receipt increases stock balances, updates WAC, creates payable debt record, leaves cash and COGS unchanged")
    void testTc008UnpaidGoodsReceipt() throws Exception {
        // Line 1: Milk (lot tracked), 5 packs of 24 = 120 lon @ 25,000 = 3,000,000đ
        CreatePurchaseReceiptLineCommand line1 = new CreatePurchaseReceiptLineCommand(
                itemMilk.getId(),
                "Thùng 24 lon",
                new BigDecimal("5.000"),
                new BigDecimal("24.000"),
                new BigDecimal("120.000"),
                new BigDecimal("25000.000000"),
                new BigDecimal("3000000.00"),
                "LOT-202611-VNM",
                LocalDate.now().plusYears(1)
        );

        // Line 2: Coffee (no lot tracking), 10 kg @ 200,000 = 2,000,000đ
        CreatePurchaseReceiptLineCommand line2 = new CreatePurchaseReceiptLineCommand(
                itemCoffee.getId(),
                null,
                null,
                new BigDecimal("1.000"),
                new BigDecimal("10.000"),
                new BigDecimal("200000.000000"),
                new BigDecimal("2000000.00"),
                null,
                null
        );

        CreatePurchaseReceiptCommand command = new CreatePurchaseReceiptCommand(
                supplierVinamilk.getId(),
                LocalDate.now(),
                "HD-VNM-9988",
                PaymentStatus.UNPAID,
                LocalDate.now().plusDays(30),
                "Nhập hàng đợt 1 tháng 11 - nợ 30 ngày (TC-008)",
                true, // autoPost = true
                List.of(line1, line2)
        );

        // 1. Post receipt
        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/receipts")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNumber").value(org.hamcrest.Matchers.startsWith("PNK-")))
                .andExpect(jsonPath("$.status", is("POSTED")))
                .andExpect(jsonPath("$.paymentStatus", is("UNPAID")))
                .andExpect(jsonPath("$.invoiceTotal", is(5000000.0)))
                .andExpect(jsonPath("$.supplierName", is("Công ty Cổ phần Sữa Việt Nam (Vinamilk)")))
                .andExpect(jsonPath("$.payableId").isNotEmpty())
                .andExpect(jsonPath("$.totalLines", is(2)));

        // 2. Verify Stock Balances & Moving WAC
        StockBalanceJpaEntity milkBalance = stockBalanceRepository
                .findByBranchIdAndItemId(branchA1Id, itemMilk.getId()).orElseThrow();
        assertThat(milkBalance.getQuantity()).isEqualByComparingTo("120.000");
        assertThat(milkBalance.getInventoryValue()).isEqualByComparingTo("3000000.00");
        assertThat(milkBalance.getAvgUnitCost()).isEqualByComparingTo("25000.000000");

        StockBalanceJpaEntity coffeeBalance = stockBalanceRepository
                .findByBranchIdAndItemId(branchA1Id, itemCoffee.getId()).orElseThrow();
        assertThat(coffeeBalance.getQuantity()).isEqualByComparingTo("10.000");
        assertThat(coffeeBalance.getInventoryValue()).isEqualByComparingTo("2000000.00");
        assertThat(coffeeBalance.getAvgUnitCost()).isEqualByComparingTo("200000.000000");

        // 3. Verify Stock Lots (Milk has lot code LOT-202611-VNM)
        StockLotJpaEntity milkLot = stockLotRepository
                .findByBranchIdAndItemIdAndLotCode(branchA1Id, itemMilk.getId(), "LOT-202611-VNM").orElseThrow();
        assertThat(milkLot.getQuantity()).isEqualByComparingTo("120.000");
        assertThat(milkLot.getExpiresOn()).isEqualTo(LocalDate.now().plusYears(1));

        // 4. Verify Stock Movements (2 ledger entries)
        List<StockMovementJpaEntity> movements = stockMovementRepository.findAll();
        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(m -> m.getMovementType() == StockMovementType.PURCHASE_RECEIPT);
        assertThat(movements).allMatch(m -> m.getSourceType().equals("PURCHASE_RECEIPT"));

        // 5. Verify Payables Ledger (Debt record of 5,000,000đ created with status OPEN)
        List<PayableJpaEntity> payables = payableRepository.findAll();
        assertThat(payables).hasSize(1);
        PayableJpaEntity payable = payables.get(0);
        assertThat(payable.getBranchId()).isEqualTo(branchA1Id);
        assertThat(payable.getCounterpartyLabel()).isEqualTo("Công ty Cổ phần Sữa Việt Nam (Vinamilk)");
        assertThat(payable.getOriginalAmount()).isEqualByComparingTo("5000000.00");
        assertThat(payable.getPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(payable.getStatus().name()).isEqualTo("OPEN");

        // 6. Verify Payables GET endpoint
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/payables")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.totalOutstandingDebt", is(5000000.0)))
                .andExpect(jsonPath("$.items[0].counterpartyLabel", is("Công ty Cổ phần Sữa Việt Nam (Vinamilk)")))
                .andExpect(jsonPath("$.items[0].originalAmount", is(5000000.0)))
                .andExpect(jsonPath("$.items[0].remainingAmount", is(5000000.0)))
                .andExpect(jsonPath("$.items[0].status", is("OPEN")));

        // 7. Verify Receipts GET list endpoint
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/receipts")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.items[0].status", is("POSTED")))
                .andExpect(jsonPath("$.items[0].paymentStatus", is("UNPAID")));
    }

    @Test
    @Order(2)
    @DisplayName("Two-step workflow: Create Draft receipt -> Stock and Payables unchanged -> Post -> Stock and Payables updated")
    void testDraftThenPostWorkflow() throws Exception {
        CreatePurchaseReceiptLineCommand line = new CreatePurchaseReceiptLineCommand(
                itemCoffee.getId(),
                null,
                null,
                new BigDecimal("1.000"),
                new BigDecimal("20.000"),
                new BigDecimal("180000.000000"),
                new BigDecimal("3600000.00"),
                null,
                null
        );

        CreatePurchaseReceiptCommand command = new CreatePurchaseReceiptCommand(
                supplierVinamilk.getId(),
                LocalDate.now(),
                "HD-DRAFT-01",
                PaymentStatus.UNPAID,
                LocalDate.now().plusDays(15),
                "Draft receipt test",
                false, // autoPost = false -> DRAFT
                List.of(line)
        );

        // 1. Create Draft
        String responseContent = mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/receipts")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.payableId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String receiptIdStr = objectMapper.readTree(responseContent).get("id").asText();
        UUID receiptId = UUID.fromString(receiptIdStr);

        // Ensure stock balances NOT changed
        Optional<StockBalanceJpaEntity> balanceOpt = stockBalanceRepository.findByBranchIdAndItemId(branchA1Id, itemCoffee.getId());
        assertThat(balanceOpt).isEmpty();

        // Ensure payables NOT created
        assertThat(payableRepository.findAll()).isEmpty();

        // 2. Fetch Detail
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/receipts/" + receiptId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(receiptId.toString())))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].itemCode", is("ING-COFFEE-01")));

        // 3. Post Receipt
        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/receipts/" + receiptId + "/post")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("POSTED")))
                .andExpect(jsonPath("$.payableId").isNotEmpty());

        // Now stock balance is created
        StockBalanceJpaEntity balance = stockBalanceRepository.findByBranchIdAndItemId(branchA1Id, itemCoffee.getId()).orElseThrow();
        assertThat(balance.getQuantity()).isEqualByComparingTo("20.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("3600000.00");

        // Now payable is created
        assertThat(payableRepository.findAll()).hasSize(1);

        // 4. Try posting again -> 409 Conflict
        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/receipts/" + receiptId + "/post")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:receipt-already-posted")))
                .andExpect(jsonPath("$.title", is("Receipt Already Posted")));
    }

    @Test
    @Order(3)
    @DisplayName("Negative test: Missing lot code on lot-tracked item returns 400 Bad Request")
    void testMissingLotCodeOnLotTrackedItem() throws Exception {
        // itemMilk requires lot tracking, but lotCode is null
        CreatePurchaseReceiptLineCommand line = new CreatePurchaseReceiptLineCommand(
                itemMilk.getId(),
                "Thùng",
                new BigDecimal("1.000"),
                new BigDecimal("24.000"),
                new BigDecimal("24.000"),
                new BigDecimal("25000.000000"),
                new BigDecimal("600000.00"),
                null, // missing lot code!
                null
        );

        CreatePurchaseReceiptCommand command = new CreatePurchaseReceiptCommand(
                supplierVinamilk.getId(),
                LocalDate.now(),
                "HD-NO-LOT",
                PaymentStatus.UNPAID,
                null,
                "Missing lot test",
                true,
                List.of(line)
        );

        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/receipts")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:missing-lot-code")))
                .andExpect(jsonPath("$.title", is("Missing Lot Code")));
    }

    @Test
    @Order(4)
    @DisplayName("Negative test: Zero or negative received quantity returns 400 Bad Request")
    void testZeroReceivedQuantity() throws Exception {
        CreatePurchaseReceiptLineCommand line = new CreatePurchaseReceiptLineCommand(
                itemCoffee.getId(),
                null,
                null,
                new BigDecimal("1.000"),
                new BigDecimal("0.000"), // zero qty!
                new BigDecimal("100000.000000"),
                BigDecimal.ZERO,
                null,
                null
        );

        CreatePurchaseReceiptCommand command = new CreatePurchaseReceiptCommand(
                supplierVinamilk.getId(),
                LocalDate.now(),
                "HD-ZERO-QTY",
                PaymentStatus.UNPAID,
                null,
                "Zero quantity test",
                false,
                List.of(line)
        );

        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/receipts")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-receipt-quantity")))
                .andExpect(jsonPath("$.title", is("Invalid Receipt Quantity")));
    }

    @Test
    @Order(5)
    @DisplayName("Negative test: Nonexistent supplier returns 404 Not Found")
    void testNonexistentSupplier() throws Exception {
        UUID nonExistentSupplierId = UUID.randomUUID();
        CreatePurchaseReceiptLineCommand line = new CreatePurchaseReceiptLineCommand(
                itemCoffee.getId(), null, null, BigDecimal.ONE,
                new BigDecimal("10.000"), new BigDecimal("100000.000000"),
                new BigDecimal("1000000.00"), null, null
        );

        CreatePurchaseReceiptCommand command = new CreatePurchaseReceiptCommand(
                nonExistentSupplierId,
                LocalDate.now(),
                "HD-BAD-SUP",
                PaymentStatus.UNPAID,
                null,
                "Bad supplier test",
                false,
                List.of(line)
        );

        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/receipts")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:supplier-not-found")))
                .andExpect(jsonPath("$.title", is("Supplier Not Found")));
    }

    @Test
    @Order(6)
    @DisplayName("Negative test: Cross-branch access returns 403 Forbidden")
    void testCrossBranchAccessForbidden() throws Exception {
        // Manager B (Branch B1) tries to access Branch A1 receipts -> 403 Forbidden
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/receipts")
                        .header("Authorization", "Bearer " + tokenManagerB))
                .andExpect(status().isForbidden());

        // User with no access tries to access Branch A1 payables -> 403 Forbidden
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/payables")
                        .header("Authorization", "Bearer " + tokenNoAccess))
                .andExpect(status().isForbidden());
    }
}
