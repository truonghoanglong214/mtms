package com.mtms.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.identity.application.dto.LoginCommand;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.identity.infrastructure.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityProtectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private UserJpaEntity activeUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        activeUser = new UserJpaEntity(
                UUID.randomUUID(),
                "secadmin",
                passwordEncoder.encode("StrongP@ss123"),
                "Security Admin",
                UserStatus.ACTIVE,
                Instant.now()
        );
        userRepository.save(activeUser);
    }

    @Test
    @DisplayName("TC-001: Unauthenticated request to /api/v1/organizations/entities must return 401 Unauthorized")
    void testUnauthenticatedAccessBlockedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/entities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:unauthorized"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Public endpoint /actuator/health is accessible without authentication")
    void testActuatorHealthPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public endpoint /api/v1/auth/login is accessible without authentication")
    void testLoginPermitted() throws Exception {
        LoginCommand command = new LoginCommand("secadmin", "StrongP@ss123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    @DisplayName("Authenticated request with valid Bearer token should access protected endpoint")
    void testAuthenticatedAccessAllowed() throws Exception {
        String token = jwtTokenService.generateAccessToken(activeUser.getId(), activeUser.getLoginIdentifier());

        mockMvc.perform(get("/api/v1/organizations/entities")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Request with revoked token should return 401 Unauthorized")
    void testRevokedTokenBlockedWith401() throws Exception {
        String token = jwtTokenService.generateAccessToken(activeUser.getId(), activeUser.getLoginIdentifier());
        tokenBlacklistService.revokeToken(token, Instant.now().plusSeconds(3600));

        mockMvc.perform(get("/api/v1/organizations/entities")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:unauthorized"));
    }

    @Test
    @DisplayName("Refresh token used as access token should return 401 Unauthorized")
    void testRefreshTokenAsAccessTokenBlocked() throws Exception {
        String refreshToken = jwtTokenService.generateRefreshToken(activeUser.getId());

        mockMvc.perform(get("/api/v1/organizations/entities")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:mtms:error:unauthorized"));
    }
}
