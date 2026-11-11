# Bắt đầu làm việc trong MTMS

1. Mở `README.md` và `AGENTS.md` ngay tại root `mtms/`.
2. Chạy `py -3 scripts\validate_workspace.py` (chỉ kiểm tra tĩnh, không thay đổi dữ liệu).
3. Đọc `docs/planning/CURRENT_STATE.md` và `docs/planning/G0_SIGNOFF_CHECKLIST.md`.
4. Mở `agy` tại root; xác nhận `/agents` có 6 Agent, `/skills` có 8 Skill; sử dụng `/skills reload` nếu cần.
5. Gửi nội dung `docs/ai/prompts/01_READ_ONLY_SMOKE_TEST.md` để xác nhận AI không tự code trước G0.
6. Chuẩn bị G0 với `docs/ai/prompts/02_G0_REVIEW.md`.
7. Sau khi G0 được phê duyệt và có duyệt task riêng, dùng `docs/ai/prompts/03_P0_ORG_01_PLAN.md` rồi `04_P0_ORG_01_IMPLEMENT.md`.

**Mọi lần bổ sung task về sau, xem `docs/ai/HOW_TO_ADD_FILES.md`.** Không tạo một repo khác theo giai đoạn.
