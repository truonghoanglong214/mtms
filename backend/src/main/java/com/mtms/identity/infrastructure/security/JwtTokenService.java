package com.mtms.identity.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long accessTokenValidityMinutes;
    private final long refreshTokenValidityDays;

    public JwtTokenService(
            @Value("${mtms.security.jwt.secret:default-mtms-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256}")
            String secret,
            @Value("${mtms.security.jwt.access-token-validity-minutes:60}")
            long accessTokenValidityMinutes,
            @Value("${mtms.security.jwt.refresh-token-validity-days:7}")
            long refreshTokenValidityDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMinutes = accessTokenValidityMinutes;
        this.refreshTokenValidityDays = refreshTokenValidityDays;
    }

    public String generateAccessToken(UUID userId, String loginIdentifier) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenValidityMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("loginIdentifier", loginIdentifier)
                .claim("tokenType", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenValidityDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseAndValidateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired JWT token: " + ex.getMessage());
        }
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = parseAndValidateToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseAndValidateToken(token);
        return "REFRESH".equals(claims.get("tokenType", String.class));
    }
}
