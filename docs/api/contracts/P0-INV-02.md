# API & Data Contract — P0-INV-02: Opening Inventory & Valuation

- **Task ID:** P0-INV-02
- **Related FE:** FE-P0-25 (Opening inventory capture)
- **Requirements:** BR-INV-08 (Physical count + Excel import with verified qty/value seed per branch)
- **Test Cases:** TC-024 (Excel opening stock import retried -> Single approved opening balance)
- **Dependencies:** P0-SEC-04 (Import Staging), P0-INV-01 (Item Catalog & Units)
- **Status:** PROPOSED (Pending Owner Plan Approval)

---

## 1. Domain Entities & Database Schema (Flyway V009)

### 1.1 `mtms.stock_balances` (Per-Branch Item Stock & Moving WAC)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Balance ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `branch_id` | UUID | NOT NULL REFERENCES mtms.branches(id) | Branch scope |
| `item_id` | UUID | NOT NULL REFERENCES mtms.catalog_items(id) | Catalog Item |
| `quantity` | NUMERIC(18,3) | NOT NULL DEFAULT 0 CHECK (quantity >= 0) | Available on-hand quantity |
| `inventory_value` | NUMERIC(22,2) | NOT NULL DEFAULT 0 CHECK (inventory_value >= 0) | Total inventory value in VND |
| `avg_unit_cost` | NUMERIC(20,6) | NOT NULL DEFAULT 0 CHECK (avg_unit_cost >= 0) | Per-branch Moving WAC cost |
| `version` | BIGINT | NOT NULL DEFAULT 0 CHECK (version >= 0) | Optimistic concurrency control |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Updated timestamp |
| **Unique Index** | | `UNIQUE (branch_id, item_id)` | Single balance row per branch/item |

### 1.2 `mtms.stock_lots` (Lot & Expiry Tracking)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Lot ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `branch_id` | UUID | NOT NULL REFERENCES mtms.branches(id) | Branch scope |
| `item_id` | UUID | NOT NULL REFERENCES mtms.catalog_items(id) | Catalog Item |
| `lot_code` | VARCHAR(80) | NOT NULL | Lot identifier (e.g. `LOT-20261116-01`) |
| `expires_on` | DATE | NULLABLE | Expiration date |
| `quantity` | NUMERIC(18,3) | NOT NULL DEFAULT 0 CHECK (quantity >= 0) | Quantity in this lot |
| `version` | BIGINT | NOT NULL DEFAULT 0 CHECK (version >= 0) | Optimistic concurrency control |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |
| **Unique Index** | | `UNIQUE (branch_id, item_id, lot_code)` | Unique lot per branch and item |

### 1.3 `mtms.stock_movements` (Immutable Stock Audit Ledger)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Movement ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `branch_id` | UUID | NOT NULL REFERENCES mtms.branches(id) | Branch scope |
| `item_id` | UUID | NOT NULL REFERENCES mtms.catalog_items(id) | Catalog Item |
| `lot_id` | UUID | NULLABLE REFERENCES mtms.stock_lots(id) | Optional Lot reference |
| `business_date` | DATE | NOT NULL | Business operation date |
| `movement_type` | VARCHAR(28) | NOT NULL | `OPENING_BALANCE`, `PURCHASE_RECEIPT`, `MANUAL_ISSUE` |
| `signed_quantity` | NUMERIC(18,3) | NOT NULL CHECK (signed_quantity <> 0) | Positive = In, Negative = Out |
| `signed_value` | NUMERIC(22,2) | NOT NULL | Total monetary impact in VND |
| `booked_avg_cost` | NUMERIC(20,6) | NOT NULL CHECK (booked_avg_cost >= 0) | Snapshot unit cost at movement time |
| `source_type` | VARCHAR(50) | NOT NULL | E.g. `OPENING_INVENTORY_BATCH` |
| `source_id` | UUID | NOT NULL | ID of the source transaction |
| `source_ref` | VARCHAR(120) | NULLABLE | Human-readable ref (e.g. filename) |
| `created_by` | UUID | NULLABLE REFERENCES mtms.users(id) | Actor user |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |

