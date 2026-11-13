-- P0-ORG-02 / ORG02-DB-01
-- Forward-only organization and identity baseline.
-- IDs are supplied by the application; no database UUID extension is required.

CREATE SCHEMA IF NOT EXISTS mtms;

CREATE TABLE mtms.business_entities (
    id UUID PRIMARY KEY,
    entity_code VARCHAR(32) NOT NULL,
    registered_name VARCHAR(200) NOT NULL,
    registration_ref VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_business_entities_entity_code UNIQUE (entity_code),
    CONSTRAINT ck_business_entities_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE mtms.branches (
    id UUID PRIMARY KEY,
    business_entity_id UUID NOT NULL,
    branch_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_branches_business_entity
        FOREIGN KEY (business_entity_id)
        REFERENCES mtms.business_entities (id),
    CONSTRAINT uq_branches_entity_branch_code
        UNIQUE (business_entity_id, branch_code),
    CONSTRAINT uq_branches_entity_id
        UNIQUE (business_entity_id, id),
    CONSTRAINT ck_branches_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX idx_branches_business_entity_id
    ON mtms.branches (business_entity_id);

CREATE TABLE mtms.users (
    id UUID PRIMARY KEY,
    login_identifier VARCHAR(200) NOT NULL,
    password_hash TEXT NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_login_identifier UNIQUE (login_identifier),
    CONSTRAINT ck_users_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DISABLED'))
);
