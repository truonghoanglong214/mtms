package com.mtms.identity.application;

import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.identity.application.dto.GrantRoleCommand;
import com.mtms.identity.application.dto.ScopedRoleGrantDto;
import com.mtms.identity.domain.ScopedRoleGrant;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-ORG-05 / BR-ORG-04
 * Application service for managing branch-scoped role grants.
 */
@Service
@Transactional
public class ScopedPermissionService {

    private final ScopedRoleGrantJpaRepository grantRepository;
    private final UserJpaRepository userRepository;

    public ScopedPermissionService(ScopedRoleGrantJpaRepository grantRepository,
                                   UserJpaRepository userRepository) {
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
    }

    public ScopedRoleGrantDto grantRole(GrantRoleCommand cmd, UUID actorId) {
        Objects.requireNonNull(cmd, "Grant command must not be null");
        Objects.requireNonNull(actorId, "Actor ID must not be null");

        if (!userRepository.existsById(cmd.userId())) {
            throw new ResourceNotFoundException("User not found with id: " + cmd.userId());
        }

        ScopedRoleGrant grant = ScopedRoleGrant.create(
                cmd.businessEntityId(),
                cmd.branchId(),
                cmd.userId(),
                cmd.roleKey(),
                cmd.actionScope(),
                cmd.rowScope(),
                cmd.reportPermissionGroup(),
                cmd.validFrom(),
                cmd.validTo(),
                actorId
        );

        ScopedRoleGrantJpaEntity entity = ScopedRoleGrantJpaEntity.fromDomain(grant);
        ScopedRoleGrantJpaEntity saved = grantRepository.save(entity);

        return ScopedRoleGrantDto.fromDomain(saved.toDomain());
    }

    public void revokeGrant(UUID grantId) {
        Objects.requireNonNull(grantId, "Grant ID must not be null");
        if (!grantRepository.existsById(grantId)) {
            throw new ResourceNotFoundException("Role grant not found with id: " + grantId);
        }
        grantRepository.deleteById(grantId);
    }

    @Transactional(readOnly = true)
    public List<ScopedRoleGrantDto> getUserGrants(UUID userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        return grantRepository.findByUserId(userId).stream()
                .map(ScopedRoleGrantJpaEntity::toDomain)
                .map(ScopedRoleGrantDto::fromDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScopedRoleGrantDto> getActiveGrants(UUID userId) {
        Objects.requireNonNull(userId, "User ID must not be null");
        return grantRepository.findActiveGrantsByUserId(userId, LocalDate.now()).stream()
                .map(ScopedRoleGrantJpaEntity::toDomain)
                .map(ScopedRoleGrantDto::fromDomain)
                .toList();
    }
}