### 1.4 `mtms.opening_inventory_records` (Branch Opening Lock — Enforces TC-024)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Opening Record ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `branch_id` | UUID | NOT NULL UNIQUE REFERENCES mtms.branches(id) | **UNIQUE per branch (Post Once)** |
| `import_batch_id` | UUID | NULLABLE REFERENCES mtms.import_batches(id) | Linked import staging batch |
| `opening_date` | DATE | NOT NULL | Opening inventory business date |
| `total_items` | INTEGER | NOT NULL CHECK (total_items > 0) | Number of distinct items seeded |
| `total_quantity` | NUMERIC(18,3) | NOT NULL CHECK (total_quantity > 0) | Total quantity seeded |
| `total_value` | NUMERIC(22,2) | NOT NULL CHECK (total_value >= 0) | Total initial valuation |
| `status` | VARCHAR(24) | NOT NULL DEFAULT 'COMMITTED' | `COMMITTED` |
| `approved_by` | UUID | NOT NULL REFERENCES mtms.users(id) | Owner/Manager who approved opening |
| `approved_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Approval timestamp |
| `note` | TEXT | NULLABLE | Optional audit note |

---

## 2. REST Endpoints Handshake

### 2.1 Branch Opening Status Check
#### `GET /api/v1/branches/{branchId}/inventory/opening`
- **Scope:** `@branchAuth.hasBranchAccess(#branchId)`
- **Response `200 OK` (When not opened yet):**
```json
{
  "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "branchName": "Chi nhánh Quận 1",
  "isOpeningCommitted": false,
  "openingRecord": null
}
```
- **Response `200 OK` (When already committed / TC-024):**
```json
{
  "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "branchName": "Chi nhánh Quận 1",
  "isOpeningCommitted": true,
  "openingRecord": {
    "id": "e0b8219c-95a3-4882-965a-8b839dfec241",
    "openingDate": "2026-11-16",
    "totalItems": 15,
    "totalQuantity": "150.000",
    "totalValue": "15000000.00",
    "status": "COMMITTED",
    "approvedBy": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "approvedByName": "Nguyễn Văn Quản Lý",
    "approvedAt": "2026-11-16T08:00:00Z",
    "note": "Khởi tạo kiểm kê đầu kỳ khai trương"
  }
}
```

---

### 2.2 Opening Inventory Staging & Validation Preview
#### `POST /api/v1/branches/{branchId}/inventory/opening/preview`
- **Scope:** `@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')`
- **Request Body (from Staged Import Batch):**
```json
{
  "importBatchId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
  "openingDate": "2026-11-16"
}
```
- **Alternative Request Body (Direct Manual Rows):**
```json
{
  "openingDate": "2026-11-16",
  "rows": [
    {
      "itemCode": "ING-MILK-OT-380G",
      "quantity": "50.000",
      "unitCost": "24000.000000",
      "lotCode": "LOT-202611-01",
      "expiresOn": "2027-11-16"
    },
    {
      "itemCode": "ING-COFFEE-01",
      "quantity": "20.000",
      "unitCost": "150000.000000",
      "lotCode": null,
      "expiresOn": null
    }
  ]
}
```
- **Response `200 OK`:**
```json
{
  "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "openingDate": "2026-11-16",
  "totalItems": 2,
  "totalQuantity": "70.000",
  "totalValuation": "4200000.00",
  "hasErrors": false,
  "previewRows": [
    {
      "rowNumber": 1,
      "itemId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
      "itemCode": "ING-MILK-OT-380G",
      "itemName": "Sữa đặc có đường Ông Thọ đỏ lon 380g",
      "unitCode": "CAN",
      "unitName": "Lon",
      "quantity": "50.000",
      "unitCost": "24000.000000",
      "lineTotal": "1200000.00",
      "calculatedWac": "24000.000000",
      "lotCode": "LOT-202611-01",
      "expiresOn": "2027-11-16",
      "lotTrackingRequired": true,
      "status": "VALID",
      "errors": []
    },
    {
      "rowNumber": 2,
      "itemId": "8b6d5c41-2109-3719-942c-16c0376e50b2",
      "itemCode": "ING-COFFEE-01",
      "itemName": "Cà phê hạt Robusta",
      "unitCode": "KG",
      "unitName": "Kilogram",
      "quantity": "20.000",
      "unitCost": "150000.000000",
      "lineTotal": "3000000.00",
      "calculatedWac": "150000.000000",
      "lotCode": null,
      "expiresOn": null,
      "lotTrackingRequired": false,
      "status": "VALID",
      "errors": []
    }
  ]
}
```

---

### 2.3 Commit Opening Inventory (Post Once & TC-024 Enforcement)
#### `POST /api/v1/branches/{branchId}/inventory/opening/commit`
- **Scope:** `@branchAuth.hasBranchAccess(#branchId, 'OWNER', 'MANAGER')`
- **Header:** `Idempotency-Key: <UUID>`
- **Request Body:**
```json
{
  "importBatchId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
  "openingDate": "2026-11-16",
  "note": "Khai trương đầu kỳ chi nhánh Q1"
}
```
- **Business Behavior (TC-024):**
  1. Checks if `mtms.opening_inventory_records` already exists for this `branch_id`.
  2. If already committed:
     - If the same `Idempotency-Key` is used: return existing snapshot with header `Idempotent-Replayed: true`.
     - Otherwise, reject with `409 Conflict` (`urn:mtms:error:opening-inventory-already-posted`).
  3. Validates that all items belong to the branch's business entity and lot tracking rules are satisfied.
  4. In a single atomic transaction:
     - Creates `mtms.opening_inventory_records`.
     - Inserts `mtms.stock_balances` (`avg_unit_cost = unitCost`, `inventory_value = qty * cost`).
     - Inserts `mtms.stock_lots` (if lot tracking enabled).
     - Inserts `mtms.stock_movements` (`movement_type = 'OPENING_BALANCE'`).
     - Updates `mtms.import_batches` status to `COMMITTED` (if batch provided).
- **Response `201 Created`:**
```json
{
  "id": "e0b8219c-95a3-4882-965a-8b839dfec241",
  "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "openingDate": "2026-11-16",
  "totalItems": 2,
  "totalQuantity": "70.000",
  "totalValue": "4200000.00",
  "status": "COMMITTED",
  "approvedBy": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "approvedAt": "2026-11-16T08:00:00Z",
  "note": "Khai trương đầu kỳ chi nhánh Q1"
}
```

---

### 2.4 Branch Stock Balances Read API
#### `GET /api/v1/branches/{branchId}/inventory/balances`
- **Scope:** `@branchAuth.hasBranchAccess(#branchId)`
- **Query Params:** `search`, `page`, `size`
- **Response `200 OK`:**
```json
{
  "items": [
    {
      "id": "11223344-5566-7788-99aa-bbccddeeff00",
      "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "itemId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
      "itemCode": "ING-MILK-OT-380G",
      "itemName": "Sữa đặc có đường Ông Thọ đỏ lon 380g",
      "unitCode": "CAN",
      "unitName": "Lon",
      "quantity": "50.000",
      "avgUnitCost": "24000.000000",
      "inventoryValue": "1200000.00",
      "updatedAt": "2026-11-16T08:00:00Z"
    }
  ],
  "totalItems": 1,
  "totalBranchValue": "1200000.00"
}
```

---

## 3. Strict RFC 7807 Error Code Catalog

| HTTP Status | `type` URI | `title` | Description |
| :--- | :--- | :--- | :--- |
| `409 Conflict` | `urn:mtms:error:opening-inventory-already-posted` | Opening Already Posted | Branch opening inventory has already been posted and locked (`TC-024`) |
| `400 Bad Request` | `urn:mtms:error:invalid-stock-quantity` | Invalid Stock Quantity | Quantity <= 0 or invalid decimal format |
| `400 Bad Request` | `urn:mtms:error:missing-lot-code` | Missing Lot Code | Item requires lot tracking but no lot code was provided |
| `404 Not Found` | `urn:mtms:error:item-not-found` | Item Not Found | Item code in import row does not exist in catalog |
| `404 Not Found` | `urn:mtms:error:batch-not-found` | Batch Not Found | Staged import batch does not exist or wrong branch |
| `403 Forbidden` | `urn:mtms:error:forbidden` | Access Forbidden | User does not have branch permissions |
