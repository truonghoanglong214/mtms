# MTMS — Đề xuất thay đổi phạm vi nhân sự v1.1

**Trạng thái:** PROPOSED  
**Ngày lập:** 2026-11-12  
**Nguồn quyết định:** Chỉ đạo trực tiếp của Business Owner trong phiên làm việc ngày 2026-11-12  
**Gate:** G0 NOT SIGNED — tài liệu này là delta để Owner duyệt, không phải quyền triển khai  
**Baseline được đối chiếu:** `MTMS_Business_Spec_Roadmap_v1_0.docx`, `MTMS_Implementation_Handbook_v1_0.md`, `MTMS_ERD_Architecture_Mermaid_v1_0.md`, `MTMS_PostgreSQL_Schema_Starter_v1_0.sql`, `MTMS_Development_Tracker_v1_1_PHASE0.xlsx`

## 1. Quyết định phạm vi nghiệp vụ được đề xuất

MTMS không còn là hệ thống chấm công hoặc tính/trả lương. Một phần mềm bên ngoài chịu trách nhiệm cho các nghiệp vụ đó.

MTMS chỉ giữ các chức năng nhân sự sau:

1. Danh sách nhân viên và quan hệ nhân viên–chi nhánh ở mức cần thiết cho xếp lịch và ghi nhận chi phí.
2. Xếp lịch làm việc theo tuần/tháng, phân ca và hiển thị lịch cho nhân viên.
3. Có thể giữ yêu cầu đổi ca/nghỉ nếu Owner xác nhận đây là một phần của quy trình xếp lịch; dữ liệu này không được dùng để tính công hoặc tính lương.
4. Nhập thủ công chi phí nhân công của từng nhân viên cho từng chi nhánh và tháng, lấy kết quả từ phần mềm bên ngoài.
5. Cộng các khoản chi phí nhân công đã xác nhận vào báo cáo P&L quản trị của chi nhánh trong tháng tương ứng.

MTMS loại khỏi phạm vi:

- GPS check-in/check-out, geofence và lưu lịch sử vị trí.
- Chấm công, timesheet, duyệt giờ/ngày công và sửa công.
- Chính sách lương giờ/lương tháng, mức lương hiệu lực theo ngày và ngày công chuẩn.
- Tự động tính lương, làm tròn, tăng ca, ca đêm, nghỉ có/không lương, thưởng, phụ cấp và khấu trừ.
- Duyệt bảng lương, phiếu lương, công nợ lương, ghi nhận trả lương tiền mặt/chuyển khoản và đối soát thanh toán lương.
- Tự động xử lý thuế, bảo hiểm hoặc tuân thủ lao động.

Không có tích hợp tự động với phần mềm chấm công/tính lương bên ngoài trong phạm vi thay đổi này. Một tích hợp tương lai phải là yêu cầu và task được Owner duyệt riêng.

## 2. Chuẩn hóa cách hiểu “trừ vào tổng thu”

Doanh thu tháng phải được giữ nguyên, không ghi giảm bởi chi phí nhân viên. Chi phí nhân công nhập thủ công là một dòng chi phí riêng trong P&L quản trị:

```text
Doanh thu quản trị
- Giá vốn nguyên liệu tiêu hao
- Chi phí nhân công nhập thủ công
- Chi phí vận hành thuộc kỳ
- Thuế/nghĩa vụ ghi nhận cho kỳ
- Điều chỉnh được duyệt
= Kết quả quản trị TẠM TÍNH / PROVISIONAL
```

Việc nhập chi phí nhân công:

- Không tạo hoặc thay đổi doanh thu.
- Không tự động tạo giao dịch tiền mặt/ngân hàng.
- Không tạo công nợ lương hoặc trạng thái đã trả/chưa trả.
- Không tính số tiền từ lịch làm việc, số giờ, số ngày, mức lương hay chính sách lương.
- Chỉ cung cấp số chi phí thuộc tháng để tổng kết P&L quản trị.

## 3. Yêu cầu nghiệp vụ thay thế đề xuất

### BR-HR-11 — Nhập chi phí nhân công tháng

**Trạng thái:** PROPOSED

