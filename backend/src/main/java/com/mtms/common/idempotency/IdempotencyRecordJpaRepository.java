package com.mtms.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Repository for idempotency records persistence.
 */
@Repository
public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecordJpaEntity, UUID> {

    Optional<IdempotencyRecordJpaEntity> findByActorUserIdAndIdempotencyKey(UUID actorUserId, String idempotencyKey);

    void deleteByExpiresAtBefore(Instant now);
}
