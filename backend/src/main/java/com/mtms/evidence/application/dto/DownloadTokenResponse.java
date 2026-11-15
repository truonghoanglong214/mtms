package com.mtms.evidence.application.dto;

import java.time.Instant;

/**
 * P0-SEC-01 / BR-SEC-01 / TC-043
 * Response containing short-lived signed access token and download URL.
 */
public record DownloadTokenResponse(
        String token,
        Instant expiresAt,
        String downloadUrl
) {
}
