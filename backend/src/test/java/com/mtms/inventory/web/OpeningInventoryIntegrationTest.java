package com.mtms.inventory.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.idempotency.IdempotencyRecordJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.application.dto.CommitOpeningCommand;
import com.mtms.inventory.application.dto.OpeningItemRow;
import com.mtms.inventory.application.dto.PreviewOpeningCommand;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaEntity;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.OpeningInventoryRecordJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaEntity;
import com.mtms.inventory.infrastructure.persistence.StockBalanceJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockLotJpaRepository;
import com.mtms.inventory.infrastructure.persistence.StockMovementJpaRepository;
import com.mtms.inventory.infrastructure.persistence.SupplierJpaRepository;
import com.mtms.inventory.infrastructure.persistence.SupplierPackConversionJpaRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpeningInventoryIntegrationTest {

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
    private SupplierPackConversionJpaRepository conversionRepository;

    @Autowired
    private SupplierJpaRepository supplierRepository;

    @Autowired
    private StockBalanceJpaRepository stockBalanceRepository;

    @Autowired
    private StockLotJpaRepository stockLotRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private OpeningInventoryRecordJpaRepository openingRecordRepository;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityAId;
    private UUID branchA1Id;
    private UUID branchA2Id;
    private UUID entityBId;
    private UUID branchB1Id;

    private UserJpaEntity managerA;
    private UserJpaEntity managerB;
    private UserJpaEntity userNoAccess;

    private String tokenManagerA;
    private String tokenManagerB;
    private String tokenNoAccess;

    private CatalogItemJpaEntity itemMilk;
    private CatalogItemJpaEntity itemCoffee;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        stockLotRepository.deleteAll();
        stockBalanceRepository.deleteAll();
        openingRecordRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        conversionRepository.deleteAll();
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

        entityBId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityBId, "ENT_B", "Entity Beta Co.", "0109999002", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        // 2. Branches
        branchA1Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchA1Id, entityAId, "BR_A1", "Chi nhánh Quận 1", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        ));

        branchA2Id = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchA2Id, entityAId, "BR_A2", "Chi nhánh Quận 3", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
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

        // 4. Catalog Items under Entity A
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

        // 5. Users
        managerA = createUser("manager_a", "Nguyễn Văn Quản Lý");
        managerB = createUser("manager_b", "Trần Quản Lý B");
        userNoAccess = createUser("no_access_user", "Người Không Có Quyền");

        LocalDate today = LocalDate.now();

        // Grant managerA to branchA1 and branchA2
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityAId, branchA1Id, managerA.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), managerA.getId(), Instant.now()
        ));
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityAId, branchA2Id, managerA.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), managerA.getId(), Instant.now()
        ));

        // Grant managerB to branchB1
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
    @DisplayName("GET /opening: uncommitted branch returns isOpeningCommitted=false")
    void testGetOpeningStatusUncommitted() throws Exception {
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/opening")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId", is(branchA1Id.toString())))
                .andExpect(jsonPath("$.branchName", is("Chi nhánh Quận 1")))
                .andExpect(jsonPath("$.isOpeningCommitted", is(false)))
                .andExpect(jsonPath("$.openingRecord").doesNotExist());
    }

    @Test
    @Order(2)
    @DisplayName("POST /opening/preview: calculates initial WAC and valuations accurately")
    void testPreviewOpeningInventory() throws Exception {
        List<OpeningItemRow> rows = List.of(
                new OpeningItemRow("ING-MILK-OT-380G", new BigDecimal("50.000"), new BigDecimal("24000.000000"), "LOT-202611-01", LocalDate.of(2027, 11, 16)),
                new OpeningItemRow("ING-COFFEE-01", new BigDecimal("20.000"), new BigDecimal("150000.000000"), null, null)
        );
        PreviewOpeningCommand command = new PreviewOpeningCommand(null, LocalDate.of(2026, 11, 16), rows);

        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/opening/preview")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId", is(branchA1Id.toString())))
                .andExpect(jsonPath("$.openingDate", is("2026-11-16")))
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.totalQuantity", is("70.000")))
                .andExpect(jsonPath("$.totalValuation", is("4200000.00")))
                .andExpect(jsonPath("$.hasErrors", is(false)))
                .andExpect(jsonPath("$.previewRows", hasSize(2)))
                .andExpect(jsonPath("$.previewRows[0].itemCode", is("ING-MILK-OT-380G")))
                .andExpect(jsonPath("$.previewRows[0].calculatedWac", is("24000.000000")))
                .andExpect(jsonPath("$.previewRows[0].lineTotal", is("1200000.00")))
                .andExpect(jsonPath("$.previewRows[0].lotTrackingRequired", is(true)))
                .andExpect(jsonPath("$.previewRows[1].itemCode", is("ING-COFFEE-01")))
                .andExpect(jsonPath("$.previewRows[1].calculatedWac", is("150000.000000")))
                .andExpect(jsonPath("$.previewRows[1].lineTotal", is("3000000.00")))
                .andExpect(jsonPath("$.previewRows[1].lotTrackingRequired", is(false)));
    }

    @Test
    @Order(3)
    @DisplayName("TC-024 Full Lifecycle: Commit opening, enforce single post lock, prevent double counting")
    void testCommitOpeningAndTc024RetryLock() throws Exception {
        List<OpeningItemRow> rows = List.of(
                new OpeningItemRow("ING-MILK-OT-380G", new BigDecimal("50.000"), new BigDecimal("24000.000000"), "LOT-202611-01", LocalDate.of(2027, 11, 16)),
                new OpeningItemRow("ING-COFFEE-01", new BigDecimal("20.000"), new BigDecimal("150000.000000"), null, null)
        );
        CommitOpeningCommand command = new CommitOpeningCommand(null, LocalDate.of(2026, 11, 16), rows, "Khai trương đầu kỳ chi nhánh Q1");

        String idempotencyKey = UUID.randomUUID().toString();

        // 1. First commit: Succeeds (201 Created)
        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/opening/commit")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.branchId", is(branchA1Id.toString())))
                .andExpect(jsonPath("$.status", is("COMMITTED")))
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.totalQuantity", is("70.000")))
                .andExpect(jsonPath("$.totalValue", is("4200000.00")))
                .andExpect(jsonPath("$.approvedByName", is("Nguyễn Văn Quản Lý")));

        // Verify Database State
        List<StockBalanceJpaEntity> balances = stockBalanceRepository.findByBranchId(branchA1Id);
        assertThat(balances).hasSize(2);

        BigDecimal totalStock = balances.stream().map(StockBalanceJpaEntity::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalStock).isEqualByComparingTo(new BigDecimal("70.000"));

        BigDecimal totalValuation = balances.stream().map(StockBalanceJpaEntity::getInventoryValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalValuation).isEqualByComparingTo(new BigDecimal("4200000.00"));

        assertThat(stockLotRepository.findByBranchId(branchA1Id)).hasSize(1);
        assertThat(stockMovementRepository.findByBranchId(branchA1Id)).hasSize(2);

        // 2. Retry with SAME Idempotency-Key: Idempotent replay
        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/opening/commit")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.totalQuantity", is("70.000")));

        // 3. Retry with NEW Idempotency-Key: Must be rejected with 409 Conflict (TC-024)
        mockMvc.perform(post("/api/v1/branches/" + branchA1Id + "/inventory/opening/commit")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:opening-inventory-already-posted")))
                .andExpect(jsonPath("$.title", is("Opening Already Posted")));

        // 4. Verify stock balances were NOT duplicated! (Must still be 70.000, NOT 140.000)
        List<StockBalanceJpaEntity> balancesAfterRetry = stockBalanceRepository.findByBranchId(branchA1Id);
        assertThat(balancesAfterRetry).hasSize(2);

        BigDecimal totalStockAfterRetry = balancesAfterRetry.stream().map(StockBalanceJpaEntity::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalStockAfterRetry).isEqualByComparingTo(new BigDecimal("70.000"));

        // 5. GET /opening: Confirmed isOpeningCommitted=true with record details
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/opening")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOpeningCommitted", is(true)))
                .andExpect(jsonPath("$.openingRecord.totalItems", is(2)))
                .andExpect(jsonPath("$.openingRecord.totalQuantity", is("70.000")))
                .andExpect(jsonPath("$.openingRecord.totalValue", is("4200000.00")))
                .andExpect(jsonPath("$.openingRecord.approvedByName", is("Nguyễn Văn Quản Lý")));

        // 6. GET /balances: Check stock balances listing
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/balances")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.totalBranchValue", is("4200000.00")))
                .andExpect(jsonPath("$.items", hasSize(2)));

        // 7. GET /balances with search filter:
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/balances?search=MILK")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].itemCode", is("ING-MILK-OT-380G")));
    }

    @Test
    @Order(4)
    @DisplayName("POST /commit rejects missing lot code when lot tracking is required")
    void testCommitRejectsMissingLotCode() throws Exception {
        List<OpeningItemRow> rows = List.of(
                new OpeningItemRow("ING-MILK-OT-380G", new BigDecimal("50.000"), new BigDecimal("24000.000000"), null, null)
        );
        CommitOpeningCommand command = new CommitOpeningCommand(null, LocalDate.of(2026, 11, 16), rows, "Missing lot");

        mockMvc.perform(post("/api/v1/branches/" + branchA2Id + "/inventory/opening/commit")
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:mtms:error:missing-lot-code")));
    }

    @Test
    @Order(5)
    @DisplayName("Branch isolation: managerB cannot access branchA1 resources (403 Forbidden)")
    void testBranchIsolationForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/opening")
                        .header("Authorization", "Bearer " + tokenManagerB))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/balances")
                        .header("Authorization", "Bearer " + tokenManagerB))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/branches/" + branchA1Id + "/inventory/opening")
                        .header("Authorization", "Bearer " + tokenNoAccess))
                .andExpect(status().isForbidden());
    }
}
