package com.mtms.identity.infrastructure.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revokeToken(String token, Instant expiresAt) {
        if (token != null && !token.trim().isEmpty()) {
            revokedTokens.put(token, expiresAt != null ? expiresAt : Instant.now().plusSeconds(3600 * 24));
        }
    }

    public boolean isRevoked(String token) {
        if (token == null) {
            return false;
        }
        Instant expiry = revokedTokens.get(token);
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }
}
