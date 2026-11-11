# Cách bổ sung file ở những task tiếp theo

**Chỉ có một repo `mtms/`**, không có thư mục `phase1/`, `phase2/` hoặc ZIP dự án mới mỗi lần. Những mã `P0-...`, `P1-...` trong Tracker vẫn giữ nguyên vì đó là mã nhiệm vụ gốc.

## Quy tắc bổ sung
1. Mỗi lần thực hiện một task, luôn mở repo hiện có; không giải nén một bản workspace khác đè lên repo đang phát triển.
2. Danh sách thay đổi phải ghi rõ `ADD` (file mới), `MODIFY` (chỉnh file có sẵn), `KEEP` (không đụng), `DELETE` (chỉ khi có phê duyệt).
3. `ADD`: tạo đúng đường dẫn tương đối từ root, ví dụ `backend/src/main/.../Branch.java`.
4. `MODIFY`: kiểm tra diff và backup trước; tuyệt đối không ghi đè `.env`, migration đã chạy, code người dùng đã viết hoặc `.git`.
5. Chỉ sửa file tracker `docs/planning/MTMS_Development_Tracker.xlsx` sau khi task có bằng chứng nghiệm thu.
6. Luôn cập nhật `docs/planning/CURRENT_STATE.md` và `docs/ai/CHANGELOG.md` với những thay đổi được chấp nhận.

## Ví dụ các đầu việc tương lai (chưa được phép thực thi trước G0)
| Task / nhóm | File dự kiến thêm/chỉnh | Chỉ dẫn |
|---|---|---|
| `P0-ORG-01` | `backend/pom.xml`, `backend/src/...`, `frontend/package.json`, `frontend/src/...`, `.github/workflows/...` | Tạo project ngay trong `backend/`, `frontend/`, không tạo `backend/backend/` |
| `P0-ORG-02` | `infra/docker/compose.yml`, `database/migrations/V001__...sql`, cấu hình Flyway | Chỉ sau khi task trước nghiệm thu và G0 đã ký |
| Auth/RBAC | `backend/src/.../identity/`, các FE pages phù hợp, API contract, tests | Bắt buộc test quyền theo chi nhánh |
| Revenue/Finance | Module revenue/finance, API/DB/FE cụ thể | Không cộng `delivery_net` vào tiền ngân hàng tự động |
| Inventory/Payroll | Modules, migrations kế tiếp, giao diện/QA tương ứng | Khóa dữ liệu, history, BigDecimal và tests |
| V1 payouts | Capital flow, payment attempts, tests và ADR | Không triển khai/chi trả thật trước G5 chính sách xác minh |

## Thư mục chuẩn cần dùng
- Java source → `backend/src/main/java/com/mtms/...`
- Java tests → `backend/src/test/java/com/mtms/...`
- Java resource/migrations (quyết định chính xác nơi Flyway đọc ở P0-ORG-02) → `backend/src/main/resources/` và `database/migrations/`; tránh có hai nguồn migration đồng thời.
- React source → `frontend/src/`
- HTTP/API contracts → `docs/api/`
- ADR → `docs/decisions/`
- Work plans + tracking → `docs/planning/`
- E2E/contract tests → `tests/`
- Runbooks / deployment → `infra/` và `docs/operations/`

## Mẫu mô tả thay đổi trong lần tiếp theo
```text
Task: P0-ORG-01
ADD:
- backend/pom.xml
- backend/src/main/java/com/mtms/MtmsApplication.java
- frontend/package.json
- frontend/src/main.tsx
MODIFY:
- README.md (hướng dẫn chạy)
- docs/planning/CURRENT_STATE.md (sau nghiệm thu)
KEEP:
- docs/business/*
- docs/planning/MTMS_Development_Tracker.xlsx (trước nghiệm thu)
DELETE: none
Test evidence: NOT RUN / kết quả thực tế có link
```

**Lưu ý:** các ví dụ bên trên không xác nhận rằng file đã được tạo. Chỉ danh sách trong `docs/ai/CHANGELOG.md` mới ghi nhận thay đổi thực tế.
