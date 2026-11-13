package com.mtms.organization;

import com.mtms.organization.application.OrganizationService;
import com.mtms.organization.application.dto.BranchDto;
import com.mtms.organization.application.dto.BusinessEntityDto;
import com.mtms.organization.application.dto.CreateBranchCommand;
import com.mtms.organization.application.dto.CreateBusinessEntityCommand;
import com.mtms.organization.application.exception.DuplicateBranchCodeException;
import com.mtms.organization.application.exception.DuplicateEntityCodeException;
import com.mtms.organization.application.exception.EntityNotFoundException;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private BusinessEntityJpaRepository entityRepository;

    @Mock
    private BranchJpaRepository branchRepository;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(entityRepository, branchRepository);
    }

    @Test
    @DisplayName("Should create business entity successfully")
    void testCreateBusinessEntitySuccess() {
        CreateBusinessEntityCommand cmd = new CreateBusinessEntityCommand(
                "ENT_01",
                "Cong ty Tra Sua Sai Gon",
                "DKKD-12345",
                BusinessEntityStatus.ACTIVE
        );

        when(entityRepository.existsByEntityCode("ENT_01")).thenReturn(false);
        when(entityRepository.save(any(BusinessEntityJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessEntityDto dto = organizationService.createBusinessEntity(cmd);

        assertThat(dto.id()).isNotNull();
        assertThat(dto.entityCode()).isEqualTo("ENT_01");
        assertThat(dto.registeredName()).isEqualTo("Cong ty Tra Sua Sai Gon");
        assertThat(dto.status()).isEqualTo(BusinessEntityStatus.ACTIVE);
        verify(entityRepository).save(any(BusinessEntityJpaEntity.class));
    }

    @Test
    @DisplayName("Should reject duplicate business entity code")
    void testCreateBusinessEntityDuplicateCodeThrows() {
        CreateBusinessEntityCommand cmd = new CreateBusinessEntityCommand(
                "ENT_01",
                "Cong ty Tra Sua Sai Gon",
                null,
                BusinessEntityStatus.ACTIVE
        );

        when(entityRepository.existsByEntityCode("ENT_01")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.createBusinessEntity(cmd))
                .isInstanceOf(DuplicateEntityCodeException.class)
                .hasMessageContaining("ENT_01");
    }

    @Test
    @DisplayName("Should create branch successfully under existing entity")
    void testCreateBranchSuccess() {
        UUID entityId = UUID.randomUUID();
        CreateBranchCommand cmd = new CreateBranchCommand(
                entityId,
                "BR_Q1",
                "Chi nhanh Quan 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE
        );

        when(entityRepository.existsById(entityId)).thenReturn(true);
        when(branchRepository.existsByBusinessEntityIdAndBranchCode(entityId, "BR_Q1")).thenReturn(false);
        when(branchRepository.save(any(BranchJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BranchDto dto = organizationService.createBranch(cmd);

        assertThat(dto.id()).isNotNull();
        assertThat(dto.businessEntityId()).isEqualTo(entityId);
        assertThat(dto.branchCode()).isEqualTo("BR_Q1");
        assertThat(dto.name()).isEqualTo("Chi nhanh Quan 1");
        assertThat(dto.status()).isEqualTo(BranchStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should reject branch creation under non-existent business entity")
    void testCreateBranchNonexistentEntityThrows() {
        UUID missingEntityId = UUID.randomUUID();
        CreateBranchCommand cmd = new CreateBranchCommand(
                missingEntityId,
                "BR_Q1",
                "Chi nhanh Quan 1",
                null,
                BranchStatus.ACTIVE
        );

        when(entityRepository.existsById(missingEntityId)).thenReturn(false);

        assertThatThrownBy(() -> organizationService.createBranch(cmd))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("BusinessEntity");
    }

    @Test
    @DisplayName("Should reject duplicate branch code under the same business entity")
    void testCreateBranchDuplicateCodeThrows() {
        UUID entityId = UUID.randomUUID();
        CreateBranchCommand cmd = new CreateBranchCommand(
                entityId,
                "BR_Q1",
                "Chi nhanh Quan 1",
                null,
                BranchStatus.ACTIVE
        );

        when(entityRepository.existsById(entityId)).thenReturn(true);
        when(branchRepository.existsByBusinessEntityIdAndBranchCode(entityId, "BR_Q1")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.createBranch(cmd))
                .isInstanceOf(DuplicateBranchCodeException.class)
                .hasMessageContaining("BR_Q1");
    }

    @Test
    @DisplayName("Should get branches by entity successfully")
    void testGetBranchesByEntity() {
        UUID entityId = UUID.randomUUID();
        BranchJpaEntity branchEntity = new BranchJpaEntity(
                UUID.randomUUID(),
                entityId,
                "BR_01",
                "Branch 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );

        when(entityRepository.existsById(entityId)).thenReturn(true);
        when(branchRepository.findByBusinessEntityId(entityId)).thenReturn(List.of(branchEntity));

        List<BranchDto> branches = organizationService.getBranchesByEntity(entityId);
        assertThat(branches).hasSize(1);
        assertThat(branches.get(0).branchCode()).isEqualTo("BR_01");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when branch not found by ID")
    void testGetBranchByIdNotFoundThrows() {
        UUID entityId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        when(branchRepository.findByBusinessEntityIdAndId(entityId, branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getBranchById(entityId, branchId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
