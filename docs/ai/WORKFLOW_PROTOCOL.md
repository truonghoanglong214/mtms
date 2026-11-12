# MTMS Multi-Agent Workflow Protocol (Quy trình Điều phối Đa Agent)

Tài liệu này quy định chuẩn mực tiếp nhận, phân loại và triển khai công việc giữa **User (Project Owner)** và **Dàn 6 AI Agent** trong dự án `mtms`.

---

## 1. Vòng lặp Đối thoại 2 Chiều (Bi-Directional Feedback Loop)

Khi User yêu cầu phân tích một luồng nghiệp vụ hoặc tính năng mới, quy trình diễn ra theo đúng chu trình khép kín:

```text
[1. User yêu cầu nghiệp vụ]
          ↓
[2. TechLead tiếp nhận & phỏng vấn sơ bộ 2-3 câu]
          ↓
[3. TechLead chạy lệnh gọi BA:]
    herdr agent prompt mtms-business-analyst "Phân tích và đưa ra các phương án..." --wait
          ↓
[4. BA phân tích sâu, so sánh Trade-off & xuất các Phương án (Option 1, Option 2)]
          ↓
[5. TechLead thu thập kết quả từ BA, tóm tắt súc tích & TRÌNH CHO USER CHỐT]
    "Thưa User, BA đã phân tích xong. Dưới đây là 2 phương án tối ưu: ... Bạn chốt phương án nào?"
          ↓
[6. User phản hồi quyết định chốt]
          ↓
[7. TechLead chuyển kết quả chốt cho Architect lên Contract & giao Dev triển khai]
```

---

## 2. Nguyên tắc Phân loại Yêu cầu (Request Triage)

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
* **Tiêu chí:** Thêm tính năng mới, đổi luồng xử lý, đổi công thức tính tiền/kho/phân quyền.
* **Quy trình xử lý 5 bước chuẩn chỉ:**
  1. **TechLead Clarification Gate:** Hỏi User 2-3 câu làm rõ ý đồ.
  2. **BA Analysis & Options:** BA phân tích trade-off, đưa ra Phương án 1, 2 kèm ưu nhược điểm.
  3. **User Sign-Off Gate:** TechLead trình bày các phương án để User chính thức chốt.
  4. **Architect Contract Gate:** Architect chốt DB Schema & API DTO theo phương án User đã chọn.
  5. **Devs & QA Gate:** TechLead chia task theo model tier, Dev code, QA nghiệm thu độc lập.

---

## 3. Khung câu hỏi phỏng vấn của TechLead (Clarification Framework)

Khi yêu cầu thuộc **Nhánh B**, TechLead **CHƯA ĐƯỢC GIAO VIỆC NGAY** mà phải gửi lại User 2 - 3 câu hỏi thuộc khung câu hỏi sau để làm rõ ý đồ đại khái:

1. **Về phạm vi (Scope):**
   > *"Tính năng/luồng này áp dụng cho toàn hệ thống hay chỉ cho chi nhánh cụ thể / đối tượng người dùng nào?"*
2. **Về xử lý ngoại lệ (Edge Case):**
   > *"Khi gặp tình huống bất thường (ví dụ: tồn kho không đủ, mạng lỗi, số tiền âm), hệ thống nên chặn lại ngay hay cho phép lưu tạm/báo lỗi?"*
3. **Về tính tương thích dữ liệu cũ (Backward Compatibility):**
   > *"Dữ liệu/đơn hàng đã phát sinh trước thời điểm này có cần chạy script chuyển đổi theo luồng mới hay giữ nguyên trạng?"*

---

## 4. Tiêu chuẩn bàn giao giữa các vai trò (Handover Standards)

### A. Từ BA sang TechLead & User (Options & Acceptance Criteria)
BA bắt buộc phải xuất:
1. **Bảng so sánh phương án (Trade-off Matrix):**
   - Phương án A (Ưu/Nhược điểm).
   - Phương án B (Ưu/Nhược điểm).
   - Khuyến nghị tối ưu của BA.
2. **Acceptance Criteria (Gherkin):** Given - When - Then cho từng kịch bản.

### B. Từ Architect sang Dev (Technical Contract)
Architect chuẩn hóa giao diện kỹ thuật:
- Endpoint: `POST /api/v1/...`
- Request / Response DTO schema (TypeScript interface hoặc Java Record).
- Danh sách migration table / columns cần thêm (nếu có).

### C. Từ TechLead sang Dev (Task Ticket)
- **Task ID:** `TASK-xxx`
- **Model Tier:** `high_tier_model` (Pro/Sonnet) hoặc `low_tier_model` (Flash/4o-mini).
- **Target Files:** Danh sách file cụ thể được phép sửa.
- **Reference Spec:** Link đến tài liệu AC của BA và Contract của Architect.

### D. Từ QA sang TechLead & User (Verification Report)
- **Command:** Lệnh chạy test thực tế trên terminal.
- **Status:** PASS / FAIL (kèm log lỗi).
- **Defects:** Danh sách lỗi phân loại nghiêm trọng.

---

## 5. Nguyên tắc Vàng: Không vượt quyền (Anti-Scope-Creep)

1. **Dev:** Chỉ code theo AC của BA và Schema của Architect. Thấy nghiệp vụ bất hợp lý -> báo cáo lại, không tự ý sửa. Không tự đóng vai QA.
2. **BA:** Tập trung nghiệp vụ, trade-off và AC. Không thiết kế schema DB, không viết code.
3. **Architect:** Thiết kế kiến trúc kỹ thuật. Không tự ý thay đổi quyết định nghiệp vụ của BA và User.
4. **QA:** Độc lập kiểm thử. Không tự sửa code ứng dụng của Dev.
5. **TechLead:** Điều phối, gán model tier, đóng vai trò cầu nối báo cáo các phương án của BA cho User chốt. Không tự ý code thay Dev, không tự ý duyệt phương án nghiệp vụ thay User.
