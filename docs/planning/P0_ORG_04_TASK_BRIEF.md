# Task brief — P0-ORG-04 Authentication and sessions (IMPLEMENTED / VERIFIED)

- **Prepared:** 2026-11-13
- **Gate status:** `G0 SIGNED` on 2026-11-13; `P0-ORG-01`, `P0-ORG-02`, and `P0-ORG-03` completed and verified.
- **Parent Task:** `P0-ORG-04` — Authentication and sessions.
- **BR IDs:** `BR-ORG-04` (`BASELINED`) — Secure authentication, password hashing, and session management.
- **TC IDs:** `TC-001` (boundary negative test: unauthorized request rejected with 401; logout invalidates access token).
- **Dependency IDs + status:** `P0-ORG-02` (PostgreSQL baseline V001 with `mtms.users` table) and `P0-ORG-03` are satisfied.
- **Owner / reviewers:** Business Owner (approval & acceptance), Tech Lead (coordination & security boundaries), Architect (security architecture & JWT contracts), Backend Engineer (implementation), QA Reviewer (testing).

---

## 1. Mục tiêu nghiệp vụ (Business Intent) & Định nghĩa Hoàn thành (DoD)

### 1.1 Business Intent
- Xây dựng cơ chế xác thực người dùng an toàn: Đăng nhập bằng `login_identifier` và mật khẩu.
- Hash mật khẩu an toàn bằng BCrypt (không bao giờ lưu plaintext password).
- Cơ chế Token kép: Access Token (JWT thời hạn ngắn, e.g. 15-60 phút) và Refresh Token (dài hạn).
- Đăng xuất (`logout`): Vô hiệu hóa phiên làm việc, từ chối các request tiếp theo dùng token cũ.
- Bảo vệ các API nghiệp vụ: Mọi endpoint nhạy cảm (như `/api/v1/organizations/**`) bắt buộc phải có Bearer token hợp lệ, từ chối với HTTP 401 Unauthorized nếu chưa đăng nhập.

### 1.2 Definition of Done (DoD)
1. Đăng nhập thành công với tài khoản hợp lệ, mật khẩu đúng, trạng thái `ACTIVE` -> Trả về Access Token + Refresh Token + thông tin người dùng.
2. Từ chối đăng nhập khi sai mật khẩu, tài khoản không tồn tại hoặc tài khoản bị `SUSPENDED` / `DISABLED` -> Trả về HTTP 401.
3. Request không kèm token đến các endpoint bảo vệ bị từ chối với HTTP 401 Unauthorized.
4. Endpoint `/api/v1/auth/refresh` cấp phát token mới khi refresh token hợp lệ.
5. Endpoint `/api/v1/auth/logout` hủy phiên làm việc thành công.
6. ArchUnit tests và Unit/Integration tests cho Auth đạt 100% PASS.

---

## 2. Phạm vi thực thi (Scope)

### 2.1 Trong phạm vi (In Scope)
1. **Dependencies:**
   - Thêm `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, và `spring-security-test` vào `backend/pom.xml`.
2. **Domain Layer (`com.mtms.identity.domain`):**
   - `UserStatus`: `ACTIVE`, `SUSPENDED`, `DISABLED` (khớp với CHECK constraint bảng `mtms.users`).
   - `User`: Domain model đại diện người dùng hệ thống (`id`, `loginIdentifier`, `passwordHash`, `displayName`, `status`, `createdAt`).
3. **Persistence Layer (`com.mtms.identity.infrastructure.persistence`):**
   - `UserJpaEntity` ánh xạ vào bảng `mtms.users`.
   - `UserJpaRepository`: Tìm kiếm theo `loginIdentifier`, kiểm tra tồn tại.
4. **Security & JWT Infrastructure (`com.mtms.identity.infrastructure.security`):**
   - `JwtTokenService`: Ký và giải mã JWT token (HMAC-SHA256), trích xuất userId và loginIdentifier.
   - `JwtAuthenticationFilter`: Filter chặn mọi request, kiểm tra header `Authorization: Bearer <token>`, nạp `Authentication` vào `SecurityContextHolder`.
   - `SecurityConfig`: Cấu hình `SecurityFilterChain`, tắt CSRF cho stateless API, cấu hình phân quyền endpoint (`/api/v1/auth/login`, `/api/v1/auth/refresh`, `/actuator/health` được public; còn lại yêu cầu authenticated).
   - `TokenBlacklistService`: Quản lý danh sách token bị thu hồi sau khi logout.
5. **Application Layer (`com.mtms.identity.application`):**
   - `AuthenticationService`:
     - `LoginResult login(LoginCommand cmd)`
     - `TokenRefreshResult refresh(RefreshTokenCommand cmd)`
     - `void logout(String token)`
     - `UserDto getCurrentUser(UUID userId)`
   - DTOs: `LoginCommand`, `AuthResponse`, `RefreshTokenCommand`, `UserDto`.
6. **Web Layer (`com.mtms.identity.web`):**
   - `AuthController`:
     - `POST /api/v1/auth/login`
     - `POST /api/v1/auth/refresh`
     - `POST /api/v1/auth/logout`
     - `GET /api/v1/auth/me`
7. **Testing:**
   - `JwtTokenServiceTest`: Kiểm tra tạo, giải mã, hết hạn token.
   - `AuthenticationServiceTest`: Kiểm tra logic đăng nhập, sai mật khẩu, tài khoản bị khóa.
   - `SecurityIntegrationTest`: Kiểm tra chặn 401 khi không có token, 200 khi có token hợp lệ.

### 2.2 Ngoài phạm vi (Explicit Non-Goals)
- Không phân quyền chi tiết theo vai trò/chi nhánh (`P0-ORG-05` — Scoped permissions).
- Không tự đăng ký tài khoản (User Onboarding dành cho Admin ở `P0-ORG-06`).
- Nhân viên quán (`Employee`) là master data riêng ở module `workforce`, không cấp login hệ thống.
- Không làm giao diện FE (`P0-ORG-06`).
