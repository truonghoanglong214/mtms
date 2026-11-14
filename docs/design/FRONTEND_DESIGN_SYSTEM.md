# MTMS — Quy chuẩn Thiết kế Giao diện Frontend (Design System & UI Guidelines)

> **Mục đích tài liệu:** Đây là tài liệu quy chuẩn duy nhất (Single Source of Truth) định hình toàn bộ ngôn ngữ thiết kế, bảng màu, typography, bố cục, hiệu ứng chuyển động và trải nghiệm người dùng (UX) của dự án **MTMS Frontend**.  
> Mọi màn hình hiện tại và các tính năng phát triển trong tương lai bắt buộc phải tuân thủ nghiêm ngặt quy chuẩn trong tài liệu này.

---

## 1. Triết lý Thiết kế & Định vị Thương hiệu

- **Loại hình sản phẩm (Kind of Business):** Hệ thống Quản trị Vận hành & Kế toán Quản trị Đa chi nhánh Chuỗi Trà Sữa (**MTMS — Milk Tea Management System**).
- **Ranh giới sản phẩm bất biến (Hard Boundary):**
  - **NO POS:** Tuyệt đối không tạo giao diện bán hàng / order đồ uống tại quầy cho thu ngân.
  - **DOANH THU TỔNG HỢP NGÀY:** Doanh thu được chốt theo ca/ngày; doanh thu NET từ app giao hàng (ShopeeFood, Grab) tách biệt với tiền mặt thực thu và tiền ngân hàng.
  - **HẠCH TOÁN CHI NHÁNH ĐỘC LẬP:** Mỗi chi nhánh có quỹ tiền mặt, tài khoản ngân hàng, kho nguyên vật liệu và bảng lương riêng biệt.
  - **P&L TẠM TÍNH:** Báo cáo P&L Phase 0 luôn bắt buộc hiển thị nhãn **`TẠM TÍNH / PROVISIONAL`**.
- **Phong cách thị giác (Aesthetic Vibe):** **Modern Editorial (Linear / Supabase high-end SaaS)**.
  - Tông màu Slate / Zinc trung tính, đường viền 1px siêu mảnh, sắc cạnh và tinh tế.
  - Không sử dụng gradient lòe loẹt hoặc layout thẻ 3 cột cẩu thả ("AI slop").
  - Mọi chi tiết đều có mục đích rõ ràng, tạo cảm giác một phần mềm quản trị tài chính chuyên nghiệp, đáng tin cậy và cao cấp.
- **Giọng văn & Ngôn ngữ (Tone & Voice):**
  - Gọn gàng, chuẩn xác, không dùng từ ngữ quảng cáo hoa mỹ.
  - Sử dụng thuật ngữ nghiệp vụ F&B và Kế toán Việt Nam rõ ràng: *Doanh thu ròng, Tồn kho bình quân (WAC), Giờ công được duyệt, Lợi nhuận tạm tính, Sổ quỹ, Phiếu chi*.

---

## 2. Bảng Màu & Tokens (Color Palette)

### 2.1 Màu nhận diện thương hiệu (Brand Accent)
- **Primary (Forest Emerald):** Xanh ngọc lục bảo / xanh rêu lá trà sẫm đại diện cho tinh hoa nguyên liệu trà, đồng thời mang lại sự vững chãi, tin cậy về tài chính.
  - `primary-dark`: `#064e3b` (Emerald 900 — Nền tối, hover sâu)
  - `primary`: `#059669` (Emerald 600 — Nút chính, chỉ báo active)
  - `primary-light`: `#10b981` (Emerald 500 — Accent, highlight)
  - `primary-subtle`: `rgba(16, 185, 129, 0.08)` (Nền chip, badge active, hover tab)

### 2.2 Màu nền & Trung tính (Slate & Zinc Hierarchy)
- **Background:** `#090d16` (Dark Slate sẫm) / `#f8fafc` (Light Clean Canvas)
- **Card / Surface:** `#0f172a` (Dark Slate 900) / `#ffffff` (Light Card Surface)
- **Popover / Dropdown:** `#1e293b` (Slate 800) / `#ffffff`
- **Border:** `#334155` (Slate 700 / Dark border 1px) / `#e2e8f0` (Slate 200 / Light border 1px)
- **Muted Border:** `#1e293b` / `#f1f5f9` (Đường phân cách danh mục con)

