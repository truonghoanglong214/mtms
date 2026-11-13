package com.mtms.organization.application;

import com.mtms.organization.application.dto.BranchDto;
import com.mtms.organization.application.dto.BusinessEntityDto;
import com.mtms.organization.application.dto.CreateBranchCommand;
import com.mtms.organization.application.dto.CreateBusinessEntityCommand;
import com.mtms.organization.application.exception.DuplicateBranchCodeException;
import com.mtms.organization.application.exception.DuplicateEntityCodeException;
import com.mtms.organization.application.exception.EntityNotFoundException;
import com.mtms.organization.domain.Branch;
import com.mtms.organization.domain.BusinessEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class OrganizationService {

    private final BusinessEntityJpaRepository entityRepository;
    private final BranchJpaRepository branchRepository;

    public OrganizationService(BusinessEntityJpaRepository entityRepository,
                               BranchJpaRepository branchRepository) {
        this.entityRepository = entityRepository;
        this.branchRepository = branchRepository;
    }

    public BusinessEntityDto createBusinessEntity(CreateBusinessEntityCommand cmd) {
        Objects.requireNonNull(cmd, "CreateBusinessEntityCommand must not be null");

        String normalizedCode = cmd.entityCode().trim().toUpperCase();
        if (entityRepository.existsByEntityCode(normalizedCode)) {
            throw new DuplicateEntityCodeException(normalizedCode);
        }

        // Domain model validation
        BusinessEntity domain = new BusinessEntity(
                UUID.randomUUID(),
                normalizedCode,
                cmd.registeredName(),
                cmd.registrationRef(),
                cmd.status(),
                Instant.now()
        );

        BusinessEntityJpaEntity jpaEntity = new BusinessEntityJpaEntity(
                domain.getId(),
                domain.getEntityCode(),
                domain.getRegisteredName(),
                domain.getRegistrationRef(),
                domain.getStatus(),
                domain.getCreatedAt()
        );

        BusinessEntityJpaEntity saved = entityRepository.save(jpaEntity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public BusinessEntityDto getBusinessEntityById(UUID id) {
        return entityRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("BusinessEntity", id));
    }

    @Transactional(readOnly = true)
    public List<BusinessEntityDto> getAllBusinessEntities() {
        return entityRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public BranchDto createBranch(CreateBranchCommand cmd) {
        Objects.requireNonNull(cmd, "CreateBranchCommand must not be null");

        if (!entityRepository.existsById(cmd.businessEntityId())) {
            throw new EntityNotFoundException("BusinessEntity", cmd.businessEntityId());
        }

        String normalizedBranchCode = cmd.branchCode().trim().toUpperCase();
        if (branchRepository.existsByBusinessEntityIdAndBranchCode(cmd.businessEntityId(), normalizedBranchCode)) {
            throw new DuplicateBranchCodeException(cmd.businessEntityId(), normalizedBranchCode);
        }

        // Domain model validation
        Branch domain = new Branch(
                UUID.randomUUID(),
                cmd.businessEntityId(),
                normalizedBranchCode,
                cmd.name(),
                cmd.timeZone(),
                cmd.status(),
                Instant.now()
        );

        BranchJpaEntity jpaEntity = new BranchJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchCode(),
                domain.getName(),
                domain.getTimeZone(),
                domain.getStatus(),
                domain.getCreatedAt()
        );

        BranchJpaEntity saved = branchRepository.save(jpaEntity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchById(UUID businessEntityId, UUID branchId) {
        return branchRepository.findByBusinessEntityIdAndId(businessEntityId, branchId)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Branch", branchId));
    }

    @Transactional(readOnly = true)
    public List<BranchDto> getBranchesByEntity(UUID businessEntityId) {
        if (!entityRepository.existsById(businessEntityId)) {
            throw new EntityNotFoundException("BusinessEntity", businessEntityId);
        }
        return branchRepository.findByBusinessEntityId(businessEntityId).stream()
                .map(this::toDto)
                .toList();
    }

    private BusinessEntityDto toDto(BusinessEntityJpaEntity entity) {
        return new BusinessEntityDto(
                entity.getId(),
                entity.getEntityCode(),
                entity.getRegisteredName(),
                entity.getRegistrationRef(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    private BranchDto toDto(BranchJpaEntity branch) {
        return new BranchDto(
                branch.getId(),
                branch.getBusinessEntityId(),
                branch.getBranchCode(),
                branch.getName(),
                branch.getTimeZone(),
                branch.getStatus(),
                branch.getCreatedAt()
        );
    }
}
