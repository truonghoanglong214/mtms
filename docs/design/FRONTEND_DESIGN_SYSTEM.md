# MTMS — Quy chuẩn Thiết kế Giao diện Frontend (Design System & UI Guidelines)
## Chuỗi Cửa Hàng: Lục Trà Tinh Hoa

> **Mục đích tài liệu:** Đây là tài liệu quy chuẩn duy nhất (**Single Source of Truth**) định hình toàn bộ ngôn ngữ thiết kế, bảng màu, typography, bố cục, hiệu ứng chuyển động và trải nghiệm người dùng (UX) của dự án **MTMS Frontend** dành riêng cho chuỗi **Lục Trà Tinh Hoa**.  
> Mọi màn hình hiện tại và các tính năng phát triển trong tương lai bắt buộc phải tuân thủ nghiêm ngặt quy chuẩn trong tài liệu này.

---

## 1. Triết lý Thiết kế & Định vị Thương hiệu

- **Khách hàng & Bối cảnh Thương hiệu:** Chuỗi trà sữa & trà trái cây **Lục Trà Tinh Hoa** (hệ thống chi nhánh tại TP. Hồ Chí Minh: 768 Nguyễn Văn Quá - Quận 12, Nguyễn Gia Trí - Bình Thạnh, Cao Thắng - Quận 10, Lê Văn Lương - Quận 7, v.v.).
- **Loại hình phần mềm (Site Kind & Page Type):** Hệ thống Quản trị Vận hành & Kế toán Quản trị Đa chi nhánh (**MTMS — Internal Multi-shop Operations & Financial Management**).
- **Ranh giới sản phẩm bất biến (Hard Boundaries):**
  - **NO POS:** Tuyệt đối không tạo màn hình bán lẻ / order đồ uống tại quầy cho thu ngân.
  - **DOANH THU TỔNG HỢP NGÀY:** Doanh thu được chốt theo ca/ngày; doanh thu NET từ ứng dụng giao hàng (ShopeeFood, GrabFood, BeFood) tách biệt hoàn toàn với tiền mặt thực thu và tiền ngân hàng.
  - **HẠCH TOÁN CHI NHÁNH ĐỘC LẬP:** Mỗi chi nhánh có quỹ tiền mặt, tài khoản ngân hàng, kho nguyên vật liệu và bảng lương riêng biệt; phân quyền nghiêm ngặt theo chi nhánh (Branch Scope).
  - **P&L TẠM TÍNH:** Báo cáo P&L Phase 0 luôn bắt buộc hiển thị nhãn **`TẠM TÍNH / PROVISIONAL`**. Không thực hiện chia lợi nhuận thực tế cho nhà đầu tư trước cổng G5.

### 1.1 Bộ Quy tắc Chống Thiết kế Giả tạo (Anti-Slop Guidelines theo `design-taste-frontend`)
1. **Hình ảnh thực tế dẫn dắt (Photographs Lead):**
   - Loại bỏ hoàn toàn hình minh họa 3D generic, tranh vẽ vector AI hoặc ảnh stock vô danh.
   - Toàn bộ visual đại diện thương hiệu phải sử dụng ảnh thực tế của **Lục Trà Tinh Hoa** (quầy pha chế, lá trà tươi, các dòng thức uống đặc trưng, không gian quán).
2. **Cấm tiêu đề 2 vế văn vẻ / gượng ép (Banned Two-fragment Headings):**
   - Tuyệt đối KHÔNG dùng các câu khẩu hiệu kiểu AI marketing như: *"Chính xác từng con số. Minh bạch từng cốc trà"* hay *"Hương vị tinh hoa. Quản trị vững vàng"*.
   - Tiêu đề phải thuần túy chức năng và nghề nghiệp: *"Lục Trà Tinh Hoa — Hệ thống Quản trị Vận hành & Kế toán Quản trị Đa chi nhánh"*.
3. **Cấm dải 3 card tính năng generic (Banned AI 3-Card Feature Strip):**
   - Không xếp 3 ô tính năng vô bổ với icon xanh lá đóng hộp `p-2 rounded-lg bg-emerald-500/10 text-emerald-400` trên trang đăng nhập.
   - Thay bằng ảnh thực tế độ phân giải cao, logo thương hiệu chuẩn mực và các chỉ số vận hành thực tế.
