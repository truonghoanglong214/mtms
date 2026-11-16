package com.mtms.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * JPA entity mapping mtms.idempotency_records.
 */
@Entity
@Table(
        name = "idempotency_records",
        schema = "mtms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_actor_idempotency_key", columnNames = {"actor_user_id", "idempotency_key"})
        }
)
public class IdempotencyRecordJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "idempotency_key", length = 160, nullable = false)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private IdempotencyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_headers")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IdempotencyRecordJpaEntity() {
    }

    public IdempotencyRecordJpaEntity(UUID id,
                                      UUID actorUserId,
                                      String idempotencyKey,
                                      String requestHash,
                                      IdempotencyStatus status,
                                      Integer responseStatus,
                                      String responseHeaders,
                                      String responseBody,
                                      Instant expiresAt,
                                      Instant createdAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = status;
        this.responseStatus = responseStatus;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static IdempotencyRecordJpaEntity fromDomain(IdempotencyRecord domain) {
        return new IdempotencyRecordJpaEntity(
                domain.getId(),
                domain.getActorUserId(),
                domain.getIdempotencyKey(),
                domain.getRequestHash(),
                domain.getStatus(),
                domain.getResponseStatus(),
                domain.getResponseHeaders(),
                domain.getResponseBody(),
                domain.getExpiresAt(),
                domain.getCreatedAt()
        );
    }

    public IdempotencyRecord toDomain() {
        return new IdempotencyRecord(
                id,
                actorUserId,
                idempotencyKey,
                requestHash,
                status,
                responseStatus,
                responseHeaders,
                responseBody,
                expiresAt,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyStatus status) {
        this.status = status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
