-- V027__create_stock_transfers_and_lines.sql
-- Bounded Context: Movement (P1-MOV-01, P1-MOV-02, BR-MOV-01, BR-MOV-02, TC-027, TC-028)
-- Intra-entity branch stock transfers, Moving WAC snapshot, and receiver partial acceptance.

-- 1. Bảng phiếu điều chuyển nguyên vật liệu giữa các chi nhánh
CREATE TABLE mtms.stock_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_number VARCHAR(64) NOT NULL,
    business_entity_id UUID NOT NULL REFERENCES mtms.business_entities(id),
    from_branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    to_branch_id UUID NOT NULL REFERENCES mtms.branches(id),
    requested_on DATE NOT NULL,
    dispatched_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'DISPATCHED', 'PARTIAL_RECEIPT', 'COMPLETED', 'CANCELLED')),
    requested_by UUID NOT NULL REFERENCES mtms.users(id),
    dispatched_by UUID REFERENCES mtms.users(id),
    received_by UUID REFERENCES mtms.users(id),
    total_lines INTEGER NOT NULL DEFAULT 0 CHECK (total_lines >= 0),
    total_dispatched_quantity NUMERIC(18, 3) NOT NULL DEFAULT 0 CHECK (total_dispatched_quantity >= 0),
    total_received_quantity NUMERIC(18, 3) NOT NULL DEFAULT 0 CHECK (total_received_quantity >= 0),
    total_transit_variance_quantity NUMERIC(18, 3) NOT NULL DEFAULT 0 CHECK (total_transit_variance_quantity >= 0),
    total_dispatched_cost NUMERIC(20, 2) NOT NULL DEFAULT 0 CHECK (total_dispatched_cost >= 0),
    total_received_cost NUMERIC(20, 2) NOT NULL DEFAULT 0 CHECK (total_received_cost >= 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_stock_transfers_different_branches CHECK (from_branch_id <> to_branch_id),
    CONSTRAINT uq_stock_transfers_number UNIQUE (business_entity_id, transfer_number)
);

-- 2. Bảng chi tiết các dòng nguyên vật liệu trong phiếu điều chuyển
CREATE TABLE mtms.stock_transfer_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id UUID NOT NULL REFERENCES mtms.stock_transfers(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL CHECK (line_number > 0),
    item_id UUID NOT NULL REFERENCES mtms.catalog_items(id),
    lot_id UUID REFERENCES mtms.stock_lots(id),
    dispatched_base_qty NUMERIC(18, 3) NOT NULL CHECK (dispatched_base_qty > 0),
    received_base_qty NUMERIC(18, 3) NOT NULL DEFAULT 0 CHECK (received_base_qty >= 0),
    transit_variance_qty NUMERIC(18, 3) NOT NULL DEFAULT 0 CHECK (transit_variance_qty >= 0),
    sender_unit_cost NUMERIC(20, 6) NOT NULL DEFAULT 0 CHECK (sender_unit_cost >= 0),
    dispatched_line_cost NUMERIC(20, 2) NOT NULL DEFAULT 0 CHECK (dispatched_line_cost >= 0),
    received_line_cost NUMERIC(20, 2) NOT NULL DEFAULT 0 CHECK (received_line_cost >= 0),
    receiver_note TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_stock_transfer_lines_item UNIQUE (transfer_id, item_id),
    CONSTRAINT ck_stock_transfer_lines_quantities CHECK (received_base_qty + transit_variance_qty <= dispatched_base_qty)
);

-- Indexes tối ưu hóa truy vấn
CREATE INDEX idx_stock_transfers_from_branch ON mtms.stock_transfers (from_branch_id, status);
CREATE INDEX idx_stock_transfers_to_branch ON mtms.stock_transfers (to_branch_id, status);
CREATE INDEX idx_stock_transfers_entity ON mtms.stock_transfers (business_entity_id, status);
CREATE INDEX idx_stock_transfer_lines_transfer ON mtms.stock_transfer_lines (transfer_id);
