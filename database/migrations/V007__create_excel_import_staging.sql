-- P0-SEC-04 / BR-SEC-02 / TC-045
-- Forward-only migration for Excel and CSV import staging batches and rows.

CREATE TABLE mtms.import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL,
    import_kind VARCHAR(48) NOT NULL,
    source_filename VARCHAR(255) NOT NULL,
    source_sha256 CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PREVIEW',
    total_rows INT NOT NULL DEFAULT 0,
    valid_rows INT NOT NULL DEFAULT 0,
    error_rows INT NOT NULL DEFAULT 0,
    preview_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    committed_by UUID,
    committed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_import_batches_branch
        FOREIGN KEY (branch_id)
        REFERENCES mtms.branches (id),
    CONSTRAINT fk_import_batches_committed_by
        FOREIGN KEY (committed_by)
        REFERENCES mtms.users (id),
    CONSTRAINT ck_import_batches_status
        CHECK (status IN ('PREVIEW', 'VALIDATED', 'COMMITTED', 'REJECTED')),
    CONSTRAINT uq_import_batches_kind_branch_sha
        UNIQUE (import_kind, branch_id, source_sha256)
);

CREATE INDEX idx_import_batches_branch_id
    ON mtms.import_batches (branch_id);

CREATE INDEX idx_import_batches_created_at_desc
    ON mtms.import_batches (created_at DESC);

CREATE TABLE mtms.import_rows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_batch_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    raw_data JSONB NOT NULL,
    normalized_data JSONB,
    errors JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_import_rows_batch
        FOREIGN KEY (import_batch_id)
        REFERENCES mtms.import_batches (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_import_rows_row_number
        CHECK (row_number > 0),
    CONSTRAINT ck_import_rows_status
        CHECK (status IN ('PENDING', 'VALID', 'ERROR')),
    CONSTRAINT uq_import_rows_batch_row
        UNIQUE (import_batch_id, row_number)
);

CREATE INDEX idx_import_rows_batch_status
    ON mtms.import_rows (import_batch_id, status);
