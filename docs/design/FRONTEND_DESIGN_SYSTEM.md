# MTMS — Quy chuẩn Thiết kế Giao diện Frontend (Design System & UI Guidelines)
## Thương hiệu: Lục Trà Tinh Hoa (Tea House Premium — Warm Enterprise Dashboard)

> **Mục đích tài liệu:** Đây là tài liệu quy chuẩn duy nhất (**Single Source of Truth**) định hình toàn bộ ngôn ngữ thiết kế, bảng màu, typography, bố cục, hiệu ứng chuyển động và trải nghiệm người dùng (UX) của dự án **MTMS Frontend** dành riêng cho chuỗi **Lục Trà Tinh Hoa**.  
> Được phê duyệt theo bản đặc tả **Tea House Premium — Warm Enterprise Dashboard**. Mọi màn hình hiện tại và các tính năng phát triển trong tương lai bắt buộc phải tuân thủ nghiêm ngặt quy chuẩn trong tài liệu này.

---

## 1. Triết lý Thiết kế & Định vị Thương hiệu

- **Khách hàng & Bối cảnh Thương hiệu:** Chuỗi trà sữa & trà trái cây **Lục Trà Tinh Hoa** (hệ thống chi nhánh tại TP. Hồ Chí Minh: 768 Nguyễn Văn Quá - Quận 12, Nguyễn Gia Trí - Bình Thạnh, Cao Thắng - Quận 10, Lê Văn Lương - Quận 7, v.v.).
- **Định vị thẩm mỹ (Aesthetic Concept):** **Tea House Premium — Warm Enterprise Dashboard**.
  - Không gian làm việc thanh lịch, tĩnh tại, cao cấp của một chuỗi trà tinh hoa.
  - Tông màu kem ấm, xanh rừng vững chãi, điểm nhấn gốm nung Terracotta và xanh lá trà xô thơm Sage.
  - Phục vụ người làm việc hàng ngày: Kế toán chuỗi, Quản lý cơ sở và Chủ đầu tư. Tuyệt đối không lòe loẹt neon, không dark mode game thủ, không glassmorphism cẩu thả.
- **Ranh giới sản phẩm bất biến (Hard Boundaries):**
  - **NO POS:** Tuyệt đối không tạo màn hình bán lẻ / order đồ uống tại quầy cho thu ngân.
  - **DOANH THU TỔNG HỢP NGÀY:** Doanh thu được chốt theo ca/ngày; doanh thu NET từ ứng dụng giao hàng (ShopeeFood, GrabFood, BeFood) tách biệt hoàn toàn với tiền mặt thực thu và tiền ngân hàng.
  - **HẠCH TOÁN CHI NHÁNH ĐỘC LẬP:** Mỗi chi nhánh có quỹ tiền mặt, tài khoản ngân hàng, kho nguyên vật liệu và bảng lương riêng biệt; phân quyền nghiêm ngặt theo chi nhánh (Branch Scope).
  - **P&L TẠM TÍNH:** Báo cáo P&L Phase 0 luôn bắt buộc hiển thị nhãn **`TẠM TÍNH / PROVISIONAL`**. Không thực hiện chia lợi nhuận thực tế cho nhà đầu tư trước cổng G5.

### 1.1 Bộ Quy tắc Chống Thiết kế Giả tạo (Anti-Slop Guidelines theo `design-taste-frontend`)
1. **Hình ảnh & Bản sắc Thực tế:**
   - Sử dụng hình ảnh thực tế và logo chính thức của **Lục Trà Tinh Hoa** (`/assets/luc-tra-tinh-hoa/`).
   - Tuyệt đối không dùng ảnh minh họa 3D generic, tranh vẽ vector AI hoặc ảnh stock vô danh.
2. **Cấm tiêu đề 2 vế văn vẻ / gượng ép (Banned Two-fragment Headings):**
   - Tuyệt đối KHÔNG dùng các câu khẩu hiệu AI marketing hoa mỹ kiểu: *"Chính xác từng con số. Minh bạch từng cốc trà"*.
   - Câu chữ phải chuẩn xác, nghề nghiệp: *"Lục Trà Tinh Hoa — Hệ thống Quản trị Vận hành & Dòng tiền Đa chi nhánh"*.
