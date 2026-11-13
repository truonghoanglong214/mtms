# Backend — Spring Boot 3 & Java 21 Modular Monolith

Ứng dụng backend phục vụ quản lý vận hành và tài chính chuỗi trà sữa MTMS.

## 1. Công nghệ & Ranh giới
- **Runtime:** Java 21 LTS, Spring Boot 3.3.5.
- **Cơ sở dữ liệu & Migration:** PostgreSQL 17, Flyway trỏ trực tiếp đến canonical folder `filesystem:../database/migrations` (khi chạy từ `backend/`).
- **Kiến trúc:** Modular Monolith chia theo 11 Bounded Contexts, kiểm soát import chéo bằng ArchUnit tests (`ModuleBoundaryTest`).
- **Nguyên tắc an toàn:** Tuyệt đối không làm POS; tách biệt doanh thu NET và dòng tiền ngân hàng; số học dùng `BigDecimal`.

## 2. Lệnh thực thi cục bộ
```powershell
# Chạy bộ kiểm thử (ArchUnit + Unit + Context Smoke)
mvn -f backend/pom.xml test

# Khởi chạy server development (yêu cầu PostgreSQL đang chạy)
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```
