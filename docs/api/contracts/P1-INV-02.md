# MTMS API Contract: P1-INV-02 & FE-P1-10 — Physical Stocktaking & Discrepancy Approval

**Quy chuẩn áp dụng:** `BR-INV-07`, `TC-026`, `TC-021`, `ADR-011`, RFC 7807 ProblemDetail.

---

## 1. POST /api/v1/branches/{branchId}/stock-counts
**Mục đích:** Tạo đợt kiểm kê kho tại chi nhánh (Định kỳ hoặc Đột xuất).
- Tự động nạp toàn bộ danh mục mặt hàng có số dư tồn kho tại chi nhánh (hoặc danh sách chỉ định).
- Snapshot số lượng tồn sổ sách hiện tại (`expected_qty = balance.onHandQty`) và đơn giá Moving WAC (`snapshot_unit_cost = balance.avgUnitCost`).
- Khởi tạo `counted_qty = expected_qty`, `variance_qty = 0`, trạng thái `DRAFT`.
- **Rào cản TC-026:** Trạng thái `DRAFT` không làm thay đổi tồn kho hay sổ bút toán.

### Request Body
```json
{
  "countedOn": "2026-11-20",
  "countType": "PERIODIC",
  "notes": "Kiểm kê định kỳ cuối tuần chi nhánh Quận 1",
  "itemIds": null
}
```

### Response 201 Created (`StockCountDetailDto`)
```json
{
  "id": "11111111-2222-3333-4444-555555555555",
  "countNumber": "KK-202611-0001",
  "businessEntityId": "aaaa1111-2222-3333-4444-555555555555",
  "branchId": "bbbb1111-2222-3333-4444-555555555555",
  "branchName": "Chi nhánh Quận 1",
  "countedOn": "2026-11-20",
  "status": "DRAFT",
  "countType": "PERIODIC",
  "totalItems": 2,
  "totalSurplusQty": 0.000,
  "totalShortageQty": 0.000,
  "totalSurplusValue": 0.00,
  "totalShortageValue": 0.00,
  "netVarianceValue": 0.00,
  "notes": "Kiểm kê định kỳ cuối tuần chi nhánh Quận 1",
  "createdBy": "user-uuid",
  "createdByName": "Quản lý Cửa hàng",
  "submittedBy": null,
  "submittedByName": null,
  "submittedAt": null,
  "approvedBy": null,
  "approvedByName": null,
  "postedAt": null,
  "lines": [
    {
      "id": "line-uuid-1",
      "lineNumber": 1,
      "itemId": "item-uuid-1",
      "itemCode": "ING-CF-01",
      "itemName": "Cà phê hạt Robusta",
      "baseUnit": "KG",
      "lotId": null,
      "expectedQty": 50.000,
      "countedQty": 50.000,
      "varianceQty": 0.000,
      "snapshotUnitCost": 100000.000000,
      "varianceValue": 0.00,
      "discrepancyReason": null,
      "notes": null
    }
  ]
}
```

---

## 2. PUT /api/v1/branches/{branchId}/stock-counts/{countId}
**Mục đích:** Cập nhật số lượng đếm thực tế và lý do giải trình khi phiếu đang ở trạng thái `DRAFT` hoặc `SUBMITTED`.
- Nếu `countedQty != expectedQty`, bắt buộc phải có `discrepancyReason`.

### Request Body
```json
{
  "notes": "Đã hoàn thành kiểm đếm thực tế quầy pha chế và kho mát",
  "lines": [
    {
      "lineId": "line-uuid-1",
      "countedQty": 48.000,
      "discrepancyReason": "Hao hụt bay hơi và rơi vãi trong quá trình xay pha chế",
      "notes": null
    }
  ]
}
```

### Response 200 OK: Trả về `StockCountDetailDto` với các chỉ số chênh lệch được tính toán lại.

---