4. **Cấm hiệu ứng nền lòe loẹt:**
   - Không dùng khối cầu phát sáng `bg-emerald-500/20 blur-3xl` hoặc pattern chấm lưới `radial-gradient [background-size:24px_24px]`.
   - Bề mặt phẳng, màu sắc trung tính cao cấp (Warm Stone / Dark Slate), viền siêu mảnh 1px (`border-border`).

---

## 2. Bảng Màu & Tokens (Color Palette)

### 2.1 Màu nhận diện thương hiệu Lục Trà Tinh Hoa (Brand Accent)
- **Primary (Lục Trà - Fresh Tea Green & Deep Tea Foliage):**
  - Sắc xanh lá trà tự nhiên của thương hiệu Lục Trà Tinh Hoa, kết hợp sắc xanh rêu sẫm mang tính bảo mật và tài chính.
  - `primary-dark`: `#064e3b` (Emerald 900 — Nền tối, hover sâu)
  - `primary`: `#059669` (Emerald 600 — Nút chính, chỉ báo active)
  - `primary-light`: `#10b981` (Emerald 500 — Accent, highlight trạng thái)
  - `primary-subtle`: `rgba(5, 150, 105, 0.08)` (Nền chip, badge active, hover tab)
- **Secondary (Caramel Ô Long & Trà Ủ Đậm Vị):**
  - Màu hổ phách caramel `#92400e` (Amber 800) dùng làm điểm nhấn ấm áp, phân định khu vực dữ liệu ca trà và kho nguyên vật liệu.

### 2.2 Màu nền & Trung tính (Warm Stone & Neutral Hierarchy)
- **Background (Canvas):** `#faf8f5` (Light Stone ấm tự nhiên) / `#090d16` (Dark Charcoal cao cấp)
- **Card / Surface:** `#ffffff` (Light Card) / `#0f172a` (Dark Slate Surface)
- **Border:** `#e7e5e4` (Stone 200 — 1px siêu mảnh) / `#334155` (Dark Slate 700)
- **Foreground Text:** `#1c1917` (Stone 900 — Chữ đậm nét, độ tương phản chuẩn WCAG AA) / `#f8fafc` (Dark Text)
- **Muted Text:** `#78716c` (Stone 500 — Chú thích, nhãn phụ) / `#94a3b8` (Dark Muted)

### 2.3 Màu ngữ nghĩa Tài chính & Trạng thái (Financial Semantics)
- **Tạm tính / Provisional (Bắt buộc theo chuẩn MTMS):** Màu hổ phách ấm (`#f59e0b` / Amber 500) kết hợp badge cảnh báo rõ ràng.
- **Dương tiền / Doanh thu đạt / Hoàn thành:** Emerald (`#10b981`).
- **Âm tiền / Chi phí / Lỗi / Hủy:** Rose / Red (`#f43f5e`).
- **Đang xử lý / Chờ phê duyệt:** Sky Blue (`#0ea5e9`).
- **Khóa sổ kỳ / Đã lưu trữ:** Slate Slate (`#64748b`).

---

## 3. Typography & Chuẩn mực Hiển thị Số liệu Tài chính

- **Font chữ giao diện chính:** `Inter`, `Plus Jakarta Sans`, system-ui.
- **Quy tắc hiển thị Số tiền & Số lượng (BẮT BUỘC):**
  - Mọi con số tài chính (Doanh thu ngày, Doanh thu NET app, Chi phí lương, Tồn kho WAC, Số dư ngân hàng) **BẮT BUỘC** sử dụng font monospace căn đều ký tự (`font-mono tabular-nums`).
  - **Căn phải (Right-aligned):** Mọi cột số liệu trong bảng phải căn lề phải để người xem dễ so sánh quy mô số học.
  - **Định dạng tiền tệ chuẩn Việt Nam:** Phân cách hàng nghìn bằng dấu chấm `.`, kèm hậu tố `₫` (ví dụ: `18.450.000 ₫`). Không viết số thô `18450000`.

---

## 4. Mật độ Dữ liệu & Bố cục Giao diện (Layout & Density)

