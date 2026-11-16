package com.mtms.identity.application;

import com.mtms.audit.application.AuditLogService;
import com.mtms.audit.application.dto.RecordAuditEventCommand;
import com.mtms.common.error.ResourceConflictException;
import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.identity.application.dto.CreateUserCommand;
import com.mtms.identity.application.dto.ScopedRoleGrantDto;
import com.mtms.identity.application.dto.UpdateUserStatusCommand;
import com.mtms.identity.application.dto.UserDto;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-ORG-06 / BR-ORG-01
 * Application service for user onboarding and administrative user management.
 */
@Service
@Transactional
public class UserService {

    private final UserJpaRepository userRepository;
    private final ScopedRoleGrantJpaRepository grantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserJpaRepository userRepository,
                       ScopedRoleGrantJpaRepository grantRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.grantRepository = grantRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public UserDto createUser(CreateUserCommand cmd) {
        Objects.requireNonNull(cmd, "CreateUserCommand must not be null");

        String normalizedIdentifier = cmd.loginIdentifier().trim().toLowerCase();
        if (userRepository.existsByLoginIdentifier(normalizedIdentifier)) {
            throw new ResourceConflictException("User with login identifier '" + normalizedIdentifier + "' already exists");
        }

        String passwordHash = passwordEncoder.encode(cmd.password());
        UserJpaEntity entity = new UserJpaEntity(
                UUID.randomUUID(),
                normalizedIdentifier,
                passwordHash,
                cmd.displayName().trim(),
                UserStatus.ACTIVE,
                Instant.now()
        );

        UserJpaEntity saved = userRepository.save(entity);
        return new UserDto(
                saved.getId(),
                saved.getLoginIdentifier(),
                saved.getDisplayName(),
                saved.getStatus(),
                saved.getCreatedAt(),
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        LocalDate today = LocalDate.now();
        return userRepository.findAll().stream()
                .map(entity -> {
                    List<ScopedRoleGrantDto> grants = grantRepository.findActiveGrantsByUserId(entity.getId(), today)
                            .stream()
                            .map(ScopedRoleGrantJpaEntity::toDomain)
                            .map(ScopedRoleGrantDto::fromDomain)
                            .toList();
                    return new UserDto(
                            entity.getId(),
                            entity.getLoginIdentifier(),
                            entity.getDisplayName(),
                            entity.getStatus(),
                            entity.getCreatedAt(),
                            grants
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        LocalDate today = LocalDate.now();
        UserJpaEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<ScopedRoleGrantDto> grants = grantRepository.findActiveGrantsByUserId(entity.getId(), today)
                .stream()
                .map(ScopedRoleGrantJpaEntity::toDomain)
                .map(ScopedRoleGrantDto::fromDomain)
                .toList();

        return new UserDto(
                entity.getId(),
                entity.getLoginIdentifier(),
                entity.getDisplayName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                grants
        );
    }

    public UserDto updateUserStatus(UUID userId, UpdateUserStatusCommand cmd) {
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(cmd, "UpdateUserStatusCommand must not be null");

        UserJpaEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserStatus oldStatus = entity.getStatus();
        entity.setStatus(cmd.status());
        UserJpaEntity updated = userRepository.save(entity);

        String oldSnapshot = "{\"status\":\"" + oldStatus.name() + "\"}";
        String newSnapshot = "{\"status\":\"" + updated.getStatus().name() + "\"}";

        auditLogService.recordEvent(new RecordAuditEventCommand(
                null,
                null,
                UUID.randomUUID(),
                "USER",
                userId,
                "USER_STATUS_CHANGED",
                "User status changed from " + oldStatus + " to " + updated.getStatus(),
                oldSnapshot,
                newSnapshot
        ));

        List<ScopedRoleGrantDto> grants = grantRepository.findActiveGrantsByUserId(userId, LocalDate.now())
                .stream()
                .map(ScopedRoleGrantJpaEntity::toDomain)
                .map(ScopedRoleGrantDto::fromDomain)
                .toList();

        return new UserDto(
                updated.getId(),
                updated.getLoginIdentifier(),
                updated.getDisplayName(),
                updated.getStatus(),
                updated.getCreatedAt(),
                grants
        );
    }
}
