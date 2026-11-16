-- P0-SEC-02 / BR-SEC-03 / TC-041
-- Forward-only migration for append-only audit event spine and activity log.
-- IDs are supplied by the application; no database UUID extension is required.

CREATE TABLE mtms.audit_events (
    id UUID PRIMARY KEY,
    branch_id UUID,
    actor_user_id UUID,
    correlation_id UUID,
    target_type VARCHAR(80) NOT NULL,
    target_id UUID NOT NULL,
    event_name VARCHAR(90) NOT NULL,
    action_reason TEXT,
    old_snapshot JSONB,
    new_snapshot JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_events_branch
        FOREIGN KEY (branch_id)
        REFERENCES mtms.branches (id),
    CONSTRAINT fk_audit_events_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES mtms.users (id)
);

CREATE INDEX idx_audit_target
    ON mtms.audit_events (target_type, target_id, occurred_at);

CREATE INDEX idx_audit_branch
    ON mtms.audit_events (branch_id, occurred_at);

CREATE INDEX idx_audit_actor
    ON mtms.audit_events (actor_user_id, occurred_at);
