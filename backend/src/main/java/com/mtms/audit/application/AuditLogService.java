package com.mtms.audit.application;

import com.mtms.audit.application.dto.AuditEventDto;
import com.mtms.audit.application.dto.RecordAuditEventCommand;
import com.mtms.audit.domain.AuditEvent;
import com.mtms.audit.domain.AuditEventRepository;
import com.mtms.identity.infrastructure.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * P0-SEC-02 / BR-SEC-03 / TC-041
 * Application service for recording and retrieving immutable audit events.
 */
@Service
@Transactional
public class AuditLogService {

    private final AuditEventRepository auditEventRepository;

    public AuditLogService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Appends an immutable audit event to the activity log.
     */
    public AuditEventDto recordEvent(RecordAuditEventCommand cmd) {
        Objects.requireNonNull(cmd, "RecordAuditEventCommand must not be null");
        Objects.requireNonNull(cmd.targetType(), "targetType must not be null");
        Objects.requireNonNull(cmd.targetId(), "targetId must not be null");
        Objects.requireNonNull(cmd.eventName(), "eventName must not be null");

        UUID actorId = cmd.actorUserId() != null ? cmd.actorUserId() : resolveCurrentUserId();
        UUID correlationId = cmd.correlationId() != null ? cmd.correlationId() : UUID.randomUUID();

        AuditEvent event = AuditEvent.create(
                cmd.branchId(),
                actorId,
                correlationId,
                cmd.targetType(),
                cmd.targetId(),
                cmd.eventName(),
                cmd.actionReason(),
                cmd.oldSnapshot(),
                cmd.newSnapshot()
        );

        AuditEvent saved = auditEventRepository.save(event);
        return AuditEventDto.fromDomain(saved);
    }

    /**
     * Retrieves paginated audit history for a specific branch.
     */
    @Transactional(readOnly = true)
    public Page<AuditEventDto> listEventsByBranch(UUID branchId, Pageable pageable) {
        Objects.requireNonNull(branchId, "Branch ID must not be null");
        Pageable resolvedPageable = pageable != null ? pageable : Pageable.unpaged();
        return auditEventRepository.findByBranchId(branchId, resolvedPageable)
                .map(AuditEventDto::fromDomain);
    }

    /**
     * Retrieves paginated global system-wide audit history.
     */
    @Transactional(readOnly = true)
    public Page<AuditEventDto> listGlobalEvents(Pageable pageable) {
        Pageable resolvedPageable = pageable != null ? pageable : Pageable.unpaged();
        return auditEventRepository.findAll(resolvedPageable)
                .map(AuditEventDto::fromDomain);
    }

    /**
     * Retrieves audit history for a specific business entity/target.
     */
    @Transactional(readOnly = true)
    public List<AuditEventDto> findByTarget(String targetType, UUID targetId) {
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(targetId, "targetId must not be null");
        return auditEventRepository.findByTarget(targetType, targetId).stream()
                .map(AuditEventDto::fromDomain)
                .toList();
    }

    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}