### 2.3 Màu ngữ nghĩa Tài chính & Trạng thái (Financial Semantics)
- **Tạm tính / Provisional (Cảnh báo bắt buộc):** Màu hổ phách ấm (`#f59e0b` / Amber 500) kết hợp icon `ShieldAlert`.
- **Dương tiền / Doanh thu / Hoàn thành:** Emerald (`#10b981`).
- **Âm tiền / Chi phí / Lỗi / Hủy:** Rose / Red (`#f43f5e`).
- **Đang xử lý / Chờ duyệt (Pending):** Sky Blue (`#0ea5e9`).
- **Khóa sổ / Lưu trữ (Closed / Archived):** Slate Slate (`#64748b`).

---

## 3. Typography (Kiểu chữ & Căn chỉnh Số liệu)

- **Font chữ chính:** `Inter`, `Plus Jakarta Sans`, `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`.
- **Quy tắc hiển thị Số tiền & Số lượng (Bắt buộc):**
  - Mọi con số tài chính (Doanh thu, Chi phí, Tồn kho, Lương, Số dư ngân hàng) **BẮT BUỘC** sử dụng font monospace căn đều ký tự (`font-mono tabular-nums`).
  - Định dạng tiền tệ: Phân cách hàng nghìn bằng dấu chấm hoặc phẩy đồng nhất, luôn kèm đơn vị `₫` hoặc `VND` (e.g. `12.500.000 ₫`).
  - Số liệu trong bảng: **Luôn căn phải (Right-aligned)** để dễ so sánh độ lớn con số.
- **Thang kích thước chữ (Type Scale):**
  - `Display / Hero Title`: 28px - 32px · Bold (Trang đăng nhập, báo cáo tổng thể)
  - `Page Heading`: 20px - 22px · Bold / SemiBold
  - `Section Title`: 15px - 16px · SemiBold
  - `Body / Normal`: 13px - 14px · Regular
  - `Caption / Subtext`: 11px - 12px · Medium (Màu `text-muted-foreground`)
  - `Micro Tag`: 10px · SemiBold · Uppercase

---

## 4. Mật độ Thông tin & Bố cục (Layout & Density)

- **Visual Density:** **High-Density (Compact Data-First)**.
  - Tối ưu cho người làm việc hàng ngày: Kế toán, Quản lý chi nhánh, Chủ chuỗi cần xem tối đa dữ liệu trên một màn hình mà không phải cuộn chuột quá nhiều.
  - Chiều cao dòng trong Table: `36px - 40px` (không để dòng table cao 60px gây lãng phí không gian).
  - Khoảng cách padding trong thẻ dữ liệu: `p-4 sm:p-5`.
- **Bố cục Responsive 4 Breakpoints:**
  1. **Desktop lớn (≥ 1440px):** Layout toàn màn hình, Sidebar cố định `256px`, hiển thị đầy đủ bảng phân tích số liệu nhiều cột.
  2. **Laptop tiêu chuẩn (1024px - 1439px):** Sidebar cố định gọn gàng, nội dung căn giữa với max-width `1280px`.
  3. **Tablet (768px - 1023px):** Header thu gọn, Sidebar chuyển sang slide-over drawer khi bấm nút Menu.
  4. **Mobile (≤ 390px — Màn hình điện thoại nhân viên quán):**
     - Menu dạng Drawer trượt mượt mà.
     - Các thao tác nhạy cảm (nhập báo cáo ca, chụp phiếu chi) thiết kế nút bấm lớn tối thiểu `44px` để bấm một tay dễ dàng.

---

## 5. Quy chuẩn Bố cục Trang Đăng nhập (Split-screen 50/50)

Trang Đăng nhập là "mặt tiền" của hệ thống, loại bỏ kiểu form nằm trơ trọi giữa trang:
- **Cột Trái (50% — Visual Brand & Operational Philosophy):**
  - Nền sẫm cao cấp với hình ảnh nghệ thuật về lá trà / quy trình pha chế tinh xảo / không gian quán.
  - Logo MTMS dập nổi sắc nét.
  - Tuyên ngôn giá trị: *"Quản trị tài chính chính xác, hạch toán minh bạch cho từng cốc trà."*
  - Badge cam kết: *Bảo mật đa chi nhánh · Kiểm soát dòng tiền thời gian thực · NO POS*.
