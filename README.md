# MTMS — MilkTea Management System

**Một thư mục dự án duy nhất để phát triển lâu dài.** Đây là repo workspace có sẵn tài liệu nghiệp vụ, Tracker, AI Agents, Skills và Rules. Chưa tạo Spring Boot/Vite app hoặc chạy database vì `G0` chưa được phê duyệt. Các mã P0/P1/P2 **bên trong tài liệu nghiệp vụ** là các đợt phát hành sản phẩm đã hoạch định, không phải thư mục của bộ ZIP.

## Bắt đầu trên Windows
1. Giải nén ZIP sao cho thư mục gốc là `mtms/`. Chuyển cả thư mục tới nơi bạn muốn, ví dụ `D:\Project_CaNhan\mtms`.
2. Trong PowerShell mở đúng thư mục chứa `AGENTS.md` và `.agents`:
   ```powershell
   cd 'D:\Project_CaNhan\mtms'
   Get-ChildItem -Force
   py -3 scripts\validate_workspace.py
   agy
   ```
3. Trong `agy`, kiểm tra `/agents` (6 agents), `/skills` (8 skills); dùng `/skills reload` nếu vừa thay đổi skill.
4. Đọc `docs/ai/START_HERE.md`, rồi gửi prompt trong `docs/ai/prompts/01_READ_ONLY_SMOKE_TEST.md` để xác nhận AI hiểu phạm vi dự án.
5. Thực hiện và phê duyệt G0 trong `docs/planning/G0_SIGNOFF_CHECKLIST.md` trước khi giao task lập trình.
6. Khi G0 được phê duyệt, tạo project Java/Spring Boot **ngay trong `backend/`** và React/Vite **ngay trong `frontend/`**, theo task `P0-ORG-01`.

## Các thư mục quan trọng
| Đường dẫn | Mục đích |
|---|---|
| `AGENTS.md` | Quy tắc dự án, trạng thái G0 và nguồn chuẩn |
| `.agents/agents/` | 6 Agent chuyên trách |
| `.agents/skills/` | 8 Skills áp dụng theo task |
| `.agents/rules/` | 9 Rules về giới hạn nghiệp vụ/chất lượng |
| `docs/business/` | Đặc tả và Implementation Handbook gốc |
| `docs/architecture/` | ERD Mermaid, SQL Starter tham khảo, SQL review |
| `docs/planning/MTMS_Development_Tracker.xlsx` | Tracker làm việc v1.1; trạng thái task chưa thay đổi |
| `docs/planning/CURRENT_STATE.md` | Trạng thái công việc/next task để tiếp tục giữa các lần làm |
| `docs/ai/` | Setup Windows, hướng dẫn giao task, prompt, template, hướng dẫn thêm file |
| `docs/reference/` | Tracker v1.0 nguyên bản để đối chiếu |
| `backend/` | Tạo dự án Spring Boot tại đây, hiện chưa có code |
| `frontend/` | Tạo dự án React + TypeScript tại đây, hiện chưa có code |
| `database/migrations/` | Migration Flyway **chỉ sau khi được duyệt** |
| `infra/docker/` | Compose, Dockerfile, reverse proxy trong các task tương ứng |
| `tests/` | Test liên chức năng và E2E |
| `.github/workflows/` | CI sẽ bổ sung khi có app để build/test |

## Về các đợt bổ sung file sau này
Cấu trúc này sẽ **không đổi tên theo các giai đoạn**. Khi triển khai task mới, mình sẽ nói chính xác đường dẫn `mtms/<folder>/<file>`, file nào mới, file nào cần cập nhật và file nào giữ nguyên. Đọc `docs/ai/HOW_TO_ADD_FILES.md` và `docs/ai/CHANGELOG.md`.

## Trạng thái thực tế
- Tổng cộng 104 parent tasks: 52 P0 / 44 P1 / 8 P2, chưa có task hoàn thành có bằng chứng.
- 19 nhóm chỉnh sửa dependency, 69 FE subtasks và liên kết BR/TC trong Tracker hiện là đề xuất chờ G0.
- Chưa có code app, migration, CI thực thi hoặc kết quả test production.
- File SQL starter **không được chạy như migration**.
