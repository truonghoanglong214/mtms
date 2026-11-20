# MTMS API & Data Contract — P0-AST-01: Equipment Baseline Register

**Task References:**
- `P0-AST-01`: Equipment baseline register (`BR-AST-01`, `TC-051`, `FE-P0-30`, `ADR-023`)

**Frontend References:** `FE-P0-30` (Equipment opening register)  
**Dependencies:** `P0-ORG-03` (Entity and branch domain), `P0-ORG-05` (Scoped permissions), `P0-SEC-01` (Evidence storage)  
**Status:** APPROVED & READY FOR IMPLEMENTATION  
**Author:** MTMS Architect

---

## 1. Domain Model & Business Invariants

### 1.1 Business Invariants (`BR-AST-01`, `TC-051`, `ADR-023`)
1. **Sổ Quản Lý Thiết Bị Cơ Sở (`P0-AST-01` / `BR-AST-01`):**
   - Đăng ký và quản lý tài sản, trang thiết bị máy móc tại từng chi nhánh hoạt động.
   - Mỗi tài sản có mã nhận diện duy nhất (`asset_code` UNIQUE) trên toàn hệ thống (ví dụ: `AST-B01-CF001`).
   - Ghi nhận đầy đủ thông tin: tên thiết bị (`name`), phân loại nhóm (`category`), số serial (`serial_number`), model (`model`), ngày mua sắm (`purchased_on`), nguyên giá ban đầu (`acquisition_cost` $\ge 0$), hạn bảo hành (`warranty_until`), vị trí đặt tại quán (`location_in_branch`), nhà cung cấp (`supplier_id`), ghi chú bảo trì (`maintenance_note`), và chứng từ đính kèm (`evidence_file_id`).
   - Trạng thái tài sản (`status`): `IN_USE` (Đang sử dụng), `IN_REPAIR` (Đang sửa chữa/bảo hành), `STORED` (Lưu kho/tạm ngừng sử dụng), `DISPOSED` (Đã thanh lý/hủy), `TRANSFERRED` (Đã điều chuyển).

2. **Ranh giới Bất Biến Khấu Hao P0 (`ADR-023` & `TC-051`):**
   - **RANH GIỚI BẮT BUỘC:** Giai đoạn P0 ghi nhận tài sản cơ sở phục vụ quản lý hiện vật, kiểm kê và bảo hành, nhưng **P0 P&L rõ ràng loại trừ chi phí khấu hao** (*P0 P&L discloses exclusion of depreciation*).
   - Hệ thống **TUYỆT ĐỐI KHÔNG** tự ý sinh bút toán chi phí khấu hao hàng tháng vào báo cáo kết quả kinh doanh P&L tạm tính của P0.
   - API tóm tắt (`GET /api/v1/branches/{branchId}/assets/summary`) và giao diện `FE-P0-30` bắt buộc trả về và hiển thị thông điệp cam kết chuẩn `TC-051`:
     `"P0 P&L excludes depreciation (TC-051 / ADR-023). Chi phí khấu hao chưa được trích vào P&L tạm tính giai đoạn MVP."`
   - Nghiệp vụ trích khấu hao tự động theo thời gian sử dụng (`TC-054`) thuộc Phase 1 (`P1-AST-02`) sau khi có chính sách kế toán được phê duyệt.

3. **Phân Quyền Theo Chi Nhánh (`BR-ORG-04` / `TC-001`):**
   - Chỉ người dùng có quyền hợp lệ trên chi nhánh (`BRANCH_MANAGER`, `BRANCH_OWNER`, `SYSTEM_ADMIN`) mới được xem và quản lý tài sản của chi nhánh đó.
   - Truy cập chéo chi nhánh không được cấp phép sẽ bị chặn với mã lỗi HTTP 403 Forbidden RFC 7807 (`urn:mtms:error:forbidden`).

---

## 2. Database Schema (Flyway `V024`)

```sql
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
```

---

## 3. REST API Specification

### 3.1 `GET /api/v1/branches/{branchId}/assets`
Lấy danh sách thiết bị/tài sản của chi nhánh có hỗ trợ phân trang, lọc theo danh mục, trạng thái và tìm kiếm.

**Query Parameters:**
- `category` (optional): `AssetCategory` (`COFFEE_MACHINE`, `REFRIGERATION`, `SEALING_MACHINE`, `BLENDER`, `POS_HARDWARE`, `FURNITURE`, `KITCHEN_EQUIPMENT`, `OTHER`)
- `status` (optional): `AssetStatus` (`IN_USE`, `IN_REPAIR`, `STORED`, `DISPOSED`, `TRANSFERRED`)
- `search` (optional): Tìm kiếm theo mã tài sản, tên thiết bị, model hoặc số serial.
- `page` (optional, default: 0): Số trang.
- `size` (optional, default: 20): Số lượng dòng/trang.

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "e00e8400-e29b-41d4-a716-446655440001",
      "branchId": "b00e8400-e29b-41d4-a716-446655440001",
      "branchName": "Chi nhánh Quận 1",
      "assetCode": "AST-B01-CF001",
      "name": "Máy pha cà phê Nuova Simonelli Appia II",
      "category": "COFFEE_MACHINE",
      "serialNumber": "NS-2026-99881",
      "model": "Appia II 2 Groups",
      "purchasedOn": "2026-01-15",
      "acquisitionCost": 85000000.00,
      "warrantyUntil": "2027-01-15",
      "status": "IN_USE",
      "locationInBranch": "Quầy pha chế chính",
      "supplierId": "s00e8400-e29b-41d4-a716-446655440001",
      "supplierName": "Công ty TNHH Thiết Bị F&B Việt",
      "evidenceFileId": "f00e8400-e29b-41d4-a716-446655440001",
      "maintenanceNote": "Bảo dưỡng định kỳ 3 tháng/lần",
      "createdAt": "2026-11-20T10:00:00Z",
      "updatedAt": "2026-11-20T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 3.2 `GET /api/v1/branches/{branchId}/assets/summary`
