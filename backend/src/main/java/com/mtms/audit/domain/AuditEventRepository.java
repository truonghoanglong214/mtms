package com.mtms.audit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-02 / BR-SEC-03 / TC-041
 * Domain repository contract for audit events.
 */
public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    Optional<AuditEvent> findById(UUID id);

    Page<AuditEvent> findByBranchId(UUID branchId, Pageable pageable);

    Page<AuditEvent> findAll(Pageable pageable);

    List<AuditEvent> findByTarget(String targetType, UUID targetId);
}
