package com.mtms.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.importer.infrastructure.persistence.ImportBatchJpaRepository;
import com.mtms.importer.infrastructure.persistence.ImportRowJpaRepository;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-SEC-04 / BR-SEC-02 / TC-045
 * Integration test suite for Excel and CSV Import Staging, Formula Sanitization,
 * TC-045 Duplicate Identifier Rejection, Idempotent Commit, and Branch Scoped Authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportIntegrationTest {

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
    private ImportBatchJpaRepository importBatchRepository;

    @Autowired
    private ImportRowJpaRepository importRowRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchAId;
    private UUID branchBId;

    private UserJpaEntity userManagerA;
    private UserJpaEntity userManagerB;
    private UserJpaEntity userSystemAdmin;

    private String tokenManagerA;
    private String tokenManagerB;
    private String tokenSystemAdmin;

    @BeforeEach
    void setUp() {
        importRowRepository.deleteAll();
        importBatchRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Setup Business Entity
        entityId = UUID.randomUUID();
        BusinessEntityJpaEntity entity = new BusinessEntityJpaEntity(
                entityId,
                "TEA_VN",
                "Cong Ty Tra MTMS",
                "0108888888",
                BusinessEntityStatus.ACTIVE,
                Instant.now()
        );
        businessEntityRepository.save(entity);

        // 2. Setup Branches
        branchAId = UUID.randomUUID();
        BranchJpaEntity branchA = new BranchJpaEntity(
                branchAId,
                entityId,
                "TEA_D1",
                "MTMS Tea - Quan 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );
        branchRepository.save(branchA);

        branchBId = UUID.randomUUID();
        BranchJpaEntity branchB = new BranchJpaEntity(
                branchBId,
                entityId,
                "TEA_BT",
                "MTMS Tea - Binh Thanh",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );
        branchRepository.save(branchB);

        // 3. Setup Users
        userManagerA = createUser("manager_a", "Branch A Manager");
        userManagerB = createUser("manager_b", "Branch B Manager");
        userSystemAdmin = createUser("sys_admin", "Global System Admin");

        LocalDate today = LocalDate.now();

        // User A -> MANAGER on Branch A
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchAId,
                userManagerA.getId(),
                "MANAGER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // User B -> MANAGER on Branch B
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchBId,
                userManagerB.getId(),
                "MANAGER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // System Admin
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                null,
                userSystemAdmin.getId(),
                "SYSTEM_ADMIN",
                "*",
                "ALL",
                "ALL",
                today.minusDays(1),
                null,
                userSystemAdmin.getId(),
                Instant.now()
        ));

        tokenManagerA = jwtTokenService.generateAccessToken(userManagerA.getId(), userManagerA.getLoginIdentifier());
        tokenManagerB = jwtTokenService.generateAccessToken(userManagerB.getId(), userManagerB.getLoginIdentifier());
        tokenSystemAdmin = jwtTokenService.generateAccessToken(userSystemAdmin.getId(), userSystemAdmin.getLoginIdentifier());
    }

    private UserJpaEntity createUser(String username, String displayName) {
        UserJpaEntity user = new UserJpaEntity(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode("Secret123@Pass"),
                displayName,
                UserStatus.ACTIVE,
                Instant.now()
        );
        return userRepository.save(user);
    }

    private byte[] createSampleXlsx(String[][] data, String[] headers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                headerRow.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    row.createCell(c).setCellValue(data[r][c]);
                }
            }
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    @Test
    @DisplayName("P0-SEC-04: Successful XLSX staging, preview, row listing, and idempotent commit")
    void testXlsxStagingAndCommitFlow() throws Exception {
        String[] headers = {"item_code", "item_name", "quantity", "unit", "unit_cost"};
        String[][] rows = {
                {"CF-01", "Coffee Arabica", "100", "kg", "120000"},
                {"CF-02", "Coffee Robusta", "50", "kg", "90000"}
        };
        byte[] xlsxBytes = createSampleXlsx(rows, headers);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "opening_inventory.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes
        );

        // 1. Stage file
        MvcResult stageResult = mockMvc.perform(multipart("/api/v1/branches/{branchId}/imports/stage", branchAId)
                        .file(file)
                        .param("importKind", "OPENING_INVENTORY")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VALIDATED"))
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.validRows").value(2))
                .andExpect(jsonPath("$.data.errorRows").value(0))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(stageResult.getResponse().getContentAsString());
        String batchIdStr = responseNode.path("data").path("id").asText();
        UUID batchId = UUID.fromString(batchIdStr);

        // 2. Get batch details
        mockMvc.perform(get("/api/v1/branches/{branchId}/imports/{batchId}", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(batchIdStr))
                .andExpect(jsonPath("$.data.status").value("VALIDATED"));

        // 3. Get batch rows
        mockMvc.perform(get("/api/v1/branches/{branchId}/imports/{batchId}/rows", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].status").value("VALID"))
                .andExpect(jsonPath("$.data[1].status").value("VALID"));

        // 4. Commit batch with Idempotency-Key
        String idempotencyKey = "batch-commit-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/branches/{branchId}/imports/{batchId}/commit", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMMITTED"))
                .andExpect(jsonPath("$.data.committedBy").value(userManagerA.getId().toString()));

        // 5. Replay commit with same Idempotency-Key -> cached response with Idempotent-Replayed: true
        mockMvc.perform(post("/api/v1/branches/{branchId}/imports/{batchId}/commit", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.data.status").value("COMMITTED"));
    }

    @Test
    @DisplayName("P0-SEC-04: Successful CSV staging and validation")
    void testCsvStagingSuccess() throws Exception {
        String csvContent = """
                item_code,item_name,quantity,unit,unit_cost
                TEA-01,Thai Nguyen Green Tea,30,kg,180000
                TEA-02,Shan Tuyet Old Tree Tea,15,kg,450000
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tea_catalog.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/branches/{branchId}/imports/stage", branchAId)
                        .file(file)
                        .param("importKind", "OPENING_INVENTORY")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("VALIDATED"))
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.validRows").value(2))
                .andExpect(jsonPath("$.data.errorRows").value(0));
    }

    @Test
    @DisplayName("TC-045: Duplicate identifiers across rows flagged as ERROR and commit rejected with 400")
    void testTC045_DuplicateIdentifierRejection() throws Exception {
        // Row 2 and Row 5 share the same item_code "CF-DUPLICATE"
        String[] headers = {"item_code", "item_name", "quantity", "unit", "unit_cost"};
        String[][] rows = {
                {"CF-DUPLICATE", "Coffee First", "10", "kg", "50000"},       // row 2
                {"CF-UNIQUE-1", "Coffee Unique 1", "20", "kg", "60000"},     // row 3
                {"CF-UNIQUE-2", "Coffee Unique 2", "15", "kg", "70000"},     // row 4
                {"CF-DUPLICATE", "Coffee Duplicate", "5", "kg", "50000"}     // row 5
        };
        byte[] xlsxBytes = createSampleXlsx(rows, headers);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicate_items.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes
        );

        // 1. Stage file -> Status must be PREVIEW because errorRows > 0
        MvcResult stageResult = mockMvc.perform(multipart("/api/v1/branches/{branchId}/imports/stage", branchAId)
                        .file(file)
                        .param("importKind", "OPENING_INVENTORY")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PREVIEW"))
                .andExpect(jsonPath("$.data.totalRows").value(4))
                .andExpect(jsonPath("$.data.validRows").value(2))
                .andExpect(jsonPath("$.data.errorRows").value(2))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(stageResult.getResponse().getContentAsString());
        UUID batchId = UUID.fromString(responseNode.path("data").path("id").asText());

        // 2. Query only ERROR rows -> Exactly rows 2 and 5
        mockMvc.perform(get("/api/v1/branches/{branchId}/imports/{batchId}/rows", branchAId, batchId)
                        .param("status", "ERROR")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].rowNumber").value(2))
                .andExpect(jsonPath("$.data[0].errors[0].error").value("DUPLICATE_IDENTIFIER"))
                .andExpect(jsonPath("$.data[0].errors[0].message", containsString("Duplicate identifier 'CF-DUPLICATE' found in rows 2, 5")))
                .andExpect(jsonPath("$.data[1].rowNumber").value(5))
                .andExpect(jsonPath("$.data[1].errors[0].error").value("DUPLICATE_IDENTIFIER"))
                .andExpect(jsonPath("$.data[1].errors[0].message", containsString("Duplicate identifier 'CF-DUPLICATE' found in rows 2, 5")));

        // 3. Attempt to commit batch containing errors -> Must be rejected with HTTP 400 Bad Request
        String idempotencyKey = "commit-dup-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/branches/{branchId}/imports/{batchId}/commit", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerA)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("only VALIDATED batches can be committed")));
    }

    @Test
    @DisplayName("P0-SEC-04: Malformed unit, non-positive quantity, and formula injection sanitization")
    void testDataValidationAndFormulaSanitization() throws Exception {
        // Row with non-positive quantity and blank unit, plus formula injection cell '=cmd|' /C calc'!A0'
        String csvContent = """
                item_code,item_name,quantity,unit,unit_cost
                =cmd|' /C calc'!A0,Injection Item,-5,,10000
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid_data.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/branches/{branchId}/imports/stage", branchAId)
                        .file(file)
                        .param("importKind", "OPENING_INVENTORY")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PREVIEW"))
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.errorRows").value(1))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID batchId = UUID.fromString(responseNode.path("data").path("id").asText());

        // Check sanitized value in normalizedData and error codes
        mockMvc.perform(get("/api/v1/branches/{branchId}/imports/{batchId}/rows", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].normalizedData.item_code").value("'=cmd|' /C calc'!A0"))
                .andExpect(jsonPath("$.data[0].status").value("ERROR"))
                .andExpect(jsonPath("$.data[0].errors", hasSize(2))); // INVALID_QUANTITY + MISSING_FIELD for unit
    }

    @Test
    @DisplayName("P0-SEC-04: Cross-branch access control enforced (HTTP 403 Forbidden)")
    void testCrossBranchAccessForbidden() throws Exception {
        String csvContent = """
                item_code,item_name,quantity,unit,unit_cost
                TEA-BR-A,Branch A Item,10,kg,100000
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "branch_a_items.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Stage on Branch A
        MvcResult stageResult = mockMvc.perform(multipart("/api/v1/branches/{branchId}/imports/stage", branchAId)
                        .file(file)
                        .param("importKind", "OPENING_INVENTORY")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(stageResult.getResponse().getContentAsString());
        UUID batchId = UUID.fromString(responseNode.path("data").path("id").asText());

        // Manager B (only granted for Branch B) attempts to inspect Branch A's batch -> 403 Forbidden
        mockMvc.perform(get("/api/v1/branches/{branchId}/imports/{batchId}", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerB))
                .andExpect(status().isForbidden());

        // Manager B attempts to commit Branch A's batch -> 403 Forbidden
        mockMvc.perform(post("/api/v1/branches/{branchId}/imports/{batchId}/commit", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerB)
                        .header("Idempotency-Key", "cross-commit-" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P0-SEC-04: Cannot commit an already committed batch without idempotency replay (HTTP 409 Conflict)")
    void testCannotRecommitBatch() throws Exception {
        String csvContent = """
                item_code,item_name,quantity,unit,unit_cost
                VALID-01,Valid Item,10,kg,10000
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult stageResult = mockMvc.perform(multipart("/api/v1/branches/{branchId}/imports/stage", branchAId)
                        .file(file)
                        .param("importKind", "OPENING_INVENTORY")
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(stageResult.getResponse().getContentAsString());
        UUID batchId = UUID.fromString(responseNode.path("data").path("id").asText());

        // First commit succeeds
        mockMvc.perform(post("/api/v1/branches/{branchId}/imports/{batchId}/commit", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMMITTED"));

        // Second commit without Idempotency-Key fails with 409 Conflict
        mockMvc.perform(post("/api/v1/branches/{branchId}/imports/{batchId}/commit", branchAId, batchId)
                        .header("Authorization", "Bearer " + tokenManagerA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("has already been committed")));
    }
}
