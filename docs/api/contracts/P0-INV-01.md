# API & Data Contract — P0-INV-01: Item & Base Units Catalog

- **Task ID:** P0-INV-01
- **Related FE:** FE-P0-24 (Ingredient and base-unit catalog)
- **Requirements:** BR-INV-04
- **Test Cases:** TC-023 (Supplier pack conversion: 1 box = 24 units, receipt 2 boxes -> 48 base units snapshot)
- **Status:** PROPOSED (Pending Owner Plan Approval)

---

## 1. Domain Entities & Database Schema (Flyway V008)

### 1.1 `mtms.units` (Canonical Measurement Units)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Unit ID |
| `unit_code` | VARCHAR(20) | NOT NULL UNIQUE | E.g. `KG`, `G`, `L`, `ML`, `PIECE`, `BOX`, `CAN`, `PACK` |
| `unit_name` | VARCHAR(70) | NOT NULL | Display name: "Kilogram", "Lít", "Hộp / Lon" |
| `dimension` | VARCHAR(16) | NOT NULL CHECK (dimension IN ('MASS', 'VOLUME', 'COUNT', 'OTHER')) | Physical dimension |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |

### 1.2 `mtms.suppliers` (Suppliers Master)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Supplier ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `supplier_code` | VARCHAR(50) | NOT NULL | E.g. `SUP-VINAMILK` |
| `name` | VARCHAR(200) | NOT NULL | E.g. "Công ty Cổ phần Sữa Việt Nam" |
| `contact_note` | TEXT | NULLABLE | Phone, email, contact person |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')) | Status |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |
| **Unique Index** | | `UNIQUE (business_entity_id, supplier_code)` | Unique code per entity |

### 1.3 `mtms.catalog_items` (Catalog Items)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Item ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `item_code` | VARCHAR(50) | NOT NULL | E.g. `ING-COFFEE-01`, `ING-MILK-COND` |
| `name` | VARCHAR(160) | NOT NULL | E.g. "Cà phê hạt Robusta Buôn Ma Thuột", "Sữa đặc Ông Thọ" |
| `base_unit_id` | UUID | NOT NULL REFERENCES mtms.units(id) | Canonical Base Unit |
| `item_kind` | VARCHAR(24) | NOT NULL CHECK (item_kind IN ('INGREDIENT', 'CONSUMABLE', 'EQUIPMENT', 'OTHER')) | Item classification |
| `lot_tracking_required` | BOOLEAN | NOT NULL DEFAULT FALSE | Is lot code tracked? |
| `expiration_tracking_required` | BOOLEAN | NOT NULL DEFAULT FALSE | Is expiration date tracked? |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')) | Lifecycle status |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Updated timestamp |
| **Unique Index** | | `UNIQUE (business_entity_id, item_code)` | Unique code per entity |
| **Check Invariant** | | `CHECK (NOT expiration_tracking_required OR lot_tracking_required)` | Expiration requires Lot Tracking |

### 1.4 `mtms.supplier_pack_conversions` (Supplier Packaging Conversions)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique conversion ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `supplier_id` | UUID | NOT NULL REFERENCES mtms.suppliers(id) | Supplier |
| `item_id` | UUID | NOT NULL REFERENCES mtms.catalog_items(id) | Target catalog item |
| `pack_name` | VARCHAR(100) | NOT NULL | E.g. "Thùng 24 lon", "Bao 25kg" |
| `base_units_per_pack` | NUMERIC(18,3) | NOT NULL CHECK (base_units_per_pack > 0) | Conversion multiplier (e.g. 24.000) |
| `effective_from` | DATE | NOT NULL | Valid start date |
| `effective_to` | DATE | NULLABLE | Valid end date (NULL = current) |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |
| **Check Invariant** | | `CHECK (effective_to IS NULL OR effective_to >= effective_from)` | Valid date span |
| **Unique Index** | | `UNIQUE (supplier_id, item_id, pack_name, effective_from)` | No overlapping duplicate packs |

---

## 2. REST Endpoints Handshake

