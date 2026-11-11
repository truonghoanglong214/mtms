# Antigravity CLI — Windows setup

## 1. Giải nén và kiểm tra đúng root
Giải nén file `mtms_project_workspace.zip`; bên trong có thư mục `mtms/`. Ví dụ chuyển `mtms/` tới `D:\Project_CaNhan\mtms`.

```powershell
cd 'D:\Project_CaNhan\mtms'
Get-ChildItem -Force
py -3 scripts\validate_workspace.py
```

Phải thấy `AGENTS.md`, `.agents/`, `docs/`, `backend/`, `frontend/` cùng ở **một cấp**. `Get-ChildItem -Force` chỉ hiện file ẩn, không đổi quyền AI.

## 2. Mở Antigravity từ root
```powershell
agy
```
Kiểm tra `/agents` có `mtms-tech-lead`, `mtms-business-analyst`, `mtms-architect`, `mtms-backend-engineer`, `mtms-frontend-engineer`, `mtms-qa-reviewer`.
Kiểm tra `/skills` có 8 Skills `mtms-*`, thử `/skills reload` nếu thiếu. Chọn `mtms-tech-lead` làm Agent chính nếu CLI hỗ trợ.

## 3. Kiểm tra hiểu nghiệp vụ (không code)
Sao chép prompt từ `docs/ai/prompts/01_READ_ONLY_SMOKE_TEST.md`. Đối chiếu `docs/ai/ENVIRONMENT_CHECKLIST.md`. Nếu Agent đọc sai phạm vi hoặc G0, sửa cấu hình trước khi giao task.

## 4. Tạo project Java/React trong cùng root sau G0
Khi được phê duyệt `P0-ORG-01`, tạo Spring Boot project **trực tiếp trong `backend/`** và React Vite project **trực tiếp trong `frontend/`**. Không tạo folder `mtms/mtms`, `frontend/frontend` hay `backend/backend`. Migrations chỉ khi task `P0-ORG-02` được duyệt.

## 5. Khắc phục lỗi phát hiện Agent/Skill
- Kiểm tra workspace root có `.agents/agents/*.md`, `.agents/skills/<name>/SKILL.md`, `.agents/rules/*.md`.
- Kiểm tra tên và `description` trong YAML frontmatter; Rule cần `trigger` hợp lệ.
- Khởi động lại CLI sau khi sửa nếu `/skills reload` không cập nhật.
- Các file này đã qua kiểm tra tĩnh trong ZIP; việc CLI thực sự nạp thành công phải xác nhận trên máy bạn.

**Bảo mật:** không bật `always-proceed` cho lệnh phá hủy, migration, deploy, secrets và chi trả tài chính.