3. **Cấm dải thẻ tính năng generic & hiệu ứng nền rực rỡ:**
   - Không xếp các dải thẻ tính năng vô bổ với icon đóng hộp xanh đỏ nhạt.
   - Không dùng khối cầu phát sáng `blur-3xl` hoặc pattern chấm lưới `radial-gradient` trên nền đen.
   - Nền sáng kem ấm thanh lịch (`#F7F3EC`), mặt thẻ giấy mộc (`#FFFCF8`), đường viền sắc nét 1px (`#E5DED4`).
4. **Bảo mật giao diện Đăng nhập:**
   - Tuyệt đối không hiển thị thông tin tài khoản/mật khẩu demo lộ liễu trên giao diện đăng nhập production.

---

## 2. Bảng Màu & Hệ Thống Tokens (Color Tokens)

### 2.1 Bảng màu Sáng (Light Theme — Mặc định)
- **App Background (Nền ứng dụng):** `#F7F3EC` (Warm Cream — êm dịu mắt, ấm áp)
- **Card Background (Mặt thẻ nội dung):** `#FFFCF8` (Warm Off-white — giấy mỹ thuật cao cấp)
- **Sidebar (Thanh điều hướng):** `#304638` (Deep Forest Green — xanh rừng trầm tĩnh, vững chãi)
- **Primary Text (Chữ chính):** `#352D26` (Espresso Charcoal — tương phản chuẩn WCAG AAA **10.2:1**)
- **Secondary Text (Chữ phụ):** `#786C60` (Warm Taupe — tương phản chuẩn WCAG AA **4.65:1**)
- **Border (Đường biên):** `#E5DED4` (Warm Stone / Oatmeal — viền siêu mảnh 1px)
- **Primary Accent (Điểm nhấn chính):** `#B96849` (Terracotta — gốm nung, nút bấm chính, active tab)
- **Secondary Accent (Điểm nhấn phụ):** `#819B78` (Sage Green — lá xô thơm, badge trung tính)
- **Success (Dương tiền, Hoàn tất):** `#287548` (Forest Emerald)
- **Warning (Cảnh báo, TẠM TÍNH):** `#A96A17` (Honey Amber)
- **Danger (Âm tiền, Lỗi, Chi phí):** `#B7423E` (Terracotta Crimson)
- **Info (Thông tin hệ thống):** `#356A91` (Slate Teal Blue)

### 2.2 Bảng màu Tối (Dark Theme — Forest Charcoal)
- **Background:** `#202923` (Deep Tea Charcoal)
- **Cards:** `#2B362F` (Muted Olive Charcoal)
- **Elevated Surfaces:** `#354238` (Elevated Tea Stone)
- **Sidebar:** `#17241D` (Obsidian Pine)
- **Primary Text:** `#F3F0E8` (Rice Paper — tương phản **12.8:1**)
- **Secondary Text:** `#C8CEC4` (Sage Ash — tương phản **8.5:1**)
- **Border:** `#4B584D` (Muted Pine Border)
- **Accent:** `#D18A6A` (Warm Terracotta Highlight)

---

## 3. Typography & Chuẩn Mực Số Liệu Tài Chính

- **Font chữ chính:** `Be Vietnam Pro`, `Inter`, sans-serif.
- **Thang kích thước chuẩn (Type Scale):**
  - `Page Title`: `26px` · Weight `700`
  - `Section Heading`: `20px` · Weight `650 - 700`
  - `Card Heading`: `16px` · Weight `600`
  - `KPI Value`: `28px - 32px` · Weight `700` · `font-mono tabular-nums`
  - `Body / Normal`: `14px` · Weight `400 - 500`
  - `Table Label & Form`: `13px` · Weight `500 - 600`
  - `Supporting / Caption`: `12px` · Weight `400 - 500`
  - `Micro Tag`: `10px` · Weight `600` · Uppercase
