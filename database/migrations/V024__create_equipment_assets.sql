-- V024: Create equipment assets register for P0-AST-01 (BR-AST-01, TC-051, ADR-023)

CREATE TABLE mtms.equipment_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    asset_code VARCHAR(60) NOT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(40) NOT NULL,
    serial_number VARCHAR(100),
    model VARCHAR(100),
    purchased_on DATE,
    acquisition_cost NUMERIC(20, 2) NOT NULL DEFAULT 0,
    warranty_until DATE,
    status VARCHAR(24) NOT NULL DEFAULT 'IN_USE',
    location_in_branch VARCHAR(120),
    supplier_id UUID REFERENCES mtms.suppliers(id),
    evidence_file_id UUID REFERENCES mtms.evidence_files(id),
    maintenance_note TEXT,
    depreciation_policy JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_equipment_assets_code UNIQUE (asset_code),
    CONSTRAINT ck_equipment_assets_cost_non_negative CHECK (acquisition_cost >= 0),
    CONSTRAINT ck_equipment_assets_category CHECK (
        category IN (
            'COFFEE_MACHINE',
            'REFRIGERATION',
            'SEALING_MACHINE',
            'BLENDER',
            'POS_HARDWARE',
            'FURNITURE',
            'KITCHEN_EQUIPMENT',
            'OTHER'
        )
    ),
    CONSTRAINT ck_equipment_assets_status CHECK (
        status IN (
            'IN_USE',
            'IN_REPAIR',
            'STORED',
            'DISPOSED',
            'TRANSFERRED'
        )
    )
);

CREATE INDEX idx_equipment_assets_branch_status 
    ON mtms.equipment_assets (branch_id, status);

CREATE INDEX idx_equipment_assets_category 
    ON mtms.equipment_assets (branch_id, category);

CREATE INDEX idx_equipment_assets_warranty 
    ON mtms.equipment_assets (branch_id, warranty_until);
