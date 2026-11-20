# API & Data Contract — P0-INV-03: Supplier & Goods Receipt

- **Task ID:** P0-INV-03
- **Related FE:** FE-P0-26 (Supplier receipt form)
- **Requirements:** BR-INV-05 (Direct receive without PO, actual qty, price, discrepancy, and payable link)
- **Test Cases:** TC-008 (Unpaid 5m goods receipt -> Inventory + payable, cash unchanged, COGS unchanged)
- **Dependencies:** P0-INV-01 (Catalog items, UOM, Pack conversions), P0-INV-02 (Stock balances, Lots, Movements)
- **Status:** PROPOSED (Pending Owner Plan Approval)

---

## 1. Domain Entities & Database Schema (Flyway V010)

### 1.1 `mtms.payables` (Branch Supplier Debt / Payables Ledger)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Payable ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `branch_id` | UUID | NOT NULL REFERENCES mtms.branches(id) | Branch scope |
| `counterparty_label` | VARCHAR(200) | NOT NULL | Supplier display name (e.g. "Vinamilk") |
| `source_type` | VARCHAR(48) | NOT NULL | E.g. `PURCHASE_RECEIPT` |
| `source_id` | UUID | NULLABLE | Source transaction ID |
| `incurred_date` | DATE | NOT NULL | Debt date |
| `due_date` | DATE | NULLABLE | Payment due date |
| `original_amount` | NUMERIC(20,2) | NOT NULL CHECK (original_amount >= 0) | Total payable debt |
| `paid_amount` | NUMERIC(20,2) | NOT NULL DEFAULT 0 CHECK (paid_amount >= 0) | Total amount paid so far |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','SETTLED','DISPUTED','CANCELLED')) | Payable status |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |
| **Check Invariant** | | `CHECK (paid_amount <= original_amount)` | Paid amount cannot exceed original debt |

### 1.2 `mtms.purchase_receipts` (Goods Receipts from Suppliers)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Receipt ID |
| `business_entity_id` | UUID | NOT NULL REFERENCES mtms.business_entities(id) | Entity scope |
| `branch_id` | UUID | NOT NULL REFERENCES mtms.branches(id) | Branch scope |
| `receipt_number` | VARCHAR(60) | NOT NULL | E.g. `PNK-202611-0001` |
| `supplier_id` | UUID | NULLABLE REFERENCES mtms.suppliers(id) | Supplier |
| `received_on` | DATE | NOT NULL | Date goods arrived |
| `invoice_number` | VARCHAR(120) | NULLABLE | Supplier invoice reference |
| `invoice_total` | NUMERIC(20,2) | NOT NULL DEFAULT 0 CHECK (invoice_total >= 0) | Total goods value |
| `payment_status` | VARCHAR(24) | NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID','PARTIALLY_PAID','PAID')) | Payment mode |
| `payable_id` | UUID | NULLABLE REFERENCES mtms.payables(id) | Linked payable debt record |
| `status` | VARCHAR(20) | NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','POSTED','VOIDED')) | Receipt status |
| `notes` | TEXT | NULLABLE | Receiving notes / discrepancy |
| `posted_by` | UUID | NULLABLE REFERENCES mtms.users(id) | User who finalized receipt |
| `posted_at` | TIMESTAMPTZ | NULLABLE | Posting timestamp |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() | Created timestamp |
| **Unique Index** | | `UNIQUE (branch_id, receipt_number)` | Unique receipt code per branch |

### 1.3 `mtms.purchase_receipt_lines` (Receipt Items Detail)
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY | Unique Line ID |
| `receipt_id` | UUID | NOT NULL REFERENCES mtms.purchase_receipts(id) ON DELETE CASCADE | Receipt reference |
| `line_no` | INTEGER | NOT NULL CHECK (line_no > 0) | Sequential line number |
| `item_id` | UUID | NOT NULL REFERENCES mtms.catalog_items(id) | Received catalog item |
| `pack_name` | VARCHAR(100) | NULLABLE | E.g. "Thùng 24 lon" |
| `declared_pack_quantity` | NUMERIC(18,3) | NULLABLE CHECK (declared_pack_quantity > 0) | Input pack quantity |
| `conversion_snapshot` | NUMERIC(18,6) | NOT NULL DEFAULT 1 CHECK (conversion_snapshot > 0) | Conversion multiplier snapshot |
| `received_base_quantity` | NUMERIC(18,3) | NOT NULL CHECK (received_base_quantity > 0) | Converted base units (lon/kg) |
| `base_unit_cost` | NUMERIC(20,6) | NOT NULL CHECK (base_unit_cost >= 0) | Cost per base unit |
| `line_total` | NUMERIC(20,2) | NOT NULL CHECK (line_total >= 0) | `received_base_quantity * base_unit_cost` |
| `lot_code` | VARCHAR(80) | NULLABLE | Lot code (required if lot tracked) |
| `expires_on` | DATE | NULLABLE | Expiry date |
| **Unique Index** | | `UNIQUE (receipt_id, line_no)` | Unique line order per receipt |

---

## 2. REST Endpoints Handshake