- **Quy tắc hiển thị Số tiền & Số lượng (BẮT BUỘC):**
  - Mọi con số tài chính (Doanh thu ngày, Doanh thu NET app, Chi phí lương, Tồn kho WAC, Số dư ngân hàng) **BẮT BUỘC** sử dụng font monospace căn đều ký tự (`font-mono tabular-nums`).
  - **Căn phải (Right-aligned):** Toàn bộ cột số liệu trong bảng phải căn lề phải để người xem dễ so sánh quy mô số học.
  - **Định dạng tiền tệ Việt Nam:** Phân cách hàng nghìn bằng dấu chấm `.`, kèm hậu tố `₫` (ví dụ: `24.850.000 ₫`). Không viết số thô `24850000`.

---

## 4. Mật Độ Dữ Liệu & Bố Cục Ứng Dụng (Layout & Density)

- **Mật độ: High-Density (Compact Data-First):**
  - Chiều cao dòng trong Table: `36px - 40px` (tối ưu hóa diện tích hiển thị số liệu).
  - Chiều cao Topbar: `68px`.
  - Khoảng cách padding trang: `24px - 32px`.
  - Khoảng cách giữa các thẻ (Card gap): `16px - 20px`.
  - Bán kính bo góc (Card radius): `14px - 16px` (`rounded-xl` đến `rounded-2xl`).
- **Thanh điều hướng Sidebar (3 Chế độ):**
  1. **Desktop Mở rộng (`248px`):** Hiển thị logo, tên chuỗi, tiêu đề nhóm nghiệp vụ và text đầy đủ của từng mục.
  2. **Desktop Thu gọn (`72px`):** Icon-only chính giữa, hiển thị floating tooltip khi hover.
  3. **Mobile Drawer (`< 1024px`):** Ngăn kéo trượt từ cạnh trái với lò xo mượt trong `220ms - 260ms`, kích thước vùng bấm tối thiểu `44px`.

---

## 5. Quy Chuẩn Trang Đăng Nhập (Two-Column Split-Screen)

Trang Đăng nhập thể hiện sự trang nhã, ấm cúng và nhận diện thương hiệu Lục Trà Tinh Hoa:
- **Cột Trái (45% - 50% — Brand Panel):**
  - Nền Forest Green `#304638` (Dark mode: `#17241D`).
  - Huy hiệu tròn Logo chính thức của Lục Trà Tinh Hoa (`brand-logo-thumb.webp`).
  - Họa tiết nét vẽ vector lá trà tối giản, không dùng ảnh chụp tràn viền gây nhiễu.
  - Thông điệp: *"Hệ thống Quản trị Vận hành & Kế toán Quản trị Đa chi nhánh"*.
  - Danh sách mạng lưới 4 chi nhánh: *768 Nguyễn Văn Quá (Q12) · Nguyễn Gia Trí · Cao Thắng · Lê Văn Lương*.
  - Huy hiệu cam kết: `NO POS · HẠCH TOÁN CHI NHÁNH ĐỘC LẬP`.
- **Cột Phải (50% - 55% — Authentication Surface):**
  - Nền giấy mộc ấm `#FFFCF8` (Dark mode: `#2B362F`).
  - Form đăng nhập tinh gọn: Trường tài khoản / email và trường mật khẩu có nút bật/tắt hiển thị mật khẩu.
  - Focus ring màu Terracotta `#B96849`.
  - Nút bấm chính màu Đất nung Terracotta (`bg-[#B96849] hover:bg-[#A55639] text-white`).
  - Thông báo lỗi trích xuất từ RFC 7807 Problem Detail (lỗi 401 tài khoản sai, 403 tài khoản bị khóa, 400 validation).
  - **Không hiển thị mật khẩu demo lộ liễu trên giao diện.**

---

## 6. Quy Chuẩn Bảng Điều Khiển (Executive Dashboard)

