-- P0-INV-03 / BR-INV-05 / TC-008
-- Forward-only migration for purchase receipts, receipt lines detail, and branch payables ledger.

-- 1. mtms.payables (Branch Supplier Debt / Payables Ledger)
CREATE TABLE mtms.payables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    counterparty_label VARCHAR(200) NOT NULL,
    source_type VARCHAR(48) NOT NULL,
    source_id UUID,
    incurred_date DATE NOT NULL,
    due_date DATE,
    original_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_payables_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_payables_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT ck_payables_original_amount CHECK (original_amount >= 0),
    CONSTRAINT ck_payables_paid_amount CHECK (paid_amount >= 0),
    CONSTRAINT ck_payables_paid_le_original CHECK (paid_amount <= original_amount),
    CONSTRAINT ck_payables_status CHECK (status IN ('OPEN', 'SETTLED', 'DISPUTED', 'CANCELLED'))
);

CREATE INDEX idx_payables_branch_id ON mtms.payables(branch_id);
CREATE INDEX idx_payables_business_entity_id ON mtms.payables(business_entity_id);
CREATE INDEX idx_payables_source ON mtms.payables(source_type, source_id);
CREATE INDEX idx_payables_status ON mtms.payables(status);

-- 2. mtms.purchase_receipts (Goods Receipts from Suppliers)
CREATE TABLE mtms.purchase_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    receipt_number VARCHAR(60) NOT NULL,
    supplier_id UUID,
    received_on DATE NOT NULL,
    invoice_number VARCHAR(120),
    invoice_total NUMERIC(20, 2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(24) NOT NULL DEFAULT 'UNPAID',
    payable_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    posted_by UUID,
    posted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_purchase_receipts_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_purchase_receipts_branch FOREIGN KEY (branch_id) REFERENCES mtms.branches(id),
    CONSTRAINT fk_purchase_receipts_supplier FOREIGN KEY (supplier_id) REFERENCES mtms.suppliers(id),
    CONSTRAINT fk_purchase_receipts_payable FOREIGN KEY (payable_id) REFERENCES mtms.payables(id),
    CONSTRAINT fk_purchase_receipts_posted_by FOREIGN KEY (posted_by) REFERENCES mtms.users(id),
    CONSTRAINT uq_purchase_receipts_branch_receipt_number UNIQUE (branch_id, receipt_number),
    CONSTRAINT ck_purchase_receipts_invoice_total CHECK (invoice_total >= 0),
    CONSTRAINT ck_purchase_receipts_payment_status CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID')),
    CONSTRAINT ck_purchase_receipts_status CHECK (status IN ('DRAFT', 'POSTED', 'VOIDED'))
);

CREATE INDEX idx_purchase_receipts_branch_id ON mtms.purchase_receipts(branch_id);
CREATE INDEX idx_purchase_receipts_business_entity_id ON mtms.purchase_receipts(business_entity_id);
CREATE INDEX idx_purchase_receipts_supplier_id ON mtms.purchase_receipts(supplier_id);
CREATE INDEX idx_purchase_receipts_received_on ON mtms.purchase_receipts(received_on);
CREATE INDEX idx_purchase_receipts_status ON mtms.purchase_receipts(status);

-- 3. mtms.purchase_receipt_lines (Receipt Items Detail)
CREATE TABLE mtms.purchase_receipt_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL,
    line_no INTEGER NOT NULL,
    item_id UUID NOT NULL,
    pack_name VARCHAR(100),
    declared_pack_quantity NUMERIC(18, 3),
    conversion_snapshot NUMERIC(18, 6) NOT NULL DEFAULT 1,
    received_base_quantity NUMERIC(18, 3) NOT NULL,
    base_unit_cost NUMERIC(20, 6) NOT NULL,
    line_total NUMERIC(20, 2) NOT NULL,
    lot_code VARCHAR(80),
    expires_on DATE,
    CONSTRAINT fk_purchase_receipt_lines_receipt FOREIGN KEY (receipt_id) REFERENCES mtms.purchase_receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_receipt_lines_item FOREIGN KEY (item_id) REFERENCES mtms.catalog_items(id),
    CONSTRAINT uq_purchase_receipt_lines_receipt_line_no UNIQUE (receipt_id, line_no),
    CONSTRAINT ck_purchase_receipt_lines_line_no CHECK (line_no > 0),
    CONSTRAINT ck_purchase_receipt_lines_declared_pack_quantity CHECK (declared_pack_quantity IS NULL OR declared_pack_quantity > 0),
    CONSTRAINT ck_purchase_receipt_lines_conversion_snapshot CHECK (conversion_snapshot > 0),
    CONSTRAINT ck_purchase_receipt_lines_received_base_quantity CHECK (received_base_quantity > 0),
    CONSTRAINT ck_purchase_receipt_lines_base_unit_cost CHECK (base_unit_cost >= 0),
    CONSTRAINT ck_purchase_receipt_lines_line_total CHECK (line_total >= 0)
);

CREATE INDEX idx_purchase_receipt_lines_receipt_id ON mtms.purchase_receipt_lines(receipt_id);
CREATE INDEX idx_purchase_receipt_lines_item_id ON mtms.purchase_receipt_lines(item_id);