Người dùng có quyền theo phạm vi chi nhánh nhập chi phí nhân công do phần mềm bên ngoài xác định cho từng nhân viên, chi nhánh và tháng nghiệp vụ. MTMS không tính lại số tiền này.

Dữ liệu tối thiểu đề xuất:

- `business_month`: ngày đầu tháng nghiệp vụ theo múi giờ kinh doanh.
- `branch_id`: chi nhánh chịu chi phí.
- `employee_id`: nhân viên liên quan.
- `labor_cost_amount`: số tiền chi phí nhân công, số thập phân chính xác và không âm.
- `external_work_quantity` và `external_work_unit`: tùy chọn, chỉ để tham chiếu số công/giờ/ca từ hệ thống ngoài; không tham gia phép tính.
- `source_reference`: mã kỳ hoặc tham chiếu từ phần mềm ngoài.
- `note`: giải thích khi cần.
- Người nhập, thời điểm nhập, người xác nhận, thời điểm xác nhận và revision.

Một nhân viên làm ở nhiều chi nhánh có thể có một dòng chi phí riêng cho từng chi nhánh trong cùng tháng. MTMS không tự phân bổ theo chi nhánh chính cuối tháng.

### BR-HR-12 — Xác nhận và sửa chi phí nhân công

**Trạng thái:** PROPOSED

- Dữ liệu ở trạng thái nháp không được đưa vào P&L hoặc dùng làm điều kiện chốt tháng.
- Chỉ dòng đã xác nhận mới được cộng vào chi phí nhân công của tháng.
- Mọi thay đổi phải có lịch sử actor/time/reason và không được ghi đè âm thầm.
- Tháng đã khóa chỉ được sửa qua quy trình mở lại hoặc correction được duyệt theo chính sách kỳ kế toán hiện hành.
- Việc xác nhận là xác nhận số chi phí nhập từ nguồn ngoài, không phải duyệt bảng lương.

### BR-HR-07 — Lịch làm việc

**Trạng thái thay đổi:** PROPOSED

Giữ lịch tuần/tháng, mẫu ca, phân ca, hiển thị lịch và cảnh báo trùng lịch. Nếu Owner giữ đổi ca/nghỉ, các yêu cầu này chỉ cập nhật lịch sau khi được phê duyệt. Lịch không tạo attendance, payable hours hoặc chi phí nhân công.

### BR-FIN-03 — Chi phí nhân công thuộc kỳ

**Trạng thái thay đổi:** PROPOSED

Thay nội dung “payroll accrual” bằng: chi phí nhân công được nhập từ nguồn ngoài cho tháng nghiệp vụ nào thì thuộc P&L quản trị của tháng đó. MTMS không quản lý ngày trả lương, công nợ lương hoặc luồng tiền trả lương.

### BR-CLS-01 — Điều kiện sẵn sàng chốt tháng

**Trạng thái thay đổi:** PROPOSED

Thay điều kiện “payroll approved” bằng điều kiện “chi phí nhân công tháng đã được xác nhận hoặc đã xác nhận rõ là không phát sinh”. Quy tắc có bắt buộc một dòng cho từng nhân viên hoạt động trong tháng hay không còn là GAP-02 ở mục 8.

## 4. Yêu cầu cũ được đề xuất thay thế hoặc loại khỏi phạm vi

| ID | Trạng thái | Thay đổi đề xuất |
|---|---|---|
| `BR-HR-01` | PROPOSED | Loại chính sách mức lương cá nhân/chi nhánh và effective date khỏi MTMS. |
| `BR-HR-02` | PROPOSED | Loại GPS và approved payable hours; không còn chấm công trong MTMS. |
| `BR-HR-03` | PROPOSED | Bỏ tự động gán toàn bộ payroll theo chi nhánh chính cuối tháng; chi phí nhập rõ theo nhân viên–chi nhánh–tháng. |
| `BR-HR-04` | PROPOSED | Loại bonus/deduction/allowance phục vụ tính lương; số tiền bên ngoài được nhập là chi phí cuối cùng. |
| `BR-HR-05` | PROPOSED | Thay duyệt payroll bằng xác nhận dữ liệu chi phí nhân công nhập thủ công. |
| `BR-HR-06` | PROPOSED | Loại payday, payment channel, payable/paid và đối soát trả lương khỏi MTMS. |
| `BR-HR-07` | PROPOSED | Giữ xếp lịch; bỏ mọi liên kết từ lịch sang chấm công hoặc tính lương. |
| `BR-HR-08` | PROPOSED | Loại GPS exception và attendance correction khỏi MTMS. |
| `BR-HR-09` | PROPOSED | Loại meal/rounding/night-pay calculation; lịch không được tự tuyên bố tuân thủ pháp luật lao động. |
| `BR-HR-10` | PROPOSED | Loại tính prorate và effective-rate calculation khỏi MTMS. |
| `BR-FIN-03` | PROPOSED | Thay payroll accrual bằng chi phí nhân công tháng nhập từ nguồn ngoài. |
| `BR-CLS-01` | PROPOSED | Thay payroll approval prerequisite bằng xác nhận dữ liệu chi phí nhân công. |
| `BR-CLS-04` | PROPOSED | Impact report theo dõi thay đổi chi phí nhân công nhập thủ công, không theo dõi payroll. |

