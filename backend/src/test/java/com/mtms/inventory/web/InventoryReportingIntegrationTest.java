package com.mtms.inventory.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.StockBalance;
import com.mtms.inventory.domain.StockBalanceRepository;
import com.mtms.inventory.domain.StockMovement;
import com.mtms.inventory.domain.StockMovementType;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockDocumentJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockLotJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-INV-06 / P0-INV-07 / BR-INV-01 / BR-INV-02 / TC-020 / TC-021
 * Integration tests for Inventory Dashboard, Movements Ledger, and COGS Reconciliation Read Model.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryReportingIntegrationTest {

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
    private StockBalanceJpaRepository stockBalanceRepository;

    @Autowired
    private StockBalanceRepository domainBalanceRepository;

    @Autowired
    private StockLotJpaRepository stockLotRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private StockDocumentJpaRepository stockDocumentRepository;

    @Autowired
    private com.mtms.inventory.infrastructure.persistence.PurchaseReceiptJpaRepository purchaseReceiptRepository;

    @Autowired
    private com.mtms.inventory.infrastructure.persistence.PayableJpaRepository payableRepository;

    @Autowired
    private com.mtms.inventory.infrastructure.persistence.SupplierPackConversionJpaRepository conversionRepository;

    @Autowired
    private com.mtms.inventory.infrastructure.persistence.SupplierJpaRepository supplierRepository;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchId;
    private UserJpaEntity manager;
    private UserJpaEntity userNoAccess;

    private String tokenManager;
    private String tokenNoAccess;

    private CatalogItemJpaEntity itemCoffee;
    private CatalogItemJpaEntity itemMilk;

    @BeforeEach
    void setUp() {
        stockDocumentRepository.deleteAll();
        purchaseReceiptRepository.deleteAll();
        payableRepository.deleteAll();
        stockMovementRepository.deleteAll();
        stockLotRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        conversionRepository.deleteAll();
        catalogItemRepository.deleteAll();
        supplierRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Business Entity & Branch
        entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_ALPHA", "Alpha Coffee Corp", "0109999001", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR_ALPHA_01", "Chi nhánh Quận 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Units
        UnitJpaEntity kgUnit = unitRepository.findByUnitCode("KG").orElseGet(() ->
                unitRepository.save(new UnitJpaEntity(UUID.randomUUID(), "KG", "Kilogram", UnitDimension.MASS, Instant.now()))
        );
        UnitJpaEntity canUnit = unitRepository.findByUnitCode("CAN").orElseGet(() ->
                unitRepository.save(new UnitJpaEntity(UUID.randomUUID(), "CAN", "Lon", UnitDimension.COUNT, Instant.now()))
        );

        // 3. Catalog Items
        itemCoffee = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityId, "ING-CF-01", "Cà phê hạt Robusta",
                kgUnit.getId(), ItemKind.INGREDIENT, false, false,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        itemCoffee.setBaseUnit(kgUnit);
        itemCoffee = catalogItemRepository.save(itemCoffee);

        itemMilk = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityId, "ING-MILK-01", "Sữa đặc Ông Thọ",
                canUnit.getId(), ItemKind.INGREDIENT, false, false,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        itemMilk.setBaseUnit(canUnit);
        itemMilk = catalogItemRepository.save(itemMilk);

        // 4. Users & Roles
        manager = createUser("manager_reporting", "Quản lý Cửa hàng");
        userNoAccess = createUser("no_access_user", "Người Không Có Quyền");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, manager.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(30), manager.getId(), Instant.now()
        ));

        tokenManager = jwtTokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());
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

    private void seedTc020Data() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate purchaseDate = LocalDate.of(2026, 11, 5);
        LocalDate issueDate = LocalDate.of(2026, 11, 10);

        // 1. Beginning Inventory (Opening Balance): 10kg @ 100k = 1,000,000đ
        StockMovement openingMovement = StockMovement.createOpeningBalanceMovement(
                entityId,
                branchId,
                itemCoffee.getId(),
                null,
                startDate,
                new BigDecimal("10.000"),
                new BigDecimal("100000.000000"),
                UUID.randomUUID(),
                "DK-202611-0001",
                manager.getId()
        );
        stockMovementRepository.save(StockMovementJpaEntity.fromDomain(openingMovement));

        // 2. In-Period Purchase: 10kg @ 120k = 1,200,000đ
        StockMovement purchaseMovement = StockMovement.createPurchaseReceiptMovement(
                entityId,
                branchId,
                itemCoffee.getId(),
                null,
                purchaseDate,
                new BigDecimal("10.000"),
                new BigDecimal("1200000.00"),
                new BigDecimal("120000.000000"),
                UUID.randomUUID(),
                "PNK-202611-0001",
                manager.getId()
        );
        stockMovementRepository.save(StockMovementJpaEntity.fromDomain(purchaseMovement));

        // 3. In-Period Issue: 5kg @ 110k (Moving WAC) = 550,000đ
        StockMovement issueMovement = StockMovement.createManualIssueMovement(
                entityId,
                branchId,
                itemCoffee.getId(),
                null,
                issueDate,
                new BigDecimal("5.000"),
                new BigDecimal("550000.00"),
                new BigDecimal("110000.000000"),
                UUID.randomUUID(),
                "PXK-202611-0001",
                manager.getId()
        );
        stockMovementRepository.save(StockMovementJpaEntity.fromDomain(issueMovement));

        // 4. Update Stock Balance: Ending = 15kg @ 110k = 1,650,000đ
        StockBalance balance = StockBalance.createInitial(
                entityId,
                branchId,
                itemCoffee.getId(),
                new BigDecimal("15.000"),
                new BigDecimal("110000.000000")
        );
        domainBalanceRepository.save(balance);
    }

    @Test
    @Order(1)
    @DisplayName("TC-020 (No Double Cost): Beginning 1M, Purchases 1.2M, Issue 550k -> COGS=550k (not 1.2M, not 1.75M), reconciled=true")
    void testTc020CogsReportNoDoubleCostReconciliation() throws Exception {
        seedTc020Data();

        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/cogs-report", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId", is(branchId.toString())))
                .andExpect(jsonPath("$.branchName", is("Chi nhánh Quận 1")))
                .andExpect(jsonPath("$.period", is("2026-11")))
                .andExpect(jsonPath("$.beginningStockValue", is("1000000.00")))
                .andExpect(jsonPath("$.periodPurchasesTotal", is("1200000.00")))
                .andExpect(jsonPath("$.periodCogsTotal", is("550000.00")))
                .andExpect(jsonPath("$.periodAdjustmentsTotal", is("0.00")))
                .andExpect(jsonPath("$.calculatedEndingStockValue", is("1650000.00")))
                .andExpect(jsonPath("$.actualEndingStockValue", is("1650000.00")))
                .andExpect(jsonPath("$.reconciliationDiscrepancy", is("0.00")))
                .andExpect(jsonPath("$.reconciled", is(true)))
                .andExpect(jsonPath("$.noDoubleCostVerified", is(true)))
                .andExpect(jsonPath("$.accountingExplanation", containsString("550,000.00")))
                .andExpect(jsonPath("$.accountingExplanation", containsString("1,200,000.00")))
                .andExpect(jsonPath("$.accountingExplanation", containsString("không bị tính chi phí kép")))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].itemId", is(itemCoffee.getId().toString())))
                .andExpect(jsonPath("$.items[0].itemCode", is("ING-CF-01")))
                .andExpect(jsonPath("$.items[0].beginningQty", is("10.000")))
                .andExpect(jsonPath("$.items[0].beginningValue", is("1000000.00")))
                .andExpect(jsonPath("$.items[0].purchasedQty", is("10.000")))
                .andExpect(jsonPath("$.items[0].purchasedValue", is("1200000.00")))
                .andExpect(jsonPath("$.items[0].issuedQty", is("5.000")))
                .andExpect(jsonPath("$.items[0].cogsValue", is("550000.00")))
                .andExpect(jsonPath("$.items[0].endingQty", is("15.000")))
                .andExpect(jsonPath("$.items[0].endingWac", is("110000.000000")))
                .andExpect(jsonPath("$.items[0].endingValue", is("1650000.00")));
    }

    @Test
    @Order(2)
    @DisplayName("TC-021: Dashboard KPI summary and Movements Ledger audit trail")
    void testTc021DashboardAndMovementsLedger() throws Exception {
        seedTc020Data();

        // 1. Dashboard summary
        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/dashboard", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId", is(branchId.toString())))
                .andExpect(jsonPath("$.branchName", is("Chi nhánh Quận 1")))
                .andExpect(jsonPath("$.totalOnHandQuantity", is("15.000")))
                .andExpect(jsonPath("$.totalInventoryValuation", is("1650000.00")))
                .andExpect(jsonPath("$.totalItemsTracked", is(1)))
                .andExpect(jsonPath("$.periodReceiptsTotal", is("1200000.00")))
                .andExpect(jsonPath("$.periodIssuesTotal", is("550000.00")))
                .andExpect(jsonPath("$.topValuedItems", hasSize(1)))
                .andExpect(jsonPath("$.topValuedItems[0].itemCode", is("ING-CF-01")))
                .andExpect(jsonPath("$.topValuedItems[0].inventoryValue", is("1650000.00")));

        // 2. Movements Ledger (Full audit trail)
        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/movements", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", is(3)))
                .andExpect(jsonPath("$.items", hasSize(3)))
                // First movement in desc order is MANUAL_ISSUE (date 2026-11-10)
                .andExpect(jsonPath("$.items[0].movementType", is("MANUAL_ISSUE")))
                .andExpect(jsonPath("$.items[0].signedQuantity", is("-5.000")))
                .andExpect(jsonPath("$.items[0].signedValue", is("-550000.00")))
                .andExpect(jsonPath("$.items[0].sourceRef", is("PXK-202611-0001")))
                .andExpect(jsonPath("$.items[0].createdByName", is("Quản lý Cửa hàng")));

        // 3. Filter movements by movementType
        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/movements", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("movementType", "MANUAL_ISSUE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", is(1)))
                .andExpect(jsonPath("$.items[0].movementType", is("MANUAL_ISSUE")));

        // 4. Search movements by item code
        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/movements", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("search", "ING-CF-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", is(3)));
    }

    @Test
    @Order(3)
    @DisplayName("Branch Security: Unauthorized user without branch grant receives 403 Forbidden")
    void testBranchSecurityUnauthorizedAccessReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/dashboard", branchId)
                        .header("Authorization", "Bearer " + tokenNoAccess)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/movements", branchId)
                        .header("Authorization", "Bearer " + tokenNoAccess)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/cogs-report", branchId)
                        .header("Authorization", "Bearer " + tokenNoAccess)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("Validation: Invalid date range (endDate < startDate) returns 400 Bad Request with RFC 7807 type")
    void testValidationInvalidDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/dashboard", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("startDate", "2026-11-30")
                        .param("endDate", "2026-11-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-date-range")));

        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/cogs-report", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("startDate", "2026-11-30")
                        .param("endDate", "2026-11-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-date-range")));

        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/movements", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .param("startDate", "2026-11-30")
                        .param("endDate", "2026-11-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:invalid-date-range")));
    }

    @Test
    @Order(5)
    @DisplayName("Branch Not Found: Non-existent branch ID returns 404 Not Found")
    void testBranchNotFoundReturns404() throws Exception {
        UUID nonExistentBranchId = UUID.randomUUID();

        // Grant access to non-existent branch so authorization passes but service returns 404
        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, nonExistentBranchId, manager.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(30), manager.getId(), Instant.now()
        ));

        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/dashboard", nonExistentBranchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:branch-not-found")));
    }
}