- **Mật độ: High-Density (Compact Data-First):**
  - Tối ưu cho công việc hàng ngày của Kế toán chuỗi, Quản lý chi nhánh và Chủ cơ sở Lục Trà Tinh Hoa.
  - Chiều cao dòng trong Table: `36px - 40px` (không lãng phí không gian).
  - Padding nội bộ thẻ card: `p-4 sm:p-5`.
- **Hỗ trợ Responsive 4 Breakpoints:**
  1. **Desktop lớn (≥ 1440px):** Layout toàn màn hình, Sidebar cố định `256px`, hiển thị đầy đủ bảng phân tích số liệu nhiều cột.
  2. **Laptop tiêu chuẩn (1024px - 1439px):** Sidebar cố định gọn gàng, nội dung căn giữa với max-width `1280px`.
  3. **Tablet (768px - 1023px):** Header thu gọn, Sidebar chuyển sang slide-over drawer khi bấm nút Menu.
  4. **Mobile (≤ 390px — Màn hình điện thoại nhân viên quán):**
     - Menu dạng Drawer trượt mượt mà.
     - Các thao tác nhạy cảm (nhập báo cáo ca, chụp phiếu chi) thiết kế nút bấm lớn tối thiểu `44px` để bấm một tay dễ dàng.

---

## 5. Quy chuẩn Bố cục Trang Đăng nhập (Full-bleed Photographic Canvas)

Trang Đăng nhập là "mặt tiền" của hệ thống MTMS, thể hiện sự chỉn chu, chuyên nghiệp và nhận diện thực tế của chuỗi **Lục Trà Tinh Hoa** (được người dùng chốt phương án Canvas toàn cảnh):
- **Lớp nền Full-bleed (Toàn màn hình):**
  - Ảnh thực tế quầy pha chế & vận hành thực tế của Lục Trà Tinh Hoa (`store-counter-thumb.webp` chi nhánh Nguyễn Văn Quá).
  - Lớp phủ gradient chuyển sắc tối (`from-stone-950 via-stone-950/70 to-stone-950/60 backdrop-blur-[2px]`) bảo đảm độ tương phản văn bản đạt chuẩn WCAG AA.
- **Thanh tiêu đề thương hiệu (Top Bar):**
  - Huy hiệu tròn Logo chính thức của Lục Trà Tinh Hoa (`brand-logo-thumb.webp`).
  - Tên thương hiệu: **Lục Trà Tinh Hoa**, nhãn phiên bản **MTMS v1.1**.
  - Thanh chỉ báo mạng lưới 4 chi nhánh: *768 Nguyễn Văn Quá (Q12) · Nguyễn Gia Trí · Cao Thắng · Lê Văn Lương*.
- **Thẻ Đăng nhập nổi trung tâm (Floating Authentication Card):**
  - Thiết kế nổi trung tâm dạng kính mờ cao cấp (`bg-stone-900/90 border border-stone-800/90 shadow-2xl backdrop-blur-xl rounded-2xl p-8 max-w-md`).
  - Badge chức năng: `KẾ TOÁN QUẢN TRỊ NỘI BỘ`.
  - Input tài khoản & mật khẩu viền sắc cạnh (`border-stone-700/80 bg-stone-950/80 text-white`), focus ring xanh Lục Trà (`ring-emerald-500/50`).
  - Phím ẩn/hiện mật khẩu, phím `Enter` kích hoạt đăng nhập tức thì.
  - Thông báo lỗi trích xuất trực tiếp từ RFC 7807 Problem Detail (lỗi 401 tài khoản/mật khẩu sai, 403 khóa tài khoản, 400 validation).
  - Nút bấm `Đăng nhập` với vi hiệu ứng phản hồi nhấp chuột của `motion/react` (`whileHover={{ scale: 1.01 }}`, `whileTap={{ scale: 0.985 }}`).
- **Thanh chân trang (Footer Bar):**
  - Bản quyền & Ranh giới bất biến: *Hạch toán độc lập theo chi nhánh · Doanh thu tổng hợp ngày · NO POS*.

---

## 6. Hiệu ứng Chuyển động (Motion & Micro-interactions theo `motion`)

- **Triết lý "Quiet & Snappy":** Hiệu ứng nhanh, dứt khoát, phục vụ công việc, không gây xao nhãng.
  - Thời gian chuyển động (Duration): `150ms - 250ms`.
  - Easing: Spring với `damping: 25`, `stiffness: 250` hoặc `easeOut`.
