# Task brief — P0-SEC-06 Responsive shell UI (IMPLEMENTED / VERIFIED)

- **Prepared:** 2026-11-13
- **Gate status:** `G0 SIGNED` on 2026-11-13; `P0-ORG-01` through `P0-ORG-04` completed and verified.
- **Parent Task:** `P0-SEC-06` — Responsive shell UI.
- **BR IDs:** `BR-ORG-04` (`BASELINED`) — Secure authentication, session management, and responsive role-based layout.
- **TC IDs:** `TC-001` (boundary negative test: unauthorized request rejected with 401; logout invalidates access).
- **Dependency IDs + status:** `P0-ORG-04` (Authentication and sessions API) is satisfied.
- **Owner / reviewers:** Business Owner (approval & acceptance), Tech Lead (coordination), Frontend Engineer (implementation), QA Reviewer (testing).

---

## 1. Mục tiêu nghiệp vụ (Business Intent) & Định nghĩa Hoàn thành (DoD)

### 1.1 Business Intent
- Xây dựng giao diện Đăng nhập (Login Page) kết nối trực tiếp với backend Auth API (`/api/v1/auth/login`).
- Quản lý phiên làm việc ở Frontend: Lưu Access Token, Refresh Token an toàn, tự động gắn Header `Authorization: Bearer <token>` vào mọi HTTP request, xử lý đăng xuất (`/api/v1/auth/logout`) và tự động làm mới token (`/api/v1/auth/refresh`).
- Khung giao diện Responsive Shell (Header, Sidebar, User Menu, Branch Switcher, Mobile Drawer) phục vụ cả 2 phân hệ người dùng:
  - **Desktop (Admin / Owner / Manager):** Bảng điều khiển đầy đủ, điều hướng đa chi nhánh, menu danh mục rõ ràng.
  - **Mobile (Nhân viên quán / Employee):** Giao diện vuốt mở Drawer nhẹ nhàng, tối ưu thao tác một tay trên màn hình điện thoại (390px).
- Áp dụng 4 skills chuyên sâu:
  - `design-taste-frontend`: Layout anti-slop, visual hierarchy sắc nét, typography chuẩn mực, không dùng template rập khuôn.
  - `motion`: Chuyển cảnh mượt mà, micro-interactions 60fps cho button, input, drawer và dropdown.
  - `vercel-react-best-practices`: Tránh waterfall, tối ưu re-render qua Zustand selector, code splitting sạch.
  - `web-design-guidelines`: Chuẩn accessibility, feedback trạng thái rõ ràng, bàn phím điều hướng thân thiện.

### 1.2 Definition of Done (DoD)
1. **Trang Login (`/login`):**
   - Form đăng nhập với validation `loginIdentifier` và `password`.
   - Kết nối trực tiếp với backend `POST /api/v1/auth/login`.
   - Hiển thị lỗi rõ ràng khi sai thông tin (401), tài khoản bị khóa (403), hoặc lỗi mạng.
   - Đăng nhập thành công chuyển hướng về trang yêu cầu trước đó hoặc `/dashboard`.
2. **Quản lý Auth State:**
   - Zustand store quản lý `token`, `refreshToken`, `user`, `isAuthenticated`.
   - Axios Interceptor tự động gắn `Bearer token` vào header request.
   - Axios Interceptor bắt mã 401 để tự động gọi `/api/v1/auth/refresh`, nếu refresh thất bại thì xóa phiên và điều hướng về `/login`.
3. **App Shell Layout (`MainLayout`):**
   - **Header:** Logo thương hiệu MTMS, Branch Selector (chọn chi nhánh hiện hành), User Menu (Avatar/Name, Role, Logout).
   - **Sidebar Desktop:** Danh sách navigation trực quan, highlight mục đang active.
   - **Sidebar Mobile Drawer:** Drawer mở/đóng êm ái bằng `motion/react`, backdrop làm mờ tinh tế.
4. **Bảo vệ Routes (`ProtectedRoute`):**
   - Chặn người dùng chưa đăng nhập truy cập các trang nghiệp vụ nội bộ, tự động redirect về `/login`.
   - Chặn người dùng đã đăng nhập quay lại `/login`, redirect vào `/dashboard`.
5. **Kiểm thử & Build:**
   - `npm --prefix frontend run build` compile thành công không lỗi TypeScript hay bundle.
   - `python scripts/validate_workspace.py` đạt `SUMMARY: PASS`.

---

## 2. Phạm vi thực thi (Scope)

### 2.1 Trong phạm vi (In Scope)
- Cài đặt thư viện `motion` cho React animation.
- Tạo `src/shared/api/apiClient.ts` cấu hình Axios instance với interceptors.
- Tạo `src/features/auth/types.ts`, `authApi.ts`, `authStore.ts`.
- Xây dựng component `LoginPage.tsx`.
- Nâng cấp `src/app/layout/Header.tsx`, `Sidebar.tsx`, `MainLayout.tsx`.
- Cấu hình route bảo vệ trong `src/app/router.tsx`.
- Thêm modal/dialog hoặc dropdown Logout xác thực.

### 2.2 Ngoài phạm vi (Explicit Non-Goals)
- Không can thiệp logic backend đã hoàn thành (`P0-ORG-04`).
- Không tạo POS hay giao diện bán hàng cho khách lẻ (quy tắc bất biến `NO POS`).
- Giao diện CRUD Doanh nghiệp / Chi nhánh thuộc `P0-ORG-06`.
- Giao diện nghiệp vụ Doanh thu / Chi phí thuộc các task `P0-REV-*` và `P0-FIN-*`.
