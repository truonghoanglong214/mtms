package com.mtms.movement;

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
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockDocumentJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockLotJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
import com.mtms.inventory.infrastructure.persistence.UnitJpaEntity;
import com.mtms.inventory.infrastructure.persistence.UnitJpaRepository;
import com.mtms.movement.application.dto.TransferDtos.CreateTransferLineRequest;
import com.mtms.movement.application.dto.TransferDtos.CreateTransferRequest;
import com.mtms.movement.application.dto.TransferDtos.ReceiveTransferLineRequest;
import com.mtms.movement.application.dto.TransferDtos.ReceiveTransferRequest;
import com.mtms.movement.domain.model.TransferStatus;
import com.mtms.movement.infrastructure.persistence.SpringDataStockTransferRepository;
import com.mtms.movement.infrastructure.persistence.StockTransferJpaEntity;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
class TransferAndReceiptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataStockTransferRepository stockTransferRepository;

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

    private UUID entityAId;
    private UUID entityBId;
    private UUID branch1Id;
    private UUID branch2Id;
    private UUID branch3Id;

    private UserJpaEntity managerBranch1;
    private UserJpaEntity managerBranch2;
    private UserJpaEntity unauthorizedUser;

    private String tokenManagerBranch1;
    private String tokenManagerBranch2;
    private String tokenUnauthorized;

    private CatalogItemJpaEntity itemCoffee;

    @BeforeEach
    void setUp() {
        stockTransferRepository.deleteAll();
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

        // 1. Entities
        entityAId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityAId, "ENT_A", "Cong Ty A", "0100000001", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        entityBId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityBId, "ENT_B", "Cong Ty B Khac", "0100000002", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        // 2. Branches
        // Branch 1 & Branch 2 are under entityA (Intra-entity allowed)
        branch1Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branch1Id, entityAId, "BR_01", "Chi Nhanh 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        branch2Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branch2Id, entityAId, "BR_02", "Chi Nhanh 2", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // Branch 3 is under entityB (Inter-entity forbidden TC-028)
        branch3Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branch3Id, entityBId, "BR_03", "Chi Nhanh 3 Khac Entity", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        // 3. Units
        UnitJpaEntity kgUnit = unitRepository.findByUnitCode("KG").orElseGet(() ->
                unitRepository.save(new UnitJpaEntity(UUID.randomUUID(), "KG", "Kilogram", UnitDimension.MASS, Instant.now()))
        );

        // 4. Catalog Items
        itemCoffee = new CatalogItemJpaEntity(
                UUID.randomUUID(), entityAId, "CF-ROB-01", "Ca Phe Robusta",
                kgUnit.getId(), ItemKind.INGREDIENT, false, false,
                ItemStatus.ACTIVE, Instant.now(), Instant.now()
        );
        itemCoffee.setBaseUnit(kgUnit);
        itemCoffee = catalogItemRepository.save(itemCoffee);

        // 5. Users & Grants
        managerBranch1 = createUser("mgr_branch1", "Quan Ly CN 1");
        managerBranch2 = createUser("mgr_branch2", "Quan Ly CN 2");
        unauthorizedUser = createUser("unauth_user", "Nguoi Khong Co Quyen");

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityAId, branch1Id, managerBranch1.getId(),
                "BRANCH_MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), managerBranch1.getId(), Instant.now()
        ));

        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityAId, branch2Id, managerBranch2.getId(),
                "BRANCH_MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), managerBranch2.getId(), Instant.now()
        ));

        tokenManagerBranch1 = jwtTokenService.generateAccessToken(managerBranch1.getId(), managerBranch1.getLoginIdentifier());
        tokenManagerBranch2 = jwtTokenService.generateAccessToken(managerBranch2.getId(), managerBranch2.getLoginIdentifier());
        tokenUnauthorized = jwtTokenService.generateAccessToken(unauthorizedUser.getId(), unauthorizedUser.getLoginIdentifier());
    }

    private UserJpaEntity createUser(String login, String name) {
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
                entityAId,
                branchId,
                itemId,
                qty,
                avgCost
        );
        domainBalanceRepository.save(balance);
    }

    @Test
    @Order(1)
    @DisplayName("P1-MOV-01: Create and dispatch transfer successfully snapshots moving WAC and deducts sender stock")
    void testCreateAndDispatchTransferSuccessfully() throws Exception {
        // Given Branch 1 has 50.000 KG @ 100,000 VND
        seedInitialBalance(branch1Id, itemCoffee.getId(), new BigDecimal("50.000"), new BigDecimal("100000.0000"));

        CreateTransferRequest request = new CreateTransferRequest(
                branch2Id,
                LocalDate.now(),
                false,
                "Dieu chuyen ca phe tu CN1 sang CN2",
                List.of(new CreateTransferLineRequest(itemCoffee.getId(), null, new BigDecimal("20.000"), null))
        );

        // 1. Create Draft Transfer
        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/transfers", branch1Id)
                        .header("Authorization", "Bearer " + tokenManagerBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.fromBranchId", is(branch1Id.toString())))
                .andExpect(jsonPath("$.toBranchId", is(branch2Id.toString())))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].dispatchedBaseQty", is(20.0)))
                .andReturn().getResponse().getContentAsString();

        UUID transferId = UUID.fromString(objectMapper.readTree(createRes).get("id").asText());

        // Stock at branch 1 should still be 50.000 (DRAFT doesn't deduct stock)
        StockBalance preDispatch = domainBalanceRepository.findByBranchIdAndItemId(branch1Id, itemCoffee.getId()).orElseThrow();
        assertThat(preDispatch.getQuantity()).isEqualByComparingTo("50.000");

        // 2. Dispatch Transfer
        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/dispatch", branch1Id, transferId)
                        .header("Authorization", "Bearer " + tokenManagerBranch1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISPATCHED")))
                .andExpect(jsonPath("$.lines[0].dispatchedBaseQty", is(20.0)))
                .andExpect(jsonPath("$.lines[0].senderUnitCost", is(100000.0)));

        // Verify Branch 1 balance deducted to 30.000 KG @ 100,000 VND
        StockBalance postDispatch = domainBalanceRepository.findByBranchIdAndItemId(branch1Id, itemCoffee.getId()).orElseThrow();
        assertThat(postDispatch.getQuantity()).isEqualByComparingTo("30.000");
        assertThat(postDispatch.getAvgUnitCost()).isEqualByComparingTo("100000.0000");

        // Verify Stock Movement created for TRANSFER_OUT
        List<StockMovementJpaEntity> movements = stockMovementRepository.findAll();
        StockMovementJpaEntity transferOutMove = movements.stream()
                .filter(m -> m.getMovementType() == StockMovementType.TRANSFER_OUT)
                .findFirst().orElseThrow();
        assertThat(transferOutMove.getSignedQuantity()).isEqualByComparingTo(new BigDecimal("-20.000"));
        assertThat(transferOutMove.getBookedAvgCost()).isEqualByComparingTo(new BigDecimal("100000.0000"));
    }

    @Test
    @Order(2)
    @DisplayName("TC-028: Inter-entity transfer strictly forbidden (rejects with 400 Bad Request)")
    void testInterEntityTransferForbiddenTc028() throws Exception {
        // Attempt transfer from Branch 1 (Entity A) to Branch 3 (Entity B)
        CreateTransferRequest request = new CreateTransferRequest(
                branch3Id,
                LocalDate.now(),
                false,
                "Thu nghiem dieu chuyen sang Cong Ty B khac phap nhan",
                List.of(new CreateTransferLineRequest(itemCoffee.getId(), null, new BigDecimal("10.000"), null))
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers", branch1Id)
                        .header("Authorization", "Bearer " + tokenManagerBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:inter-entity-transfer-forbidden")))
                .andExpect(jsonPath("$.detail", containsString("Cấm điều chuyển nội bộ giữa hai pháp nhân khác nhau (TC-028)")));
    }

    @Test
    @Order(3)
    @DisplayName("P1-MOV-01: Same branch transfer strictly forbidden (rejects with 400 Bad Request)")
    void testSameBranchTransferForbidden() throws Exception {
        // Attempt transfer from Branch 1 to Branch 1
        CreateTransferRequest request = new CreateTransferRequest(
                branch1Id,
                LocalDate.now(),
                false,
                "Dieu chuyen cung chi nhanh",
                List.of(new CreateTransferLineRequest(itemCoffee.getId(), null, new BigDecimal("10.000"), null))
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers", branch1Id)
                        .header("Authorization", "Bearer " + tokenManagerBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:same-branch-transfer-forbidden")));
    }

    @Test
    @Order(4)
    @DisplayName("TC-021: Insufficient stock rejects dispatch with 400 Bad Request and overdraft forbidden")
    void testInsufficientStockRejectionTc021() throws Exception {
        // Given Branch 1 has only 5.000 KG
        seedInitialBalance(branch1Id, itemCoffee.getId(), new BigDecimal("5.000"), new BigDecimal("100000.0000"));

        CreateTransferRequest request = new CreateTransferRequest(
                branch2Id,
                LocalDate.now(),
                false,
                "Dieu chuyen vuot ton kho",
                List.of(new CreateTransferLineRequest(itemCoffee.getId(), null, new BigDecimal("15.000"), null))
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/transfers", branch1Id)
                        .header("Authorization", "Bearer " + tokenManagerBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID transferId = UUID.fromString(objectMapper.readTree(createRes).get("id").asText());

        // Dispatch must be rejected because 15.000 > 5.000 available
        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/dispatch", branch1Id, transferId)
                        .header("Authorization", "Bearer " + tokenManagerBranch1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:insufficient-stock")))
                .andExpect(jsonPath("$.detail", containsString("Tồn kho không đủ để xuất điều chuyển")));

        // Verify status remains DRAFT
        StockTransferJpaEntity transfer = stockTransferRepository.findById(transferId).orElseThrow();
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.DRAFT);
    }

    @Test
    @Order(5)
    @DisplayName("P1-MOV-02: Full receipt updates receiver moving WAC accurately and marks transfer COMPLETED")
    void testFullReceiptUpdatesReceiverMovingWac() throws Exception {
        // Sender (Branch 1): 50.000 KG @ 100,000 VND
        seedInitialBalance(branch1Id, itemCoffee.getId(), new BigDecimal("50.000"), new BigDecimal("100000.0000"));

        // Receiver (Branch 2): existing 10.000 KG @ 80,000 VND (Total value = 800,000 VND)
        seedInitialBalance(branch2Id, itemCoffee.getId(), new BigDecimal("10.000"), new BigDecimal("80000.0000"));

        // Create & Dispatch 10.000 KG @ 100,000 VND from Branch 1 to Branch 2
        CreateTransferRequest request = new CreateTransferRequest(
                branch2Id,
                LocalDate.now(),
                false,
                "Dieu chuyen nguyen vat lieu sang CN2",
                List.of(new CreateTransferLineRequest(itemCoffee.getId(), null, new BigDecimal("10.000"), null))
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/transfers", branch1Id)
                        .header("Authorization", "Bearer " + tokenManagerBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode createNode = objectMapper.readTree(createRes);
        UUID transferId = UUID.fromString(createNode.get("id").asText());
        UUID lineId = UUID.fromString(createNode.get("lines").get(0).get("id").asText());

        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/dispatch", branch1Id, transferId)
                        .header("Authorization", "Bearer " + tokenManagerBranch1))
                .andExpect(status().isOk());

        // Branch 2 receives full 10.000 KG
        ReceiveTransferRequest receiveReq = new ReceiveTransferRequest(
                "Nhan du hang hoa day du",
                List.of(new ReceiveTransferLineRequest(lineId, new BigDecimal("10.000"), null))
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/receive", branch2Id, transferId)
                        .header("Authorization", "Bearer " + tokenManagerBranch2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.lines[0].receivedBaseQty", is(10.0)))
                .andExpect(jsonPath("$.lines[0].transitVarianceQty", is(0.0)));

        // Receiver Moving WAC Check:
        // Existing: 10 KG * 80,000 = 800,000 VND
        // Received: 10 KG * 100,000 = 1,000,000 VND
        // Total: 20 KG, Value = 1,800,000 VND => WAC = 90,000 VND
        StockBalance receiverBalance = domainBalanceRepository.findByBranchIdAndItemId(branch2Id, itemCoffee.getId()).orElseThrow();
        assertThat(receiverBalance.getQuantity()).isEqualByComparingTo("20.000");
        assertThat(receiverBalance.getAvgUnitCost()).isEqualByComparingTo("90000.0000");

        // Verify TRANSFER_IN movement created for Branch 2
        StockMovementJpaEntity transferInMove = stockMovementRepository.findAll().stream()
                .filter(m -> m.getBranchId().equals(branch2Id) && m.getMovementType() == StockMovementType.TRANSFER_IN)
                .findFirst().orElseThrow();
        assertThat(transferInMove.getSignedQuantity()).isEqualByComparingTo(new BigDecimal("10.000"));
        assertThat(transferInMove.getBookedAvgCost()).isEqualByComparingTo(new BigDecimal("100000.0000"));
    }

    @Test
    @Order(6)
    @DisplayName("TC-027: Partial receipt retains in-transit discrepancy (goods do not vanish) with status PARTIAL_RECEIPT")
    void testPartialReceiptRetainsInTransitDiscrepancyTc027() throws Exception {
        // Sender has 50.000 KG @ 100,000 VND
        seedInitialBalance(branch1Id, itemCoffee.getId(), new BigDecimal("50.000"), new BigDecimal("100000.0000"));

        CreateTransferRequest request = new CreateTransferRequest(
                branch2Id,
                LocalDate.now(),
                false,
                "Dieu chuyen 20kg",
                List.of(new CreateTransferLineRequest(itemCoffee.getId(), null, new BigDecimal("20.000"), null))
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/transfers", branch1Id)
                        .header("Authorization", "Bearer " + tokenManagerBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode createNode = objectMapper.readTree(createRes);
        UUID transferId = UUID.fromString(createNode.get("id").asText());
        UUID lineId = UUID.fromString(createNode.get("lines").get(0).get("id").asText());

        // Dispatch 20.000 KG
        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/dispatch", branch1Id, transferId)
                        .header("Authorization", "Bearer " + tokenManagerBranch1))
                .andExpect(status().isOk());

        // Branch 2 receives only 18.000 KG, variance = 2.000 KG (TC-027 No Vanish)
        ReceiveTransferRequest receiveReq = new ReceiveTransferRequest(
                "Nhan thieu do rach bao",
                List.of(new ReceiveTransferLineRequest(
                        lineId,
                        new BigDecimal("18.000"),
                        "2 bao bi rach, rot bot ca phe tren duong van chuyen"
                ))
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/receive", branch2Id, transferId)
                        .header("Authorization", "Bearer " + tokenManagerBranch2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PARTIAL_RECEIPT")))
                .andExpect(jsonPath("$.lines[0].dispatchedBaseQty", is(20.0)))
                .andExpect(jsonPath("$.lines[0].receivedBaseQty", is(18.0)))
                .andExpect(jsonPath("$.lines[0].transitVarianceQty", is(2.0)))
                .andExpect(jsonPath("$.lines[0].receiverNote", containsString("2 bao bi rach")));

        // Receiver stock balance should only increase by received 18.000 KG
        StockBalance receiverBalance = domainBalanceRepository.findByBranchIdAndItemId(branch2Id, itemCoffee.getId()).orElseThrow();
        assertThat(receiverBalance.getQuantity()).isEqualByComparingTo("18.000");

        // Verify summary endpoint surfaces transit discrepancy
        mockMvc.perform(get("/api/v1/branches/{branchId}/transfers/summary", branch2Id)
                        .header("Authorization", "Bearer " + tokenManagerBranch2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inboundCount", is(1)))
                .andExpect(jsonPath("$.discrepancyCount", is(1)));
    }

    @Test
    @Order(7)
    @DisplayName("P1-MOV-02: Over-receipt strictly forbidden (receivedQty > dispatchedQty rejects with 400)")
    void testOverReceiptRejection() throws Exception {
        seedInitialBalance(branch1Id, itemCoffee.getId(), new BigDecimal("50.000"), new BigDecimal("100000.0000"));

        CreateTransferRequest request = new CreateTransferRequest(
                branch2Id,
                LocalDate.now(),
                false,
                "Dieu chuyen 10kg",
                List.of(new CreateTransferLineRequest(itemCoffee.getId(), null, new BigDecimal("10.000"), null))
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/transfers", branch1Id)
                        .header("Authorization", "Bearer " + tokenManagerBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode createNode = objectMapper.readTree(createRes);
        UUID transferId = UUID.fromString(createNode.get("id").asText());
        UUID lineId = UUID.fromString(createNode.get("lines").get(0).get("id").asText());

        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/dispatch", branch1Id, transferId)
                        .header("Authorization", "Bearer " + tokenManagerBranch1))
                .andExpect(status().isOk());

        // Attempt to receive 12.000 KG when dispatched was 10.000 KG
        ReceiveTransferRequest receiveReq = new ReceiveTransferRequest(
                "Nhan thua",
                List.of(new ReceiveTransferLineRequest(lineId, new BigDecimal("12.000"), null))
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/receive", branch2Id, transferId)
                        .header("Authorization", "Bearer " + tokenManagerBranch2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiveReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:over-receipt-denied")))
                .andExpect(jsonPath("$.detail", containsString("Số lượng thực nhận")));
    }

    @Test
    @Order(8)
    @DisplayName("TC-001: Branch authorization protects transfer operations (unauthorized user gets 403 Forbidden)")
    void testNegativeBranchAuthorizationTc001() throws Exception {
        CreateTransferRequest request = new CreateTransferRequest(
                branch2Id,
                LocalDate.now(),
                false,
                "Thu nghiem unauthorized",
                List.of(new CreateTransferLineRequest(itemCoffee.getId(), null, new BigDecimal("10.000"), null))
        );

        // Unauthorized user attempts create transfer for Branch 1
        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers", branch1Id)
                        .header("Authorization", "Bearer " + tokenUnauthorized)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Manager of Branch 1 attempts to receive transfer targeting Branch 2 without Branch 2 grant
        mockMvc.perform(post("/api/v1/branches/{branchId}/transfers/{transferId}/receive", branch2Id, UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenManagerBranch1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReceiveTransferRequest(
                                "test",
                                List.of(new ReceiveTransferLineRequest(UUID.randomUUID(), BigDecimal.ONE, null))
                        ))))
                .andExpect(status().isForbidden());
    }
}