### 2.1 Canonical Units
#### `GET /api/v1/units`
- **Scope:** Authenticated user
- **Response `200 OK`:**
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "unitCode": "KG",
    "unitName": "Kilogram",
    "dimension": "MASS"
  },
  {
    "id": "e0b8219c-95a3-4882-965a-8b839dfec241",
    "unitCode": "CAN",
    "unitName": "Lon",
    "dimension": "COUNT"
  }
]
```

#### `POST /api/v1/units`
- **Scope:** `@branchAuth.isSystemAdmin()`
- **Request:**
```json
{
  "unitCode": "BOX",
  "unitName": "Hộp / Thùng",
  "dimension": "COUNT"
}
```
- **Response `201 Created`** with created `UnitDto`.

---

### 2.2 Suppliers
#### `GET /api/v1/entities/{entityId}/suppliers`
- **Scope:** `@branchAuth.hasEntityAccess(#entityId)`
- **Query Params:** `status` (optional: ACTIVE, INACTIVE)
- **Response `200 OK`:**
```json
[
  {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "businessEntityId": "550e8400-e29b-41d4-a716-446655440000",
    "supplierCode": "SUP-VINAMILK",
    "name": "Công ty Cổ phần Sữa Việt Nam (Vinamilk)",
    "contactNote": "Hotline: 1900 636 979 - Sales: sales@vinamilk.vn",
    "status": "ACTIVE",
    "createdAt": "2026-11-16T08:00:00Z"
  }
]
```

#### `POST /api/v1/entities/{entityId}/suppliers`
- **Scope:** `@branchAuth.hasEntityAccess(#entityId)`
- **Request:**
```json
{
  "supplierCode": "SUP-VINAMILK",
  "name": "Công ty Cổ phần Sữa Việt Nam (Vinamilk)",
  "contactNote": "Hotline: 1900 636 979"
}
```
- **Response `201 Created`**

---

### 2.3 Catalog Items
#### `GET /api/v1/entities/{entityId}/items`
- **Scope:** `@branchAuth.hasEntityAccess(#entityId)`
- **Query Params:**
  - `itemKind` (optional: `INGREDIENT`, `CONSUMABLE`, `EQUIPMENT`, `OTHER`)
  - `status` (optional: `ACTIVE`, `INACTIVE`, `ARCHIVED`)
  - `search` (optional string matching code or name)
- **Response `200 OK`:**
```json
[
  {
    "id": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
    "businessEntityId": "550e8400-e29b-41d4-a716-446655440000",
    "itemCode": "ING-MILK-OT-380G",
    "name": "Sữa đặc có đường Ông Thọ đỏ lon 380g",
    "baseUnitId": "e0b8219c-95a3-4882-965a-8b839dfec241",
    "baseUnitCode": "CAN",
    "baseUnitName": "Lon",
    "itemKind": "INGREDIENT",
    "lotTrackingRequired": true,
    "expirationTrackingRequired": true,
    "status": "ACTIVE",
    "packConversionCount": 1,
    "createdAt": "2026-11-16T08:30:00Z",
    "updatedAt": "2026-11-16T08:30:00Z"
  }
]
```

#### `POST /api/v1/entities/{entityId}/items`
- **Scope:** `@branchAuth.hasEntityAccess(#entityId)`
- **Request:**
```json
{
  "itemCode": "ING-MILK-OT-380G",
  "name": "Sữa đặc có đường Ông Thọ đỏ lon 380g",
  "baseUnitId": "e0b8219c-95a3-4882-965a-8b839dfec241",
  "itemKind": "INGREDIENT",
  "lotTrackingRequired": true,
  "expirationTrackingRequired": true
}
```
- **Validation Rules:**
  - If `expirationTrackingRequired == true`, then `lotTrackingRequired` MUST be `true` (RFC 7807 `400 Bad Request` if violated).
  - `itemCode` must match regex `^[A-Z0-9_-]{3,50}$`.
  - Duplicate `itemCode` within entity returns `409 Conflict`.
- **Response `201 Created`** with `CatalogItemDto`.

#### `PUT /api/v1/entities/{entityId}/items/{itemId}`
- **Scope:** `@branchAuth.hasEntityAccess(#entityId)`
- **Request:**
```json
{
  "name": "Sữa đặc có đường Ông Thọ đỏ lon 380g (Mẫu mới)",
  "itemKind": "INGREDIENT",
  "lotTrackingRequired": true,
  "expirationTrackingRequired": true,
  "status": "ACTIVE"
}
```
- **Response `200 OK`** with updated `CatalogItemDto`.

---

### 2.4 Supplier Pack Conversions (BR-INV-04 / TC-023)
#### `GET /api/v1/entities/{entityId}/items/{itemId}/pack-conversions`
- **Scope:** `@branchAuth.hasEntityAccess(#entityId)`
- **Response `200 OK`:**
```json
[
  {
    "id": "f2a14bc0-93a5-4927-b952-41d3d63b018b",
    "businessEntityId": "550e8400-e29b-41d4-a716-446655440000",
    "supplierId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "supplierName": "Công ty Cổ phần Sữa Việt Nam (Vinamilk)",
    "itemId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
    "itemCode": "ING-MILK-OT-380G",
    "packName": "Thùng 24 lon",
    "baseUnitsPerPack": "24.000",
    "effectiveFrom": "2026-01-01",
    "effectiveTo": null,
    "createdAt": "2026-11-16T09:00:00Z"
  }
]
```

#### `POST /api/v1/entities/{entityId}/items/{itemId}/pack-conversions`
- **Scope:** `@branchAuth.hasEntityAccess(#entityId)`
- **Request:**
```json
{
  "supplierId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "packName": "Thùng 24 lon",
  "baseUnitsPerPack": "24.000",
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null
}
```
- **Response `201 Created`** with `SupplierPackConversionDto`.

#### `POST /api/v1/entities/{entityId}/pack-conversions/calculate` (TC-023 Calculation & Snapshot Verification)
- **Scope:** `@branchAuth.hasEntityAccess(#entityId)`
- **Request:**
```json
{
  "itemId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
  "supplierId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "packQuantity": "2.000",
  "businessDate": "2026-11-16"
}
```
- **Business Math (TC-023):**
  `calculatedBaseQuantity = packQuantity * baseUnitsPerPack = 2.000 * 24.000 = 48.000`
- **Response `200 OK`:**
```json
{
  "itemId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
  "itemCode": "ING-MILK-OT-380G",
  "itemName": "Sữa đặc có đường Ông Thọ đỏ lon 380g",
  "supplierId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "supplierName": "Công ty Cổ phần Sữa Việt Nam (Vinamilk)",
  "packName": "Thùng 24 lon",
  "packQuantity": "2.000",
  "baseUnitsPerPack": "24.000",
  "baseUnitCode": "CAN",
  "baseUnitName": "Lon",
  "calculatedBaseQuantity": "48.000",
  "conversionSnapshot": "24.000",
  "formulaExplanation": "2.000 Thùng 24 lon × 24.000 = 48.000 Lon"
}
```

---

## 3. Strict RFC 7807 Error Code Catalog

| HTTP Status | `type` URI | `title` | Description |
| :--- | :--- | :--- | :--- |
| `400 Bad Request` | `urn:mtms:error:invalid-tracking-invariant` | Invalid Tracking Invariant | Expiration tracking enabled without lot tracking |
| `400 Bad Request` | `urn:mtms:error:invalid-conversion-rate` | Invalid Conversion Rate | `baseUnitsPerPack <= 0` or invalid date range |
| `404 Not Found` | `urn:mtms:error:item-not-found` | Item Not Found | `itemId` does not exist in target entity |
| `404 Not Found` | `urn:mtms:error:unit-not-found` | Unit Not Found | `baseUnitId` does not exist |
| `404 Not Found` | `urn:mtms:error:conversion-not-found` | Conversion Not Found | No active conversion for item + supplier on business date |
| `409 Conflict` | `urn:mtms:error:duplicate-item-code` | Duplicate Item Code | Item code already exists in business entity |
| `409 Conflict` | `urn:mtms:error:duplicate-conversion` | Duplicate Pack Conversion | Conversion with same pack name & start date exists |
| `403 Forbidden` | `urn:mtms:error:forbidden` | Access Forbidden | User does not have access to entity |
