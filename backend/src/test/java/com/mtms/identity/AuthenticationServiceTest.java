package com.mtms.identity;

import com.mtms.identity.application.AuthenticationService;
import com.mtms.identity.application.dto.AuthResponse;
import com.mtms.identity.application.dto.LoginCommand;
import com.mtms.identity.application.dto.RefreshTokenCommand;
import com.mtms.identity.application.dto.TokenRefreshResponse;
import com.mtms.identity.application.exception.AccountSuspendedException;
import com.mtms.identity.application.exception.AuthenticationFailedException;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.identity.infrastructure.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository,
                passwordEncoder,
                jwtTokenService,
                tokenBlacklistService
        );
    }

    @Test
    @DisplayName("Should successfully login active user with valid credentials")
    void testLoginSuccess() {
        UUID userId = UUID.randomUUID();
        String loginId = "admin@mtms.local";
        String rawPassword = "password123";
        String encodedHash = "$2a$12$somehash";

        UserJpaEntity entity = new UserJpaEntity(
                userId,
                loginId,
                encodedHash,
                "Admin User",
                UserStatus.ACTIVE,
                Instant.now()
        );

        when(userRepository.findByLoginIdentifier(loginId)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches(rawPassword, encodedHash)).thenReturn(true);
        when(jwtTokenService.generateAccessToken(userId, loginId)).thenReturn("mock.access.token");
        when(jwtTokenService.generateRefreshToken(userId)).thenReturn("mock.refresh.token");

        LoginCommand cmd = new LoginCommand(loginId, rawPassword);
        AuthResponse response = authenticationService.login(cmd);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock.access.token");
        assertThat(response.refreshToken()).isEqualTo("mock.refresh.token");
        assertThat(response.user().loginIdentifier()).isEqualTo(loginId);
    }

    @Test
    @DisplayName("Should reject login when user not found")
    void testLoginUserNotFoundThrows() {
        when(userRepository.findByLoginIdentifier("unknown@mtms.local")).thenReturn(Optional.empty());

        LoginCommand cmd = new LoginCommand("unknown@mtms.local", "password");
        assertThatThrownBy(() -> authenticationService.login(cmd))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("Should reject login when password is incorrect")
    void testLoginWrongPasswordThrows() {
        String loginId = "admin@mtms.local";
        UserJpaEntity entity = new UserJpaEntity(
                UUID.randomUUID(),
                loginId,
                "$2a$12$encoded",
                "Admin",
                UserStatus.ACTIVE,
                Instant.now()
        );

        when(userRepository.findByLoginIdentifier(loginId)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("wrongpass", "$2a$12$encoded")).thenReturn(false);

        LoginCommand cmd = new LoginCommand(loginId, "wrongpass");
        assertThatThrownBy(() -> authenticationService.login(cmd))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("Should reject login when user account is SUSPENDED or DISABLED")
    void testLoginSuspendedUserThrows() {
        String loginId = "suspended@mtms.local";
        UserJpaEntity entity = new UserJpaEntity(
                UUID.randomUUID(),
                loginId,
                "$2a$12$encoded",
                "Suspended User",
                UserStatus.SUSPENDED,
                Instant.now()
        );

        when(userRepository.findByLoginIdentifier(loginId)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("pass", "$2a$12$encoded")).thenReturn(true);

        LoginCommand cmd = new LoginCommand(loginId, "pass");
        assertThatThrownBy(() -> authenticationService.login(cmd))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessageContaining("SUSPENDED");
    }

    @Test
    @DisplayName("Should refresh access token when refresh token is valid")
    void testRefreshTokenSuccess() {
        UUID userId = UUID.randomUUID();
        String refreshToken = "valid.refresh.token";

        when(tokenBlacklistService.isRevoked(refreshToken)).thenReturn(false);
        when(jwtTokenService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenService.getUserIdFromToken(refreshToken)).thenReturn(userId);

        UserJpaEntity entity = new UserJpaEntity(
                userId,
                "user@mtms.local",
                "$2a$12$encoded",
                "User",
                UserStatus.ACTIVE,
                Instant.now()
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(jwtTokenService.generateAccessToken(userId, "user@mtms.local")).thenReturn("new.access.token");

        TokenRefreshResponse response = authenticationService.refresh(new RefreshTokenCommand(refreshToken));
        assertThat(response.accessToken()).isEqualTo("new.access.token");
    }

    @Test
    @DisplayName("Should reject refresh token when token has been revoked in blacklist")
    void testRefreshTokenRevokedThrows() {
        String refreshToken = "revoked.refresh.token";
        when(tokenBlacklistService.isRevoked(refreshToken)).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.refresh(new RefreshTokenCommand(refreshToken)))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("revoked");
    }
}
