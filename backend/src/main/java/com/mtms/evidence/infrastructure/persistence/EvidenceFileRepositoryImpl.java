package com.mtms.evidence.infrastructure.persistence;

import com.mtms.evidence.domain.EvidenceFile;
import com.mtms.evidence.domain.EvidenceFileRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-01 / BR-SEC-01
 * Persistence adapter bridging domain EvidenceFileRepository to Spring Data JPA.
 */
@Repository
public class EvidenceFileRepositoryImpl implements EvidenceFileRepository {

    private final EvidenceFileJpaRepository jpaRepository;

    public EvidenceFileRepositoryImpl(EvidenceFileJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EvidenceFile save(EvidenceFile file) {
        EvidenceFileJpaEntity entity = toEntity(file);
        EvidenceFileJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<EvidenceFile> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<EvidenceFile> findByBranchId(UUID branchId) {
        return jpaRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    private EvidenceFile toDomain(EvidenceFileJpaEntity entity) {
        return new EvidenceFile(
                entity.getId(),
                entity.getBusinessEntityId(),
                entity.getBranchId(),
                entity.getStorageObjectKey(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getByteSize(),
                entity.getChecksumSha256(),
                entity.getVisibilityClass(),
                entity.getRetentionUntil(),
                entity.getUploadedBy(),
                entity.getCreatedAt()
        );
    }

    private EvidenceFileJpaEntity toEntity(EvidenceFile domain) {
        return new EvidenceFileJpaEntity(
                domain.getId(),
                domain.getBusinessEntityId(),
                domain.getBranchId(),
                domain.getStorageObjectKey(),
                domain.getOriginalFilename(),
                domain.getContentType(),
                domain.getByteSize(),
                domain.getChecksumSha256(),
                domain.getVisibilityClass(),
                domain.getRetentionUntil(),
                domain.getUploadedBy(),
                domain.getCreatedAt()
        );
    }
}