- **Các tương tác vi mô chuẩn:**
  - **Button Hover & Tap:** `whileHover={{ scale: 1.01 }}` và `whileTap={{ scale: 0.98 }}`.
  - **Form Validation Alert:** Trượt nhẹ từ trên xuống (`initial={{ opacity: 0, y: -8 }}` `animate={{ opacity: 1, y: 0 }}`).
  - **Drawer Navigation (Mobile):** Trượt từ trái qua phải kèm lớp phủ mờ trong 200ms.
- **Quy tắc chống giật (Anti-Jank):**
  - Tuyệt đối không animate các thuộc tính gây layout reflow (`width`, `height`, `margin`, `top`, `left`).
  - Chỉ animate `transform (x, y, scale)` và `opacity` để GPU tăng tốc 60fps mượt mà.

---

## 7. Hiệu năng & Kiến trúc Mã nguồn (theo `vercel-react-best-practices`)

- **Triệt tiêu Waterfall Requests:** Tải song song danh sách chi nhánh và thông tin tài khoản qua `Promise.all` hoặc React Query song song.
- **Code-Splitting qua React.lazy:** Toàn bộ các route tính năng (`/revenue`, `/workforce`, `/finance`, `/inventory`, `/closing`, `/reporting`) được lazy-load để giữ bundle trang đăng nhập ban đầu dưới 180kB.
- **Atomic State với Zustand:** Dùng selector chuyên biệt `useAuthStore(s => s.user)` thay vì subscribe cả store, tránh re-render thừa.
- **Quản lý Token an toàn:** Lưu trữ Access Token trong memory/store, Refresh Token trong cookie httpOnly hoặc cơ chế xoay vòng bảo mật; bắt 401 tự động refresh trong suốt.

---

## 8. Trải nghiệm & Tiếp cận (theo `web-design-guidelines`)

- **Hỗ trợ bàn phím đầy đủ:** `Tab` chuyển trường tuần tự, `Enter` submit, `Escape` đóng modal.
- **Tương phản màu:** Đạt chuẩn tối thiểu WCAG AA (tỷ lệ tương phản ≥ 4.5:1 đối với văn bản thông thường, ≥ 3:1 đối với chữ lớn).
- **Phản hồi trạng thái nút:** Luôn hiển thị biểu tượng đang tải (spinner xoay) và khóa nút (`disabled`) khi đang gửi yêu cầu mạng để ngăn chặn nhấn đúp (double submit).

---

## 9. Danh mục Tài nguyên Hình ảnh Thực tế (Asset Registry)

Toàn bộ hình ảnh thực tế của Lục Trà Tinh Hoa đã được lưu trữ sẵn trong thư mục `frontend/public/assets/luc-tra-tinh-hoa/`:

| Tên file | Định dạng / Kích thước | Mục đích sử dụng |
|---|---|---|
| `brand-logo-thumb.webp` | WebP · 26 KB (280x280) | Logo nhận diện thương hiệu Lục Trà Tinh Hoa (Header, Login, Sidebar) |
| `store-counter-thumb.webp` | WebP · 199 KB | Ảnh thực tế quầy pha chế & vận hành cửa hàng (Hero Cột Trái trang Đăng nhập) |
| `tea-craft-thumb.webp` | WebP · 169 KB | Cận cảnh quy trình ủ trà và lá trà nguyên bản (Hero phụ / Section Giới thiệu) |
| `product-luc-tra-thumb.webp` | WebP · 325 KB | Ảnh chụp ly Lục Trà Tinh Hoa đặc trưng kèm lá trà tươi và đá tuyết |
| `product-thai-tea-thumb.webp` | WebP · 305 KB | Ảnh chụp món Cacao sữa Thái / Trà sữa Thái best-seller |
| `product-oolong-thumb.webp` | WebP · 394 KB | Ảnh chụp sản phẩm trà Ô long thanh nhiệt |
| `hero-banner.webp` | WebP · 885 KB | Banner ngang không gian cửa hàng (dùng cho báo cáo hoặc trang chủ) |
| `brand-logo.png` | PNG · 3.6 MB (2500x2500) | File gốc chất lượng cao phục vụ xuất bản in ấn hoặc favicon |