## 5. Tác động lên task và test

### Task giữ lại hoặc sửa

| Task | Trạng thái | Đề xuất |
|---|---|---|
| `P0-HR-01` | PROPOSED | Giữ employee directory/branch assignment, thu hẹp mục đích cho xếp lịch và nhập chi phí. |
| `P1-HR-01` | PROPOSED | Giữ work schedule model; lịch không sinh công hoặc chi phí. |
| `P1-HR-02` | PROPOSED | Giữ nếu Owner xác nhận đổi ca/nghỉ thuộc phạm vi xếp lịch. |
| `P0-CLS-01` | PROPOSED | Đổi prerequisite từ payroll approved sang monthly labor cost confirmed/explicit zero. |
| `P0-CLS-02` | PROPOSED | P&L dùng tổng chi phí nhân công đã xác nhận; không dùng payroll accrual. |
| `P0-CLS-04` | PROPOSED | Dashboard hiển thị trạng thái thiếu/xác nhận chi phí nhân công. |

### Task đề xuất mới

| Task | Parent/BR | Phạm vi | Test đề xuất |
|---|---|---|---|
| `P0-HR-10` | `BR-HR-11`, `BR-HR-12` | Model/API nhập và xác nhận chi phí nhân công theo nhân viên–chi nhánh–tháng; audit, decimal và row-scope authorization. | `TC-057`, `TC-058`, `TC-060` |
| `P0-HR-11` | `BR-HR-11`, `BR-HR-12` | Giao diện nhập danh sách, tổng theo chi nhánh, nguồn ngoài, xác nhận và correction history. | `TC-057`, `TC-059`, `TC-061` |

### Task đề xuất loại khỏi backlog triển khai

`P0-HR-02`, `P0-HR-03`, `P0-HR-04`, `P0-HR-05`, `P0-HR-06`, `P0-HR-07`, `P0-HR-08`, `P0-HR-09`, `P1-HR-03`, `P1-HR-04`, `P1-HR-05`, `P1-HR-06`, `P1-HR-07`.

Các task này chưa được xóa hoặc đổi trạng thái trong Tracker khi G0 chưa ký. Khi Owner duyệt delta, Tracker phải ghi rõ `SCOPE REMOVED/SUPERSEDED` theo quy ước được Owner chấp nhận thay vì đánh dấu `Done`.

### Test đề xuất

| TC | Pha | Kịch bản | Kết quả mong đợi |
|---|---|---|---|
| `TC-057` | P0 | Nhập 3 chi phí nhân viên đã xác nhận cho cùng chi nhánh/tháng | P&L cộng đúng một lần tổng 3 dòng bằng decimal chính xác. |
| `TC-058` | P0 | Xác nhận chi phí nhân công tháng | Doanh thu, cash và bank không thay đổi; chỉ dòng chi phí P&L thay đổi. |
| `TC-059` | P0 | Sửa chi phí của tháng OPEN rồi gửi lại cùng idempotency key | Không ghi trùng; revision cũ và actor/reason được giữ. |
| `TC-060` | P0 | Người dùng chi nhánh A đọc/ghi chi phí nhân công chi nhánh B | Bị từ chối mà không rò dữ liệu. |
| `TC-061` | P0 | Chốt tháng khi chi phí nhân công còn nháp hoặc chưa xác nhận | Xử lý theo quyết định GAP-02; nếu chọn blocking thì trả danh sách thiếu. |
| `TC-062` | P1 | Thay đổi lịch/đổi ca/nghỉ | Lịch cập nhật đúng nhưng không sinh attendance, payable hours hoặc labor cost. |

