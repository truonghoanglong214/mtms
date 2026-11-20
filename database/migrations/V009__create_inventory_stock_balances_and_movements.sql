-- P0-INV-02 / BR-INV-08 / TC-024
-- Forward-only migration for inventory stock balances, lot & expiry tracking,
-- immutable stock movement ledger, and post-once branch opening inventory lock.

-- 1. mtms.stock_balances (Per-Branch Item Stock & Moving WAC)
CREATE TABLE mtms.stock_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantity NUMERIC(18, 3) NOT NULL DEFAULT 0,
    inventory_value NUMERIC(22, 2) NOT NULL DEFAULT 0,
    avg_unit_cost NUMERIC(20, 6) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_stock_balances_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_stock_balances_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_stock_balances_item FOREIGN KEY (item_id) REFERENCES mtms.catalog_items(id),
    CONSTRAINT uq_stock_balances_branch_item UNIQUE (branch_id, item_id),
    CONSTRAINT ck_stock_balances_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_stock_balances_inventory_value CHECK (inventory_value >= 0),
    CONSTRAINT ck_stock_balances_avg_unit_cost CHECK (avg_unit_cost >= 0),
    CONSTRAINT ck_stock_balances_version CHECK (version >= 0)
);

CREATE INDEX idx_stock_balances_branch_id ON mtms.stock_balances(branch_id);
CREATE INDEX idx_stock_balances_item_id ON mtms.stock_balances(item_id);
CREATE INDEX idx_stock_balances_business_entity_id ON mtms.stock_balances(business_entity_id);

-- 2. mtms.stock_lots (Lot & Expiry Tracking)
CREATE TABLE mtms.stock_lots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    item_id UUID NOT NULL,
    lot_code VARCHAR(80) NOT NULL,
    expires_on DATE,
    quantity NUMERIC(18, 3) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_stock_lots_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_stock_lots_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_stock_lots_item FOREIGN KEY (item_id) REFERENCES mtms.catalog_items(id),
    CONSTRAINT uq_stock_lots_branch_item_lot UNIQUE (branch_id, item_id, lot_code),
    CONSTRAINT ck_stock_lots_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_stock_lots_version CHECK (version >= 0)
);

CREATE INDEX idx_stock_lots_branch_item ON mtms.stock_lots(branch_id, item_id);
CREATE INDEX idx_stock_lots_expires_on ON mtms.stock_lots(expires_on);

-- 3. mtms.stock_movements (Immutable Stock Audit Ledger)
CREATE TABLE mtms.stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    item_id UUID NOT NULL,
    lot_id UUID,
    business_date DATE NOT NULL,
    movement_type VARCHAR(28) NOT NULL,
    signed_quantity NUMERIC(18, 3) NOT NULL,
    signed_value NUMERIC(22, 2) NOT NULL,
    booked_avg_cost NUMERIC(20, 6) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id UUID NOT NULL,
    source_ref VARCHAR(120),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_stock_movements_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_stock_movements_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_stock_movements_item FOREIGN KEY (item_id) REFERENCES mtms.catalog_items(id),
    CONSTRAINT fk_stock_movements_lot FOREIGN KEY (lot_id) REFERENCES mtms.stock_lots(id),
    CONSTRAINT fk_stock_movements_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT ck_stock_movements_signed_quantity CHECK (signed_quantity <> 0),
    CONSTRAINT ck_stock_movements_booked_avg_cost CHECK (booked_avg_cost >= 0)
);

CREATE INDEX idx_stock_movements_branch_date ON mtms.stock_movements(branch_id, business_date);
CREATE INDEX idx_stock_movements_item ON mtms.stock_movements(item_id);
CREATE INDEX idx_stock_movements_source ON mtms.stock_movements(source_type, source_id);

-- 4. mtms.opening_inventory_records (Branch Opening Lock — Enforces TC-024 Post Once)
CREATE TABLE mtms.opening_inventory_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    import_batch_id UUID,
    opening_date DATE NOT NULL,
    total_items INTEGER NOT NULL,
    total_quantity NUMERIC(18, 3) NOT NULL,
    total_value NUMERIC(22, 2) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'COMMITTED',
    approved_by UUID NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    note TEXT,
    CONSTRAINT fk_opening_records_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_opening_records_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_opening_records_import_batch FOREIGN KEY (import_batch_id) REFERENCES mtms.import_batches(id),
    CONSTRAINT fk_opening_records_approved_by FOREIGN KEY (approved_by) REFERENCES mtms.users(id),
    CONSTRAINT uq_opening_records_branch UNIQUE (branch_id),
    CONSTRAINT ck_opening_records_total_items CHECK (total_items > 0),
    CONSTRAINT ck_opening_records_total_quantity CHECK (total_quantity > 0),
    CONSTRAINT ck_opening_records_total_value CHECK (total_value >= 0),
    CONSTRAINT ck_opening_records_status CHECK (status IN ('COMMITTED'))
);

CREATE INDEX idx_opening_records_branch_id ON mtms.opening_inventory_records(branch_id);
CREATE INDEX idx_opening_records_business_entity_id ON mtms.opening_inventory_records(business_entity_id);
