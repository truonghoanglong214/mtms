-- P0-INV-01 / BR-INV-04 / TC-023
-- Forward-only migration for canonical measurement units, suppliers master,
-- catalog items with lot/expiration tracking invariants, and supplier packaging conversions.

-- 1. Canonical Measurement Units
CREATE TABLE mtms.units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_code VARCHAR(20) NOT NULL,
    unit_name VARCHAR(70) NOT NULL,
    dimension VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_units_unit_code UNIQUE (unit_code),
    CONSTRAINT ck_units_dimension CHECK (dimension IN ('MASS', 'VOLUME', 'COUNT', 'OTHER'))
);

-- Seed standard canonical units
INSERT INTO mtms.units (id, unit_code, unit_name, dimension) VALUES
    ('00000000-0000-0000-0001-000000000001', 'KG', 'Kilogram', 'MASS'),
    ('00000000-0000-0000-0001-000000000002', 'G', 'Gram', 'MASS'),
    ('00000000-0000-0000-0001-000000000003', 'L', 'Lít', 'VOLUME'),
    ('00000000-0000-0000-0001-000000000004', 'ML', 'Mililít', 'VOLUME'),
    ('00000000-0000-0000-0001-000000000005', 'PIECE', 'Cái / Miếng', 'COUNT'),
    ('00000000-0000-0000-0001-000000000006', 'CAN', 'Lon', 'COUNT'),
    ('00000000-0000-0000-0001-000000000007', 'BOTTLE', 'Chai', 'COUNT'),
    ('00000000-0000-0000-0001-000000000008', 'BAG', 'Bao / Túi', 'COUNT'),
    ('00000000-0000-0000-0001-000000000009', 'BOX', 'Hộp / Thùng', 'COUNT')
ON CONFLICT (unit_code) DO NOTHING;

-- 2. Suppliers Master
CREATE TABLE mtms.suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    supplier_code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    contact_note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_suppliers_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT uq_suppliers_entity_code UNIQUE (business_entity_id, supplier_code),
    CONSTRAINT ck_suppliers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_suppliers_business_entity_id ON mtms.suppliers(business_entity_id);

-- 3. Catalog Items
CREATE TABLE mtms.catalog_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    name VARCHAR(160) NOT NULL,
    base_unit_id UUID NOT NULL,
    item_kind VARCHAR(24) NOT NULL,
    lot_tracking_required BOOLEAN NOT NULL DEFAULT FALSE,
    expiration_tracking_required BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_catalog_items_business_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_catalog_items_base_unit FOREIGN KEY (base_unit_id) REFERENCES mtms.units(id),
    CONSTRAINT uq_catalog_items_entity_code UNIQUE (business_entity_id, item_code),
    CONSTRAINT ck_catalog_items_item_kind CHECK (item_kind IN ('INGREDIENT', 'CONSUMABLE', 'EQUIPMENT', 'OTHER')),
    CONSTRAINT ck_catalog_items_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_catalog_items_exp_lot CHECK (NOT expiration_tracking_required OR lot_tracking_required)
);

CREATE INDEX idx_catalog_items_business_entity_id ON mtms.catalog_items(business_entity_id);
CREATE INDEX idx_catalog_items_base_unit_id ON mtms.catalog_items(base_unit_id);

-- 4. Supplier Pack Conversions
CREATE TABLE mtms.supplier_pack_conversions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_entity_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    item_id UUID NOT NULL,
    pack_name VARCHAR(100) NOT NULL,
    base_units_per_pack NUMERIC(18, 3) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_supplier_pack_conversions_entity FOREIGN KEY (business_entity_id) REFERENCES mtms.business_entities(id),
    CONSTRAINT fk_supplier_pack_conversions_supplier FOREIGN KEY (supplier_id) REFERENCES mtms.suppliers(id),
    CONSTRAINT fk_supplier_pack_conversions_item FOREIGN KEY (item_id) REFERENCES mtms.catalog_items(id),
    CONSTRAINT ck_supplier_pack_conversions_rate CHECK (base_units_per_pack > 0),
    CONSTRAINT ck_pack_conversion_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT uq_supplier_pack_conversion UNIQUE (supplier_id, item_id, pack_name, effective_from)
);

CREATE INDEX idx_supplier_pack_conversions_entity_item ON mtms.supplier_pack_conversions(business_entity_id, item_id);
CREATE INDEX idx_supplier_pack_conversions_supplier ON mtms.supplier_pack_conversions(supplier_id);