Đề xuất thay thế hoặc ngừng áp dụng các test payroll/attendance cũ: `TC-013`, `TC-014`, `TC-016`, `TC-017`, `TC-018`, `TC-048`, `TC-056`. `TC-015`, `TC-019`, `TC-040` cần viết lại theo phạm vi mới; `TC-041` vẫn áp dụng cho khóa kỳ/correction nói chung.

## 6. Tác động lên kiến trúc, ERD, SQL và API

### ERD/SQL đề xuất giữ

- `employees`
- `employee_assignments`
- `work_shifts`
- `shift_assignments`
- `leave_requests` chỉ khi Owner giữ quy trình nghỉ/đổi ca; bỏ thuộc tính `is_paid` vì MTMS không tính lương.

### ERD/SQL đề xuất loại

- `salary_policies`
- `attendance_events`
- `timesheet_entries`
- `payroll_periods`
- `payroll_lines`
- `payroll_adjustments`
- `payroll_payments`
- Các index, constraint, prerequisite và comment chỉ phục vụ payroll/attendance.

### Thực thể thay thế đề xuất

`monthly_employee_labor_costs`:

```text
id
branch_id
employee_id
business_month
labor_cost_amount
external_work_quantity (nullable, reference only)
external_work_unit (nullable, reference only)
source_reference (nullable)
note (nullable)
status: DRAFT | CONFIRMED | SUPERSEDED | VOIDED
revision
is_current
entered_by / created_at
confirmed_by / confirmed_at
correction_reason (nullable)
```

Ràng buộc đề xuất: tháng phải là ngày 1; số tiền không âm; revision dương; tối đa một revision current cho mỗi nhân viên–chi nhánh–tháng; xác nhận/correction phải có authorization theo entity/branch/action/row và audit.

### API đề xuất

- Giữ/tạo API employee và schedule theo phạm vi được duyệt.
- Thay `/api/v1/branches/{branchId}/payroll` bằng `/api/v1/branches/{branchId}/monthly-labor-costs`.
- Các thao tác dự kiến: list, create/update draft, confirm, create correction/revision.
- Write request dùng idempotency key/version token; server luôn đối chiếu `branchId` với membership và row ownership.
- Không cung cấp API attendance, GPS, timesheet, salary policy, payroll calculation hoặc payroll payment.

SQL starter v1.0 vẫn là thiết kế PROPOSED và tuyệt đối không được chạy như migration. Delta này cũng không cho phép tạo Flyway migration trước G0 và task plan approval.

## 7. Tác động theo từng tệp baseline

| Tệp | Vùng cần sửa trong bản v1.1 sau khi duyệt |
|---|---|
| `MTMS_Business_Spec_Roadmap_v1_0.docx` | Mục nhân sự/chấm công/lương; công thức P&L; P0/P1 scope; BR-HR-01…10; BR-FIN-03; BR-CLS-01/04; ADR và gate G3/G4. |
| `MTMS_Implementation_Handbook_v1_0.md` | Purpose/success criteria; invariants; mục Payroll; task HR/CLS; test matrix; ADR; risk register; release checklist. |
| `MTMS_ERD_Architecture_Mermaid_v1_0.md` | Context/module diagram; Workforce ERD; API payroll; ADR-029; G3/G4. |
| `MTMS_PostgreSQL_Schema_Starter_v1_0.sql` | Khối WORKFORCE/TIMESHEETS/PAYROLL; index; month-close comments; thay bằng schedule + monthly labor cost design. |
| `MTMS_Development_Tracker_v1_1_PHASE0.xlsx` | START HERE, Tasks, Requirements, Test Matrix, Decisions, Risk Register, Release Gates, Modules, Dependency Review, BR/TC Mapping và FE Backlog. |

Không ghi đè các tệp v1.0. Sau khi Owner chấp nhận các GAP, nên phát hành bộ tài liệu v1.1 mới và lưu v1.0 làm lịch sử.

