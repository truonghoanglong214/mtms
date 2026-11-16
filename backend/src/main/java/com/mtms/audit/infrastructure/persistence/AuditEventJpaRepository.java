package com.mtms.audit.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventJpaRepository extends JpaRepository<AuditEventJpaEntity, UUID> {

    Page<AuditEventJpaEntity> findByBranchIdOrderByOccurredAtDesc(UUID branchId, Pageable pageable);

    Page<AuditEventJpaEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);

    List<AuditEventJpaEntity> findByTargetTypeAndTargetIdOrderByOccurredAtDesc(String targetType, UUID targetId);
}
