package com.mtms.identity.application.dto;

public record TokenRefreshResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
    public static TokenRefreshResponse of(String accessToken, long expiresInSeconds) {
        return new TokenRefreshResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