Lấy tổng quan KPI tài sản và thông báo cam kết loại trừ khấu hao P0 (`TC-051` / `ADR-023`).

**Response `200 OK`:**
```json
{
  "branchId": "b00e8400-e29b-41d4-a716-446655440001",
  "totalAssets": 15,
  "totalAcquisitionCost": 320000000.00,
  "inUseCount": 14,
  "inRepairCount": 1,
  "storedCount": 0,
  "disposedCount": 0,
  "warrantyExpiringCount": 2,
  "warrantyExpiredCount": 1,
  "categoryBreakdown": {
    "COFFEE_MACHINE": 2,
    "REFRIGERATION": 4,
    "SEALING_MACHINE": 1,
    "BLENDER": 3,
    "POS_HARDWARE": 2,
    "FURNITURE": 2,
    "KITCHEN_EQUIPMENT": 1,
    "OTHER": 0
  },
  "depreciationDisclaimer": "P0 P&L excludes depreciation (TC-051 / ADR-023). Chi phí khấu hao chưa được trích vào P&L tạm tính giai đoạn MVP."
}
```

---

### 3.3 `GET /api/v1/branches/{branchId}/assets/{id}`
Xem chi tiết một tài sản cụ thể.

**Response `200 OK`:** (Đối tượng `EquipmentAssetDto`)

---

### 3.4 `POST /api/v1/branches/{branchId}/assets`
Đăng ký thiết bị mới tại chi nhánh.

**Request Body:**
```json
{
  "assetCode": "AST-B01-CF001",
  "name": "Máy pha cà phê Nuova Simonelli Appia II",
  "category": "COFFEE_MACHINE",
  "serialNumber": "NS-2026-99881",
  "model": "Appia II 2 Groups",
  "purchasedOn": "2026-01-15",
  "acquisitionCost": 85000000.00,
  "warrantyUntil": "2027-01-15",
  "status": "IN_USE",
  "locationInBranch": "Quầy pha chế chính",
  "supplierId": "s00e8400-e29b-41d4-a716-446655440001",
  "evidenceFileId": "f00e8400-e29b-41d4-a716-446655440001",
  "maintenanceNote": "Bảo dưỡng định kỳ 3 tháng/lần"
}
```

**Response `201 Created`:** Đối tượng `EquipmentAssetDto`

---

### 3.5 `PUT /api/v1/branches/{branchId}/assets/{id}`
Cập nhật thông tin tài sản / trạng thái / vị trí / ghi chú.

**Request Body:**
```json
{
  "name": "Máy pha cà phê Nuova Simonelli Appia II (Đã thay van áp)",
  "category": "COFFEE_MACHINE",
  "serialNumber": "NS-2026-99881",
  "model": "Appia II 2 Groups",
  "purchasedOn": "2026-01-15",
  "acquisitionCost": 85000000.00,
  "warrantyUntil": "2027-01-15",
  "status": "IN_REPAIR",
  "locationInBranch": "Quầy pha chế chính",
  "supplierId": "s00e8400-e29b-41d4-a716-446655440001",
  "evidenceFileId": "f00e8400-e29b-41d4-a716-446655440001",
  "maintenanceNote": "Đang gọi kỹ thuật viên hãng bảo dưỡng van hơi"
}
```

**Response `200 OK`:** Đối tượng `EquipmentAssetDto`

---

### 3.6 `DELETE /api/v1/branches/{branchId}/assets/{id}`
Xóa tài sản khỏi danh mục chi nhánh.

**Response `204 No Content`**

---

## 4. RFC 7807 Problem Details

| HTTP Status | Type URI | Title | Detail Ví dụ |
| :--- | :--- | :--- | :--- |
| `400 Bad Request` | `urn:mtms:error:invalid-asset-cost` | Invalid Asset Cost | Nguyên giá tài sản không được là số âm (acquisition_cost >= 0). |
| `404 Not Found` | `urn:mtms:error:asset-not-found` | Asset Not Found | Không tìm thấy thiết bị với ID được cung cấp tại chi nhánh. |
| `409 Conflict` | `urn:mtms:error:asset-code-conflict` | Asset Code Conflict | Mã tài sản 'AST-B01-CF001' đã tồn tại trong hệ thống. |
| `403 Forbidden` | `urn:mtms:error:forbidden` | Access Denied | Người dùng không có quyền truy cập vào tài nguyên của chi nhánh này. |
