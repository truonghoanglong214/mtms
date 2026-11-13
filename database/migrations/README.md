# Database Migrations

Thư mục `database/migrations/` là **nguồn di trú duy nhất (Single Source of Truth)** của hệ thống MTMS.

## 1. Quy tắc vận hành
- **Chỉ tiến lên (Forward-only):** Mọi file migration (`V001__...`, `V002__...`) khi đã áp dụng là **bất biến (immutable)**. Tuyệt đối không chỉnh sửa nội dung file migration đã chạy. Mọi sửa đổi, bổ sung schema đều phải tạo file mới kế tiếp (`V002+`).
- **Không dùng migration hạ cấp (No Downgrade):** MTMS không sử dụng file rollback tự động; phục hồi lỗi thông qua forward-fix migration hoặc khôi phục snapshot database.
- **Schema chuyên dụng:** Tất cả các bảng nghiệp vụ được đặt trong schema `mtms`.

## 2. Danh mục Migrations hiện tại
- `V001__create_organization_identity_baseline.sql`: Khởi tạo schema `mtms` và 3 bảng nền tảng tổ chức (`business_entities`, `branches`, `users`).
