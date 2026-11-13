package com.mtms.identity.application;

import com.mtms.identity.application.dto.AuthResponse;
import com.mtms.identity.application.dto.LoginCommand;
import com.mtms.identity.application.dto.RefreshTokenCommand;
import com.mtms.identity.application.dto.TokenRefreshResponse;
import com.mtms.identity.application.dto.UserDto;
import com.mtms.identity.application.exception.AccountSuspendedException;
import com.mtms.identity.application.exception.AuthenticationFailedException;
import com.mtms.identity.domain.User;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.identity.infrastructure.security.JwtTokenService;
import com.mtms.identity.infrastructure.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class AuthenticationService {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthenticationService(UserJpaRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenService jwtTokenService,
                                 TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public AuthResponse login(LoginCommand cmd) {
        Objects.requireNonNull(cmd, "LoginCommand must not be null");

        String normalizedIdentifier = cmd.loginIdentifier().trim().toLowerCase();
        UserJpaEntity entity = userRepository.findByLoginIdentifier(normalizedIdentifier)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid login identifier or password"));

        User user = new User(
                entity.getId(),
                entity.getLoginIdentifier(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                entity.getStatus(),
                entity.getCreatedAt()
        );

        if (!passwordEncoder.matches(cmd.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid login identifier or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountSuspendedException("User account is " + user.getStatus() + ". Login not permitted.");
        }

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getLoginIdentifier());
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        UserDto userDto = new UserDto(
                user.getId(),
                user.getLoginIdentifier(),
                user.getDisplayName(),
                user.getStatus(),
                user.getCreatedAt()
        );

        return AuthResponse.of(accessToken, refreshToken, 3600, userDto);
    }

    public TokenRefreshResponse refresh(RefreshTokenCommand cmd) {
        Objects.requireNonNull(cmd, "RefreshTokenCommand must not be null");

        String token = cmd.refreshToken().trim();
        if (tokenBlacklistService.isRevoked(token)) {
            throw new AuthenticationFailedException("Refresh token has been revoked");
        }

        if (!jwtTokenService.isRefreshToken(token)) {
            throw new AuthenticationFailedException("Provided token is not a refresh token");
        }

        UUID userId = jwtTokenService.getUserIdFromToken(token);
        UserJpaEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        if (entity.getStatus() != UserStatus.ACTIVE) {
            throw new AccountSuspendedException("User account is " + entity.getStatus());
        }

        String newAccessToken = jwtTokenService.generateAccessToken(entity.getId(), entity.getLoginIdentifier());
        return TokenRefreshResponse.of(newAccessToken, 3600);
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return;
        }

        try {
            Claims claims = jwtTokenService.parseAndValidateToken(token);
            tokenBlacklistService.revokeToken(token, claims.getExpiration().toInstant());
        } catch (Exception ex) {
            // Already invalid/expired token, blacklist for safety
            tokenBlacklistService.revokeToken(token, null);
        }
    }

    @Transactional(readOnly = true)
    public UserDto getUserProfile(UUID userId) {
        return userRepository.findById(userId)
                .map(entity -> new UserDto(
                        entity.getId(),
                        entity.getLoginIdentifier(),
                        entity.getDisplayName(),
                        entity.getStatus(),
                        entity.getCreatedAt()
                ))
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));
    }
}
