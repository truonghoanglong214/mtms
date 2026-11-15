package com.mtms.evidence.application;

import com.mtms.common.error.AccessForbiddenException;
import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.evidence.application.dto.DownloadTokenResponse;
import com.mtms.evidence.application.dto.EvidenceFileDto;
import com.mtms.evidence.application.dto.EvidenceUploadResponse;
import com.mtms.evidence.application.dto.FilePayload;
import com.mtms.evidence.domain.EvidenceFile;
import com.mtms.evidence.domain.EvidenceFileRepository;
import com.mtms.evidence.domain.VisibilityClass;
import com.mtms.evidence.infrastructure.storage.StorageService;
import com.mtms.identity.infrastructure.security.BranchAuthorizationService;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01 / TC-043
 * Core application service managing evidence uploads, metadata persistence,
 * branch boundary access, and signed-token content streaming.
 */
@Service
public class EvidenceService {

    private final EvidenceFileRepository evidenceFileRepository;
    private final StorageService storageService;
    private final EvidenceTokenService evidenceTokenService;
    private final BranchJpaRepository branchJpaRepository;
    private final BranchAuthorizationService branchAuthorizationService;

    public EvidenceService(
            EvidenceFileRepository evidenceFileRepository,
            StorageService storageService,
            EvidenceTokenService evidenceTokenService,
            BranchJpaRepository branchJpaRepository,
            BranchAuthorizationService branchAuthorizationService
    ) {
        this.evidenceFileRepository = evidenceFileRepository;
        this.storageService = storageService;
        this.evidenceTokenService = evidenceTokenService;
        this.branchJpaRepository = branchJpaRepository;
        this.branchAuthorizationService = branchAuthorizationService;
    }

    /**
     * Uploads an evidence file to private storage, calculates SHA-256 checksum,
     * and persists metadata. Enforces size <= 5MB and allowed MIME types.
     */
    @Transactional
    public EvidenceUploadResponse uploadEvidence(
            UUID branchId,
            MultipartFile file,
            VisibilityClass visibility,
            UUID uploadedBy
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Evidence file cannot be empty");
        }

        BranchJpaEntity branch = branchJpaRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        long byteSize = file.getSize();
        if (byteSize > EvidenceFile.MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 5MB (" + byteSize + " bytes)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !EvidenceFile.ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType +
                    ". Allowed types: " + EvidenceFile.ALLOWED_CONTENT_TYPES);
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file content", e);
        }

        String checksumSha256 = calculateSha256(fileBytes);
        UUID fileId = UUID.randomUUID();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence_" + fileId;
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String storageObjectKey = "branches/" + branchId + "/" + fileId + "_" + sanitizedFilename;

        // Store file in private storage
        storageService.store(storageObjectKey, new ByteArrayInputStream(fileBytes), byteSize, contentType);

        // Persist immutable metadata
        EvidenceFile evidenceFile = new EvidenceFile(
                fileId,
                branch.getBusinessEntityId(),
                branchId,
                storageObjectKey,
                sanitizedFilename,
                contentType,
                byteSize,
                checksumSha256,
                visibility != null ? visibility : VisibilityClass.BRANCH_PRIVATE,
                null,
                uploadedBy,
                Instant.now()
        );

        evidenceFileRepository.save(evidenceFile);

        return new EvidenceUploadResponse(
                evidenceFile.getId(),
                evidenceFile.getBranchId(),
                evidenceFile.getOriginalFilename(),
                evidenceFile.getContentType(),
                evidenceFile.getByteSize(),
                evidenceFile.getChecksumSha256(),
                evidenceFile.getVisibilityClass(),
                evidenceFile.getUploadedBy(),
                evidenceFile.getCreatedAt()
        );
    }

    /**
     * Retrieves evidence file metadata by ID.
     */
    @Transactional(readOnly = true)
    public EvidenceFileDto getEvidenceFile(UUID fileId) {
        return evidenceFileRepository.findById(fileId)
                .map(EvidenceFileDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence file not found: " + fileId));
    }

    /**
     * Lists evidence files belonging to a specific branch.
     */
    @Transactional(readOnly = true)
    public List<EvidenceFileDto> listEvidenceByBranch(UUID branchId) {
        return evidenceFileRepository.findByBranchId(branchId)
                .stream()
                .map(EvidenceFileDto::from)
                .toList();
    }

    /**
     * Generates a 15-minute time-limited signed download token.
     * TC-043 Boundary: Enforces that the requesting user has valid active grant for the file's branch.
     */
    @Transactional(readOnly = true)
    public DownloadTokenResponse generateDownloadToken(UUID fileId, UUID currentUserId) {
        EvidenceFile file = evidenceFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence file not found: " + fileId));

        // TC-043 Check: Enforce user has active access to the branch owning this evidence file
        if (!branchAuthorizationService.hasBranchAccess(file.getBranchId(), "ADMIN", "OWNER", "MANAGER", "FINANCE", "EMPLOYEE")) {
            throw new AccessForbiddenException("User is forbidden from accessing evidence files for branch " + file.getBranchId());
        }

        String token = evidenceTokenService.generateToken(file.getId(), file.getBranchId());
        EvidenceTokenService.TokenClaims claims = evidenceTokenService.validateToken(file.getId(), token);

        String downloadUrl = "/api/v1/evidence/" + fileId + "/content?token=" + token;
        return new DownloadTokenResponse(token, claims.expiresAt(), downloadUrl);
    }

    /**
     * Streams the content of the evidence file after validating the signed token.
     */
    @Transactional(readOnly = true)
    public FilePayload loadContentStream(UUID fileId, String token) {
        // Validate signed token (checks signature, fileId match, expiration)
        evidenceTokenService.validateToken(fileId, token);

        EvidenceFile file = evidenceFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence file not found: " + fileId));

        InputStream stream = storageService.load(file.getStorageObjectKey());

        return new FilePayload(
                stream,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getByteSize()
        );
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String sanitizeFilename(String filename) {
        String cleaned = filename.replace("\\", "/");
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash >= 0) {
            cleaned = cleaned.substring(lastSlash + 1);
        }
        return cleaned.trim();
    }
}
