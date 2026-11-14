package com.mtms.identity;

import com.mtms.common.error.ResourceNotFoundException;
import com.mtms.identity.application.ScopedPermissionService;
import com.mtms.identity.application.dto.GrantRoleCommand;
import com.mtms.identity.application.dto.ScopedRoleGrantDto;
import com.mtms.identity.domain.RoleKey;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScopedPermissionServiceTest {

    @Mock
    private ScopedRoleGrantJpaRepository grantRepository;

    @Mock
    private UserJpaRepository userRepository;

    private ScopedPermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new ScopedPermissionService(grantRepository, userRepository);
    }

    @Test
    @DisplayName("Successfully grant branch role when input is valid and user exists")
    void testGrantRoleSuccess() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        GrantRoleCommand cmd = new GrantRoleCommand(
                entityId,
                branchId,
                userId,
                RoleKey.BRANCH_OWNER,
                "ALL",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(6)
        );

        when(userRepository.existsById(userId)).thenReturn(true);
        when(grantRepository.save(any(ScopedRoleGrantJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScopedRoleGrantDto result = permissionService.grantRole(cmd, actorId);

        assertThat(result).isNotNull();
        assertThat(result.businessEntityId()).isEqualTo(entityId);
        assertThat(result.branchId()).isEqualTo(branchId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.roleKey()).isEqualTo(RoleKey.BRANCH_OWNER);
        assertThat(result.grantedBy()).isEqualTo(actorId);

        ArgumentCaptor<ScopedRoleGrantJpaEntity> captor = ArgumentCaptor.forClass(ScopedRoleGrantJpaEntity.class);
        verify(grantRepository).save(captor.capture());
        assertThat(captor.getValue().getRoleKey()).isEqualTo("BRANCH_OWNER");
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException when granting role to nonexistent user")
    void testGrantRoleUserNotFound() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        GrantRoleCommand cmd = new GrantRoleCommand(
                entityId,
                branchId,
                userId,
                RoleKey.MANAGER,
                "ALL",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(6)
        );

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> permissionService.grantRole(cmd, actorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Throw IllegalArgumentException when validTo is before validFrom")
    void testGrantRoleInvalidDateRange() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        GrantRoleCommand cmd = new GrantRoleCommand(
                entityId,
                branchId,
                userId,
                RoleKey.MANAGER,
                "ALL",
                "ALL",
                "ALL",
                LocalDate.now().plusDays(10),
                LocalDate.now() // before validFrom
        );

        when(userRepository.existsById(userId)).thenReturn(true);

        assertThatThrownBy(() -> permissionService.grantRole(cmd, actorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be before valid from date");
    }

    @Test
    @DisplayName("Throw IllegalArgumentException when branchId is null for non-system admin role")
    void testGrantRoleMissingBranchForNonAdmin() {
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        GrantRoleCommand cmd = new GrantRoleCommand(
                entityId,
                null, // null branchId
                userId,
                RoleKey.BRANCH_OWNER,
                "ALL",
                "ALL",
                "ALL",
                LocalDate.now(),
                LocalDate.now().plusMonths(1)
        );

        when(userRepository.existsById(userId)).thenReturn(true);

        assertThatThrownBy(() -> permissionService.grantRole(cmd, actorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Branch ID is required for non-system admin role");
    }

    @Test
    @DisplayName("Allow null branchId for SYSTEM_ADMIN role")
    void testGrantRoleSystemAdminAllowsNullBranch() {
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        GrantRoleCommand cmd = new GrantRoleCommand(
                entityId,
                null,
                userId,
                RoleKey.SYSTEM_ADMIN,
                "*",
                "ALL",
                "ALL",
                LocalDate.now(),
                null
        );

        when(userRepository.existsById(userId)).thenReturn(true);
        when(grantRepository.save(any(ScopedRoleGrantJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScopedRoleGrantDto result = permissionService.grantRole(cmd, actorId);

        assertThat(result.roleKey()).isEqualTo(RoleKey.SYSTEM_ADMIN);
        assertThat(result.branchId()).isNull();
    }

    @Test
    @DisplayName("Successfully revoke existing grant")
    void testRevokeGrantSuccess() {
        UUID grantId = UUID.randomUUID();
        when(grantRepository.existsById(grantId)).thenReturn(true);

        permissionService.revokeGrant(grantId);

        verify(grantRepository).deleteById(grantId);
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException when revoking nonexistent grant")
    void testRevokeGrantNotFound() {
        UUID grantId = UUID.randomUUID();
        when(grantRepository.existsById(grantId)).thenReturn(false);

        assertThatThrownBy(() -> permissionService.revokeGrant(grantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role grant not found");
    }
}
