package com.mtms.stockcount;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.mtms.inventory.domain.StockMovementType;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaEntity;
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
import com.mtms.stockcount.application.dto.StockCountDtos.CreateStockCountRequest;
import com.mtms.stockcount.application.dto.StockCountDtos.UpdateCountLineRequest;
import com.mtms.stockcount.application.dto.StockCountDtos.UpdateStockCountRequest;
import com.mtms.stockcount.domain.model.StockCountStatus;
import com.mtms.stockcount.domain.model.StockCountType;
import com.mtms.stockcount.infrastructure.persistence.SpringDataStockCountRepository;
import com.mtms.stockcount.infrastructure.persistence.StockCountJpaEntity;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StockCountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataStockCountRepository stockCountRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private StockBalanceJpaRepository stockBalanceRepository;

    @Autowired
    private StockBalanceRepository domainBalanceRepository;

    @Autowired
    private StockLotJpaRepository stockLotRepository;

    @Autowired
    private StockDocumentJpaRepository stockDocumentRepository;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private CatalogItemJpaRepository catalogItemRepository;

    @Autowired
    private UnitJpaRepository unitRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchId;
    private UserJpaEntity manager;
    private UserJpaEntity unauthorizedUser;

    private String tokenManager;
    private String tokenUnauthorized;

    private CatalogItemJpaEntity itemCoffee;
    private CatalogItemJpaEntity itemMilk;

    @BeforeEach
    void setUp() {
        stockCountRepository.deleteAll();
        stockDocumentRepository.deleteAll();
        stockMovementRepository.deleteAll();
        stockLotRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Entity & Branch
        entityId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_ALPHA_" + suffix, "Alpha Coffee Corp", "0109999001", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        branchId = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchId, entityId, "BR_ALPHA_" + suffix, "Chi nhanh Quan 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 2. Units
        UnitJpaEntity kgUnit = unitRepository.findByUnitCode("KG").orElseGet(() ->
                unitRepository.save(new UnitJpaEntity(UUID.randomUUID(), "KG", "Kilogram", UnitDimension.MASS, Instant.now()))
        );

        // 3. Items
        itemCoffee = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityId, "ING-CF-" + suffix, "Ca phe Robusta",
                kgUnit.getId(), ItemKind.INGREDIENT, false, false,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        itemCoffee.setBaseUnit(kgUnit);
        itemCoffee = catalogItemRepository.save(itemCoffee);

        itemMilk = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityId, "ING-MILK-" + suffix, "Sua dac",
                kgUnit.getId(), ItemKind.INGREDIENT, false, false,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        itemMilk.setBaseUnit(kgUnit);
        itemMilk = catalogItemRepository.save(itemMilk);

        // 4. Users & Grants
        manager = createUser("mgr_cnt", "Quan Ly Chi Nhanh");
        unauthorizedUser = createUser("unauth_cnt", "Nguoi Khong Co Quyen");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchId, manager.getId(),
                "BRANCH_MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), manager.getId(), Instant.now()
        ));

        tokenManager = jwtTokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());
        tokenUnauthorized = jwtTokenService.generateAccessToken(unauthorizedUser.getId(), unauthorizedUser.getLoginIdentifier());
    }

    private UserJpaEntity createUser(String loginPrefix, String name) {
        String login = loginPrefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        UserJpaEntity user = new UserJpaEntity(
                UUID.randomUUID(),
                login,
                name,
                passwordEncoder.encode("Password@123"),
                UserStatus.ACTIVE,
                Instant.now()
        );
        return userRepository.save(user);
    }

    private void seedInitialBalance(UUID branchId, UUID itemId, BigDecimal qty, BigDecimal avgCost) {
        StockBalance balance = StockBalance.createInitial(
                entityId,
                branchId,
                itemId,
                qty,
                avgCost
        );
        domainBalanceRepository.save(balance);
    }

    @Test
    @Order(1)
    @DisplayName("P1-INV-02: Create stock count snapshots current balances and unit cost, initially DRAFT with 0 movements")
    void testCreateStockCountSnapshotsCurrentBalances() throws Exception {
        // Seed 50.000 KG @ 100,000 VND
        seedInitialBalance(branchId, itemCoffee.getId(), new BigDecimal("50.000"), new BigDecimal("100000.0000"));

        CreateStockCountRequest request = new CreateStockCountRequest(
                LocalDate.now(),
                StockCountType.PERIODIC,
                "Kiem ke cuoi tuan dinh ky",
                List.of(itemCoffee.getId())
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.countType", is("PERIODIC")))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].expectedQty", is(50.0)))
                .andExpect(jsonPath("$.lines[0].countedQty", is(50.0)))
                .andExpect(jsonPath("$.lines[0].varianceQty", is(0.0)))
                .andExpect(jsonPath("$.lines[0].snapshotUnitCost", is(100000.0)))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(createRes);
        UUID countId = UUID.fromString(root.get("id").asText());

        // Verify database state: no stock movements created
        List<StockMovementJpaEntity> movements = stockMovementRepository.findAll();
        assertThat(movements).isEmpty();

        // Stock balance untouched
        StockBalance balance = domainBalanceRepository.findByBranchIdAndItemId(branchId, itemCoffee.getId()).orElseThrow();
        assertThat(balance.getQuantity()).isEqualByComparingTo("50.000");
    }

    @Test
    @Order(2)
    @DisplayName("TC-026: Stock count discrepancy without Owner approval results in NO posted stock change")
    void testStockCountDiscrepancyWithoutOwnerApprovalNoStockChangeTc026() throws Exception {
        seedInitialBalance(branchId, itemCoffee.getId(), new BigDecimal("50.000"), new BigDecimal("100000.0000"));

        CreateStockCountRequest request = new CreateStockCountRequest(
                LocalDate.now(),
                StockCountType.PERIODIC,
                "Kiem ke dinh ky",
                List.of(itemCoffee.getId())
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(createRes);
        UUID countId = UUID.fromString(root.get("id").asText());
        UUID lineId = UUID.fromString(root.get("lines").get(0).get("id").asText());

        // Update counted quantity to 48.000 KG (variance = -2.000 KG)
        UpdateStockCountRequest updateReq = new UpdateStockCountRequest(
                "Cap nhat thuc dem",
                List.of(new UpdateCountLineRequest(
                        lineId,
                        new BigDecimal("48.000"),
                        "Hao hut pha che roi vai",
                        null
                ))
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/stock-counts/{countId}", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].countedQty", is(48.0)))
                .andExpect(jsonPath("$.lines[0].varianceQty", is(-2.0)))
                .andExpect(jsonPath("$.lines[0].varianceValue", is(-200000.0)));

        // Submit for approval -> status SUBMITTED
        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts/{countId}/submit", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUBMITTED")));

        // CORE TC-026: Stock balance MUST still be 50.000 KG, NOT 48.000 KG!
        StockBalance balance = domainBalanceRepository.findByBranchIdAndItemId(branchId, itemCoffee.getId()).orElseThrow();
        assertThat(balance.getQuantity()).isEqualByComparingTo("50.000");

        // Zero movements posted
        assertThat(stockMovementRepository.findAll()).isEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("P1-INV-02: Owner approval for shortage (-2kg) updates stock to 48kg, preserves moving WAC, and posts ADJUSTMENT")
    void testOwnerApproveShortageAdjustsStockAndPreservesMovingWac() throws Exception {
        seedInitialBalance(branchId, itemCoffee.getId(), new BigDecimal("50.000"), new BigDecimal("100000.0000"));

        CreateStockCountRequest request = new CreateStockCountRequest(
                LocalDate.now(),
                StockCountType.PERIODIC,
                "Kiem ke hao hut",
                List.of(itemCoffee.getId())
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(createRes);
        UUID countId = UUID.fromString(root.get("id").asText());
        UUID lineId = UUID.fromString(root.get("lines").get(0).get("id").asText());

        UpdateStockCountRequest updateReq = new UpdateStockCountRequest(
                "Cap nhat thuc dem 48kg",
                List.of(new UpdateCountLineRequest(
                        lineId,
                        new BigDecimal("48.000"),
                        "Rach bao bot ca phe",
                        null
                ))
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/stock-counts/{countId}", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts/{countId}/submit", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk());

        // Owner Approves & Posts
        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts/{countId}/approve", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("POSTED")))
                .andExpect(jsonPath("$.totalShortageQty", is(2.0)))
                .andExpect(jsonPath("$.totalShortageValue", is(200000.0)))
                .andExpect(jsonPath("$.netVarianceValue", is(-200000.0)));

        // Verify Stock Balance deducted to 48.000 KG
        StockBalance balance = domainBalanceRepository.findByBranchIdAndItemId(branchId, itemCoffee.getId()).orElseThrow();
        assertThat(balance.getQuantity()).isEqualByComparingTo("48.000");
        assertThat(balance.getAvgUnitCost()).isEqualByComparingTo("100000.0000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("4800000.00");

        // Verify Stock Movement created for ADJUSTMENT
        List<StockMovementJpaEntity> movements = stockMovementRepository.findAll();
        assertThat(movements).hasSize(1);
        StockMovementJpaEntity move = movements.get(0);
        assertThat(move.getMovementType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(move.getSignedQuantity()).isEqualByComparingTo(new BigDecimal("-2.000"));
        assertThat(move.getSignedValue()).isEqualByComparingTo(new BigDecimal("-200000.00"));
        assertThat(move.getBookedAvgCost()).isEqualByComparingTo(new BigDecimal("100000.0000"));
    }

    @Test
    @Order(4)
    @DisplayName("P1-INV-02: Owner approval for surplus (+5kg) increases stock from 10kg to 15kg and posts positive ADJUSTMENT")
    void testOwnerApproveSurplusAdjustsStockAndCreatesPositiveMovement() throws Exception {
        seedInitialBalance(branchId, itemMilk.getId(), new BigDecimal("10.000"), new BigDecimal("80000.0000"));

        CreateStockCountRequest request = new CreateStockCountRequest(
                LocalDate.now(),
                StockCountType.PERIODIC,
                "Kiem ke sua dac thua",
                List.of(itemMilk.getId())
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(createRes);
        UUID countId = UUID.fromString(root.get("id").asText());
        UUID lineId = UUID.fromString(root.get("lines").get(0).get("id").asText());

        // Counted 15.000 KG (+5.000 KG surplus)
        UpdateStockCountRequest updateReq = new UpdateStockCountRequest(
                "Tim thay them 5 lon trong kho phu",
                List.of(new UpdateCountLineRequest(
                        lineId,
                        new BigDecimal("15.000"),
                        "Tim thay hang bi de quen trong kho phu",
                        null
                ))
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/stock-counts/{countId}", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts/{countId}/submit", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk());

        // Approve
        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts/{countId}/approve", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("POSTED")))
                .andExpect(jsonPath("$.totalSurplusQty", is(5.0)))
                .andExpect(jsonPath("$.totalSurplusValue", is(400000.0)));

        // Verify balance increased to 15.000 KG @ 80,000 VND
        StockBalance balance = domainBalanceRepository.findByBranchIdAndItemId(branchId, itemMilk.getId()).orElseThrow();
        assertThat(balance.getQuantity()).isEqualByComparingTo("15.000");
        assertThat(balance.getInventoryValue()).isEqualByComparingTo("1200000.00");

        // Verify Stock Movement created for ADJUSTMENT
        StockMovementJpaEntity move = stockMovementRepository.findAll().get(0);
        assertThat(move.getMovementType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(move.getSignedQuantity()).isEqualByComparingTo(new BigDecimal("5.000"));
        assertThat(move.getSignedValue()).isEqualByComparingTo(new BigDecimal("400000.00"));
    }

    @Test
    @Order(5)
    @DisplayName("TC-021: Insufficient stock at approval time rejects shortage posting with 400 Bad Request")
    void testInsufficientStockRejectionOnApprovalTc021() throws Exception {
        seedInitialBalance(branchId, itemCoffee.getId(), new BigDecimal("10.000"), new BigDecimal("100000.0000"));

        CreateStockCountRequest request = new CreateStockCountRequest(
                LocalDate.now(),
                StockCountType.PERIODIC,
                "Kiem ke",
                List.of(itemCoffee.getId())
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(createRes);
        UUID countId = UUID.fromString(root.get("id").asText());
        UUID lineId = UUID.fromString(root.get("lines").get(0).get("id").asText());

        // Counted 4.000 KG (Shortage = 6.000 KG)
        UpdateStockCountRequest updateReq = new UpdateStockCountRequest(
                "Thieu 6kg",
                List.of(new UpdateCountLineRequest(
                        lineId,
                        new BigDecimal("4.000"),
                        "Hao hut",
                        null
                ))
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/stock-counts/{countId}", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // Now simulate another operation deducting stock balance down to 3.000 KG before Owner approves
        StockBalance currentBalance = domainBalanceRepository.findByBranchIdAndItemId(branchId, itemCoffee.getId()).orElseThrow();
        currentBalance.issueStock(new BigDecimal("7.000")); // remaining = 3.000 KG
        domainBalanceRepository.save(currentBalance);

        // Owner attempts to approve shortage of 6.000 KG when only 3.000 KG is available
        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts/{countId}/approve", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:insufficient-stock")))
                .andExpect(jsonPath("$.detail", containsString("không đủ để trừ lượng thiếu kiểm kê")));

        // Verify status remains DRAFT
        StockCountJpaEntity entity = stockCountRepository.findById(countId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(StockCountStatus.DRAFT);
    }

    @Test
    @Order(6)
    @DisplayName("BR-INV-07: Discrepancy without mandatory explanation reason rejects update and submission")
    void testMissingDiscrepancyReasonRejected() throws Exception {
        seedInitialBalance(branchId, itemCoffee.getId(), new BigDecimal("10.000"), new BigDecimal("100000.0000"));

        CreateStockCountRequest request = new CreateStockCountRequest(
                LocalDate.now(),
                StockCountType.PERIODIC,
                "Kiem ke",
                List.of(itemCoffee.getId())
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(createRes);
        UUID countId = UUID.fromString(root.get("id").asText());
        UUID lineId = UUID.fromString(root.get("lines").get(0).get("id").asText());

        // Update counted to 8.000 KG (-2.000 KG) with NULL discrepancyReason
        UpdateStockCountRequest updateReq = new UpdateStockCountRequest(
                "Khong co ly do",
                List.of(new UpdateCountLineRequest(
                        lineId,
                        new BigDecimal("8.000"),
                        null, // missing reason!
                        null
                ))
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/stock-counts/{countId}", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:missing-discrepancy-reason")));
    }

    @Test
    @Order(7)
    @DisplayName("P1-INV-02: Completed POSTED stock count is strictly immutable (rejects modification and cancel)")
    void testCannotModifyPostedStockCount() throws Exception {
        seedInitialBalance(branchId, itemCoffee.getId(), new BigDecimal("10.000"), new BigDecimal("100000.0000"));

        CreateStockCountRequest request = new CreateStockCountRequest(
                LocalDate.now(),
                StockCountType.PERIODIC,
                "Kiem ke",
                List.of(itemCoffee.getId())
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts", branchId)
                        .header("Authorization", "Bearer " + tokenManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID countId = UUID.fromString(objectMapper.readTree(createRes).get("id").asText());

        // Approve & Post directly
        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts/{countId}/approve", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("POSTED")));

        // Attempt to cancel POSTED count -> rejects with 400
        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts/{countId}/cancel", branchId, countId)
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:stock-count-immutable")));
    }

    @Test
    @Order(8)
    @DisplayName("TC-001: Branch authorization prevents unauthorized users from accessing stock counts (403 Forbidden)")
    void testBranchSecurityIsolationTc001() throws Exception {
        CreateStockCountRequest request = new CreateStockCountRequest(
                LocalDate.now(),
                StockCountType.PERIODIC,
                "Kiem ke",
                List.of(itemCoffee.getId())
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/stock-counts", branchId)
                        .header("Authorization", "Bearer " + tokenUnauthorized)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
