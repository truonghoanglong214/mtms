package com.mtms.inventory.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.application.dto.CreateStockIssueCommand;
import com.mtms.inventory.application.dto.CreateStockIssueLineCommand;
import com.mtms.inventory.application.service.StockIssueService;
import com.mtms.inventory.domain.InsufficientStockException;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.StockBalance;
import com.mtms.inventory.domain.StockBalanceRepository;
import com.mtms.inventory.domain.StockDocumentStatus;
import com.mtms.inventory.domain.StockDocumentType;
import com.mtms.inventory.domain.StockMovementType;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockDocumentJpaEntity;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StockIssueIntegrationTest {

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
    private StockIssueService stockIssueService;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchId;
    private UserJpaEntity manager;
    private UserJpaEntity employee;
    private UserJpaEntity userNoAccess;

    private String tokenManager;
    private String tokenEmployee;
    private String tokenNoAccess;

    private CatalogItemJpaEntity itemCoffee;
    private CatalogItemJpaEntity itemMilk;

    @BeforeEach
    void setUp() {
        stockDocumentRepository.deleteAll();
        stockMovementRepository.deleteAll();
        stockLotRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        catalogItemRepository.deleteAll();
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
        manager = createUser("manager_user", "Quản lý Cửa hàng");
        employee = createUser("barista_user", "Barista Ca Trưởng");
        userNoAccess = createUser("unauthorized_user", "Người Không Có Quyền");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, manager.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), manager.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, employee.getId(),
                "EMPLOYEE", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), manager.getId(), Instant.now()
        ));

        tokenManager = jwtTokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());
        tokenEmployee = jwtTokenService.generateAccessToken(employee.getId(), employee.getLoginIdentifier());
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
    @DisplayName("TC-020: 10kg@100k + 10kg@120k -> WAC=110k, issue 5kg -> Booked Cost=550k, remaining WAC=110k, ledger negative movement")
    void testTc020CreateAndPostConsumptionIssueWithMovingWac() throws Exception {
        // Setup initial balance: 10kg @ 100,000đ = 1,000,000đ
        StockBalance balance = StockBalance.createInitial(
                entityId, branchId, itemCoffee.getId(),
                new BigDecimal("10.000"), new BigDecimal("100000.000000")
        );
        // Receive another 10kg @ 120,000đ = 1,200,000đ -> total 20kg @ 110,000đ = 2,200,000đ
        balance.receiveGoods(new BigDecimal("10.000"), new BigDecimal("1200000.00"));
        domainBalanceRepository.save(balance);

        // Issue 5kg with autoPost=true
        CreateStockIssueLineCommand line = new CreateStockIssueLineCommand(
                itemCoffee.getId(), new BigDecimal("5.000"), null, "Xuất quầy bar pha chế"
        );
        CreateStockIssueCommand cmd = new CreateStockIssueCommand(
                LocalDate.now(), StockDocumentType.CONSUMPTION_ISSUE, "Xuất quầy bar ca sáng", true, List.of(line)
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/inventory/issues", branchId)
                        .header("Authorization", "Bearer " + tokenEmployee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentNumber", startsWith("PXK-")))
                .andExpect(jsonPath("$.status", is("POSTED")))
                .andExpect(jsonPath("$.totalLines", is(1)))
                .andExpect(jsonPath("$.totalQuantity", is(5.0)))
                .andExpect(jsonPath("$.totalBookedCost", is(550000.0)))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].itemCode", is("ING-CF-01")))
                .andExpect(jsonPath("$.lines[0].issueQuantity", is(5.0)))
                .andExpect(jsonPath("$.lines[0].bookedUnitCost", is(110000.0)))
                .andExpect(jsonPath("$.lines[0].bookedTotal", is(550000.0)));

        // Verify StockBalance updated: 15kg remaining, 1,650,000đ, WAC preserved at 110,000đ/kg
        StockBalanceJpaEntity updatedBalance = stockBalanceRepository.findByBranchIdAndItemId(branchId, itemCoffee.getId()).orElseThrow();
        assertThat(updatedBalance.getQuantity()).isEqualByComparingTo("15.000");
        assertThat(updatedBalance.getInventoryValue()).isEqualByComparingTo("1650000.00");
        assertThat(updatedBalance.getAvgUnitCost()).isEqualByComparingTo("110000.000000");

        // Verify StockMovement recorded
        List<StockMovementJpaEntity> movements = stockMovementRepository.findAll();
        assertThat(movements).hasSize(1);
        StockMovementJpaEntity mov = movements.get(0);
        assertThat(mov.getMovementType()).isEqualTo(StockMovementType.MANUAL_ISSUE);
        assertThat(mov.getSignedQuantity()).isEqualByComparingTo("-5.000");
        assertThat(mov.getSignedValue()).isEqualByComparingTo("-550000.00");
        assertThat(mov.getBookedAvgCost()).isEqualByComparingTo("110000.000000");
        assertThat(mov.getSourceType()).isEqualTo("STOCK_DOCUMENT");
    }

    @Test
    @Order(2)
    @DisplayName("TC-021: Request issue quantity > on-hand balance -> 400 Bad Request (urn:mtms:error:insufficient-stock), zero stock mutation")
    void testTc021RejectOverdraftIssueQuantity() throws Exception {
        // Setup initial balance: 4kg @ 50,000đ = 200,000đ
        StockBalance balance = StockBalance.createInitial(
                entityId, branchId, itemCoffee.getId(),
                new BigDecimal("4.000"), new BigDecimal("50000.000000")
        );
        domainBalanceRepository.save(balance);

        // Attempt to issue 5kg (exceeds 4kg available)
        CreateStockIssueLineCommand line = new CreateStockIssueLineCommand(
                itemCoffee.getId(), new BigDecimal("5.000"), null, "Overdraft attempt"
        );
        CreateStockIssueCommand cmd = new CreateStockIssueCommand(
                LocalDate.now(), StockDocumentType.CONSUMPTION_ISSUE, "Xuất lố", true, List.of(line)
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/inventory/issues", branchId)
                        .header("Authorization", "Bearer " + tokenEmployee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:insufficient-stock")))
                .andExpect(jsonPath("$.title", is("Insufficient Stock")));

        // Verify balance is completely untouched
        StockBalanceJpaEntity currentBalance = stockBalanceRepository.findByBranchIdAndItemId(branchId, itemCoffee.getId()).orElseThrow();
        assertThat(currentBalance.getQuantity()).isEqualByComparingTo("4.000");
        assertThat(currentBalance.getInventoryValue()).isEqualByComparingTo("200000.00");
        assertThat(currentBalance.getAvgUnitCost()).isEqualByComparingTo("50000.000000");

        // Verify zero stock movements and zero documents posted
        assertThat(stockMovementRepository.count()).isZero();
        assertThat(stockDocumentRepository.count()).isZero();
    }

    @Test
    @Order(3)
    @DisplayName("Two-step lifecycle: Create DRAFT document, query detail, post document, and reject duplicate post (409 Conflict)")
    void testDraftCreationAndSubsequentPosting() throws Exception {
        // Setup stock: 10kg @ 60,000đ = 600,000đ
        StockBalance balance = StockBalance.createInitial(
                entityId, branchId, itemMilk.getId(),
                new BigDecimal("10.000"), new BigDecimal("60000.000000")
        );
        domainBalanceRepository.save(balance);

        // Step 1: Create DRAFT issue (autoPost = false)
        CreateStockIssueLineCommand line = new CreateStockIssueLineCommand(
                itemMilk.getId(), new BigDecimal("3.000"), null, "Xuất sữa đặc pha cafe sữa"
        );
        CreateStockIssueCommand cmd = new CreateStockIssueCommand(
                LocalDate.now(), StockDocumentType.CONSUMPTION_ISSUE, "Phiếu xuất nháp", false, List.of(line)
        );

        String responseContent = mockMvc.perform(post("/api/v1/branches/{branchId}/inventory/issues", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.totalBookedCost", is(0.0)))
                .andReturn().getResponse().getContentAsString();

        UUID documentId = UUID.fromString(objectMapper.readTree(responseContent).get("id").asText());

        // Stock balance untouched during DRAFT
        StockBalanceJpaEntity balanceAfterDraft = stockBalanceRepository.findByBranchIdAndItemId(branchId, itemMilk.getId()).orElseThrow();
        assertThat(balanceAfterDraft.getQuantity()).isEqualByComparingTo("10.000");
        assertThat(stockMovementRepository.count()).isZero();

        // Step 2: Query by ID
        mockMvc.perform(get("/api/v1/branches/{branchId}/inventory/issues/{documentId}", branchId, documentId)
                        .header("Authorization", "Bearer " + tokenEmployee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(documentId.toString())))
                .andExpect(jsonPath("$.status", is("DRAFT")));

        // Step 3: Post the DRAFT document
        mockMvc.perform(post("/api/v1/branches/{branchId}/inventory/issues/{documentId}/post", branchId, documentId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("POSTED")))
                .andExpect(jsonPath("$.totalBookedCost", is(180000.0))) // 3 * 60,000đ = 180,000đ
                .andExpect(jsonPath("$.lines[0].bookedUnitCost", is(60000.0)))
                .andExpect(jsonPath("$.lines[0].bookedTotal", is(180000.0)));

        // Verify balance deducted: 7kg remaining, 420,000đ
        StockBalanceJpaEntity balanceAfterPost = stockBalanceRepository.findByBranchIdAndItemId(branchId, itemMilk.getId()).orElseThrow();
        assertThat(balanceAfterPost.getQuantity()).isEqualByComparingTo("7.000");
        assertThat(balanceAfterPost.getInventoryValue()).isEqualByComparingTo("420000.00");

        // Step 4: Duplicate post attempt -> 409 Conflict (urn:mtms:error:document-already-posted)
        mockMvc.perform(post("/api/v1/branches/{branchId}/inventory/issues/{documentId}/post", branchId, documentId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:document-already-posted")))
                .andExpect(jsonPath("$.title", is("Document Already Posted")));
    }

    @Test
    @Order(4)
    @DisplayName("Security: User without branch grant is rejected with 403 Forbidden")
    void testBranchAuthorizationForbidden() throws Exception {
        CreateStockIssueLineCommand line = new CreateStockIssueLineCommand(
                itemCoffee.getId(), new BigDecimal("1.000"), null, "Unauthorized attempt"
        );
        CreateStockIssueCommand cmd = new CreateStockIssueCommand(
                LocalDate.now(), StockDocumentType.CONSUMPTION_ISSUE, "Hacker attempt", true, List.of(line)
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/inventory/issues", branchId)
                        .header("Authorization", "Bearer " + tokenNoAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("TC-022: Concurrency test - Pessimistic Locking ensures at most available stock (5kg) is issued, balance never negative")
    void testTc022ConcurrentIssuesPessimisticLockingNeverOverdrafts() throws Exception {
        // Setup initial balance: exactly 5.000kg @ 100,000đ
        StockBalance balance = StockBalance.createInitial(
                entityId, branchId, itemCoffee.getId(),
                new BigDecimal("5.000"), new BigDecimal("100000.000000")
        );
        domainBalanceRepository.save(balance);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronize thread takeoff

                    CreateStockIssueLineCommand line = new CreateStockIssueLineCommand(
                            itemCoffee.getId(), new BigDecimal("5.000"), null, "Concurrent claim of last 5kg"
                    );
                    CreateStockIssueCommand cmd = new CreateStockIssueCommand(
                            LocalDate.now(), StockDocumentType.CONSUMPTION_ISSUE, "Race condition issue", true, List.of(line)
                    );

                    stockIssueService.createIssueDocument(branchId, cmd, manager.getId());
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    insufficientStockCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getCause() instanceof InsufficientStockException) {
                        insufficientStockCount.incrementAndGet();
                    }
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // Fire both threads simultaneously

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // Exactly 1 thread must succeed and exactly 1 must fail due to InsufficientStockException
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(insufficientStockCount.get()).isEqualTo(1);

        // Final balance must be exactly 0.000kg, NEVER negative!
        StockBalanceJpaEntity finalBalance = stockBalanceRepository.findByBranchIdAndItemId(branchId, itemCoffee.getId()).orElseThrow();
        assertThat(finalBalance.getQuantity()).isEqualByComparingTo("0.000");
        assertThat(finalBalance.getInventoryValue()).isEqualByComparingTo("0.00");
        assertThat(finalBalance.getAvgUnitCost()).isEqualByComparingTo("100000.000000");

        // Exactly one manual issue movement of 5.000kg was logged
        List<StockMovementJpaEntity> issueMovements = stockMovementRepository.findAll().stream()
                .filter(m -> m.getMovementType() == StockMovementType.MANUAL_ISSUE)
                .toList();
        assertThat(issueMovements).hasSize(1);
        assertThat(issueMovements.get(0).getSignedQuantity()).isEqualByComparingTo("-5.000");
    }
}
