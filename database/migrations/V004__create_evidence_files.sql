-- P0-SEC-01 / BR-SEC-01 / TC-043
-- Forward-only migration for private evidence storage and branch boundary metadata.
-- IDs are supplied by the application; no database UUID extension is required.

CREATE TABLE mtms.evidence_files (
    id UUID PRIMARY KEY,
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    storage_object_key TEXT NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size > 0),
    checksum_sha256 CHAR(64) NOT NULL,
    visibility_class VARCHAR(32) NOT NULL DEFAULT 'BRANCH_PRIVATE',
    retention_until DATE,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_evidence_files_business_entity
        FOREIGN KEY (business_entity_id)
        REFERENCES mtms.business_entities (id),
    CONSTRAINT fk_evidence_files_branch
        FOREIGN KEY (branch_id)
        REFERENCES mtms.branches (id),
    CONSTRAINT fk_evidence_files_uploaded_by
        FOREIGN KEY (uploaded_by)
        REFERENCES mtms.users (id),
    CONSTRAINT ck_evidence_files_visibility
        CHECK (visibility_class IN ('BRANCH_PRIVATE', 'ENTITY_PRIVATE', 'GLOBAL_ADMIN_ONLY'))
);

CREATE INDEX idx_evidence_scope
    ON mtms.evidence_files (branch_id, created_at);

CREATE INDEX idx_evidence_files_entity
    ON mtms.evidence_files (business_entity_id);
