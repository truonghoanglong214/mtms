-- P0-INV-04 / P0-INV-05 / BR-INV-02 / BR-INV-03 / TC-020 / TC-021 / TC-022
-- Forward-only migration for stock issue documents and document line items detail.

-- 1. mtms.stock_documents (Manual Stock Issue & Adjustment Documents)
CREATE TABLE mtms.stock_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    document_number VARCHAR(60) NOT NULL,
    document_type VARCHAR(28) NOT NULL DEFAULT 'CONSUMPTION_ISSUE',
    document_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reason TEXT,
    total_lines INTEGER NOT NULL DEFAULT 0,
    total_quantity NUMERIC(18, 3) NOT NULL DEFAULT 0,
    total_booked_cost NUMERIC(22, 2) NOT NULL DEFAULT 0,
    created_by UUID NOT NULL,
    approved_by UUID,
    posted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_stock_documents_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_stock_documents_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_stock_documents_created_by FOREIGN KEY (created_by) REFERENCES mtms.users(id),
    CONSTRAINT fk_stock_documents_approved_by FOREIGN KEY (approved_by) REFERENCES mtms.users(id),
    CONSTRAINT uq_stock_documents_branch_doc_num UNIQUE (branch_id, document_number),
    CONSTRAINT ck_stock_documents_doc_type CHECK (document_type IN ('CONSUMPTION_ISSUE', 'SCRAP_DISPOSAL', 'ADJUSTMENT')),
    CONSTRAINT ck_stock_documents_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'POSTED', 'CANCELLED')),
    CONSTRAINT ck_stock_documents_total_lines CHECK (total_lines >= 0),
    CONSTRAINT ck_stock_documents_total_quantity CHECK (total_quantity >= 0),
    CONSTRAINT ck_stock_documents_total_booked_cost CHECK (total_booked_cost >= 0)
);

CREATE INDEX idx_stock_documents_branch_date ON mtms.stock_documents(branch_id, document_date);
CREATE INDEX idx_stock_documents_business_entity_id ON mtms.stock_documents(business_entity_id);
CREATE INDEX idx_stock_documents_status ON mtms.stock_documents(status);
CREATE INDEX idx_stock_documents_created_by ON mtms.stock_documents(created_by);

-- 2. mtms.stock_document_lines (Issue Document Items Detail with Moving WAC Snapshots)
CREATE TABLE mtms.stock_document_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    line_no INTEGER NOT NULL,
    item_id UUID NOT NULL,
    lot_id UUID,
    issue_quantity NUMERIC(18, 3) NOT NULL,
    booked_unit_cost NUMERIC(20, 6) NOT NULL DEFAULT 0,
    booked_total NUMERIC(22, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    CONSTRAINT fk_stock_document_lines_document FOREIGN KEY (document_id) REFERENCES mtms.stock_documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_document_lines_item FOREIGN KEY (item_id) REFERENCES mtms.catalog_items(id),
    CONSTRAINT fk_stock_document_lines_lot FOREIGN KEY (lot_id) REFERENCES mtms.stock_lots(id),
    CONSTRAINT uq_stock_document_lines_doc_line UNIQUE (document_id, line_no),
    CONSTRAINT ck_stock_document_lines_line_no CHECK (line_no > 0),
    CONSTRAINT ck_stock_document_lines_issue_quantity CHECK (issue_quantity > 0),
    CONSTRAINT ck_stock_document_lines_booked_unit_cost CHECK (booked_unit_cost >= 0),
    CONSTRAINT ck_stock_document_lines_booked_total CHECK (booked_total >= 0)
);

CREATE INDEX idx_stock_document_lines_document_id ON mtms.stock_document_lines(document_id);
CREATE INDEX idx_stock_document_lines_item_id ON mtms.stock_document_lines(item_id);
CREATE INDEX idx_stock_document_lines_lot_id ON mtms.stock_document_lines(lot_id);
