# MTMS Multi-Agent Workflow Protocol (Quy trình Điều phối Đa Agent)

Tài liệu này quy định chuẩn mực tiếp nhận, phân loại và triển khai công việc giữa **User (Project Owner)** và **Dàn 6 AI Agent** trong dự án `mtms`.

---

## 1. Nguyên tắc Phân loại Yêu cầu (Request Triage)

Mỗi khi nhận yêu cầu từ User, **TechLead (`mtms-tech-lead`)** là đầu mối duy nhất tiếp nhận và bắt buộc phải phân loại thành một trong hai nhánh:

### Nhánh A: Kỹ thuật thuần túy (Fast-Track / Technical Patch)
* **Tiêu chí:**
  - Sửa lỗi cú pháp, typing, null-pointer check.
  - Sửa style giao diện CSS/HTML, đổi text tĩnh, căn chỉnh responsive.
  - Tối ưu hóa code nội bộ, refactoring, thêm comment, tối ưu index SQL.
  - **ĐIỀU KIỆN TIÊN QUYẾT:** Tuyệt đối không thay đổi logic kinh doanh, luồng tính tiền, công thức kho hay quyền chi nhánh.
* **Quy trình xử lý:**
  1. TechLead chỉ định ngay danh sách file được sửa (`target_files`) và phân tier model:
     - Dễ/Boilerplate -> Giao cho Junior Dev / Model thấp (`gemini-flash` hoặc `gpt-4o-mini`).
     - Tối ưu thuật toán/Logic khó -> Giao cho Senior Dev / Model cao (`gpt-4o`).
  2. Dev thực hiện sửa mã nguồn.
  3. QA Reviewer chạy lệnh build/test kiểm tra regression.
  4. Báo cáo hoàn thành. *(Không cần thông qua BA)*.

---

### Nhánh B: Thay đổi luồng / Tính năng mới (Full-SDLC Track)
* **Tiêu chí:**
  - Thêm tính năng mới, tạo màn hình nghiệp vụ mới.
  - Thay đổi luồng xử lý (ví dụ: luồng duyệt chi phí, luồng ghi nhận doanh thu).
  - Thay đổi công thức tính toán tài chính, chiết khấu, kho (WAC) hoặc phân quyền.
* **Quy trình xử lý 5 bước chuẩn chỉ:**

```text
[User Yêu Cầu]
      ↓
[1. TechLead Clarification Gate] (Đặt 2-3 câu hỏi cốt lõi để chốt phạm vi)
      ↓
[2. BA Deep Analysis Gate] (Phân tích edge-cases, so sánh phương án, chốt Acceptance Criteria)
      ↓
[3. Architect Contract Gate] (Chốt DB Schema, API DTO, Module boundary)
      ↓
[4. TechLead Task Breakdown] (Phân rã sub-tasks, gán model tier, chỉ định target_files)
      ↓
[5. Devs Implementation] (Backend & Frontend code đúng theo AC)
      ↓
[6. QA Independent Verification] (Đối chiếu AC, chạy test thực tế trên terminal)
```

---

## 2. Khung câu hỏi phỏng vấn của TechLead (Clarification Framework)

Khi yêu cầu thuộc **Nhánh B**, TechLead **CHƯA ĐƯỢC GIAO VIỆC NGAY** mà phải gửi lại User 2 - 3 câu hỏi thuộc khung câu hỏi sau để làm rõ ý đồ đại khái:

1. **Về phạm vi (Scope):**
   > *"Tính năng/luồng này áp dụng cho toàn hệ thống hay chỉ cho chi nhánh cụ thể / đối tượng người dùng nào?"*
2. **Về xử lý ngoại lệ (Edge Case):**
   > *"Khi gặp tình huống bất thường (ví dụ: tồn kho không đủ, mạng lỗi, số tiền âm), hệ thống nên chặn lại ngay hay cho phép lưu tạm/báo lỗi?"*
3. **Về tính tương thích dữ liệu cũ (Backward Compatibility):**
   > *"Dữ liệu/đơn hàng đã phát sinh trước thời điểm này có cần chạy script chuyển đổi theo luồng mới hay giữ nguyên trạng?"*

Sau khi User phản hồi, TechLead mới đóng gói toàn bộ bối cảnh và chuyển cho BA.

---

## 3. Tiêu chuẩn bàn giao giữa các vai trò (Handover Standards)

### A. Từ BA sang TechLead & Dev (Acceptance Criteria - AC)
BA bắt buộc phải xuất Acceptance Criteria theo định dạng **Gherkin (Given - When - Then)**:
```markdown
### Kịch bản: [Tên kịch bản]
- **GIVEN (Điều kiện ban đầu):** Chi nhánh X có tồn kho sản phẩm Y là 10.
- **WHEN (Hành động kích hoạt):** Người dùng yêu cầu xuất kho số lượng 12.
- **THEN (Kết quả mong đợi):** Hệ thống từ chối xuất, trả về lỗi INSUFFICIENT_STOCK và không ghi nhận phiếu xuất.
```

### B. Từ Architect sang Dev (Technical Contract)
Architect chuẩn hóa giao diện kỹ thuật trước khi Dev viết code:
- Endpoint: `POST /api/v1/inventory/transfers`
- Request / Response DTO schema (TypeScript interface hoặc Java Record).
- Danh sách migration table / columns cần thêm (nếu có).

### C. Từ TechLead sang Dev (Task Ticket)
TechLead cung cấp thông tin nhiệm vụ rõ ràng:
- **Task ID:** `TASK-xxx`
- **Model Tier:** `high_tier_model` (Pro/Sonnet) hoặc `low_tier_model` (Flash/4o-mini).
- **Target Files:** Danh sách file cụ thể được phép sửa.
- **Reference Spec:** Link đến tài liệu AC của BA.

### D. Từ QA sang TechLead & User (Verification Report)
QA Reviewer chỉ nghiệm thu khi có bằng chứng chạy lệnh thực tế:
- **Command:** `mvn test` hoặc `npm test` hoặc `pytest`.
- **Status:** PASS / FAIL (kèm log lỗi nếu có).
- **Defects:** Danh sách lỗi phân theo mức độ (Blocker, Critical, Major, Minor).

---

## 4. Nguyên tắc Vàng: Không vượt quyền (Anti-Scope-Creep)

1. **Dev:** Chỉ code theo AC của BA và Schema của Architect. Thấy nghiệp vụ bất hợp lý -> **báo cáo lại**, tuyệt đối không tự sửa cách tính. Không tự đóng vai QA.
2. **BA:** Tập trung nghiệp vụ và AC. Không thiết kế schema DB, không viết code.
3. **Architect:** Thiết kế kiến trúc kỹ thuật. Không tự ý thay đổi quyết định nghiệp vụ của BA.
4. **QA:** Độc lập kiểm thử. Không tự sửa code ứng dụng của Dev.
5. **TechLead:** Điều phối và gán model tier. Không tự ý code thay Dev, không tự duyệt release khi chưa có kết quả QA.
