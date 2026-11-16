package com.mtms.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Core service managing idempotency acquisition, caching, and completion recording.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordJpaRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Attempts to acquire an idempotency key lock.
     * If key is new: records status IN_PROGRESS and returns ACQUIRED.
     * If key exists and COMPLETED: validates requestHash and returns CACHED or throws IdempotencyPayloadMismatchException.
     * If key exists and IN_PROGRESS: throws IdempotencyConflictException (or allows recovery if expired).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AcquireResult tryAcquire(UUID actorUserId, String key, String requestHash, Duration ttl) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(key, "idempotency key must not be null");
        Objects.requireNonNull(requestHash, "requestHash must not be null");
        Duration effectiveTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? ttl : Duration.ofHours(24);

        Optional<IdempotencyRecordJpaEntity> existingOpt =
                repository.findByActorUserIdAndIdempotencyKey(actorUserId, key);

        if (existingOpt.isPresent()) {
            return handleExistingRecord(existingOpt.get(), requestHash, effectiveTtl);
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(effectiveTtl);

        IdempotencyRecordJpaEntity newEntity = new IdempotencyRecordJpaEntity(
                UUID.randomUUID(),
                actorUserId,
                key,
                requestHash,
                IdempotencyStatus.IN_PROGRESS,
                null,
                null,
                null,
                expiresAt,
                now
        );

        try {
            IdempotencyRecordJpaEntity saved = repository.saveAndFlush(newEntity);
            log.debug("Acquired new idempotency key '{}' for actor '{}'", key, actorUserId);
            return AcquireResult.acquired(saved.toDomain());
        } catch (DataIntegrityViolationException dive) {
            // Concurrent race condition: lookup winner and evaluate
            log.info("Concurrent insert conflict on key '{}' for actor '{}'. Falling back to existing record.", key, actorUserId);
            IdempotencyRecordJpaEntity concurrentRecord = repository
                    .findByActorUserIdAndIdempotencyKey(actorUserId, key)
                    .orElseThrow(() -> dive);
            return handleExistingRecord(concurrentRecord, requestHash, effectiveTtl);
        }
    }

    private AcquireResult handleExistingRecord(IdempotencyRecordJpaEntity existing, String requestHash, Duration ttl) {
        Instant now = Instant.now();

        // 1. If record is COMPLETED
        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            if (Objects.equals(existing.getRequestHash(), requestHash)) {
                log.debug("Replaying cached idempotent response for key '{}'", existing.getIdempotencyKey());
                return AcquireResult.cached(existing.toDomain());
            } else {
                throw new IdempotencyPayloadMismatchException(
                        "Payload mismatch for idempotency key '" + existing.getIdempotencyKey() +
                                "'. The request body does not match the original request."
                );
            }
        }

        // 2. If record is IN_PROGRESS
        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            if (existing.getExpiresAt().isBefore(now)) {
                // Lock expired: allow takeover
                log.warn("Idempotency key '{}' expired while IN_PROGRESS. Allowing acquisition takeover.", existing.getIdempotencyKey());
                existing.setRequestHash(requestHash);
                existing.setExpiresAt(now.plus(ttl));
                IdempotencyRecordJpaEntity updated = repository.saveAndFlush(existing);
                return AcquireResult.acquired(updated.toDomain());
            }
            throw new IdempotencyConflictException(
                    "A request with idempotency key '" + existing.getIdempotencyKey() +
                            "' is currently in progress. Please wait for completion before retrying."
            );
        }

        // 3. If record is FAILED
        if (existing.getStatus() == IdempotencyStatus.FAILED) {
            if (!Objects.equals(existing.getRequestHash(), requestHash)) {
                throw new IdempotencyPayloadMismatchException(
                        "Payload mismatch for idempotency key '" + existing.getIdempotencyKey() +
                                "'. The request body does not match the original request."
                );
            }
            // Allow retry of previously failed invocation
            log.info("Idempotency key '{}' previously FAILED. Allowing retry attempt.", existing.getIdempotencyKey());
            existing.setStatus(IdempotencyStatus.IN_PROGRESS);
            existing.setResponseStatus(null);
            existing.setResponseHeaders(null);
            existing.setResponseBody(null);
            existing.setExpiresAt(now.plus(ttl));
            IdempotencyRecordJpaEntity updated = repository.saveAndFlush(existing);
            return AcquireResult.acquired(updated.toDomain());
        }

        throw new IllegalStateException("Unexpected idempotency status: " + existing.getStatus());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID actorUserId, String key, int statusCode, String responseBody) {
        recordSuccess(actorUserId, key, statusCode, null, responseBody);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID actorUserId, String key, int statusCode, Map<String, String> headers, String responseBody) {
        repository.findByActorUserIdAndIdempotencyKey(actorUserId, key).ifPresent(entity -> {
            entity.setStatus(IdempotencyStatus.COMPLETED);
            entity.setResponseStatus(statusCode);
            entity.setResponseBody(responseBody);
            if (headers != null && !headers.isEmpty()) {
                entity.setResponseHeaders(serializeHeaders(headers));
            }
            repository.saveAndFlush(entity);
            log.debug("Recorded successful idempotency execution for key '{}', status={}", key, statusCode);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID actorUserId, String key, int statusCode, String responseBody) {
        repository.findByActorUserIdAndIdempotencyKey(actorUserId, key).ifPresent(entity -> {
            entity.setStatus(IdempotencyStatus.FAILED);
            entity.setResponseStatus(statusCode);
            entity.setResponseBody(responseBody);
            repository.saveAndFlush(entity);
            log.debug("Recorded failed idempotency execution for key '{}', status={}", key, statusCode);
        });
    }

    private String serializeHeaders(Map<String, String> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize response headers to JSON: {}", e.getMessage());
            return null;
        }
    }
}
