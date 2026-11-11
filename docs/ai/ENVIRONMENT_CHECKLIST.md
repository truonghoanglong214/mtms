# MTMS — Checklist xác nhận môi trường

## Kiểm tra gói (đã làm trong ZIP)
- [x] Có 6 Agent, 8 Skills, 9 Rules và `AGENTS.md`.
- [x] File nghiệp vụ gốc còn nguyên SHA-256 sau đổi tên.
- [x] Tracker v1.1 được chuyển sang path ổn định, không thay đổi workbook.
- [x] Có sẵn thư mục app, kiến trúc, tài liệu và scripts.
- [x] Chạy validator tĩnh thành công tại thời điểm đóng ZIP.

## Người dùng cần kiểm tra trên Windows
- [ ] Giải nén đúng cấu trúc `mtms/`.
- [ ] `py -3 scripts\validate_workspace.py` chạy PASS.
- [ ] `agy` mở từ root, `/agents` hiển thị 6 Agent.
- [ ] `/skills` hiển thị 8 Skills.
- [ ] Prompt kiểm tra đọc đúng NO POS và G0 chưa duyệt.
- [ ] G0 do Owner duyệt chính thức trước khi triển khai task đầu tiên.

**Không bao gồm:** Java project, React project, Flyway migration, CI chạy thực tế, hay bằng chứng phần mềm đã hoạt động.
