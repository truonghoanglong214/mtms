-- P0-ORG-05 / BR-ORG-04
-- Forward-only scoped role grants for branch-level authorization.
-- Composite foreign key guarantees branch belongs to business_entity.

CREATE TABLE mtms.scoped_role_grants (
    id UUID PRIMARY KEY,
    business_entity_id UUID NOT NULL,
    branch_id UUID,
    user_id UUID NOT NULL,
    role_key VARCHAR(40) NOT NULL,
    action_scope VARCHAR(80) NOT NULL DEFAULT '*',
    row_scope VARCHAR(80),
    report_permission_group VARCHAR(50),
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_to DATE,
    granted_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scoped_role_grants_entity
        FOREIGN KEY (business_entity_id)
        REFERENCES mtms.business_entities (id),
    CONSTRAINT fk_scoped_role_grants_user
        FOREIGN KEY (user_id)
        REFERENCES mtms.users (id),
    CONSTRAINT fk_scoped_role_grants_granted_by
        FOREIGN KEY (granted_by)
        REFERENCES mtms.users (id),
    CONSTRAINT fk_scoped_role_grants_entity_branch
        FOREIGN KEY (business_entity_id, branch_id)
        REFERENCES mtms.branches (business_entity_id, id),
    CONSTRAINT ck_scoped_role_grants_role_key
        CHECK (role_key IN ('SYSTEM_ADMIN', 'BRANCH_OWNER', 'MANAGER', 'FINANCE', 'EMPLOYEE', 'INVESTOR')),
    CONSTRAINT ck_scoped_role_grants_valid_range
        CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT ck_scoped_role_grants_branch_required
        CHECK (branch_id IS NOT NULL OR role_key = 'SYSTEM_ADMIN')
);

CREATE INDEX idx_scoped_role_grants_lookup
    ON mtms.scoped_role_grants (user_id, branch_id, role_key, valid_from, valid_to);

CREATE INDEX idx_scoped_role_grants_branch
    ON mtms.scoped_role_grants (branch_id);

CREATE INDEX idx_scoped_role_grants_entity
    ON mtms.scoped_role_grants (business_entity_id);