- **Cột Phải (50% — Authentication Form):**
  - Thiết kế tối giản, tinh khiết, viền 1px sắc sảo.
  - Input nổi bật trạng thái focus ring emerald.
  - Nút chuyển đổi ẩn/hiện mật khẩu.
  - Phím Enter kích hoạt đăng nhập tức thì.
  - Thông báo lỗi hiển thị dạng alert banner êm ái, trích xuất rõ lỗi từ backend RFC 7807 (401 sai pass, 403 khóa tài khoản, 400 validation).

---

## 6. Hiệu ứng Chuyển động (Motion & Micro-interactions)

Áp dụng thư viện `motion/react` theo chuẩn:
- **Nguyên tắc "Quiet & Snappy":** Hiệu ứng nhanh, dứt khoát, không làm chậm nhịp thao tác của người dùng.
  - Thời gian chuyển động (Duration): `150ms - 250ms`.
  - Đường cong chuyển động (Easing): Spring với `damping: 25`, `stiffness: 250` hoặc `easeOut`.
- **Micro-interactions bắt buộc:**
  - **Button Hover:** Nút nhấc nhẹ `scale: 1.01`, khi click bấm xuống `scale: 0.98`.
  - **Card Hover:** Các card chức năng hoặc bảng biểu nhấc nhẹ 1-2px, viền sáng nhẹ.
  - **Drawer Mobile:** Slide-in từ cạnh trái với lò xo mượt, nền mờ backdrop blur dần trong 200ms.
  - **Dropdown & Popover:** Phóng to nhẹ từ 95% lên 100% kèm fade-in trong 150ms.
- **Quy tắc chống giật (Anti-Jank):**
  - Không bao giờ animate thuộc tính `width`, `height`, `top`, `left` gây reflow layout.
  - Chỉ animate `transform (x, y, scale)` và `opacity` (tận dụng GPU tăng tốc 60fps).

---

## 7. Quy chuẩn Hiệu năng & Kiến trúc Code (Vercel React Best Practices)

- **Tránh Waterfall Requests:**
  - Mọi request độc lập (ví dụ: thông tin user profile + danh sách chi nhánh) phải được fetch song song bằng `Promise.all` hoặc React Query song song.
- **Code-Splitting qua React.lazy:**
  - Tất cả các trang chức năng ngoài trang chủ (`MonthlyRevenuePage`, `WorkforceExpensePage`, `InventoryPage`, `FinancePage`, `ClosingPage`, `ReportingPage`) phải được lazy load để giữ bundle ban đầu dưới 180kB.
- **Atomic State với Zustand:**
  - Phân tách rõ ràng giữa Session state (Token, User) và UI state (Branch filter, Sidebar toggle).
  - Dùng selector cụ thể: `useAuthStore(state => state.user)` thay vì subscribe toàn bộ store để tránh re-render vô ích.
- **Quản lý Token an toàn:**
  - Token tự động đính kèm qua Axios Interceptor.
  - Bắt lỗi 401 tự động xếp hàng gọi `/api/v1/auth/refresh` và retry request gốc trong suốt, không làm gián đoạn người dùng.

---

## 8. Quy chuẩn Trải nghiệm Người dùng & Accessibility (Web Design Guidelines)

- **Hỗ trợ bàn phím đầy đủ:** Phím `Tab` duyệt theo thứ tự logic, `Enter` submit form, `Esc` đóng modal / drawer / dropdown.
- **Trạng thái trực quan (Visual Feedback):**
  - Mọi nút bấm khi đang gọi API phải có spinner xoay và vô hiệu hóa (`disabled`) để chống double-click.
  - Thông báo lỗi form phải chỉ rõ trường nào lỗi (ví dụ: viền đỏ kèm icon cảnh báo và thông điệp cụ thể dưới trường nhập liệu).
- **Tương phản chuẩn WCAG AA:** Văn bản trên nền tối hay sáng đều phải đảm bảo tỷ lệ tương phản tối thiểu 4.5:1.
