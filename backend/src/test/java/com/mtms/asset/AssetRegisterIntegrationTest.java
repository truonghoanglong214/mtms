package com.mtms.asset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.asset.application.dto.AssetDtos.CreateEquipmentAssetRequest;
import com.mtms.asset.application.dto.AssetDtos.UpdateEquipmentAssetRequest;
import com.mtms.asset.domain.model.AssetCategory;
import com.mtms.asset.domain.model.AssetStatus;
import com.mtms.asset.infrastructure.persistence.SpringDataEquipmentAssetRepository;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AssetRegisterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataEquipmentAssetRepository assetRepository;

    @Autowired
    private BusinessEntityJpaRepository entityRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository grantRepository;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID branchIdA;
    private UUID branchIdB;
    private UUID managerUserId;
    private String managerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        assetRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        entityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Business Entity
        UUID entityId = UUID.randomUUID();
        entityRepository.save(new BusinessEntityJpaEntity(
                entityId, "ENT_AST_" + UUID.randomUUID().toString().substring(0, 8),
                "Asset Test Business Entity", "0109999888",
                BusinessEntityStatus.ACTIVE, Instant.now()
        ));

        // 2. Create Branch A
        branchIdA = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchIdA, entityId, "BR_AST_A_" + UUID.randomUUID().toString().substring(0, 8),
                "Chi nhánh Quận 1", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        // 3. Create Branch B
        branchIdB = UUID.randomUUID();
        branchRepository.save(new BranchJpaEntity(
                branchIdB, entityId, "BR_AST_B_" + UUID.randomUUID().toString().substring(0, 8),
                "Chi nhánh Quận 3", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, Instant.now()
        ));

        // 4. Create Manager user granted to Branch A
        UserJpaEntity manager = new UserJpaEntity(
                UUID.randomUUID(), "ast_mgr_" + UUID.randomUUID().toString().substring(0, 8),
                passwordEncoder.encode("Password@123"), "Quản lý Chi Nhánh A",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(manager);
        managerUserId = manager.getId();

        LocalDate today = LocalDate.now();
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchIdA, manager.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), manager.getId(), Instant.now()
        ));

        managerToken = tokenService.generateAccessToken(manager.getId(), manager.getLoginIdentifier());

        // 5. Create Other user without access to Branch A
        UserJpaEntity otherUser = new UserJpaEntity(
                UUID.randomUUID(), "other_user_" + UUID.randomUUID().toString().substring(0, 8),
                passwordEncoder.encode("Password@123"), "Other User",
                UserStatus.ACTIVE, Instant.now()
        );
        userRepository.save(otherUser);

        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(), entityId, branchIdB, otherUser.getId(),
                "MANAGER", "ALL", "ALL", "ALL",
                today.minusDays(30), today.plusDays(60), otherUser.getId(), Instant.now()
        ));

        otherToken = tokenService.generateAccessToken(otherUser.getId(), otherUser.getLoginIdentifier());
    }

    @Test
    @Order(1)
    @DisplayName("P0-AST-01: Successfully create an equipment asset")
    void testCreateAssetSuccessfully() throws Exception {
        CreateEquipmentAssetRequest request = new CreateEquipmentAssetRequest(
                "AST-CF-001",
                "Máy pha cà phê Nuova Simonelli Appia II",
                AssetCategory.COFFEE_MACHINE,
                "NS-2026-99112",
                "Appia II 2 Groups",
                LocalDate.of(2026, 1, 15),
                new BigDecimal("85000000.00"),
                LocalDate.of(2027, 1, 15),
                AssetStatus.IN_USE,
                "Quầy bar chính",
                null,
                null,
                "Bảo dưỡng định kỳ 3 tháng/lần"
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/assets", branchIdA)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.branchId").value(branchIdA.toString()))
                .andExpect(jsonPath("$.assetCode").value("AST-CF-001"))
                .andExpect(jsonPath("$.name").value("Máy pha cà phê Nuova Simonelli Appia II"))
                .andExpect(jsonPath("$.category").value("COFFEE_MACHINE"))
                .andExpect(jsonPath("$.serialNumber").value("NS-2026-99112"))
                .andExpect(jsonPath("$.model").value("Appia II 2 Groups"))
                .andExpect(jsonPath("$.acquisitionCost").value(85000000.00))
                .andExpect(jsonPath("$.status").value("IN_USE"))
                .andExpect(jsonPath("$.locationInBranch").value("Quầy bar chính"));
    }

    @Test
    @Order(2)
    @DisplayName("P0-AST-01: Reject duplicate asset_code with 409 Conflict")
    void testCreateAssetWithDuplicateCodeFails() throws Exception {
        CreateEquipmentAssetRequest request1 = new CreateEquipmentAssetRequest(
                "AST-DUP-001",
                "Máy xay sinh tố Omniblend",
                AssetCategory.BLENDER,
                "OMNI-1",
                "V-TM800A",
                LocalDate.of(2026, 2, 1),
                new BigDecimal("3500000.00"),
                null,
                AssetStatus.IN_USE,
                "Quầy pha chế",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/assets", branchIdA)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateEquipmentAssetRequest request2 = new CreateEquipmentAssetRequest(
                "AST-DUP-001", // Duplicate code
                "Máy xay sinh tố Omniblend 2",
                AssetCategory.BLENDER,
                "OMNI-2",
                "V-TM800A",
                LocalDate.of(2026, 2, 1),
                new BigDecimal("3500000.00"),
                null,
                AssetStatus.IN_USE,
                "Quầy pha chế",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/assets", branchIdA)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:asset-code-conflict"))
                .andExpect(jsonPath("$.detail").value(containsString("AST-DUP-001")));
    }

    @Test
    @Order(3)
    @DisplayName("P0-AST-01: Reject negative acquisition cost with 400 Bad Request")
    void testCreateAssetWithNegativeCostFails() throws Exception {
        CreateEquipmentAssetRequest request = new CreateEquipmentAssetRequest(
                "AST-NEG-001",
                "Máy dập nắp cốc Fest",
                AssetCategory.SEALING_MACHINE,
                "FEST-01",
                "RC-95",
                LocalDate.of(2026, 1, 10),
                new BigDecimal("-500000.00"), // Negative cost
                null,
                AssetStatus.IN_USE,
                "Quầy thu ngân",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/branches/{branchId}/assets", branchIdA)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("P0-AST-01 / TC-001: Branch isolation rejects unauthorized access with 403 Forbidden")
    void testBranchIsolationNegative() throws Exception {
        mockMvc.perform(get("/api/v1/branches/{branchId}/assets", branchIdA)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }

    @Test
    @Order(5)
    @DisplayName("P0-AST-01: Update asset metadata and status transition")
    void testUpdateAsset() throws Exception {
        CreateEquipmentAssetRequest createReq = new CreateEquipmentAssetRequest(
                "AST-UPD-001",
                "Tủ lạnh Sanaky Inverter",
                AssetCategory.REFRIGERATION,
                "SNK-100",
                "VH-408K",
                LocalDate.of(2026, 1, 1),
                new BigDecimal("12000000.00"),
                LocalDate.of(2028, 1, 1),
                AssetStatus.IN_USE,
                "Kho nguyên liệu",
                null,
                null,
                null
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/assets", branchIdA)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String assetId = objectMapper.readTree(createRes).get("id").asText();

        UpdateEquipmentAssetRequest updateReq = new UpdateEquipmentAssetRequest(
                "Tủ lạnh Sanaky Inverter (Đang bảo hành van lạnh)",
                AssetCategory.REFRIGERATION,
                "SNK-100",
                "VH-408K",
                LocalDate.of(2026, 1, 1),
                new BigDecimal("12000000.00"),
                LocalDate.of(2028, 1, 1),
                AssetStatus.IN_REPAIR, // Changed to IN_REPAIR
                "Kho nguyên liệu",
                null,
                null,
                "Đang gửi hãng sửa chữa quạt gió"
        );

        mockMvc.perform(put("/api/v1/branches/{branchId}/assets/{id}", branchIdA, assetId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tủ lạnh Sanaky Inverter (Đang bảo hành van lạnh)"))
                .andExpect(jsonPath("$.status").value("IN_REPAIR"))
                .andExpect(jsonPath("$.maintenanceNote").value("Đang gửi hãng sửa chữa quạt gió"));
    }

    @Test
    @Order(6)
    @DisplayName("P0-AST-01 / TC-051: Asset summary calculates KPIs and explicitly discloses exclusion of depreciation")
    void testAssetSummaryAndTC051Disclosure() throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Asset in use with active warranty
        CreateEquipmentAssetRequest req1 = new CreateEquipmentAssetRequest(
                "AST-SUM-001",
                "Máy POS Sunmi D2",
                AssetCategory.POS_HARDWARE,
                "POS-001",
                "Sunmi D2 Mini",
                today.minusMonths(2),
                new BigDecimal("6000000.00"),
                today.plusMonths(10), // Active warranty
                AssetStatus.IN_USE,
                "Quầy thu ngân",
                null, null, null
        );

        // 2. Asset with warranty expiring soon (in 15 days)
        CreateEquipmentAssetRequest req2 = new CreateEquipmentAssetRequest(
                "AST-SUM-002",
                "Máy dập nắp",
                AssetCategory.SEALING_MACHINE,
                "SEAL-001",
                "Yubann",
                today.minusMonths(11),
                new BigDecimal("4000000.00"),
                today.plusDays(15), // Expiring in 15 days
                AssetStatus.IN_USE,
                "Quầy pha chế",
                null, null, null
        );

        // 3. Asset with warranty already expired
        CreateEquipmentAssetRequest req3 = new CreateEquipmentAssetRequest(
                "AST-SUM-003",
                "Bàn ghế gỗ thông",
                AssetCategory.FURNITURE,
                null,
                "Bộ 4 ghế",
                today.minusYears(2),
                new BigDecimal("10000000.00"),
                today.minusMonths(6), // Expired warranty
                AssetStatus.IN_USE,
                "Sảnh chính",
                null, null, null
        );

        // 4. Asset in repair
        CreateEquipmentAssetRequest req4 = new CreateEquipmentAssetRequest(
                "AST-SUM-004",
                "Máy làm đá Hải Âu",
                AssetCategory.REFRIGERATION,
                "ICE-001",
                "HA-60",
                today.minusMonths(6),
                new BigDecimal("25000000.00"),
                today.plusMonths(6),
                AssetStatus.IN_REPAIR,
                "Bếp",
                null, null, "Sửa củ bơm nước"
        );

        for (CreateEquipmentAssetRequest r : List.of(req1, req2, req3, req4)) {
            mockMvc.perform(post("/api/v1/branches/{branchId}/assets", branchIdA)
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(r)))
                    .andExpect(status().isCreated());
        }

        // Call summary endpoint
        mockMvc.perform(get("/api/v1/branches/{branchId}/assets/summary", branchIdA)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(branchIdA.toString()))
                .andExpect(jsonPath("$.totalAssets").value(4))
                .andExpect(jsonPath("$.totalAcquisitionCost").value(45000000.00)) // 6M + 4M + 10M + 25M = 45M
                .andExpect(jsonPath("$.inUseCount").value(3))
                .andExpect(jsonPath("$.inRepairCount").value(1))
                .andExpect(jsonPath("$.warrantyExpiringCount").value(1))
                .andExpect(jsonPath("$.warrantyExpiredCount").value(1))
                .andExpect(jsonPath("$.categoryBreakdown.POS_HARDWARE").value(1))
                .andExpect(jsonPath("$.categoryBreakdown.SEALING_MACHINE").value(1))
                .andExpect(jsonPath("$.categoryBreakdown.FURNITURE").value(1))
                .andExpect(jsonPath("$.categoryBreakdown.REFRIGERATION").value(1))
                // CRUCIAL TC-051 / ADR-023 verification:
                .andExpect(jsonPath("$.depreciationDisclaimer")
                        .value("P0 P&L excludes depreciation (TC-051 / ADR-023). Chi phí khấu hao chưa được trích vào P&L tạm tính giai đoạn MVP."));
    }

    @Test
    @Order(7)
    @DisplayName("P0-AST-01: Delete asset successfully removes record")
    void testDeleteAsset() throws Exception {
        CreateEquipmentAssetRequest createReq = new CreateEquipmentAssetRequest(
                "AST-DEL-001",
                "Quạt đảo trần Panasonic",
                AssetCategory.OTHER,
                "PANA-01",
                "F-409MB",
                LocalDate.now(),
                new BigDecimal("1500000.00"),
                null,
                AssetStatus.IN_USE,
                "Khu vực tầng 1",
                null, null, null
        );

        String createRes = mockMvc.perform(post("/api/v1/branches/{branchId}/assets", branchIdA)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String assetId = objectMapper.readTree(createRes).get("id").asText();

        mockMvc.perform(delete("/api/v1/branches/{branchId}/assets/{id}", branchIdA, assetId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/branches/{branchId}/assets/{id}", branchIdA, assetId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:asset-not-found"));
    }
}