## 3. POST /api/v1/branches/{branchId}/stock-counts/{countId}/submit
**Mục đích:** Nộp đợt kiểm kê chờ Chủ sở hữu (Owner) duyệt.
- Trạng thái chuyển thành `SUBMITTED`.
- **TC-026 Invariant:** Vẫn chưa có bất kỳ thay đổi nào trên tồn kho hay sổ bút toán.

---

## 4. POST /api/v1/branches/{branchId}/stock-counts/{countId}/approve
**Mục đích:** Chủ sở hữu (Owner) phê duyệt đợt kiểm kê và hạch toán điều chỉnh sổ kho.
- **Rào cản TC-026:** Đây là bước DUY NHẤT được phép tác động vào `stock_balances` và tạo `stock_movements`.
- Thực hiện khóa pessimistic write lock trên từng dòng `stock_balances`:
  - `varianceQty > 0` (Thừa kho): Tăng số lượng và giá trị tồn kho; tạo bút toán `StockMovement` loại `ADJUSTMENT` với số lượng dương `+varianceQty`.
  - `varianceQty < 0` (Thiếu kho): Giảm số lượng và giá trị tồn kho; tạo bút toán `StockMovement` loại `ADJUSTMENT` với số lượng âm `-varianceQty`; giữ nguyên đơn giá Moving WAC.
  - **TC-021 (Chặn âm kho):** Nếu `onHandQty < |varianceQty|`, từ chối phê duyệt với 400 Bad Request (`urn:mtms:error:insufficient-stock`).
- Cập nhật trạng thái phiếu thành `POSTED`, lưu `approved_by` và `posted_at`. Phiếu đóng băng vĩnh viễn (immutable).

### Response 200 OK: Trả về `StockCountDetailDto` với `status: "POSTED"`.

---

## 5. POST /api/v1/branches/{branchId}/stock-counts/{countId}/cancel
**Mục đích:** Hủy đợt kiểm kê chưa duyệt (`DRAFT` hoặc `SUBMITTED`).
- Không tác động tồn kho, chuyển trạng thái thành `CANCELLED`.

---

## 6. GET /api/v1/branches/{branchId}/stock-counts
**Mục đích:** Danh sách đợt kiểm kê tại chi nhánh.
- **Query Params:** `status`, `countType`, `startDate`, `endDate`, `search`, `page`, `size`.

---

## 7. GET /api/v1/branches/{branchId}/stock-counts/{countId}
**Mục đích:** Xem chi tiết đầy đủ một đợt kiểm kê và toàn bộ danh sách mặt hàng.

---

## 8. GET /api/v1/branches/{branchId}/stock-counts/summary
**Mục đích:** 4 thẻ KPI tóm tắt tình hình kiểm kê của chi nhánh.

### Response 200 OK (`StockCountSummaryDto`)
```json
{
  "totalCounts": 12,
  "pendingApprovalCount": 1,
  "totalSurplusValue": 150000.00,
  "totalShortageValue": 450000.00,
  "netVarianceValue": -300000.00
}
```

---

## 9. Bảng Mã Lỗi RFC 7807 ProblemDetail

| HTTP Status | Error Type URI | Ý nghĩa |
|---|---|---|
| `400 Bad Request` | `urn:mtms:error:stock-count-invalid-status` | Thao tác không hợp lệ với trạng thái hiện tại của phiếu kiểm kê |
| `400 Bad Request` | `urn:mtms:error:missing-discrepancy-reason` | Phát hiện chênh lệch nhưng thiếu lý do giải trình bắt buộc |
| `400 Bad Request` | `urn:mtms:error:insufficient-stock` | **TC-021:** Tồn kho hiện tại không đủ để trừ lượng thiếu kho |
| `400 Bad Request` | `urn:mtms:error:stock-count-immutable` | Phiếu đã chốt POSTED hoặc CANCELLED, cấm sửa đổi |
| `403 Forbidden` | `urn:mtms:error:forbidden` | **TC-001:** Người dùng không có quyền truy cập chi nhánh |
| `404 Not Found` | `urn:mtms:error:stock-count-not-found` | Không tìm thấy đợt kiểm kê |
