package com.mtms.evidence.application;

import com.mtms.common.error.AccessForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01 / TC-043
 * Cryptographic service generating and verifying time-limited HMAC-SHA256 signed tokens
 * for secure, branch-scoped evidence downloads.
 */
@Service
public class EvidenceTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final byte[] secretKeyBytes;

    public EvidenceTokenService(
            @Value("${mtms.security.evidence-token-secret:mtms-default-secure-evidence-signing-secret-key-32b}")
            String secretKey
    ) {
        this.secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    public record TokenClaims(UUID fileId, UUID branchId, Instant expiresAt) {}

    /**
     * Generates a signed, URL-safe access token valid for 15 minutes.
     */
    public String generateToken(UUID fileId, UUID branchId) {
        return generateToken(fileId, branchId, DEFAULT_TTL);
    }

    public String generateToken(UUID fileId, UUID branchId, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        String payload = fileId.toString() + ":" + branchId.toString() + ":" + expiresAt.getEpochSecond();
        String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        byte[] signatureBytes = computeHmacSha256(payload.getBytes(StandardCharsets.UTF_8));
        String signatureBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        return payloadBase64 + "." + signatureBase64;
    }

    /**
     * Validates that the token:
     * 1. Is structurally well-formed.
     * 2. Matches the target fileId.
     * 3. Has not expired.
     * 4. Has a valid HMAC-SHA256 signature that has not been tampered with.
     */
    public TokenClaims validateToken(UUID targetFileId, String token) {
        if (token == null || token.isBlank()) {
            throw new AccessForbiddenException("Evidence access token is missing or blank");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new AccessForbiddenException("Malformed evidence access token");
        }

        String payloadBase64 = parts[0];
        String signatureBase64 = parts[1];

        String payloadString;
        byte[] signatureProvided;
        try {
            payloadString = new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);
            signatureProvided = Base64.getUrlDecoder().decode(signatureBase64);
        } catch (IllegalArgumentException e) {
            throw new AccessForbiddenException("Invalid base64 encoding in evidence access token");
        }

        String[] payloadParts = payloadString.split(":");
        if (payloadParts.length != 3) {
            throw new AccessForbiddenException("Malformed payload in evidence access token");
        }

        UUID tokenFileId;
        UUID tokenBranchId;
        long epochSecond;
        try {
            tokenFileId = UUID.fromString(payloadParts[0]);
            tokenBranchId = UUID.fromString(payloadParts[1]);
            epochSecond = Long.parseLong(payloadParts[2]);
        } catch (IllegalArgumentException e) {
            throw new AccessForbiddenException("Invalid fields in evidence access token payload");
        }

        // TC-043 Boundary: Ensure token belongs to the requested file
        if (targetFileId != null && !targetFileId.equals(tokenFileId)) {
            throw new AccessForbiddenException("Evidence access token does not match requested file ID");
        }

        Instant expiresAt = Instant.ofEpochSecond(epochSecond);
        if (Instant.now().isAfter(expiresAt)) {
            throw new AccessForbiddenException("Evidence access token has expired");
        }

        // Verify cryptographic signature with constant-time equality check
        byte[] expectedSignature = computeHmacSha256(payloadString.getBytes(StandardCharsets.UTF_8));
        if (!MessageDigest.isEqual(expectedSignature, signatureProvided)) {
            throw new AccessForbiddenException("Invalid or tampered evidence token signature");
        }

        return new TokenClaims(tokenFileId, tokenBranchId, expiresAt);
    }

    private byte[] computeHmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to initialize HMAC-SHA256 signer", e);
        }
    }
}
