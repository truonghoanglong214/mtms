-- P0-SEC-03 / BR-SEC-03 / TC-011
-- Forward-only migration for idempotency records table.
-- Deduplication store guarding mutating requests against duplicate execution.

CREATE TABLE mtms.idempotency_records (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    response_status INTEGER,
    response_headers JSONB,
    response_body TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_idempotency_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES mtms.users (id),
    CONSTRAINT uq_actor_idempotency_key
        UNIQUE (actor_user_id, idempotency_key)
);

CREATE INDEX idx_idempotency_expires_at
    ON mtms.idempotency_records (expires_at);