## 8. GAP cần Owner xác nhận trước khi phát hành baseline v1.1

| GAP | Trạng thái | Câu hỏi quyết định | Khuyến nghị |
|---|---|---|---|
| `GAP-01` | GAP | “Nhập công” chỉ nhập số tiền chi phí, hay nhập cả số công/giờ/ca từ hệ thống ngoài? | Bắt buộc nhập `labor_cost_amount`; cho phép quantity/unit tùy chọn chỉ để đối chiếu, không tính toán. |
| `GAP-02` | GAP | Có chặn chốt tháng khi còn thiếu chi phí nhân công không? | Có; mỗi nhân viên hoạt động trong tháng phải có dòng confirmed hoặc explicit zero. |
| `GAP-03` | GAP | Nhân viên làm nhiều chi nhánh được nhập một tổng hay tách theo chi nhánh? | Tách một dòng theo nhân viên–chi nhánh–tháng để P&L từng quán đúng. |
| `GAP-04` | GAP | Có giữ workflow xin nghỉ/đổi ca không? | Giữ vì phục vụ xếp lịch, nhưng bỏ `is_paid` và mọi tác động đến công/lương. |
| `GAP-05` | GAP | Xếp lịch tiếp tục ở P1 hay đưa vào P0? | Đưa lịch cơ bản vào P0 nếu đây là chức năng nhân sự chính còn lại; workflow đổi ca/nghỉ có thể ở P1. |
| `GAP-06` | GAP | Vai trò nào được nhập và vai trò nào được xác nhận chi phí? | Manager/Finance nhập theo chi nhánh; Owner hoặc quyền tài chính được ủy quyền xác nhận. |

## 9. Bảng truy vết bằng chứng

| BR | Task | TC | Nguồn | Vấn đề | Người/phê duyệt tiếp theo |
|---|---|---|---|---|---|
| `BR-HR-01…06`, `BR-HR-08…10` | Các task payroll/attendance nêu ở mục 5 | `TC-013/014/016/017/018/048/056` | Business Spec v1.0, Handbook v1.0, Tracker v1.1 | Mâu thuẫn với chỉ đạo bỏ chấm công và tính/trả lương. | Owner duyệt supersede trong baseline v1.1. |
| `BR-HR-07` | `P1-HR-01`, `P1-HR-02` | `TC-019`, `TC-062` | Business Spec và Tracker | Xếp lịch được giữ nhưng phải tách hoàn toàn khỏi công/lương. | Owner quyết định GAP-04/05. |
| `BR-HR-11`, `BR-HR-12` | `P0-HR-10`, `P0-HR-11` | `TC-057…061` | Chỉ đạo Owner ngày 2026-11-12 | Chưa tồn tại trong baseline/tracker hiện tại. | Owner duyệt nội dung và GAP-01/02/03/06. |
| `BR-FIN-03` | `P0-CLS-02` | `TC-057/058` | Business Spec và Tracker | Payroll accrual phải đổi thành monthly external labor cost. | Owner + Finance xác nhận cách ghi nhận kỳ. |
| `BR-CLS-01` | `P0-CLS-01`, `P0-CLS-04` | `TC-040`, `TC-061` | Business Spec và Tracker | Điều kiện payroll approved không còn phù hợp. | Owner quyết định GAP-02. |
| ADR-014…017/024/029 | ADR v1.1 chưa cấp số | Các TC mới | Handbook/Tracker | Các ADR cũ mô tả salary, payable hours, payment, attendance và labour compliance. | Tech Lead soạn ADR thay thế sau khi Owner duyệt GAP. |

## 10. Điều kiện để áp dụng

1. Owner xác nhận các GAP ở mục 8 và chấp nhận delta này làm nội dung baseline v1.1.
2. Phát hành bản v1.1 riêng; không ghi đè bản v1.0.
3. Cập nhật đồng bộ Business Spec, Handbook, ERD, SQL starter và Tracker; không chỉ sửa một nguồn.
4. Rà lại tổng số task/test, dependency, FE backlog và gate G0 sau khi thay đổi.
5. G0 phải được ký rõ ràng.
6. Mỗi task implementation vẫn cần một scoped plan được Owner duyệt riêng trước khi code hoặc migration.

