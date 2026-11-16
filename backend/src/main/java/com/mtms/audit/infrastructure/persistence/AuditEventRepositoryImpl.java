package com.mtms.audit.infrastructure.persistence;

import com.mtms.audit.domain.AuditEvent;
import com.mtms.audit.domain.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AuditEventRepositoryImpl implements AuditEventRepository {

    private final AuditEventJpaRepository jpaRepository;

    public AuditEventRepositoryImpl(AuditEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        Objects.requireNonNull(event, "AuditEvent must not be null");
        AuditEventJpaEntity entity = AuditEventJpaEntity.fromDomain(event);
        AuditEventJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<AuditEvent> findById(UUID id) {
        Objects.requireNonNull(id, "ID must not be null");
        return jpaRepository.findById(id).map(AuditEventJpaEntity::toDomain);
    }

    @Override
    public Page<AuditEvent> findByBranchId(UUID branchId, Pageable pageable) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        return jpaRepository.findByBranchIdOrderByOccurredAtDesc(branchId, pageable)
                .map(AuditEventJpaEntity::toDomain);
    }

    @Override
    public Page<AuditEvent> findAll(Pageable pageable) {
        return jpaRepository.findAllByOrderByOccurredAtDesc(pageable)
                .map(AuditEventJpaEntity::toDomain);
    }

    @Override
    public List<AuditEvent> findByTarget(String targetType, UUID targetId) {
        Objects.requireNonNull(targetType, "Target type must not be null");
        Objects.requireNonNull(targetId, "Target ID must not be null");
        return jpaRepository.findByTargetTypeAndTargetIdOrderByOccurredAtDesc(targetType, targetId).stream()
                .map(AuditEventJpaEntity::toDomain)
                .toList();
    }
}
