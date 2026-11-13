package com.mtms.identity;

import com.mtms.identity.infrastructure.security.InvalidTokenException;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(
                "this-is-a-secure-and-long-test-secret-key-that-satisfies-the-256-bit-requirement",
                15,
                7
        );
    }

    @Test
    @DisplayName("Should generate and validate access token")
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        String loginIdentifier = "admin@mtms.local";

        String token = jwtTokenService.generateAccessToken(userId, loginIdentifier);
        assertThat(token).isNotBlank();

        Claims claims = jwtTokenService.parseAndValidateToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("loginIdentifier", String.class)).isEqualTo(loginIdentifier);
        assertThat(claims.get("tokenType", String.class)).isEqualTo("ACCESS");
        assertThat(jwtTokenService.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("Should generate and validate refresh token")
    void testGenerateAndValidateRefreshToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenService.generateRefreshToken(userId);
        assertThat(token).isNotBlank();

        Claims claims = jwtTokenService.parseAndValidateToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("tokenType", String.class)).isEqualTo("REFRESH");
        assertThat(jwtTokenService.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token is malformed")
    void testInvalidTokenThrows() {
        assertThatThrownBy(() -> jwtTokenService.parseAndValidateToken("invalid.token.string"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
