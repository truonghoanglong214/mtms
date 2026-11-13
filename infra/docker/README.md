# Infrastructure — Docker & Compose

Hạ tầng Docker cung cấp môi trường cơ sở dữ liệu và triển khai cục bộ/staging cho MTMS.

## 1. Dịch vụ cơ bản
- **`postgres`:** PostgreSQL 17 Alpine (`postgres:17-alpine`), dữ liệu lưu trữ tại named volume `mtms-pgdata`.
- **`backend`:** Spring Boot REST API với volume mount read-only thư mục migration `../../database/migrations:/app/migrations:ro`.
- **`frontend`:** React Vite SPA phục vụ qua Nginx reverse proxy.

## 2. Quy tắc an toàn & Biến môi trường
- Sao chép `.env.example` thành `.env` để cấu hình mật khẩu bí mật. **Tuyệt đối không commit file `.env` lên git**.
- Biến `POSTGRES_PASSWORD` bắt buộc phải có giá trị thì Compose mới cho phép khởi chạy.

## 3. Lệnh vận hành
```powershell
# Kiểm tra cấu hình compose hợp lệ
docker compose --env-file infra/docker/.env -f infra/docker/compose.yml config

# Khởi chạy dịch vụ PostgreSQL cục bộ
docker compose --env-file infra/docker/.env -f infra/docker/compose.yml up -d postgres

# Khởi chạy toàn bộ hệ thống
docker compose --env-file infra/docker/.env -f infra/docker/compose.yml up --build -d

# Tắt dịch vụ (KHÔNG thêm cờ -v để tránh xóa mất dữ liệu persistent volume)
docker compose -f infra/docker/compose.yml down
```