### 2.1 List Purchase Receipts
#### `GET /api/v1/branches/{branchId}/inventory/receipts`
- **Scope:** `@branchAuth.hasBranchAccess(#branchId)`
- **Query Params:** `status`, `supplierId`, `startDate`, `endDate`, `search`, `page`, `size`
- **Response `200 OK`:**
```json
{
  "items": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "receiptNumber": "PNK-202611-0001",
      "supplierId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
      "supplierName": "Công ty Cổ phần Sữa Việt Nam (Vinamilk)",
      "receivedOn": "2026-11-16",
      "invoiceNumber": "HD-VNM-9988",
      "invoiceTotal": "5000000.00",
      "paymentStatus": "UNPAID",
      "payableId": "11223344-5566-7788-99aa-bbccddeeff00",
      "status": "POSTED",
      "totalLines": 2,
      "postedByName": "Quản lý Chi nhánh Q1",
      "postedAt": "2026-11-16T09:30:00Z"
    }
  ],
  "totalItems": 1
}
```

---

### 2.2 Create Purchase Receipt (Direct Receive or Draft)
#### `POST /api/v1/branches/{branchId}/inventory/receipts`
- **Scope:** `@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')`
- **Header:** `Idempotency-Key: <UUID>`
- **Request Body:**
```json
{
  "supplierId": "9a7e6b52-3210-482a-a53d-27d1487f61c3",
  "receivedOn": "2026-11-16",
  "invoiceNumber": "HD-VNM-9988",
  "paymentStatus": "UNPAID",
  "dueDate": "2026-12-16",
  "notes": "Nhập hàng đợt 1 tháng 11 - nợ 30 ngày",
  "autoPost": true,
  "lines": [
    {
      "itemId": "e0b8219c-95a3-4882-965a-8b839dfec241",
      "packName": "Thùng 24 lon",
      "declaredPackQuantity": "5.000",
      "conversionSnapshot": "24.000",
      "receivedBaseQuantity": "120.000",
      "baseUnitCost": "25000.000000",
      "lineTotal": "3000000.00",
      "lotCode": "LOT-202611-VNM",
      "expiresOn": "2027-11-16"
    },
    {
      "itemId": "8b6d5c41-2109-3719-942c-16c0376e50b2",
      "packName": null,
      "declaredPackQuantity": null,
      "conversionSnapshot": "1.000",
      "receivedBaseQuantity": "10.000",
      "baseUnitCost": "200000.000000",
      "lineTotal": "2000000.00",
      "lotCode": null,
      "expiresOn": null
    }
  ]
}
```
- **Business Behavior (TC-008 Invariant Enforcement):**
  1. Validates that all items exist and belong to the branch's business entity.
  2. If an item requires lot tracking (`lot_tracking_required = true`), `lotCode` cannot be blank.
  3. When `autoPost = true` (or when posted):
     - Calculates `invoiceTotal = sum(lineTotal)` = `5,000,000.00`.
     - Atomically updates `mtms.stock_balances` (increases quantity & recalculates Moving WAC).
     - If lot tracking enabled: increases `mtms.stock_lots`.
     - Logs `mtms.stock_movements` with `movement_type = 'PURCHASE_RECEIPT'`.
     - If `paymentStatus = 'UNPAID'`: creates `mtms.payables` with `original_amount = 5,000,000.00`, `paid_amount = 0`, `status = 'OPEN'`.
     - **TC-008 Verification:** Cash/bank balance is NOT changed. COGS expense is NOT created.
- **Response `201 Created`** with full `PurchaseReceiptDto`.

---

### 2.3 Post Existing Draft Receipt
#### `POST /api/v1/branches/{branchId}/inventory/receipts/{receiptId}/post`
- **Scope:** `@branchAuth.hasBranchAccess(#branchId, 'MANAGER', 'OWNER')`
- **Header:** `Idempotency-Key: <UUID>`
- **Response `200 OK`** with updated status `POSTED` and generated `payableId`.

---

### 2.4 Branch Payables Ledger API (TC-008 / TC-009 Debt Inspection)
#### `GET /api/v1/branches/{branchId}/inventory/payables`
- **Scope:** `@branchAuth.hasBranchAccess(#branchId)`
- **Query Params:** `status`, `search`, `page`, `size`
- **Response `200 OK`:**
```json
{
  "items": [
    {
      "id": "11223344-5566-7788-99aa-bbccddeeff00",
      "branchId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "counterpartyLabel": "Công ty Cổ phần Sữa Việt Nam (Vinamilk)",
      "sourceType": "PURCHASE_RECEIPT",
      "sourceId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "incurredDate": "2026-11-16",
      "dueDate": "2026-12-16",
      "originalAmount": "5000000.00",
      "paidAmount": "0.00",
      "remainingAmount": "5000000.00",
      "status": "OPEN",
      "createdAt": "2026-11-16T09:30:00Z"
    }
  ],
  "totalItems": 1,
  "totalOutstandingDebt": "5000000.00"
}
```

---

## 3. Strict RFC 7807 Error Code Catalog

| HTTP Status | `type` URI | `title` | Description |
| :--- | :--- | :--- | :--- |
| `400 Bad Request` | `urn:mtms:error:missing-lot-code` | Missing Lot Code | Received item requires lot tracking but no lot code provided |
| `400 Bad Request` | `urn:mtms:error:invalid-receipt-quantity` | Invalid Receipt Quantity | Received quantity must be strictly positive |
| `409 Conflict` | `urn:mtms:error:receipt-already-posted` | Receipt Already Posted | Receipt has already been posted and locked |
| `404 Not Found` | `urn:mtms:error:receipt-not-found` | Receipt Not Found | Receipt ID not found in branch |
| `404 Not Found` | `urn:mtms:error:supplier-not-found` | Supplier Not Found | Supplier ID not found |
| `403 Forbidden` | `urn:mtms:error:forbidden` | Access Forbidden | User does not have branch permissions |
