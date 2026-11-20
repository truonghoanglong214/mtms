package com.mtms.inventory.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.inventory.application.dto.CreateCatalogItemCommand;
import com.mtms.inventory.application.dto.CreateSupplierCommand;
import com.mtms.inventory.application.dto.CreateSupplierPackConversionCommand;
import com.mtms.inventory.application.dto.CreateUnitCommand;
import com.mtms.inventory.application.dto.PackConversionCalculationRequest;
import com.mtms.inventory.application.dto.UpdateCatalogItemCommand;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import com.mtms.inventory.domain.UnitDimension;
import com.mtms.inventory.infrastructure.persistence.CatalogItemJpaRepository;
import com.mtms.inventory.infrastructure.persistence.SupplierJpaRepository;
import com.mtms.inventory.infrastructure.persistence.SupplierPackConversionJpaRepository;
import com.mtms.inventory.infrastructure.persistence.UnitJpaEntity;
import com.mtms.inventory.infrastructure.persistence.UnitJpaRepository;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UnitJpaRepository unitRepository;

    @Autowired
    private SupplierJpaRepository supplierRepository;

    @Autowired
    private CatalogItemJpaRepository catalogItemRepository;

    @Autowired
    private SupplierPackConversionJpaRepository conversionRepository;

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

    private UserJpaEntity userEntityA;
    private UserJpaEntity userEntityB;
    private UserJpaEntity userSystemAdmin;

    private String tokenEntityA;
    private String tokenEntityB;
    private String tokenSystemAdmin;

    private UnitJpaEntity unitCan;

    @BeforeEach
    void setUp() {
        conversionRepository.deleteAll();
        catalogItemRepository.deleteAll();
        supplierRepository.deleteAll();
        grantRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Business Entities
        entityAId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityAId, "ENT_A", "Entity A Corp", "0101111111", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        entityBId = UUID.randomUUID();
        businessEntityRepository.save(new BusinessEntityJpaEntity(
                entityBId, "ENT_B", "Entity B Corp", "0102222222", BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        // 2. Ensure canonical CAN unit is present
        unitCan = unitRepository.findByUnitCode("CAN").orElseGet(() ->
                unitRepository.save(new UnitJpaEntity(
                        UUID.randomUUID(), "CAN", "Lon", UnitDimension.COUNT, Instant.now()
                ))
        );

        // 3. Create Users
        userEntityA = createUser("manager_ent_a", "Manager Entity A");
        userEntityB = createUser("manager_ent_b", "Manager Entity B");
        userSystemAdmin = createUser("admin_sys", "System Administrator");

        LocalDate today = LocalDate.now();

        // Grant Entity A manager
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityAId, null, userEntityA.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), userSystemAdmin.getId(), Instant.now()
        ));

        // Grant Entity B manager
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityBId, null, userEntityB.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), userSystemAdmin.getId(), Instant.now()
        ));

        // Grant SYSTEM_ADMIN
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityAId, null, userSystemAdmin.getId(),
                "SYSTEM_ADMIN", "ALL", "ALL", "ALL",
                today.minusDays(1), today.plusDays(30), userSystemAdmin.getId(), Instant.now()
        ));

        tokenEntityA = jwtTokenService.generateAccessToken(userEntityA.getId(), userEntityA.getLoginIdentifier());
        tokenEntityB = jwtTokenService.generateAccessToken(userEntityB.getId(), userEntityB.getLoginIdentifier());
        tokenSystemAdmin = jwtTokenService.generateAccessToken(userSystemAdmin.getId(), userSystemAdmin.getLoginIdentifier());
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

    // ==================== CANONICAL UNITS TESTS ====================

    @Test
    @DisplayName("GET /api/v1/units: Authenticated user can list units")
    void shouldListCanonicalUnits() throws Exception {
        mockMvc.perform(get("/api/v1/units")
                        .header("Authorization", "Bearer " + tokenEntityA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/units: System Admin can create unit, but non-admin gets 403 Forbidden")
    void shouldEnforceAdminOnUnitCreation() throws Exception {
        CreateUnitCommand command = new CreateUnitCommand("BARREL", "Thùng phi", UnitDimension.VOLUME);

        // Non-admin receives 403
        mockMvc.perform(post("/api/v1/units")
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isForbidden());

        // System Admin succeeds
        mockMvc.perform(post("/api/v1/units")
                        .header("Authorization", "Bearer " + tokenSystemAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitCode").value("BARREL"))
                .andExpect(jsonPath("$.dimension").value("VOLUME"));
    }

    // ==================== SUPPLIERS TESTS ====================

    @Test
    @DisplayName("POST /api/v1/entities/{entityId}/suppliers: Create supplier and reject duplicate code within same entity")
    void shouldCreateSupplierAndRejectDuplicate() throws Exception {
        CreateSupplierCommand command = new CreateSupplierCommand(
                "SUP-VINAMILK",
                "Công ty Cổ phần Sữa Việt Nam",
                "Hotline 1900 636 979"
        );

        // 1. Create supplier in Entity A
        mockMvc.perform(post("/api/v1/entities/{entityId}/suppliers", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierCode").value("SUP-VINAMILK"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 2. Reject duplicate code in Entity A -> 409 Conflict
        mockMvc.perform(post("/api/v1/entities/{entityId}/suppliers", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:resource-conflict"));

        // 3. User B (from Entity B) cannot access Entity A suppliers -> 403 Forbidden
        mockMvc.perform(get("/api/v1/entities/{entityId}/suppliers", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }

    // ==================== CATALOG ITEMS TESTS ====================

    @Test
    @DisplayName("POST /api/v1/entities/{entityId}/items: Invariant reject expiration without lot tracking (400 Bad Request)")
    void shouldRejectExpirationWithoutLotTracking() throws Exception {
        CreateCatalogItemCommand invalidCommand = new CreateCatalogItemCommand(
                "ING-MILK-OT-380G",
                "Sữa đặc có đường Ông Thọ",
                unitCan.getId(),
                ItemKind.INGREDIENT,
                false, // lotTracking = false
                true   // expirationTracking = true -> INVALID
        );

        mockMvc.perform(post("/api/v1/entities/{entityId}/items", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCommand)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:invalid-tracking-invariant"))
                .andExpect(jsonPath("$.title").value("Invalid Tracking Invariant"));
    }

    @Test
    @DisplayName("POST /api/v1/entities/{entityId}/items: Create valid item, reject duplicate item code, and PUT update")
    void shouldCreateAndManageCatalogItem() throws Exception {
        CreateCatalogItemCommand validCommand = new CreateCatalogItemCommand(
                "ING-MILK-OT-380G",
                "Sữa đặc có đường Ông Thọ đỏ lon 380g",
                unitCan.getId(),
                ItemKind.INGREDIENT,
                true,
                true
        );

        // 1. Create item
        String responseStr = mockMvc.perform(post("/api/v1/entities/{entityId}/items", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemCode").value("ING-MILK-OT-380G"))
                .andExpect(jsonPath("$.baseUnitCode").value("CAN"))
                .andExpect(jsonPath("$.baseUnitName").value("Lon"))
                .andExpect(jsonPath("$.lotTrackingRequired").value(true))
                .andExpect(jsonPath("$.expirationTrackingRequired").value(true))
                .andReturn().getResponse().getContentAsString();

        UUID createdItemId = UUID.fromString(objectMapper.readTree(responseStr).get("id").asText());

        // 2. Reject duplicate itemCode -> 409 Conflict
        mockMvc.perform(post("/api/v1/entities/{entityId}/items", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCommand)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:duplicate-item-code"));

        // 3. Update item
        UpdateCatalogItemCommand updateCommand = new UpdateCatalogItemCommand(
                "Sữa đặc có đường Ông Thọ đỏ lon 380g (Mẫu mới)",
                ItemKind.INGREDIENT,
                true,
                true,
                ItemStatus.ACTIVE
        );

        mockMvc.perform(put("/api/v1/entities/{entityId}/items/{itemId}", entityAId, createdItemId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommand)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sữa đặc có đường Ông Thọ đỏ lon 380g (Mẫu mới)"));

        // 4. Query items with search filter
        mockMvc.perform(get("/api/v1/entities/{entityId}/items", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .param("search", "Ông Thọ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemCode").value("ING-MILK-OT-380G"));
    }

    // ==================== PACK CONVERSIONS & TC-023 CALCULATION TESTS ====================

    @Test
    @DisplayName("TC-023 End-to-End: Create pack conversion (1 box = 24 cans) and calculate 2 boxes = 48 cans snapshot")
    void tc023_shouldCalculatePackConversionSnapshotLive() throws Exception {
        // 1. Create supplier
        CreateSupplierCommand supCmd = new CreateSupplierCommand("SUP-VINAMILK", "Vinamilk", "1900");
        String supRes = mockMvc.perform(post("/api/v1/entities/{entityId}/suppliers", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(supCmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID supplierId = UUID.fromString(objectMapper.readTree(supRes).get("id").asText());

        // 2. Create catalog item (Base Unit = CAN)
        CreateCatalogItemCommand itemCmd = new CreateCatalogItemCommand(
                "ING-MILK-OT-380G", "Sữa đặc Ông Thọ", unitCan.getId(), ItemKind.INGREDIENT, true, true
        );
        String itemRes = mockMvc.perform(post("/api/v1/entities/{entityId}/items", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemCmd)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID itemId = UUID.fromString(objectMapper.readTree(itemRes).get("id").asText());

        // 3. Create Supplier Pack Conversion: "Thùng 24 lon", rate = 24.000
        CreateSupplierPackConversionCommand convCmd = new CreateSupplierPackConversionCommand(
                supplierId,
                "Thùng 24 lon",
                new BigDecimal("24.000"),
                LocalDate.of(2026, 1, 1),
                null
        );

        mockMvc.perform(post("/api/v1/entities/{entityId}/items/{itemId}/pack-conversions", entityAId, itemId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.packName").value("Thùng 24 lon"))
                .andExpect(jsonPath("$.baseUnitsPerPack").value("24.000"))
                .andExpect(jsonPath("$.supplierName").value("Vinamilk"))
                .andExpect(jsonPath("$.itemCode").value("ING-MILK-OT-380G"));

        // 4. Duplicate pack conversion returns 409 Conflict
        mockMvc.perform(post("/api/v1/entities/{entityId}/items/{itemId}/pack-conversions", entityAId, itemId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convCmd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:duplicate-conversion"));

        // 5. TC-023 Live Calculator API: Receipt 2 boxes -> 48 base units snapshot
        PackConversionCalculationRequest calcReq = new PackConversionCalculationRequest(
                itemId,
                supplierId,
                new BigDecimal("2.000"),
                LocalDate.of(2026, 11, 16)
        );

        mockMvc.perform(post("/api/v1/entities/{entityId}/pack-conversions/calculate", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(calcReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(itemId.toString()))
                .andExpect(jsonPath("$.itemCode").value("ING-MILK-OT-380G"))
                .andExpect(jsonPath("$.supplierId").value(supplierId.toString()))
                .andExpect(jsonPath("$.supplierName").value("Vinamilk"))
                .andExpect(jsonPath("$.packName").value("Thùng 24 lon"))
                .andExpect(jsonPath("$.packQuantity").value("2.000"))
                .andExpect(jsonPath("$.baseUnitsPerPack").value("24.000"))
                .andExpect(jsonPath("$.baseUnitCode").value("CAN"))
                .andExpect(jsonPath("$.baseUnitName").value("Lon"))
                .andExpect(jsonPath("$.calculatedBaseQuantity").value("48.000"))
                .andExpect(jsonPath("$.conversionSnapshot").value("24.000"))
                .andExpect(jsonPath("$.formulaExplanation").value("2.000 Thùng 24 lon × 24.000 = 48.000 Lon"));

        // 6. Verify item's packConversionCount is now 1 in list endpoint
        mockMvc.perform(get("/api/v1/entities/{entityId}/items", entityAId)
                        .header("Authorization", "Bearer " + tokenEntityA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].packConversionCount").value(1));
    }
}
