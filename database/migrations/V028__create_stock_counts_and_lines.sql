-- P1-INV-02 / BR-INV-07 / TC-026 / TC-021
-- Forward-only migration for physical stocktaking (stock counts) and item discrepancy reconciliation.

-- 1. mtms.stock_counts
CREATE TABLE mtms.stock_counts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    count_number VARCHAR(60) NOT NULL,
    counted_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    count_type VARCHAR(20) NOT NULL DEFAULT 'PERIODIC',
    total_items INTEGER NOT NULL DEFAULT 0,
    total_surplus_qty NUMERIC(18, 3) NOT NULL DEFAULT 0,
    total_shortage_qty NUMERIC(18, 3) NOT NULL DEFAULT 0,
    total_surplus_value NUMERIC(22, 2) NOT NULL DEFAULT 0,
    total_shortage_value NUMERIC(22, 2) NOT NULL DEFAULT 0,
    net_variance_value NUMERIC(22, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_by UUID NOT NULL,
    submitted_by UUID,
    submitted_at TIMESTAMPTZ,
    approved_by UUID,
    posted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_stock_counts_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_stock_counts_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_stock_counts_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT fk_stock_counts_submitted_by FOREIGN KEY (submitted_by) REFERENCES mtms.users(id),
    CONSTRAINT fk_stock_counts_approved_by FOREIGN KEY (approved_by) REFERENCES mtms.users(id),
    CONSTRAINT uq_stock_counts_branch_count_number UNIQUE (branch_id, count_number),
    CONSTRAINT ck_stock_counts_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'POSTED', 'CANCELLED')),
    CONSTRAINT ck_stock_counts_type CHECK (count_type IN ('PERIODIC', 'SURPRISE')),
    CONSTRAINT ck_stock_counts_total_items CHECK (total_items >= 0),
    CONSTRAINT ck_stock_counts_total_surplus_qty CHECK (total_surplus_qty >= 0),
    CONSTRAINT ck_stock_counts_total_shortage_qty CHECK (total_shortage_qty >= 0),
    CONSTRAINT ck_stock_counts_total_surplus_val CHECK (total_surplus_value >= 0),
    CONSTRAINT ck_stock_counts_total_shortage_val CHECK (total_shortage_value >= 0)
);

CREATE INDEX idx_stock_counts_branch_status ON mtms.stock_counts(branch_id, status);
CREATE INDEX idx_stock_counts_branch_date ON mtms.stock_counts(branch_id, counted_on);
CREATE INDEX idx_stock_counts_entity ON mtms.stock_counts(business_entity_id);

-- 2. mtms.stock_count_lines
CREATE TABLE mtms.stock_count_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_count_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    item_id UUID NOT NULL,
    lot_id UUID,
    expected_qty NUMERIC(18, 3) NOT NULL,
    counted_qty NUMERIC(18, 3) NOT NULL,
    variance_qty NUMERIC(18, 3) NOT NULL,
    snapshot_unit_cost NUMERIC(20, 6) NOT NULL DEFAULT 0,
    variance_value NUMERIC(22, 2) NOT NULL DEFAULT 0,
    discrepancy_reason TEXT,
    notes TEXT,
    CONSTRAINT fk_stock_count_lines_count FOREIGN KEY (stock_count_id) REFERENCES mtms.stock_counts(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_count_lines_item FOREIGN KEY (item_id) REFERENCES mtms.catalog_items(id),
    CONSTRAINT fk_stock_count_lines_lot FOREIGN KEY (lot_id) REFERENCES mtms.stock_lots(id),
    CONSTRAINT uq_stock_count_lines_count_item UNIQUE (stock_count_id, item_id),
    CONSTRAINT ck_stock_count_lines_line_number CHECK (line_number > 0),
    CONSTRAINT ck_stock_count_lines_expected_qty CHECK (expected_qty >= 0),
    CONSTRAINT ck_stock_count_lines_counted_qty CHECK (counted_qty >= 0),
    CONSTRAINT ck_stock_count_lines_unit_cost CHECK (snapshot_unit_cost >= 0)
);

CREATE INDEX idx_stock_count_lines_count_id ON mtms.stock_count_lines(stock_count_id);
CREATE INDEX idx_stock_count_lines_item_id ON mtms.stock_count_lines(item_id);
