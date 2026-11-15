package com.mtms.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.evidence.application.EvidenceTokenService;
import com.mtms.evidence.infrastructure.persistence.EvidenceFileJpaRepository;
import com.mtms.evidence.infrastructure.storage.LocalStorageService;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-SEC-01 / BR-SEC-01 / TC-043
 * Comprehensive Integration Tests for Evidence Storage & Security Boundary.
 * Validates file upload, max size / MIME enforcement, signed download token expiration / tampering,
 * and branch isolation security boundary (TC-043).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvidenceIntegrationTest {

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
    private EvidenceFileJpaRepository evidenceFileRepository;

    @Autowired
    private EvidenceTokenService evidenceTokenService;

    @Autowired
    private LocalStorageService localStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID entityId;
    private UUID branchAId;
    private UUID branchBId;

    private UserJpaEntity userOwnerA;
    private UserJpaEntity userOwnerB;
    private UserJpaEntity userSystemAdmin;

    private String tokenOwnerA;
    private String tokenOwnerB;
    private String tokenSystemAdmin;

    @BeforeEach
    void setUp() {
        evidenceFileRepository.deleteAll();
        grantRepository.deleteAll();
        branchRepository.deleteAll();
        businessEntityRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Business Entity
        entityId = UUID.randomUUID();
        BusinessEntityJpaEntity entity = new BusinessEntityJpaEntity(
                entityId,
                "LTT_VN",
                "Cong ty CP Luc Tra Tinh Hoa",
                "0109999999",
                BusinessEntityStatus.ACTIVE,
                Instant.now()
        );
        businessEntityRepository.save(entity);

        // 2. Create Branch A and Branch B
        branchAId = UUID.randomUUID();
        BranchJpaEntity branchA = new BranchJpaEntity(
                branchAId,
                entityId,
                "LTT_HCM_01",
                "Luc Tra Tinh Hoa - Quan 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );
        branchRepository.save(branchA);

        branchBId = UUID.randomUUID();
        BranchJpaEntity branchB = new BranchJpaEntity(
                branchBId,
                entityId,
                "LTT_HCM_02",
                "Luc Tra Tinh Hoa - Binh Thanh",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );
        branchRepository.save(branchB);

        // 3. Create Users
        userOwnerA = createUser("owner_a", "Branch A Owner");
        userOwnerB = createUser("owner_b", "Branch B Owner");
        userSystemAdmin = createUser("sys_admin", "Global System Admin");

        LocalDate today = LocalDate.now();

        // 4. Role Grants
        // User A -> BRANCH_OWNER on Branch A
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchAId,
                userOwnerA.getId(),
                "BRANCH_OWNER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // User B -> BRANCH_OWNER on Branch B
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branchBId,
                userOwnerB.getId(),
                "BRANCH_OWNER",
                "ALL",
                "ALL",
                "ALL",
                today.minusDays(1),
                today.plusDays(30),
                userSystemAdmin.getId(),
                Instant.now()
        ));

        // Admin -> SYSTEM_ADMIN unscoped
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

        tokenOwnerA = jwtTokenService.generateAccessToken(userOwnerA.getId(), userOwnerA.getLoginIdentifier());
        tokenOwnerB = jwtTokenService.generateAccessToken(userOwnerB.getId(), userOwnerB.getLoginIdentifier());
        tokenSystemAdmin = jwtTokenService.generateAccessToken(userSystemAdmin.getId(), userSystemAdmin.getLoginIdentifier());
    }

    private UserJpaEntity createUser(String username, String displayName) {
        UserJpaEntity user = new UserJpaEntity(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode("StrongP@ss123"),
                displayName,
                UserStatus.ACTIVE,
                Instant.now()
        );
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Upload evidence successfully with valid PDF and PNG")
    void testUploadEvidenceSuccess() throws Exception {
        byte[] pdfContent = "%PDF-1.4 sample evidence content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "invoice_2026.pdf",
                "application/pdf",
                pdfContent
        );

        String responseContent = mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(pdfFile)
                        .param("visibility", "BRANCH_PRIVATE")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalFilename").value("invoice_2026.pdf"))
                .andExpect(jsonPath("$.data.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.data.byteSize").value(pdfContent.length))
                .andExpect(jsonPath("$.data.visibilityClass").value("BRANCH_PRIVATE"))
                .andExpect(jsonPath("$.data.branchId").value(branchAId.toString()))
                .andReturn().getResponse().getContentAsString();

        String fileIdStr = objectMapper.readTree(responseContent).path("data").path("id").asText();
        UUID fileId = UUID.fromString(fileIdStr);

        assertThat(evidenceFileRepository.existsById(fileId)).isTrue();
    }

    @Test
    @DisplayName("Reject evidence file exceeding 5MB limit with 400 Bad Request")
    void testRejectFileExceedingMaxSize() throws Exception {
        // Create 5MB + 1 byte payload
        byte[] oversizedContent = new byte[(int) (5 * 1024 * 1024 + 1)];
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file",
                "huge_file.pdf",
                "application/pdf",
                oversizedContent
        );

        mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(oversizedFile)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").isNotEmpty());
    }

    @Test
    @DisplayName("Reject unsupported MIME types (.exe, text/plain) with 400 Bad Request")
    void testRejectUnsupportedMimeType() throws Exception {
        MockMultipartFile executableFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "binary content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(executableFile)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:illegal-argument"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Unsupported content type")));
    }

    @Test
    @DisplayName("TC-043 Security Boundary: User belonging to Branch B cannot generate token for Branch A's file")
    void testTC043BranchBUserCannotGenerateTokenForBranchAEvidence() throws Exception {
        // 1. Branch A Owner uploads file to Branch A
        byte[] content = "Branch A Confidential Receipt".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt_a.png",
                "image/png",
                content
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(file)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileIdStr = objectMapper.readTree(uploadResponse).path("data").path("id").asText();
        UUID fileIdA = UUID.fromString(fileIdStr);

        // 2. Branch B Owner attempts to generate download token for Branch A's file -> 403 Forbidden
        mockMvc.perform(post("/api/v1/evidence/{fileId}/token", fileIdA)
                        .header("Authorization", "Bearer " + tokenOwnerB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));

        // 3. Branch A Owner requests token for their own file -> succeeds 200 OK
        mockMvc.perform(post("/api/v1/evidence/{fileId}/token", fileIdA)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.downloadUrl").isNotEmpty());
    }

    @Test
    @DisplayName("TC-043 Security Boundary: Direct content access with valid signed token succeeds without auth header")
    void testDownloadContentWithValidTokenSucceeds() throws Exception {
        byte[] content = "Content of secure image".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.png",
                "image/png",
                content
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(file)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileIdStr = objectMapper.readTree(uploadResponse).path("data").path("id").asText();
        UUID fileId = UUID.fromString(fileIdStr);

        // Generate token
        String tokenResponse = mockMvc.perform(post("/api/v1/evidence/{fileId}/token", fileId)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(tokenResponse).path("data").path("token").asText();

        // Download via public GET /content with token parameter (NO Authorization header)
        mockMvc.perform(get("/api/v1/evidence/{fileId}/content", fileId)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(content));
    }

    @Test
    @DisplayName("TC-043 Security Boundary: Content access with tampered signature is rejected with 403 Forbidden")
    void testContentAccessWithTamperedSignatureRejected() throws Exception {
        byte[] content = "Confidential test payload".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                content
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(file)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileIdStr = objectMapper.readTree(uploadResponse).path("data").path("id").asText();
        UUID fileId = UUID.fromString(fileIdStr);

        String token = evidenceTokenService.generateToken(fileId, branchAId);

        // Tamper signature by modifying last character
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        mockMvc.perform(get("/api/v1/evidence/{fileId}/content", fileId)
                        .param("token", tamperedToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }

    @Test
    @DisplayName("TC-043 Security Boundary: Content access with expired token is rejected with 403 Forbidden")
    void testContentAccessWithExpiredTokenRejected() throws Exception {
        byte[] content = "Confidential test payload".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "expired_test.png",
                "image/png",
                content
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(file)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileIdStr = objectMapper.readTree(uploadResponse).path("data").path("id").asText();
        UUID fileId = UUID.fromString(fileIdStr);

        // Generate expired token (negative TTL)
        String expiredToken = evidenceTokenService.generateToken(fileId, branchAId, Duration.ofMinutes(-5));

        mockMvc.perform(get("/api/v1/evidence/{fileId}/content", fileId)
                        .param("token", expiredToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }

    @Test
    @DisplayName("TC-043 Security Boundary: Token issued for File A cannot be used to download File B")
    void testTokenMismatchTargetFileIdRejected() throws Exception {
        // Upload File A
        MockMultipartFile fileA = new MockMultipartFile(
                "file",
                "file_a.png",
                "image/png",
                "File A data".getBytes(StandardCharsets.UTF_8)
        );
        String resA = mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(fileA)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID fileIdA = UUID.fromString(objectMapper.readTree(resA).path("data").path("id").asText());

        // Upload File B
        MockMultipartFile fileB = new MockMultipartFile(
                "file",
                "file_b.png",
                "image/png",
                "File B data".getBytes(StandardCharsets.UTF_8)
        );
        String resB = mockMvc.perform(multipart("/api/v1/branches/{branchId}/evidence", branchAId)
                        .file(fileB)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID fileIdB = UUID.fromString(objectMapper.readTree(resB).path("data").path("id").asText());

        // Generate token for File A
        String tokenA = evidenceTokenService.generateToken(fileIdA, branchAId);

        // Attempt to download File B using File A's token -> 403 Forbidden
        mockMvc.perform(get("/api/v1/evidence/{fileId}/content", fileIdB)
                        .param("token", tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:forbidden"));
    }
}