Bảng điều khiển trung tâm phản ánh chính xác tình hình kinh doanh trong ngày:
- **Bộ lọc ngữ cảnh:** Bộ chọn chi nhánh (Branch Scope Selector) + Bộ chọn khoảng ngày (Date Range Picker).
- **4 Thẻ KPI Cốt Lõi:**
  1. **Doanh thu kỳ:** Phân tách rõ Tiền mặt, Ngân hàng và **Doanh thu NET ứng dụng (ShopeeFood, GrabFood, BeFood)**.
  2. **Chi phí hoạt động:** Phân tách Chi phí nguyên vật liệu, Lương tạm tính và Chi phí khác.
  3. **Lợi nhuận Quản trị:** Bắt buộc hiển thị nhãn nổi bật **`TẠM TÍNH / PROVISIONAL`** màu hổ phách (`#A96A17`).
  4. **Giá trị Tồn kho WAC:** Tổng giá trị theo giá vốn bình quân di động và số mặt hàng cận date.
- **Biểu đồ & Cơ cấu:**
  - Biểu đồ đường & cột: Xu hướng doanh thu theo ngày (Net Doanh thu vs Kênh bán).
  - Biểu đồ Donut: Cơ cấu doanh thu theo kênh (Tiền mặt / Chuyển khoản / App NET).
  - Bảng so sánh doanh thu chi nhánh (Chỉ hiển thị cho Owner/Admin).
  - Danh sách cảnh báo vận hành (Báo cáo ca chưa chốt, Lô sắp hết hạn, Chênh lệch đối soát ngân hàng).
  - Thao tác nhanh theo vai trò và nhật ký kiểm toán gần đây.

---

## 7. Hiệu Ứng Chuyển Động & Tiếp Cận (Motion & Accessibility)

- **Triết lý "Quiet & Restrained":** Hiệu ứng nhanh, dứt khoát, phục vụ công việc, không gây xao nhãng.
  - Thời lượng: `140ms` (hover/tap), `180ms` (dropdown/modal), `220ms - 260ms` (sidebar/drawer).
  - Easing: `cubic-bezier(0.22, 1, 0.36, 1)`.
  - Tuân thủ `prefers-reduced-motion` của hệ điều hành.
- **Tiếp cận WCAG 2.2 AA:**
  - Hỗ trợ bàn phím đầy đủ: `Tab` di chuyển, `Enter` kích hoạt, `Escape` đóng modal/drawer.
  - Vòng focus hiển thị sắc nét: `ring-2 ring-[#B96849] ring-offset-2`.
  - Tỷ lệ tương phản văn bản chính trên nền đạt chuẩn AAA (`10.2:1`).

---

## 8. Danh Mục Tài Nguyên Hình Ảnh Thực Tế

Lưu trữ tại `frontend/public/assets/luc-tra-tinh-hoa/`:

| Tên file | Định dạng / Kích thước | Mục đích sử dụng |
|---|---|---|
| `brand-logo-thumb.webp` | WebP · 26 KB (280x280) | Logo nhận diện thương hiệu Lục Trà Tinh Hoa (Header, Login, Sidebar, Favicon) |
| `store-counter-thumb.webp` | WebP · 199 KB | Ảnh thực tế quầy pha chế & vận hành cửa hàng (Tư liệu tham khảo / Modal chi nhánh) |
| `tea-craft-thumb.webp` | WebP · 169 KB | Cận cảnh quy trình ủ trà và lá trà nguyên bản (Tư liệu thương hiệu) |
| `product-luc-tra-thumb.webp` | WebP · 325 KB | Ảnh sản phẩm Lục Trà Tinh Hoa đặc trưng |
| `product-thai-tea-thumb.webp` | WebP · 305 KB | Ảnh sản phẩm Cacao / Trà Thái |
| `product-oolong-thumb.webp` | WebP · 394 KB | Ảnh sản phẩm trà Ô long |
| `hero-banner.webp` | WebP · 885 KB | Banner không gian cửa hàng |
